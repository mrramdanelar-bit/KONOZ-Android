package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.AIResult
import com.example.data.ai.AIService
import com.example.data.local.KnowledgeBaseSeed
import com.example.data.local.KnowledgeRuleEntity
import com.example.data.local.KonozDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.media.ImageStorageManager
import com.example.data.repository.AnalysisExecutionResult
import com.example.data.repository.KonozRepository
import com.example.domain.engine.SymbolComparisonEngine
import com.example.domain.model.AnalysisReport
import com.example.domain.model.DetectedSymbol
import com.example.domain.model.FollowUpMessage
import com.example.domain.model.KnowledgeBackupCodec
import com.example.domain.model.KnowledgeImportResult
import com.example.domain.model.RuleCategory
import com.example.domain.model.VisualContext
import com.example.domain.model.WarningSeverity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KonozComprehensiveTest {

    private val seedRules: List<KnowledgeRuleEntity> = KnowledgeBaseSeed.defaultRules()

    // 1. صورة تحتوي على حرف واحد
    @Test
    fun test01_singleLetterImageAudit() {
        val aiReport = AnalysisReport(
            caseSummary = "تم رصد حرف L منفرد على صخرة.",
            visualContext = VisualContext(surfaceType = "صخرة", imageClarity = "واضحة"),
            detectedSymbols = listOf(DetectedSymbol(symbol = "L", symbolType = "حرف لاتيني", recognitionClarity = "عالية"))
        )
        val audited = SymbolComparisonEngine.auditAndEnrichAiReport(aiReport, "", seedRules, isImageMode = true)

        assertEquals(1, audited.detectedSymbols.size)
        assertTrue(audited.dictionaryMatches.any { it.symbol == "L" && it.meaningInGuide.contains("لانقار") && it.isFoundInGuide })
        assertFalse(audited.confidence.visualRecognitionLevel.contains("%"))
    }

    // 2. صورة تحتوي على عدة حروف
    @Test
    fun test02_multipleLettersImageAudit() {
        val aiReport = AnalysisReport(
            caseSummary = "عدة حروف على جدار حجري.",
            detectedSymbols = listOf(
                DetectedSymbol(symbol = "F", symbolType = "حرف لاتيني"),
                DetectedSymbol(symbol = "H", symbolType = "حرف لاتيني"),
                DetectedSymbol(symbol = "Y", symbolType = "حرف لاتيني")
            )
        )
        val audited = SymbolComparisonEngine.auditAndEnrichAiReport(aiReport, "", seedRules, isImageMode = true)

        assertEquals(3, audited.detectedSymbols.size)
        assertTrue(audited.dictionaryMatches.any { it.symbol == "F" && it.meaningInGuide.contains("فيرمة") })
        assertTrue(audited.dictionaryMatches.any { it.symbol == "H" && it.meaningInGuide.contains("حوش") })
        assertTrue(audited.dictionaryMatches.any { it.symbol == "Y" && it.meaningInGuide.contains("ملتقى") })
        assertTrue(audited.symbolRelationships.isNotEmpty())
    }

    // 3. صورة تحتوي على رقم وحرف
    @Test
    fun test03_numberAndLetterCombined() {
        val text = "وجدت حرف L على صخرة، بجانبه حرف I، وتحته رقم 4، وهناك نقطة كبيرة قرب الحروف."
        val extracted = SymbolComparisonEngine.extractSymbolsFromText(text, seedRules)
        val report = SymbolComparisonEngine.buildLocalReport(text, extracted, seedRules, isImageMode = true)

        val keys = report.detectedSymbols.map { it.symbol }.toSet()
        assertTrue(keys.contains("L"))
        assertTrue(keys.contains("I"))
        assertTrue(keys.contains("4"))
        assertTrue(keys.contains("النقطة"))
        assertTrue(report.dictionaryMatches.any { it.symbol == "4" && it.meaningInGuide.contains("غرفة") })
        assertTrue(report.dictionaryMatches.any { it.symbol == "النقطة" && it.meaningInGuide.contains("صخرة كبيرة") })
    }

    // 4. صورة تحتوي على رموز مقلوبة
    @Test
    fun test04_invertedSymbolsHandling() {
        val aiReport = AnalysisReport(
            caseSummary = "حرف A مقلوب على صخرة.",
            detectedSymbols = listOf(
                DetectedSymbol(symbol = "A", symbolType = "حرف لاتيني", orientation = "مقلوب", isInverted = true)
            )
        )
        val audited = SymbolComparisonEngine.auditAndEnrichAiReport(aiReport, "حرف A مقلوب", seedRules, isImageMode = true)

        assertTrue(audited.detectedSymbols.first().isInverted)
        assertTrue(audited.dictionaryMatches.any { it.symbol == "A" && it.meaningInGuide.contains("مرتفع") })
        assertTrue(audited.symbolRelationships.any { it.contains("مقلوب") || it.contains("التضاريس") })
    }

    // 5. صورة تحتوي على شكل هندسي (علامة خطر)
    @Test
    fun test05_geometricHazardShapeTriggersMandatoryWarning() {
        val aiReport = AnalysisReport(
            caseSummary = "مثلث محفور على صخرة.",
            detectedSymbols = listOf(
                DetectedSymbol(symbol = "المثلث", symbolType = "شكل هندسي")
            )
        )
        val audited = SymbolComparisonEngine.auditAndEnrichAiReport(aiReport, "مثلث على صخرة", seedRules, isImageMode = true)

        assertTrue(
            audited.warnings.any {
                it.severity == WarningSeverity.CRITICAL_HAZARD &&
                    it.message == SymbolComparisonEngine.MANDATORY_HAZARD_MESSAGE
            }
        )
    }

    // 6. وصف نصي يحتوي على L و I و B
    @Test
    fun test06_textDescriptionLIB() {
        val desc = "شاهدت تركيب L + I + B على صخرة."
        val extracted = SymbolComparisonEngine.extractSymbolsFromText(desc, seedRules)
        val report = SymbolComparisonEngine.buildLocalReport(desc, extracted, seedRules)

        assertTrue(report.dictionaryMatches.any { it.symbol == "L" && it.meaningInGuide.contains("لانقار") })
        assertTrue(report.dictionaryMatches.any { it.symbol == "I" && it.meaningInGuide.contains("الشمال") })
        assertTrue(report.dictionaryMatches.any { it.symbol == "B" && it.meaningInGuide.contains("باسان") })
        assertTrue(report.symbolRelationships.any { it.contains("L، I، B") })
    }

    // 7. وصف نصي يحتوي على E و 5 و T
    @Test
    fun test07_textDescriptionE5T() {
        val desc = "يوجد نقش يحتوي على E + 5 + T."
        val extracted = SymbolComparisonEngine.extractSymbolsFromText(desc, seedRules)
        val report = SymbolComparisonEngine.buildLocalReport(desc, extracted, seedRules)

        assertTrue(report.dictionaryMatches.any { it.symbol == "E" && it.meaningInGuide.contains("الشرق") })
        assertTrue(report.dictionaryMatches.any { it.symbol == "5" && it.meaningInGuide.contains("غرفة تحت الأرض") })
        assertTrue(report.dictionaryMatches.any { it.symbol == "T" && it.meaningInGuide.contains("قبر") })
        assertTrue(report.warnings.any { it.severity == WarningSeverity.HERITAGE_ADVISORY })
    }

    // 8. الرمز S في سياقين مختلفين
    @Test
    fun test08_symbolSInTwoContextsAndConflictDetection() {
        val pathDesc = "حرف S بجانب طريق ووادٍ وحرف Y"
        val pathReport = SymbolComparisonEngine.buildLocalReport(
            pathDesc,
            SymbolComparisonEngine.extractSymbolsFromText(pathDesc, seedRules),
            seedRules
        )
        val pathConflict = pathReport.conflicts.firstOrNull { it.symbol == "S" }
        assertTrue(pathConflict != null && pathConflict.meanings.size >= 2)
        assertTrue(pathConflict!!.resolutionNote.contains("طريق"))

        val moneyDesc = "حرف S مع كلمة LOVE ولون أزرق"
        val moneyReport = SymbolComparisonEngine.buildLocalReport(
            moneyDesc,
            SymbolComparisonEngine.extractSymbolsFromText(moneyDesc, seedRules),
            seedRules
        )
        val moneyConflict = moneyReport.conflicts.firstOrNull { it.symbol == "S" }
        assertTrue(moneyConflict != null && moneyConflict.resolutionNote.contains("أموال ورقية"))

        // Ambiguous S without cues must explicitly state it cannot prefer one meaning
        val ambiguousDesc = "حرف S منفرد"
        val ambiguousReport = SymbolComparisonEngine.buildLocalReport(
            ambiguousDesc,
            SymbolComparisonEngine.extractSymbolsFromText(ambiguousDesc, seedRules),
            seedRules
        )
        val ambiguousConflict = ambiguousReport.conflicts.firstOrNull { it.symbol == "S" }
        assertTrue(ambiguousConflict != null && ambiguousConflict.resolutionNote.contains("لا يمكن ترجيح تفسير واحد من المعطيات الحالية"))
    }

    // 9. رمز غير موجود في القاموس (لا يختلق معنى)
    @Test
    fun test09_unknownSymbolNotFabricated() {
        val desc = "وجدت حرف Q محفوراً على حجر."
        val extracted = SymbolComparisonEngine.extractSymbolsFromText(desc, seedRules)
        val report = SymbolComparisonEngine.buildLocalReport(desc, extracted, seedRules)

        val qMatch = report.dictionaryMatches.firstOrNull { it.symbol == "Q" }
        assertTrue(qMatch != null)
        assertFalse(qMatch!!.isFoundInGuide)
        assertTrue(qMatch.meaningInGuide.contains("غير موجود"))
    }

    // 10. صورة غير واضحة (لا يخمن الحروف أو الأرقام)
    @Test
    fun test10_unclearImageRefusesToGuess() {
        val aiReport = AnalysisReport(
            caseSummary = "صورة ضبابية جداً.",
            isImageUnclear = true,
            clarityNote = "الإضاءة سيئة والصورة مهزوزة.",
            detectedSymbols = listOf(DetectedSymbol(symbol = "A", symbolType = "تخمين")) // Must be stripped!
        )
        val audited = SymbolComparisonEngine.auditAndEnrichAiReport(aiReport, "", seedRules, isImageMode = true)

        assertTrue(audited.isImageUnclear)
        assertTrue("Unclear image must NOT fabricate or retain guessed symbols", audited.detectedSymbols.isEmpty())
        assertTrue(audited.dictionaryMatches.isEmpty())
    }

    // 11. فشل الاتصال بالذكاء الاصطناعي
    @Test
    fun test11_aiConnectionFailureReturnsFriendlyArabicError() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, KonozDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val failingAI = object : AIService {
            override val providerLabel: String = "FailingTestProvider"
            override suspend fun analyzeImages(
                bitmaps: List<Bitmap>,
                optionalDescription: String,
                activeRules: List<KnowledgeRuleEntity>,
                timeoutMs: Long
            ) = AIResult.Failure("تعذر إكمال التحليل بسبب مشكلة في الاتصال. بياناتك محفوظة ويمكنك إعادة المحاولة.")

            override suspend fun analyzeTextDescription(
                description: String,
                activeRules: List<KnowledgeRuleEntity>,
                timeoutMs: Long
            ) = AIResult.Failure("تعذر إكمال التحليل بسبب مشكلة في الاتصال. بياناتك محفوظة ويمكنك إعادة المحاولة.")

            override suspend fun continueFollowUp(
                originalDescription: String,
                currentReport: AnalysisReport,
                history: List<FollowUpMessage>,
                newUserInfo: String,
                activeRules: List<KnowledgeRuleEntity>,
                timeoutMs: Long
            ) = AIResult.Failure("تعذر إكمال التحليل بسبب مشكلة في الاتصال. بياناتك محفوظة ويمكنك إعادة المحاولة.")
        }

        val repo = KonozRepository(
            context = context,
            dao = db.konozDao(),
            settingsStore = SettingsDataStore(context),
            imageStorageManager = ImageStorageManager(context),
            overrideAIService = failingAI,
            networkChecker = { true }
        )

        val result = repo.runTextAnalysis("حرف L على صخرة", useLocalOfflineOnly = false)
        assertTrue(result is AnalysisExecutionResult.Error)
        val err = (result as AnalysisExecutionResult.Error).arabicMessage
        assertTrue(err.contains("بياناتك محفوظة"))
        db.close()
    }

    // 12. استعادة نسخة JSON دون حذف القواعد القديمة + معالجة ملف تالف
    @Test
    fun test12_jsonBackupExportAndNonDestructiveRestore() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, KonozDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val repo = KonozRepository(
            context = context,
            dao = db.konozDao(),
            settingsStore = SettingsDataStore(context),
            imageStorageManager = ImageStorageManager(context)
        )

        repo.ensureKnowledgeBaseSeeded()
        val initialCount = db.konozDao().countRules()
        assertTrue(initialCount > 60)

        // Export valid JSON containing a new custom rule
        val customRule = KnowledgeRuleEntity(
            symbolKey = "Q_CUSTOM",
            displayName = "رمز تجريبي Q",
            category = RuleCategory.EXTRA_SYMBOLS.name,
            meaning = "معنى مضاف من النسخة الاحتياطية",
            ruleSource = "نسخة احتياطية"
        )
        val exportedJson = KnowledgeBackupCodec.exportToJson(listOf(customRule))
        val importRes = repo.importKnowledgeBaseJson(exportedJson)

        assertTrue(importRes.isSuccess)
        assertEquals(initialCount + 1, db.konozDao().countRules())
        assertTrue(db.konozDao().findRuleBySymbolKey("Q_CUSTOM") != null)
        // Old rules must NOT be deleted
        assertTrue(db.konozDao().findRuleBySymbolKey("L") != null)

        // Malformed JSON must fail gracefully with Arabic message
        val badRes = KnowledgeBackupCodec.importFromJson("{not-valid-json")
        assertTrue(badRes is KnowledgeImportResult.Error)
        db.close()
    }

    // Additional verification: Compound numbers 41, 1.4, 6+9, and 4-digit numbers are preserved without splitting
    @Test
    fun testCompoundNumbersPreservedWithoutSplitting() {
        val desc = "يظهر الرقم 41 وبجانبه 1.4 وكذلك 6 + 9 ورقم 1948 للتثبيت."
        val extracted = SymbolComparisonEngine.extractSymbolsFromText(desc, seedRules)
        val keys = extracted.map { it.symbol }.toSet()
        assertTrue(keys.contains("41"))
        assertTrue(keys.contains("1.4"))
        assertTrue(keys.contains("6 + 9"))
        assertTrue(keys.contains("1948"))
        assertFalse("41 should not be split into standalone 4", keys.contains("4"))
        assertFalse("41 should not be split into standalone 1", keys.contains("1"))
    }
}
