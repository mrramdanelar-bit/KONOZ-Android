package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.ai.AIResult
import com.example.data.ai.AIService
import com.example.data.ai.CustomProxyAIService
import com.example.data.ai.FirebaseGeminiAIService
import com.example.data.local.AnalysisRecordEntity
import com.example.data.local.KnowledgeBaseSeed
import com.example.data.local.KnowledgeRuleEntity
import com.example.data.local.KonozDao
import com.example.data.local.SettingsDataStore
import com.example.data.media.ImageStorageManager
import com.example.domain.engine.SymbolComparisonEngine
import com.example.domain.model.AIProviderType
import com.example.domain.model.AnalysisReport
import com.example.domain.model.AppSettings
import com.example.domain.model.FollowUpMessage
import com.example.domain.model.KnowledgeBackupCodec
import com.example.domain.model.KnowledgeImportResult
import com.example.domain.model.ReportJsonCodec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

sealed class AnalysisExecutionResult {
    data class Saved(val recordId: Long, val report: AnalysisReport) : AnalysisExecutionResult()
    data class Error(val arabicMessage: String) : AnalysisExecutionResult()
}

data class ImportSummary(
    val addedCount: Int,
    val updatedCount: Int,
    val message: String
)

class KonozRepository(
    private val context: Context,
    private val dao: KonozDao,
    private val settingsStore: SettingsDataStore,
    val imageStorageManager: ImageStorageManager,
    private val overrideAIService: AIService? = null,
    private val networkChecker: (() -> Boolean)? = null
) {

    val allRules: Flow<List<KnowledgeRuleEntity>> = dao.observeAllRules()
    val allAnalyses: Flow<List<AnalysisRecordEntity>> = dao.observeAllAnalyses()
    val favoriteAnalyses: Flow<List<AnalysisRecordEntity>> = dao.observeFavoriteAnalyses()
    val settingsFlow: Flow<AppSettings> = settingsStore.settingsFlow

    fun observeAnalysis(id: Long): Flow<AnalysisRecordEntity?> = dao.observeAnalysisById(id)

    suspend fun ensureKnowledgeBaseSeeded() {
        if (dao.countRules() == 0) {
            dao.insertRules(KnowledgeBaseSeed.defaultRules())
        }
    }

    private fun isNetworkAvailable(): Boolean {
        networkChecker?.let { return it() }
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }

    private fun resolveAIService(settings: AppSettings): AIService {
        overrideAIService?.let { return it }
        return when (settings.providerType) {
            AIProviderType.FIREBASE_GEMINI -> FirebaseGeminiAIService(
                modelNameProvider = { settings.geminiModel }
            )
            AIProviderType.CUSTOM_PROXY -> CustomProxyAIService(
                endpointUrlProvider = { settings.customProxyUrl },
                authTokenProvider = { settings.customProxyToken }
            )
        }
    }

    // ==================== Image Analysis ====================

    suspend fun runImageAnalysis(
        imagePaths: List<String>,
        optionalDescription: String,
        useLocalOfflineOnly: Boolean = false
    ): AnalysisExecutionResult {
        ensureKnowledgeBaseSeeded()
        if (imagePaths.isEmpty()) {
            return AnalysisExecutionResult.Error("يرجى التقاط صورة أو اختيار صورة من الهاتف أولاً.")
        }

        val activeRules = dao.getEnabledRulesOnce()
        val settings = settingsStore.settingsFlow.first()

        if (useLocalOfflineOnly) {
            if (optionalDescription.isBlank()) {
                return AnalysisExecutionResult.Error(
                    "المطابقة المحلية دون إنترنت تعتمد على الوصف النصي للعلامات. أضف وصفاً للرموز أو اتصل بالإنترنت لتفعيل التحليل البصري الذكي."
                )
            }
            val extracted = SymbolComparisonEngine.extractSymbolsFromText(optionalDescription, activeRules)
            val report = SymbolComparisonEngine.buildLocalReport(
                inputDescription = optionalDescription,
                detectedSymbols = extracted,
                activeRules = activeRules,
                isImageMode = true
            )
            return persistReport(
                mode = "IMAGE",
                inputDescription = optionalDescription,
                imagePaths = imagePaths,
                report = report,
                providerUsed = "محرك المطابقة المحلي (دون إنترنت)"
            )
        }

        if (!isNetworkAvailable()) {
            return AnalysisExecutionResult.Error(
                "التحليل الذكي للصور يتطلب اتصالاً بالإنترنت. صورك ووصفك محفوظة؛ يمكنك الاتصال بالإنترنت وإعادة المحاولة أو استخدام المطابقة المحلية للوصف."
            )
        }

        val bitmaps = imagePaths.mapNotNull { imageStorageManager.decodeScaledBitmapForAI(it) }
        if (bitmaps.isEmpty()) {
            return AnalysisExecutionResult.Error("الصورة المختارة فارغة أو تالفة. يرجى اختيار صورة أخرى صالحة.")
        }

        val aiService = resolveAIService(settings)
        val timeoutMs = settings.timeoutSeconds.coerceIn(30, 60) * 1000L

        return try {
            when (val result = aiService.analyzeImages(bitmaps, optionalDescription, activeRules, timeoutMs)) {
                is AIResult.Failure -> AnalysisExecutionResult.Error(result.userArabicMessage)
                is AIResult.Success -> {
                    val audited = SymbolComparisonEngine.auditAndEnrichAiReport(
                        aiReport = result.data,
                        inputDescription = optionalDescription,
                        activeRules = activeRules,
                        isImageMode = true
                    )
                    persistReport(
                        mode = "IMAGE",
                        inputDescription = optionalDescription.ifBlank { "تحليل صورة (${imagePaths.size} صورة)" },
                        imagePaths = imagePaths,
                        report = audited,
                        providerUsed = result.providerLabel
                    )
                }
            }
        } finally {
            bitmaps.forEach { if (!it.isRecycled) it.recycle() }
        }
    }

    // ==================== Text Description Analysis ====================

    suspend fun runTextAnalysis(
        description: String,
        useLocalOfflineOnly: Boolean = false
    ): AnalysisExecutionResult {
        ensureKnowledgeBaseSeeded()
        val trimmed = description.trim()
        if (trimmed.isBlank()) {
            return AnalysisExecutionResult.Error("يرجى كتابة وصف العلامات والرموز أولاً.")
        }

        val activeRules = dao.getEnabledRulesOnce()
        val settings = settingsStore.settingsFlow.first()

        if (useLocalOfflineOnly || !isNetworkAvailable()) {
            val extracted = SymbolComparisonEngine.extractSymbolsFromText(trimmed, activeRules)
            if (extracted.isEmpty()) {
                return AnalysisExecutionResult.Error("لم يتم رصد رموز معروفة في الوصف المحلي. أضف تفاصيل أوضح أو اتصل بالإنترنت للتحليل الذكي.")
            }
            val report = SymbolComparisonEngine.buildLocalReport(
                inputDescription = trimmed,
                detectedSymbols = extracted,
                activeRules = activeRules,
                isImageMode = false
            )
            val label = if (useLocalOfflineOnly) {
                "محرك المطابقة المحلي (دون إنترنت)"
            } else {
                "محرك المطابقة المحلي (دون اتصال بالإنترنت)"
            }
            return persistReport(
                mode = "TEXT",
                inputDescription = trimmed,
                imagePaths = emptyList(),
                report = report,
                providerUsed = label
            )
        }

        val aiService = resolveAIService(settings)
        val timeoutMs = settings.timeoutSeconds.coerceIn(30, 60) * 1000L

        return when (val result = aiService.analyzeTextDescription(trimmed, activeRules, timeoutMs)) {
            is AIResult.Failure -> AnalysisExecutionResult.Error(result.userArabicMessage)
            is AIResult.Success -> {
                val audited = SymbolComparisonEngine.auditAndEnrichAiReport(
                    aiReport = result.data,
                    inputDescription = trimmed,
                    activeRules = activeRules,
                    isImageMode = false
                )
                persistReport(
                    mode = "TEXT",
                    inputDescription = trimmed,
                    imagePaths = emptyList(),
                    report = audited,
                    providerUsed = result.providerLabel
                )
            }
        }
    }

    // ==================== Follow-up Conversation Re-evaluation ====================

    suspend fun submitFollowUp(
        recordId: Long,
        newUserInfo: String
    ): Result<String> {
        val trimmed = newUserInfo.trim()
        if (trimmed.isBlank()) return Result.failure(IllegalArgumentException("أدخل المعلومة الإضافية أولاً."))

        val existing = dao.getAnalysisByIdOnce(recordId)
            ?: return Result.failure(IllegalStateException("تعذر العثور على سجل التحليل."))

        val activeRules = dao.getEnabledRulesOnce()
        val settings = settingsStore.settingsFlow.first()
        val currentReport = ReportJsonCodec.decodeReport(existing.reportJson)
        val history = ReportJsonCodec.decodeFollowUpMessages(existing.followUpMessagesJson).toMutableList()

        // Re-evaluate combined context locally so the updated report always reflects new info
        val combinedDescription = "${existing.inputDescription}\nمعلومة متابعة: $trimmed"
        val combinedExtracted = SymbolComparisonEngine.extractSymbolsFromText(combinedDescription, activeRules)
        val mergedSymbols = (currentReport.detectedSymbols + combinedExtracted).distinctBy { it.symbol.uppercase() }
        val localReEvaluation = SymbolComparisonEngine.buildLocalReport(
            inputDescription = combinedDescription,
            detectedSymbols = mergedSymbols,
            activeRules = activeRules,
            isImageMode = existing.mode == "IMAGE",
            visualContext = currentReport.visualContext
        )

        val assistantReply: String = if (isNetworkAvailable()) {
            val aiService = resolveAIService(settings)
            val timeoutMs = settings.timeoutSeconds.coerceIn(30, 60) * 1000L
            when (val res = aiService.continueFollowUp(
                originalDescription = existing.inputDescription,
                currentReport = currentReport,
                history = history,
                newUserInfo = trimmed,
                activeRules = activeRules,
                timeoutMs = timeoutMs
            )) {
                is AIResult.Success -> res.data
                is AIResult.Failure -> {
                    buildLocalFollowUpReply(trimmed, localReEvaluation, fallbackNote = res.userArabicMessage)
                }
            }
        } else {
            buildLocalFollowUpReply(trimmed, localReEvaluation, fallbackNote = "تمت إعادة التقييم عبر المحرك المحلي لعدم توفر اتصال بالإنترنت.")
        }

        history.add(FollowUpMessage(role = "user", content = trimmed))
        history.add(FollowUpMessage(role = "assistant", content = assistantReply))

        val updatedRecord = existing.copy(
            reportJson = ReportJsonCodec.encodeReport(localReEvaluation),
            followUpMessagesJson = ReportJsonCodec.encodeFollowUpMessages(history),
            hasHazardWarning = localReEvaluation.warnings.any { it.severity == com.example.domain.model.WarningSeverity.CRITICAL_HAZARD },
            updatedAt = System.currentTimeMillis()
        )
        dao.updateAnalysis(updatedRecord)
        return Result.success(assistantReply)
    }

    private fun buildLocalFollowUpReply(
        newInfo: String,
        reEvaluated: AnalysisReport,
        fallbackNote: String
    ): String = buildString {
        appendLine("إعادة التقييم بناءً على المعلومة الجديدة («$newInfo») مع الاحتفاظ بكامل المعطيات السابقة:")
        appendLine("• الملخص المحدّث: ${reEvaluated.caseSummary}")
        if (reEvaluated.conflicts.isNotEmpty()) {
            appendLine("• التعارضات المرصودة: ${reEvaluated.conflicts.joinToString(" | ") { "${it.symbol}: ${it.resolutionNote}" }}")
        }
        reEvaluated.warnings.firstOrNull { it.severity == com.example.domain.model.WarningSeverity.CRITICAL_HAZARD }?.let {
            appendLine("⚠️ تحذير سلامة: ${it.message}")
        }
        appendLine("ملاحظة: $fallbackNote")
    }

    private suspend fun persistReport(
        mode: String,
        inputDescription: String,
        imagePaths: List<String>,
        report: AnalysisReport,
        providerUsed: String
    ): AnalysisExecutionResult {
        return try {
            val symbolsHeadline = report.detectedSymbols
                .take(4)
                .joinToString(" • ") { it.symbol }
                .ifBlank { if (report.isImageUnclear) "صورة غير واضحة" else "تقرير حالة" }

            val titlePrefix = if (mode == "IMAGE") "تحليل صورة" else "تحليل وصف"
            val record = AnalysisRecordEntity(
                title = "$titlePrefix: $symbolsHeadline",
                mode = mode,
                inputDescription = inputDescription,
                imagePathsJson = ReportJsonCodec.encodeStringList(imagePaths),
                detectedSymbolsSummary = symbolsHeadline,
                reportJson = ReportJsonCodec.encodeReport(report),
                hasHazardWarning = report.warnings.any { it.severity == com.example.domain.model.WarningSeverity.CRITICAL_HAZARD },
                providerUsed = providerUsed
            )
            val id = dao.insertAnalysis(record)
            AnalysisExecutionResult.Saved(id, report)
        } catch (_: Exception) {
            AnalysisExecutionResult.Error("تعذر حفظ التقرير في قاعدة البيانات المحلية. يرجى المحاولة مرة أخرى.")
        }
    }

    // ==================== History & Notes ====================

    suspend fun toggleFavorite(id: Long, favorite: Boolean) = dao.setAnalysisFavorite(id, favorite)

    suspend fun updateNotes(id: Long, notes: String) = dao.updateAnalysisNotes(id, notes)

    suspend fun deleteAnalysis(id: Long) = dao.deleteAnalysisById(id)

    // ==================== Knowledge Base CRUD & JSON Backup ====================

    suspend fun saveRule(rule: KnowledgeRuleEntity) {
        val stamped = rule.copy(isUserModified = true, updatedAt = System.currentTimeMillis())
        if (rule.id == 0L) {
            dao.insertRule(stamped)
        } else {
            dao.updateRule(stamped)
        }
    }

    suspend fun setRuleEnabled(id: Long, enabled: Boolean) {
        dao.setRuleEnabled(id, enabled)
    }

    suspend fun exportKnowledgeBaseJson(): String {
        ensureKnowledgeBaseSeeded()
        val all = dao.getAllRulesOnce()
        return KnowledgeBackupCodec.exportToJson(all)
    }

    /**
     * Non-destructive JSON import (Section 21):
     * Never deletes existing rules; adds new ones and merges/updates matching symbolKeys.
     */
    suspend fun importKnowledgeBaseJson(jsonText: String): Result<ImportSummary> {
        ensureKnowledgeBaseSeeded()
        return when (val parsed = KnowledgeBackupCodec.importFromJson(jsonText)) {
            is KnowledgeImportResult.Error -> Result.failure(IllegalArgumentException(parsed.arabicMessage))
            is KnowledgeImportResult.Success -> {
                var added = 0
                var updated = 0
                for (incoming in parsed.rules) {
                    val existing = dao.findRuleBySymbolKey(incoming.symbolKey)
                    if (existing == null) {
                        dao.insertRule(incoming.copy(id = 0L))
                        added++
                    } else {
                        // Preserve old rule ID and merge alternative meanings without deleting old data
                        val oldAlts = ReportJsonCodec.decodeAlternativeMeanings(existing.alternativeMeaningsJson)
                        val newAlts = ReportJsonCodec.decodeAlternativeMeanings(incoming.alternativeMeaningsJson)
                        val mergedAlts = (oldAlts + newAlts).distinctBy { "${it.meaning}|${it.context}" }
                        dao.updateRule(
                            existing.copy(
                                displayName = incoming.displayName,
                                meaning = incoming.meaning,
                                alternativeMeaningsJson = ReportJsonCodec.encodeAlternativeMeanings(mergedAlts),
                                contextNotes = incoming.contextNotes.ifBlank { existing.contextNotes },
                                ruleSource = incoming.ruleSource.ifBlank { existing.ruleSource },
                                examples = incoming.examples.ifBlank { existing.examples },
                                isHazard = existing.isHazard || incoming.isHazard,
                                isHeritage = existing.isHeritage || incoming.isHeritage,
                                isEnabled = incoming.isEnabled,
                                isUserModified = true,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        updated++
                    }
                }
                Result.success(
                    ImportSummary(
                        addedCount = added,
                        updatedCount = updated,
                        message = "تم الاستيراد بنجاح دون حذف القواعد القديمة: إضافة $added قاعدة وتحديث $updated قاعدة."
                    )
                )
            }
        }
    }

    // ==================== Settings ====================

    suspend fun updateProviderType(type: AIProviderType) = settingsStore.updateProviderType(type)
    suspend fun updateGeminiModel(model: String) = settingsStore.updateGeminiModel(model)
    suspend fun updateCustomProxy(url: String, token: String) = settingsStore.updateCustomProxy(url, token)
    suspend fun updateTimeoutSeconds(seconds: Int) = settingsStore.updateTimeoutSeconds(seconds)
    suspend fun updateDarkTheme(dark: Boolean) = settingsStore.updateDarkTheme(dark)
    suspend fun updateLanguageCode(lang: String) = settingsStore.updateLanguageCode(lang)
}
