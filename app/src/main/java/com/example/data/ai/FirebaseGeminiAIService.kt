package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.local.KnowledgeRuleEntity
import com.example.domain.model.AlternativeMeaning
import com.example.domain.model.AnalysisReport
import com.example.domain.model.ConfidenceAssessment
import com.example.domain.model.ConflictItem
import com.example.domain.model.DetectedSymbol
import com.example.domain.model.DictionaryMatch
import com.example.domain.model.FollowUpMessage
import com.example.domain.model.InterpretationHypothesis
import com.example.domain.model.SafetyWarning
import com.example.domain.model.VisualContext
import com.example.domain.model.WarningSeverity
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.IOException

private const val TAG = "KonozGeminiAI"

class FirebaseGeminiAIService(
    private val modelNameProvider: () -> String = { "gemini-3.8-flash" }
) : AIService {

    override val providerLabel: String
        get() = "Firebase AI Logic (${modelNameProvider()})"

    private val reportSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "caseSummary" to Schema.string(description = "ملخص موجز بالعربية للحالة"),
                "isImageUnclear" to Schema.boolean(description = "true فقط إذا كانت الصورة ضبابية أو غير واضحة ولا يمكن قراءة رموزها"),
                "clarityNote" to Schema.string(description = "توضيح صريح إن كانت الصورة غير واضحة"),
                "visualContext" to Schema.obj(
                    properties = mapOf(
                        "surfaceType" to Schema.string(description = "نوع السطح: صخرة، جدار، شجرة، رسم على ورق، غير محدد"),
                        "naturalOrManMade" to Schema.string(description = "هل يبدو طبيعياً أم مرسوماً أم محفوراً دون افتراض أنه أثري"),
                        "imageClarity" to Schema.string(description = "وضوح الصورة: واضحة / متوسطة / غير واضحة"),
                        "isSingleComposition" to Schema.boolean(description = "هل تظهر العلامات ضمن تكوين واحد"),
                        "colorsObserved" to Schema.array(items = Schema.string(), description = "الألوان الظاهرة بوضوح إن وجدت")
                    )
                ),
                "detectedSymbols" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "symbol" to Schema.string(description = "الرمز الظاهر فعلياً مثل A أو L أو 41 أو النقطة أو المثلث"),
                            "symbolType" to Schema.string(description = "نوع الرمز: حرف، رقم، تركيب عددي، شكل هندسي، رمز ديني، رسم، إلخ"),
                            "positionInImage" to Schema.string(description = "موضعه النسبي في الصورة أو الوصف"),
                            "relativeSize" to Schema.string(description = "الحجم النسبي: صغير، متوسط، كبير"),
                            "orientation" to Schema.string(description = "الاتجاه: عادي، مقلوب، مائل يميناً، مائل يساراً"),
                            "isInverted" to Schema.boolean(description = "هل الرمز مقلوب"),
                            "isFramed" to Schema.boolean(description = "هل الرمز داخل مربع أو إطار"),
                            "repetitionCount" to Schema.integer(description = "عدد مرات تكرار الرمز"),
                            "recognitionClarity" to Schema.string(description = "وضوح التعرف: عالية، متوسطة، منخفضة")
                        )
                    )
                ),
                "symbolRelationships" to Schema.array(
                    items = Schema.string(),
                    description = "العلاقات المكانية والمنطقية بين الرموز دون توجيهات حفر أو مسافات"
                ),
                "interpretations" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "title" to Schema.string(description = "عنوان الاحتمال التفسيري"),
                            "observation" to Schema.string(description = "الملاحظة الفعلية المرصودة"),
                            "deduction" to Schema.string(description = "الاستنتاج المنطقي من المطابقة"),
                            "hypothesis" to Schema.string(description = "الفرضية المقترحة بصيغة احتمالية"),
                            "rationale" to Schema.string(description = "سبب هذا الاحتمال وفق الدليل")
                        )
                    )
                ),
                "missingInformation" to Schema.array(
                    items = Schema.string(),
                    description = "الأسئلة والمعلومات الناقصة التي تساعد على فهم أوضح"
                ),
                "confidence" to Schema.obj(
                    properties = mapOf(
                        "visualRecognitionLevel" to Schema.string(description = "مستوى الثقة البصرية بالكلمات فقط بدون نسب مئوية"),
                        "dictionaryMatchLevel" to Schema.string(description = "مستوى المطابقة مع الدليل بالكلمات فقط بدون نسب مئوية"),
                        "overallInferenceLevel" to Schema.string(description = "مستوى الثقة في الاستنتاج العام بالكلمات فقط بدون نسب مئوية")
                    )
                ),
                "hazardDetected" to Schema.boolean(description = "true إذا ظهرت علامات خطر أو ألغام أو أسلاك أو مسامير مشبوهة أو مثلث أو # أو هيكل عظمي"),
                "heritageDetected" to Schema.boolean(description = "true إذا ظهرت قبور أو نقوش دينية أو معالم قد تكون ذات قيمة أثرية")
            )
        )
    }

    private fun buildSystemPrompt(): String = """
        أنت مساعد تحليل بصري ومنطقي احترافي داخل تطبيق (KONOZ – كنوز) باللغة العربية.
        مهمتك: فحص الصور أو الأوصاف النصية ومقارنة العلامات الفعلية فقط بقاعدة المعرفة المرفقة (الدليل اليدوي للمستخدم).

        القواعد الصارمة غير القابلة للتجاوز:
        1. لا تختلق أبداً أي رمز أو حرف أو رقم غير ظاهر بوضوح فعلي.
        2. إذا كانت الصورة غير واضحة أو ضبابية أو مظلمة، اجعل isImageUnclear = true ولا تخمّن أي حرف أو رقم.
        3. لا تنسب إلى الدليل أي معنى غير وارد في القواعد المرفقة. إذا لم يوجد الرمز في الدليل فاذكر صراحة أنه غير موجود في الدليل.
        4. ميّز بوضوح تام بين: الملاحظة الفعلية، والاستنتاج، والفرضية غير المثبتة.
        5. احتفظ بالتراكيب العددية مثل (41، 1.4، 6 + 9، أو الأرقام من 4 خانات فأكثر) كما هي دون تفكيك تلقائي.
        6. لا تعرض نسباً مئوية (%) في مستوى الثقة أبداً؛ استخدم مستويات وصفية فقط (مرتفع / متوسط / منخفض / غير كافٍ).
        7. السلامة المطلقة: يُمنع منعاً باتاً تقديم إحداثيات، أو حساب مسافات أو اتجاهات ميدانية للوصول لدفائن، أو توجيهات للحفر أو فتح القبور، أو طرق لمس/تحريك الأسلاك أو المسامير أو تجاوز الألغام أو تعطيلها.
        8. إذا رُصدت أسلاك، مسامير، أجسام معدنية مجهولة، الفرشات القبايلية، مثلث، #، +، هيكل عظمي، أو نجمة خطر، اجعل hazardDetected = true وأوقف أي تحليل ميداني عملي.
        9. لا تفترض أن العلامات أثرية أو قديمة دون دليل بصري.
    """.trimIndent()

    private fun structuredModel() = Firebase.ai.generativeModel(
        modelName = modelNameProvider(),
        generationConfig = generationConfig {
            responseMimeType = "application/json"
            responseSchema = reportSchema
            temperature = 0.2f
        },
        systemInstruction = content { text(buildSystemPrompt()) }
    )

    private fun conversationalModel() = Firebase.ai.generativeModel(
        modelName = modelNameProvider(),
        generationConfig = generationConfig {
            temperature = 0.25f
        },
        systemInstruction = content { text(buildSystemPrompt()) }
    )

    override suspend fun analyzeImages(
        bitmaps: List<Bitmap>,
        optionalDescription: String,
        activeRules: List<KnowledgeRuleEntity>,
        timeoutMs: Long
    ): AIResult<AnalysisReport> {
        if (bitmaps.isEmpty()) {
            return AIResult.Failure("لا توجد صورة صالحة للتحليل. يرجى اختيار صورة أولاً.")
        }
        val dictionaryPrompt = buildCompactDictionaryDigest(activeRules)
        val userPrompt = buildString {
            appendLine("قم بتحليل الصورة/الصور المرفقة عبر المراحل الخمس: الفحص البصري، التعرف على السياق، مطابقة قاعدة المعرفة المرفقة، التحليل المنطقي، وإعداد التقرير بالعربية بصيغة JSON وفق المخطط.")
            if (optionalDescription.isNotBlank()) {
                appendLine("وصف إضافي من المستخدم: $optionalDescription")
            }
            appendLine()
            appendLine("قاعدة المعرفة المعتمدة (لا تخرج عنها ولا تختلق معاني إضافية):")
            appendLine(dictionaryPrompt)
        }

        return executeWithRetry(timeoutMs) {
            val response = structuredModel().generateContent(
                content {
                    bitmaps.forEach { bmp -> image(bmp) }
                    text(userPrompt)
                }
            )
            val rawJson = response.text ?: throw IllegalStateException("استجابة فارغة من النموذج.")
            parseStructuredReport(rawJson)
        }
    }

    override suspend fun analyzeTextDescription(
        description: String,
        activeRules: List<KnowledgeRuleEntity>,
        timeoutMs: Long
    ): AIResult<AnalysisReport> {
        if (description.isBlank()) {
            return AIResult.Failure("يرجى كتابة وصف العلامات قبل بدء التحليل.")
        }
        val dictionaryPrompt = buildCompactDictionaryDigest(activeRules)
        val userPrompt = buildString {
            appendLine("حلّل الوصف النصي التالي للعلامات التي شاهدها المستخدم، واستخرج الرموز المذكورة فقط دون إضافة رموز غير مذكورة، ثم طابقها بقاعدة المعرفة وقدم الاحتمالات والتعارضات بالعربية بصيغة JSON:")
            appendLine("وصف المستخدم: $description")
            appendLine()
            appendLine("قاعدة المعرفة المعتمدة:")
            appendLine(dictionaryPrompt)
        }

        return executeWithRetry(timeoutMs) {
            val response = structuredModel().generateContent(userPrompt)
            val rawJson = response.text ?: throw IllegalStateException("استجابة فارغة من النموذج.")
            parseStructuredReport(rawJson)
        }
    }

    override suspend fun continueFollowUp(
        originalDescription: String,
        currentReport: AnalysisReport,
        history: List<FollowUpMessage>,
        newUserInfo: String,
        activeRules: List<KnowledgeRuleEntity>,
        timeoutMs: Long
    ): AIResult<String> {
        val dictionaryPrompt = buildCompactDictionaryDigest(activeRules)
        val contextHeader = buildString {
            appendLine("سياق الحالة السابقة (لا تتجاهل أي معلومة سابقة عند إعادة التقييم):")
            appendLine("- المدخل الأصلي: $originalDescription")
            appendLine("- ملخص التقرير السابق: ${currentReport.caseSummary}")
            appendLine("- الرموز المرصودة سابقاً: ${currentReport.detectedSymbols.joinToString("، ") { "${it.symbol} (${it.orientation})" }}")
            appendLine("- القاموس المرجعي المختصر:")
            appendLine(dictionaryPrompt)
        }

        return executeWithRetry(timeoutMs) {
            val chatHistory = mutableListOf(
                content("user") { text(contextHeader) },
                content("model") { text("تم استيعاب جميع معطيات الحالة السابقة والرموز المرصودة وقاعدة المعرفة. ما هي المعلومات الإضافية الجديدة لإعادة التقييم؟") }
            )
            history.forEach { msg ->
                val role = if (msg.role == "user") "user" else "model"
                chatHistory.add(content(role) { text(msg.content) })
            }

            val chat = conversationalModel().startChat(history = chatHistory)
            val followUpPrompt = buildString {
                appendLine("معلومة إضافية جديدة من المستخدم: $newUserInfo")
                appendLine("أعد تقييم التفسير باللغة العربية مع الأخذ بعين الاعتبار جميع المعلومات السابقة والجديدة معاً، وافصل بين الملاحظة والاستنتاج والفرضية، واذكر أي تعارض أو تحذير سلامة، دون تقديم أي توجيهات حفر أو مسافات أو نسب مئوية.")
            }
            val response = chat.sendMessage(followUpPrompt)
            response.text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("أعاد النموذج إجابة فارغة.")
        }
    }

    private suspend fun <T> executeWithRetry(
        timeoutMs: Long,
        maxRetries: Int = 2,
        block: suspend () -> T
    ): AIResult<T> {
        var lastError: Exception? = null
        for (attempt in 0..maxRetries) {
            try {
                val result = withTimeout(timeoutMs) { block() }
                return AIResult.Success(result, providerLabel)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI request timed out on attempt ${attempt + 1}", e)
                lastError = e
                if (attempt == maxRetries) {
                    return AIResult.Failure(
                        "انتهت مهلة انتظار التحليل. بياناتك محفوظة ويمكنك إعادة المحاولة أو زيادة المهلة من الإعدادات.",
                        e.localizedMessage
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI request failed on attempt ${attempt + 1}", e)
                lastError = e
                val msg = e.localizedMessage.orEmpty().lowercase()
                val isTransient = e is IOException ||
                    msg.contains("503") ||
                    msg.contains("unavailable") ||
                    msg.contains("temporarily") ||
                    msg.contains("timeout")
                if (isTransient && attempt < maxRetries) {
                    delay(1200L * (attempt + 1))
                } else {
                    return AIResult.Failure(mapExceptionToArabic(e), e.localizedMessage)
                }
            }
        }
        return AIResult.Failure(mapExceptionToArabic(lastError), lastError?.localizedMessage)
    }

    private fun mapExceptionToArabic(e: Exception?): String {
        val raw = e?.localizedMessage.orEmpty().lowercase()
        return when {
            e is TimeoutCancellationException ->
                "انتهت مهلة انتظار التحليل. بياناتك محفوظة ويمكنك إعادة المحاولة."
            raw.contains("403") || raw.contains("app check") || raw.contains("api key") || raw.contains("unauthorized") ->
                "تعذر التحقق من صلاحية الاتصال بخدمة الذكاء الاصطناعي (App Check / الإعدادات). بياناتك محفوظة ويمكنك استخدام المطابقة المحلية مؤقتاً."
            raw.contains("503") || raw.contains("unavailable") || raw.contains("overloaded") || raw.contains("quota") ->
                "نموذج الذكاء الاصطناعي غير متاح مؤقتاً. بياناتك محفوظة ويمكنك إعادة المحاولة بعد قليل."
            raw.contains("network") || raw.contains("unable to resolve host") || raw.contains("connect") || e is IOException ->
                "تعذر إكمال التحليل بسبب مشكلة في الاتصال بالإنترنت. بياناتك محفوظة ويمكنك إعادة المحاولة أو استخدام المطابقة المحلية."
            else ->
                "تعذر إكمال التحليل بسبب مشكلة في الاتصال أو الخدمة. بياناتك محفوظة ويمكنك إعادة المحاولة."
        }
    }

    private fun buildCompactDictionaryDigest(rules: List<KnowledgeRuleEntity>): String {
        return rules.filter { it.isEnabled }.joinToString("\n") { r ->
            val flags = buildString {
                if (r.isHazard) append(" [خطر محتمل]")
                if (r.isHeritage) append(" [أثري/ديني]")
            }
            "- ${r.symbolKey}: ${r.meaning}$flags"
        }
    }

    companion object {
        fun parseStructuredReport(rawJson: String): AnalysisReport {
            val root = JSONObject(rawJson)
            val isUnclear = root.optBoolean("isImageUnclear", false)
            val clarityNote = root.optString("clarityNote", "")

            val vcObj = root.optJSONObject("visualContext")
            val visualContext = if (vcObj != null) {
                val colorsArr = vcObj.optJSONArray("colorsObserved")
                val colors = if (colorsArr != null) {
                    (0 until colorsArr.length()).mapNotNull { colorsArr.optString(it).takeIf { s -> s.isNotBlank() } }
                } else emptyList()
                VisualContext(
                    surfaceType = vcObj.optString("surfaceType", "غير محدد"),
                    naturalOrManMade = vcObj.optString("naturalOrManMade", "يحتاج إلى فحص سياقي"),
                    imageClarity = vcObj.optString("imageClarity", if (isUnclear) "غير واضحة" else "واضحة"),
                    colorsObserved = colors,
                    isSingleComposition = vcObj.optBoolean("isSingleComposition", false)
                )
            } else VisualContext()

            val detectedList = mutableListOf<DetectedSymbol>()
            val detArr = root.optJSONArray("detectedSymbols")
            if (detArr != null) {
                for (i in 0 until detArr.length()) {
                    val o = detArr.optJSONObject(i) ?: continue
                    val sym = o.optString("symbol", "").trim()
                    if (sym.isBlank()) continue
                    detectedList.add(
                        DetectedSymbol(
                            symbol = sym,
                            symbolType = o.optString("symbolType", "رمز"),
                            positionInImage = o.optString("positionInImage", "غير محدد"),
                            relativeSize = o.optString("relativeSize", "متوسط"),
                            orientation = o.optString("orientation", "عادي"),
                            isInverted = o.optBoolean("isInverted", false),
                            isFramed = o.optBoolean("isFramed", false),
                            repetitionCount = o.optInt("repetitionCount", 1).coerceAtLeast(1),
                            recognitionClarity = o.optString("recognitionClarity", "متوسطة")
                        )
                    )
                }
            }

            val relList = mutableListOf<String>()
            val relArr = root.optJSONArray("symbolRelationships")
            if (relArr != null) {
                for (i in 0 until relArr.length()) {
                    relArr.optString(i).takeIf { it.isNotBlank() }?.let { relList.add(it) }
                }
            }

            val hypList = mutableListOf<InterpretationHypothesis>()
            val hypArr = root.optJSONArray("interpretations")
            if (hypArr != null) {
                for (i in 0 until hypArr.length()) {
                    val o = hypArr.optJSONObject(i) ?: continue
                    hypList.add(
                        InterpretationHypothesis(
                            title = o.optString("title", "احتمال تفسيري"),
                            observation = o.optString("observation", ""),
                            deduction = o.optString("deduction", ""),
                            hypothesis = o.optString("hypothesis", ""),
                            rationale = o.optString("rationale", "")
                        )
                    )
                }
            }

            val missingList = mutableListOf<String>()
            val misArr = root.optJSONArray("missingInformation")
            if (misArr != null) {
                for (i in 0 until misArr.length()) {
                    misArr.optString(i).takeIf { it.isNotBlank() }?.let { missingList.add(it) }
                }
            }

            val confObj = root.optJSONObject("confidence")
            val confidence = ConfidenceAssessment(
                visualRecognitionLevel = confObj?.optString("visualRecognitionLevel", "متوسط") ?: "متوسط",
                dictionaryMatchLevel = confObj?.optString("dictionaryMatchLevel", "متوسط") ?: "متوسط",
                overallInferenceLevel = confObj?.optString("overallInferenceLevel", "فرضية غير مثبتة") ?: "فرضية غير مثبتة"
            )

            val warnings = mutableListOf<SafetyWarning>()
            if (root.optBoolean("hazardDetected", false)) {
                warnings.add(
                    SafetyWarning(
                        severity = WarningSeverity.CRITICAL_HAZARD,
                        title = "تحذير سلامة فوري",
                        message = "قد تكون هذه العلامة مرتبطة بخطر ميداني. ابتعد عن الموقع ولا تلمس العلامة أو الأجسام المحيطة بها. تواصل مع الجهات المختصة."
                    )
                )
            }
            if (root.optBoolean("heritageDetected", false)) {
                warnings.add(
                    SafetyWarning(
                        severity = WarningSeverity.HERITAGE_ADVISORY,
                        title = "توصية حماية المواقع الأثرية",
                        message = "يوصى بتوثيق الموقع دون إتلافه وإبلاغ الجهات المختصة."
                    )
                )
            }

            return AnalysisReport(
                caseSummary = root.optString("caseSummary", ""),
                visualContext = visualContext,
                isImageUnclear = isUnclear,
                clarityNote = clarityNote,
                detectedSymbols = detectedList,
                dictionaryMatches = emptyList<DictionaryMatch>(),
                symbolRelationships = relList,
                interpretations = hypList,
                conflicts = emptyList<ConflictItem>(),
                missingInformation = missingList,
                confidence = confidence,
                warnings = warnings
            )
        }
    }
}
