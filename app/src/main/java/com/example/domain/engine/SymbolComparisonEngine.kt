package com.example.domain.engine

import com.example.data.local.KnowledgeRuleEntity
import com.example.domain.model.AlternativeMeaning
import com.example.domain.model.AnalysisReport
import com.example.domain.model.ConfidenceAssessment
import com.example.domain.model.ConflictItem
import com.example.domain.model.DetectedSymbol
import com.example.domain.model.DictionaryMatch
import com.example.domain.model.InterpretationHypothesis
import com.example.domain.model.ReportJsonCodec
import com.example.domain.model.SafetyWarning
import com.example.domain.model.VisualContext
import com.example.domain.model.WarningSeverity

object SymbolComparisonEngine {

    const val MANDATORY_HAZARD_MESSAGE =
        "قد تكون هذه العلامة مرتبطة بخطر ميداني. ابتعد عن الموقع ولا تلمس العلامة أو الأجسام المحيطة بها. تواصل مع الجهات المختصة."

    const val MANDATORY_HERITAGE_MESSAGE =
        "تظهر علامات قد ترتبط بقبور أو معالم دينية أو نقوش ذات قيمة أثرية محتملة؛ يوصى بتوثيقها فوتوغرافياً دون إتلافها أو الحفر قربها وإبلاغ الجهات المختصة."

    const val MANDATORY_METHODOLOGY_DISCLAIMER =
        "القواعد الواردة في الدليل فرضيات خاصة بالمستخدم لأغراض التوثيق والدراسة فقط، وليست مرجعاً أثرياً علمياً مثبتاً. لا يقدم التطبيق إحداثيات أو مسافات أو توجيهات للحفر."

    private val hazardKeywords = listOf(
        "لغم", "ألغام", "متفجر", "سلك", "أسلاك", "مسمار", "مسامير", "البروش", "بروش",
        "الفيراي", "الكبة السلكية", "الفرشات القبايلية", "فرشات قبايلية", "مثلث", "المثلث",
        "هيكل عظمي", "الهيكل العظمي", "نجمة", "النجمة", "شمس", "#", "+", "خطين متقاطعين",
        "معدني مجهول", "جسم معدني"
    )

    private val heritageKeywords = listOf(
        "قبر", "مقبرة", "الله", "محمد", "المحراب", "محراب", "مسجد", "زاوية", "مقام", "نقش قديم"
    )

    private val financialContextKeywords = listOf(
        "أموال", "مال", "ورقية", "أورو", "ذهب", "معدن", "القلب", "قلب", "love", "حب", "أزرق", "الأزرق", "أصفر"
    )

    private val pathContextKeywords = listOf(
        "طريق", "مسار", "واد", "وادي", "شعبة", "منعطف", "ملتقى", "صخرة", "مجرى"
    )

    /**
     * Extracts symbols deterministically from Arabic/Latin user description text
     * while preserving compound numeric tokens (41, 1.4, 6 + 9, 4+ digits) and phrases.
     */
    fun extractSymbolsFromText(
        rawText: String,
        activeRules: List<KnowledgeRuleEntity>
    ): List<DetectedSymbol> {
        val text = rawText.trim()
        if (text.isBlank()) return emptyList()

        val normalizedLower = text.lowercase()
        val isInvertedGlobal = listOf("مقلوب", "مقلوبة", "معكوس", "معكوسة").any { text.contains(it) }
        val isFramedGlobal = listOf("داخل مربع", "داخل إطار", "في مربع", "ضمن إطار", "محاط بمربع").any { text.contains(it) }

        val found = mutableListOf<DetectedSymbol>()
        val consumedSpans = mutableSetOf<String>()

        // 1. Compound known combinations first (preserve order and avoid splitting!)
        if (Regex("""\bL\s*\+\s*I\s*\+\s*B\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            found.add(
                DetectedSymbol(
                    symbol = "L + I + B",
                    symbolType = "تركيب حروف مركب",
                    orientation = if (isInvertedGlobal) "مقلوب" else "عادي",
                    isInverted = isInvertedGlobal,
                    isFramed = isFramedGlobal,
                    recognitionClarity = "واضحة (وصف نصي)"
                )
            )
            consumedSpans.add("L+I+B")
        }

        if (Regex("""\bE\s*\+\s*5\s*\+\s*T\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            found.add(
                DetectedSymbol(
                    symbol = "E + 5 + T",
                    symbolType = "تركيب مركب (حروف وأرقام)",
                    orientation = if (isInvertedGlobal) "مقلوب" else "عادي",
                    isInverted = isInvertedGlobal,
                    isFramed = isFramedGlobal,
                    recognitionClarity = "واضحة (وصف نصي)"
                )
            )
            consumedSpans.add("E+5+T")
        }

        // 2. Compound numeric tokens (41, 1.4, 6 + 9, 4+ digit numbers) BEFORE single digits
        val consumedNumbers = mutableListOf<String>()

        if (Regex("""\b6\s*\+\s*9\b""").containsMatchIn(text) ||
            (text.contains("6") && text.contains("9") && text.contains("+"))
        ) {
            found.add(
                DetectedSymbol(
                    symbol = "6 + 9",
                    symbolType = "تركيب عددي",
                    isFramed = isFramedGlobal,
                    recognitionClarity = "واضحة (وصف نصي)"
                )
            )
            consumedNumbers.addAll(listOf("6", "9"))
        }

        val dotComboRegex = Regex("""(?<!\d)1\.4(?!\d)""")
        if (dotComboRegex.containsMatchIn(text)) {
            found.add(
                DetectedSymbol(
                    symbol = "1.4",
                    symbolType = "تركيب عددي",
                    isFramed = isFramedGlobal,
                    recognitionClarity = "واضحة (وصف نصي)"
                )
            )
            consumedNumbers.add("1.4")
        }

        val fortyOneRegex = Regex("""(?<!\d)41(?!\d)""")
        if (fortyOneRegex.containsMatchIn(text)) {
            found.add(
                DetectedSymbol(
                    symbol = "41",
                    symbolType = "تركيب عددي",
                    isFramed = isFramedGlobal,
                    recognitionClarity = "واضحة (وصف نصي)"
                )
            )
            consumedNumbers.add("41")
        }

        // Numbers with 4 or more digits (علامة تثبيت)
        val fourPlusRegex = Regex("""(?<!\d)\d{4,}(?!\d)""")
        fourPlusRegex.findAll(text).forEach { match ->
            val num = match.value
            found.add(
                DetectedSymbol(
                    symbol = num,
                    symbolType = "رقم من 4 خانات فأكثر",
                    isFramed = isFramedGlobal,
                    recognitionClarity = "واضحة (وصف نصي)"
                )
            )
            consumedNumbers.add(num)
        }

        // 3. Multi-word Arabic / special phrases (longest first)
        val phraseCatalog = listOf(
            "القلب داخل شجرة" to "رمز خاص",
            "القلب المقلوب" to "رمز خاص",
            "قلب مقلوب" to "رمز خاص",
            "إشارة الصليب المعقوف" to "رمز خاص",
            "الصليب المعقوف" to "رمز خاص",
            "الفرشات القبايلية" to "علامة خطر محتملة",
            "فرشات قبايلية" to "علامة خطر محتملة",
            "الله مع محمد" to "رمز ديني",
            "الله أكبر" to "رمز ديني",
            "الله اكبر" to "رمز ديني",
            "ما شاء الله" to "عبارة / رسم",
            "المثلث الذي يحتوي على خط" to "شكل هندسي / خطر محتمل",
            "مثلث به خط" to "شكل هندسي / خطر محتمل",
            "المربع الذي يحتوي على خطين متقاطعين" to "شكل هندسي / خطر محتمل",
            "مربع بخطين متقاطعين" to "شكل هندسي / خطر محتمل",
            "النجمة التي تشبه الشمس" to "شكل هندسي / خطر محتمل",
            "النجمة على الصخور" to "شكل هندسي / خطر محتمل",
            "الهيكل العظمي" to "علامة خطر محتملة",
            "هيكل عظمي" to "علامة خطر محتملة",
            "المسمار الذي يدور حوله سلك" to "رصد / أسلاك",
            "السلك المربوط من طرف إلى طرف" to "رصد / أسلاك",
            "السلك الذي يدور ثلاث دورات" to "رصد / أسلاك",
            "السلك الذي يحتوي على عقد فردية" to "رصد / أسلاك",
            "المسمار السيس" to "رصد / مسامير",
            "مسمار البروش" to "رصد / مسامير",
            "الفيراي 6" to "رصد / أسلاك",
            "المسمار 7" to "رصد / مسامير",
            "المسمار الواقف" to "رصد / مسامير",
            "الكبة السلكية" to "رصد / أسلاك",
            "الجرن المحروق" to "رصد / جرن",
            "الجرن الشيطاني" to "رصد / جرن",
            "X صغير على الحائط" to "رمز إضافي",
            "المنظار القريب على شكل V" to "منظار",
            "المنظار الطبيعي" to "منظار",
            "المنظار البعيد" to "منظار",
            "المنظار التثبيتي" to "منظار",
            "المنظار السلكي" to "منظار / سلك",
            "المنظار الحديدي" to "منظار / حديد",
            "شجرة مثمرة" to "معلم شجري",
            "شجرة محروقة" to "معلم شجري",
            "شجرة مقطوعة" to "معلم شجري",
            "غصن معوج" to "معلم شجري",
            "الغصن المعوج" to "معلم شجري",
            "حجر ملتصق بالشجرة" to "أشجار وصخور",
            "ثقوب في الشجرة" to "أشجار وصخور",
            "الثقوب في الأشجار" to "أشجار وصخور",
            "ضربات الشاقور" to "تثبيت وتوجيه",
            "الشاقور" to "تثبيت وتوجيه",
            "البنتورة" to "تثبيت وتوجيه",
            "البيكي" to "تثبيت وتوجيه",
            "البروش" to "رصد وتثبيت",
            "حدوة الحصان" to "رسم",
            "بولون كرو فيس" to "رسم",
            "10 على شجرة" to "رسم / رقم",
            "إكسبراس" to "رسم",
            "شاحنة" to "رسم",
            "وجه إنسان" to "رسم",
            "صورة حيوان" to "رسم",
            "المحراب" to "رمز ديني",
            "محراب" to "رمز ديني"
        )

        val matchedPhraseKeys = mutableSetOf<String>()
        for ((phrase, type) in phraseCatalog) {
            if (text.contains(phrase, ignoreCase = true)) {
                val canonical = canonicalizePhrase(phrase)
                if (matchedPhraseKeys.add(canonical)) {
                    found.add(
                        DetectedSymbol(
                            symbol = canonical,
                            symbolType = type,
                            isInverted = isInvertedGlobal || phrase.contains("مقلوب"),
                            isFramed = isFramedGlobal,
                            recognitionClarity = "واضحة (وصف نصي)"
                        )
                    )
                }
            }
        }

        // Single religious / name / special / geometric / color Arabic words
        val singleArabicTerms = listOf(
            "الله" to ("الله" to "رمز ديني"),
            "يوسف" to ("يوسف" to "اسم"),
            "مريم" to ("مريم" to "اسم"),
            "زكريا" to ("زكريا" to "اسم"),
            "سليمان" to ("سليمان" to "اسم"),
            "القلب" to ("القلب" to "رمز خاص"),
            "قلب" to ("القلب" to "رمز خاص"),
            "مثلث" to ("المثلث" to "شكل هندسي / خطر محتمل"),
            "المثلث" to ("المثلث" to "شكل هندسي / خطر محتمل"),
            "نجمة" to ("النجمة على الصخور" to "شكل هندسي / خطر محتمل"),
            "نقطة" to ("النقطة" to "رمز إضافي"),
            "النقطة" to ("النقطة" to "رمز إضافي"),
            "شجرة" to ("الشجرة" to "رمز إضافي"),
            "الشجرة" to ("الشجرة" to "رمز إضافي"),
            "الأزرق" to ("الأزرق" to "لون"),
            "أزرق" to ("الأزرق" to "لون"),
            "الأصفر" to ("الأصفر" to "لون"),
            "أصفر" to ("الأصفر" to "لون"),
            "الأسود" to ("الأسود" to "لون"),
            "أسود" to ("الأسود" to "لون"),
            "حب" to ("حب" to "رمز خاص"),
            "الديس" to ("الديس" to "رصد / تثبيت"),
            "الصبغة" to ("الصبغة" to "تثبيت"),
            "الحزام" to ("الحزام ونصف الحزام" to "تثبيت")
        )

        for ((needle, mapped) in singleArabicTerms) {
            val (canonical, type) = mapped
            // Avoid double-adding الله if الله مع محمد or الله أكبر or ما شاء الله already matched
            if (canonical == "الله" && (matchedPhraseKeys.contains("الله مع محمد") || matchedPhraseKeys.contains("الله أكبر") || matchedPhraseKeys.contains("ما شاء الله"))) {
                continue
            }
            if (canonical == "القلب" && (matchedPhraseKeys.contains("القلب داخل شجرة") || matchedPhraseKeys.contains("القلب المقلوب"))) {
                continue
            }
            if (canonical == "المثلث" && matchedPhraseKeys.contains("المثلث الذي يحتوي على خط")) {
                continue
            }
            if (canonical == "الشجرة" && matchedPhraseKeys.any { it.contains("شجرة") || it.contains("الأشجار") }) {
                continue
            }
            val regex = Regex("""(?<![\p{IsArabic}])""" + Regex.escape(needle) + """(?![\p{IsArabic}])""")
            if (regex.containsMatchIn(text) && matchedPhraseKeys.add(canonical)) {
                found.add(
                    DetectedSymbol(
                        symbol = canonical,
                        symbolType = type,
                        isInverted = isInvertedGlobal,
                        isFramed = isFramedGlobal,
                        recognitionClarity = "واضحة (وصف نصي)"
                    )
                )
            }
        }

        // Special word LOVE
        if (Regex("""\bLOVE\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            found.add(
                DetectedSymbol(
                    symbol = "LOVE",
                    symbolType = "رمز خاص",
                    isFramed = isFramedGlobal,
                    recognitionClarity = "واضحة (وصف نصي)"
                )
            )
        }

        // 4. Standalone Latin letters (including unknown ones so we never hide or fabricate them!)
        val cleanedForLetters = text
            .replace(Regex("""\bLOVE\b""", RegexOption.IGNORE_CASE), " ")
            .replace("X صغير على الحائط", " ", ignoreCase = true)

        val letterMatches = Regex("""(?<![A-Za-z])[A-Za-z](?![A-Za-z])""").findAll(cleanedForLetters).toList()
        val letterCounts = linkedMapOf<String, Int>()
        for (m in letterMatches) {
            val ch = m.value.uppercase()
            // Skip single 'X' if "X صغير على الحائط" already matched
            if (ch == "X" && matchedPhraseKeys.contains("X صغير على الحائط")) continue
            letterCounts[ch] = (letterCounts[ch] ?: 0) + 1
        }
        letterCounts.forEach { (letter, count) ->
            val localInverted = isInvertedGlobal ||
                Regex("""$letter\s*(مقلوب|مقلوبة)""", RegexOption.IGNORE_CASE).containsMatchIn(text) ||
                Regex("""(مقلوب|مقلوبة)\s*$letter""", RegexOption.IGNORE_CASE).containsMatchIn(text)
            found.add(
                DetectedSymbol(
                    symbol = letter,
                    symbolType = "حرف لاتيني",
                    orientation = if (localInverted) "مقلوب" else "عادي",
                    isInverted = localInverted,
                    isFramed = isFramedGlobal,
                    repetitionCount = count,
                    recognitionClarity = "واضحة (وصف نصي)"
                )
            )
        }

        // 5. Standalone single digits (0..9) not already inside compound numeric tokens
        var cleanedForDigits = text
            .replace(Regex("""(?<!\d)\d{4,}(?!\d)"""), " ")
            .replace(Regex("""(?<!\d)1\.4(?!\d)"""), " ")
            .replace(Regex("""(?<!\d)41(?!\d)"""), " ")
            .replace("10 على شجرة", " ")
            .replace("الفيراي 6", " ")
            .replace("المسمار 7", " ")

        if (consumedNumbers.contains("6") && consumedNumbers.contains("9")) {
            cleanedForDigits = cleanedForDigits.replace(Regex("""\b6\s*\+\s*9\b"""), " ")
        }

        val digitMatches = Regex("""(?<!\d)[0-9](?!\d)""").findAll(cleanedForDigits).toList()
        val digitCounts = linkedMapOf<String, Int>()
        for (m in digitMatches) {
            val d = m.value
            digitCounts[d] = (digitCounts[d] ?: 0) + 1
        }
        digitCounts.forEach { (digit, count) ->
            found.add(
                DetectedSymbol(
                    symbol = digit,
                    symbolType = "رقم",
                    isFramed = isFramedGlobal,
                    repetitionCount = count,
                    recognitionClarity = "واضحة (وصف نصي)"
                )
            )
        }

        // 6. Special punctuation hazard / guide signs (#, =, standalone + or - when described as a sign)
        if (text.contains("#")) {
            found.add(DetectedSymbol(symbol = "#", symbolType = "علامة خطر محتملة", recognitionClarity = "واضحة (وصف نصي)"))
        }
        if (text.contains("=") || text.contains("علامة يساوي")) {
            found.add(DetectedSymbol(symbol = "=", symbolType = "رمز خاص", recognitionClarity = "واضحة (وصف نصي)"))
        }
        if (text.contains("علامة +") || text.contains("رمز +") || text.contains("إشارة +")) {
            found.add(DetectedSymbol(symbol = "+", symbolType = "علامة خطر محتملة", recognitionClarity = "واضحة (وصف نصي)"))
        }
        if (text.contains("علامة -") || text.contains("رمز -") || text.contains("إشارة -")) {
            found.add(DetectedSymbol(symbol = "-", symbolType = "علامة خطر محتملة", recognitionClarity = "واضحة (وصف نصي)"))
        }

        // 7. Match any custom user-added rules in Room that weren't in the static catalog
        activeRules.filter { it.isUserModified && it.isEnabled }.forEach { customRule ->
            val key = customRule.symbolKey.trim()
            if (key.length >= 2 && normalizedLower.contains(key.lowercase()) && found.none { it.symbol.equals(key, ignoreCase = true) }) {
                found.add(
                    DetectedSymbol(
                        symbol = key,
                        symbolType = "رمز مخصص من دليل المستخدم",
                        isInverted = isInvertedGlobal,
                        isFramed = isFramedGlobal,
                        recognitionClarity = "واضحة (وصف نصي)"
                    )
                )
            }
        }

        return found.distinctBy { it.symbol.uppercase() }
    }

    /**
     * Builds a complete 9-section report purely locally using the Room Knowledge Base.
     * Used for offline matching, unit tests, and auditing AI responses.
     */
    fun buildLocalReport(
        inputDescription: String,
        detectedSymbols: List<DetectedSymbol>,
        activeRules: List<KnowledgeRuleEntity>,
        isImageMode: Boolean = false,
        isImageUnclear: Boolean = false,
        clarityNote: String = "",
        visualContext: VisualContext? = null
    ): AnalysisReport {
        val context = visualContext ?: inferContextFromText(inputDescription, isImageMode, isImageUnclear)

        if (isImageUnclear) {
            return AnalysisReport(
                caseSummary = "الصورة المقدمة غير واضحة بما يكفي للتحقق من الرموز أو قراءة الحروف والأرقام بدقة.",
                visualContext = context.copy(imageClarity = "غير واضحة"),
                isImageUnclear = true,
                clarityNote = clarityNote.ifBlank {
                    "الصورة غير واضحة؛ تم الامتناع عن تخمين الحروف أو الأرقام التزاماً بقواعد الدقة."
                },
                detectedSymbols = emptyList(),
                dictionaryMatches = emptyList(),
                symbolRelationships = listOf("تعذر تحليل العلاقات المكانية بسبب ضعف وضوح الصورة."),
                interpretations = emptyList(),
                conflicts = emptyList(),
                missingInformation = listOf(
                    "يرجى التقاط صورة أوضح بإضاءة جانبية مناسبة ومن زاوية عمودية على السطح.",
                    "اذكر نوع السطح (صخرة، جدار، شجرة، ورق) وحجم العلامة التقريبي.",
                    "أضف صورة أوسع تُظهر محيط العلامة دون الاقتراب من أي جسم مشبوه."
                ),
                confidence = ConfidenceAssessment(
                    visualRecognitionLevel = "غير كافٍ (الصورة غير واضحة)",
                    dictionaryMatchLevel = "غير ممكن حالياً",
                    overallInferenceLevel = "معلق لحين توفر صورة أوضح",
                    explanation = "تم إيقاف المطابقة وعدم تخمين أي رمز غير مقروء بوضوح."
                ),
                warnings = listOf(
                    SafetyWarning(
                        severity = WarningSeverity.METHODOLOGY_NOTICE,
                        title = "تنبيه منهجي",
                        message = MANDATORY_METHODOLOGY_DISCLAIMER
                    )
                )
            )
        }

        val matches = mutableListOf<DictionaryMatch>()
        val conflicts = mutableListOf<ConflictItem>()
        val relationships = mutableListOf<String>()
        val hypotheses = mutableListOf<InterpretationHypothesis>()
        val missingInfo = mutableListOf<String>()
        val warnings = mutableListOf<SafetyWarning>()

        var hazardDetected = false
        var heritageDetected = false

        val normalizedDesc = inputDescription.lowercase()
        val hasFinancialCue = financialContextKeywords.any { normalizedDesc.contains(it.lowercase()) } ||
            detectedSymbols.any { it.symbol.uppercase() in setOf("LOVE", "حب", "القلب", "الأزرق", "الأصفر") }
        val hasPathCue = pathContextKeywords.any { normalizedDesc.contains(it.lowercase()) } ||
            detectedSymbols.any { it.symbol.uppercase() in setOf("Y", "Z", "R", "I", "E", "W", "1", "8") }

        // Check raw description for hazard words even if not extracted as a standalone symbol
        if (hazardKeywords.any { inputDescription.contains(it, ignoreCase = true) }) {
            hazardDetected = true
        }
        if (heritageKeywords.any { inputDescription.contains(it, ignoreCase = true) }) {
            heritageDetected = true
        }

        // Match each detected symbol against active Room rules
        for (sym in detectedSymbols) {
            val matchedRule = findBestRule(sym.symbol, activeRules)
            if (matchedRule != null) {
                if (matchedRule.isHazard) hazardDetected = true
                if (matchedRule.isHeritage) heritageDetected = true

                val noteParts = mutableListOf<String>()
                if (matchedRule.contextNotes.isNotBlank()) noteParts.add(matchedRule.contextNotes)
                if (sym.isInverted) {
                    noteParts.add("الرمز موصوف بأنه مقلوب؛ ورد في الدليل أن الرمز المقلوب قد يرتبط بتغير في التضاريس.")
                }
                if (sym.isFramed) {
                    noteParts.add("الرمز محاط بإطار أو مربع مما يستدعي دراسته كوحدة مستقلة ضمن التركيب.")
                }
                if (sym.repetitionCount > 1) {
                    noteParts.add("تكرر الرمز (${sym.repetitionCount} مرات) — الأولوية للرموز المتكررة حسب الدليل.")
                }

                matches.add(
                    DictionaryMatch(
                        symbol = sym.symbol,
                        meaningInGuide = matchedRule.meaning,
                        ruleSource = matchedRule.ruleSource,
                        notes = noteParts.joinToString(" "),
                        isFoundInGuide = true
                    )
                )

                // Check for multiple / conflicting meanings
                val alternatives = ReportJsonCodec.decodeAlternativeMeanings(matchedRule.alternativeMeaningsJson)
                if (alternatives.size >= 2) {
                    val resolution = buildConflictResolutionNote(
                        symbol = sym.symbol,
                        isInverted = sym.isInverted,
                        hasFinancialCue = hasFinancialCue,
                        hasPathCue = hasPathCue
                    )
                    conflicts.add(
                        ConflictItem(
                            symbol = sym.symbol,
                            meanings = alternatives,
                            resolutionNote = resolution
                        )
                    )
                }
            } else {
                // Unknown symbol — NEVER fabricate a meaning!
                matches.add(
                    DictionaryMatch(
                        symbol = sym.symbol,
                        meaningInGuide = "غير موجود في قاعدة المعرفة الحالية (لم يتم اختلاق معنى له).",
                        ruleSource = "غير مسجل في الدليل",
                        notes = "يمكنك إضافة هذا الرمز ومعناه الموثّق لاحقاً من شاشة (إدارة الدليل).",
                        isFoundInGuide = false
                    )
                )
                missingInfo.add("الرمز (${sym.symbol}) غير مسجل في الدليل؛ هل هو رمز محلي خاص أم جزء من حرف أو نقش متآكل؟")
            }
        }

        // Relationships analysis (Section 11, 18)
        if (detectedSymbols.size > 1) {
            relationships.add(
                "تم رصد ${detectedSymbols.size} علامات في الحالة نفسها (${detectedSymbols.joinToString("، ") { it.symbol }}). لا يُفترض تلقائياً أن ترتيب الرموز يمثل ترتيباً مكانياً ميدانياً."
            )
        }
        val repeated = detectedSymbols.filter { it.repetitionCount > 1 }
        if (repeated.isNotEmpty()) {
            relationships.add(
                "توجد رموز متكررة (${repeated.joinToString("، ") { "${it.symbol} ×${it.repetitionCount}" }}): يعطي الدليل أولوية لدراسة الرموز المتكررة قبل غيرها."
            )
        }
        val hasLetters = detectedSymbols.any { it.symbolType.contains("حرف") }
        val hasNumbers = detectedSymbols.any { it.symbolType.contains("رقم") || it.symbolType.contains("عددي") }
        if (hasLetters && hasNumbers) {
            relationships.add("اجتماع حروف وأرقام في حالة واحدة يستدعي التحقق من الموضع النسبي (هل الرقم فوق الحرف أم تحته أم بجانبه).")
        }
        if (detectedSymbols.any { it.isInverted }) {
            relationships.add("وجود رمز مقلوب يطرح احتمال تغير التضاريس (مرتفع/طلعة/تلة) وفق القواعد العامة للدليل.")
        }
        if (detectedSymbols.any { it.isFramed }) {
            relationships.add("إحاطة العلامة بمربع أو إطار تميزها بصرياً عن العلامات المحيطة وتحتاج للتحقق مما إذا كانت حديثة أم قديمة.")
        }
        if (detectedSymbols.any { it.symbol.uppercase() == "L + I + B" } ||
            (detectedSymbols.any { it.symbol.uppercase() == "L" } &&
                detectedSymbols.any { it.symbol.uppercase() == "I" } &&
                detectedSymbols.any { it.symbol.uppercase() == "B" })
        ) {
            relationships.add("تطابق مجموعة (L، I، B) مع مثال التركيب الوارد في الدليل (لانقار/كوري + الشمال + باسان/بئر) بوصفه فرضية قاموسية مقترحة لا حقيقة مثبتة.")
        }
        if (detectedSymbols.any { it.symbol.uppercase() == "E + 5 + T" } ||
            (detectedSymbols.any { it.symbol.uppercase() == "E" } &&
                detectedSymbols.any { it.symbol == "5" } &&
                detectedSymbols.any { it.symbol.uppercase() == "T" })
        ) {
            relationships.add("تطابق مجموعة (E، 5، T) مع التركيب الافتراضي للتدريب في الدليل (الشرق + غرفة تحت الأرض + قبر)، وهو مثال تدريبي للمطابقة وليس توجيهاً للعثور على موقع.")
            heritageDetected = true
        }
        if (relationships.isEmpty() && detectedSymbols.isNotEmpty()) {
            relationships.add("تم رصد علامة منفردة؛ دراسة العلامة المنفردة تبقى محدودة الدلالة دون قرائن مرافقة.")
        }

        // Interpretations separating Observation / Deduction / Hypothesis
        if (hazardDetected) {
            hypotheses.add(
                InterpretationHypothesis(
                    title = "إيقاف التحليل الميداني بسبب مؤشر خطر محتمل",
                    observation = "رُصدت علامة أو مفردة مصنفة ضمن علامات الخطر أو الأسلاك أو المسامير أو الأشكال الهندسية الحساسة في الدليل.",
                    deduction = "لا يمكن المتابعة بأي تحليل ميداني عملي؛ الأولوية القصوى للسلامة الشخصية.",
                    hypothesis = "قد تشير العلامة إلى جسم خطير أو لغم محتمل أو قد تكون علامة عادية؛ وفي كل الأحوال يُمنع الاقتراب أو اللمس.",
                    rationale = "تطبيقاً لقاعدة السلامة الإلزامية في القسمين 12 و16."
                )
            )
        } else if (detectedSymbols.isNotEmpty()) {
            val matchedKnown = matches.filter { it.isFoundInGuide }
            if (matchedKnown.isNotEmpty()) {
                val matchedSummary = matchedKnown.joinToString("؛ ") { "${it.symbol}: ${it.meaningInGuide}" }
                hypotheses.add(
                    InterpretationHypothesis(
                        title = "الاحتمال الأول: القراءة القاموسية المباشرة وفق الدليل",
                        observation = "العلامات المرصودة هي: ${detectedSymbols.joinToString("، ") { it.symbol }} على سطح (${context.surfaceType}).",
                        deduction = "تطابقت ${matchedKnown.size} من أصل ${detectedSymbols.size} علامات مع مدخلات الدليل اليدوي للمستخدم.",
                        hypothesis = "وفق مرجع المستخدم تُقرأ المعاني المقترحة كالتالي: $matchedSummary — وهي فرضية مرجعية غير مثبتة.",
                        rationale = "تطابق مباشر مع القواعد المسجلة في قاعدة البيانات المحلية مع الحفاظ على عدم الجزم."
                    )
                )
            }
            if (conflicts.isNotEmpty()) {
                hypotheses.add(
                    InterpretationHypothesis(
                        title = "الاحتمال الثاني: وجود دلالات بديلة متعارضة",
                        observation = "تضم الحالة رموزاً متعددة المعاني (${conflicts.joinToString("، ") { it.symbol }}).",
                        deduction = "تغير السياق المحيط قد ينقل التفسير من دلالة تضاريسية/معلمية إلى دلالة أخرى واردة في الدليل.",
                        hypothesis = "يجب إبقاء جميع البدائل مطروحة وعدم اختيار معنى واحد عشوائياً حتى تتوفر قرائن إضافية.",
                        rationale = "تطبيقاً لقواعد إدارة التعارضات في القسم 19."
                    )
                )
            }
            hypotheses.add(
                InterpretationHypothesis(
                    title = "الاحتمال البديل: علامة حديثة أو طبيعية أو غير تكنيزية",
                    observation = "الشكل المرئي أو الوصف وحده لا يثبت العمر التاريخي أو الغرض الأصلي من العلامة.",
                    deduction = "كثير من الخطوط والشقوق والحروف على الصخور والأشجار قد تكون عوامل تعرية أو علامات ملكية/مساحة أو نقوشاً حديثة.",
                    hypothesis = "قد لا ترتبط العلامة بأي غرض تكنيزي على الإطلاق.",
                    rationale = "قاعدة التمييز بين الملاحظة والفرضية وعدم افتراض الأثرية دون دليل."
                )
            )
        }

        // Missing Information questions
        if (context.surfaceType == "غير محدد") {
            missingInfo.add("ما نوع السطح الذي تظهر عليه العلامة (صخرة، جدار، شجرة، أم رسم على ورق)؟")
        }
        if (detectedSymbols.any { it.orientation == "عادي" } && !inputDescription.contains("اتجاه")) {
            missingInfo.add("هل جميع الرموز معتدلة أم يوجد بينها رمز مقلوب أو مائل؟")
        }
        if (detectedSymbols.size > 1 && !inputDescription.contains("متباعد") && !inputDescription.contains("متجاور")) {
            missingInfo.add("هل العلامات متجاورة ضمن تكوين واحد أم متباعدة، وهل يوجد بينها إطار أو مربع؟")
        }
        if (detectedSymbols.any { it.symbol.uppercase() == "S" } && !(hasFinancialCue xor hasPathCue)) {
            missingInfo.add("بخصوص حرف S: هل يظهر ضمن رموز تضاريس وطرق أم مع رموز مالية أو ألوان محددة؟")
        }
        if (missingInfo.isEmpty()) {
            missingInfo.add("هل تبدو العلامات محفورة بعمق وبأدوات قديمة أم مرسومة حديثاً أو ناتجة عن عوامل طبيعية؟")
        }

        // Warnings (Section 12, 16, 24)
        if (hazardDetected) {
            warnings.add(
                SafetyWarning(
                    severity = WarningSeverity.CRITICAL_HAZARD,
                    title = "تحذير سلامة فوري — خطر ميداني محتمل",
                    message = MANDATORY_HAZARD_MESSAGE
                )
            )
        }
        if (heritageDetected) {
            warnings.add(
                SafetyWarning(
                    severity = WarningSeverity.HERITAGE_ADVISORY,
                    title = "توصية حماية المواقع الأثرية والقبور",
                    message = MANDATORY_HERITAGE_MESSAGE
                )
            )
        }
        warnings.add(
            SafetyWarning(
                severity = WarningSeverity.METHODOLOGY_NOTICE,
                title = "تنبيه منهجي وإخلاء مسؤولية",
                message = MANDATORY_METHODOLOGY_DISCLAIMER
            )
        )

        // Separate Confidence levels (NO percentages!)
        val knownCount = matches.count { it.isFoundInGuide }
        val visualLevel = when {
            detectedSymbols.isEmpty() -> "منخفض — لم تُرصد علامات واضحة"
            isImageMode -> "متوسط (حسب وضوح الصورة)"
            else -> "واضح (مبني على وصف المستخدم النصي)"
        }
        val dictLevel = when {
            detectedSymbols.isEmpty() -> "لا توجد مطابقة"
            knownCount == detectedSymbols.size && conflicts.isEmpty() -> "مرتفع (تطابق مباشر مع مدخلات الدليل)"
            knownCount > 0 -> "متوسط (تطابق جزئي أو وجود تعدد في المعاني)"
            else -> "منخفض (الرموز غير مسجلة في الدليل الحالي)"
        }
        val inferenceLevel = when {
            hazardDetected -> "معلق للسلامة (توقف عن أي إجراء ميداني)"
            detectedSymbols.isEmpty() || knownCount == 0 -> "غير كافٍ للاستنتاج"
            conflicts.isNotEmpty() -> "منخفض إلى متوسط (توجد تعارضات تحتاج لقرائن سياقية)"
            else -> "فرضية أولية غير مثبتة (تحتاج للتحقق السياقي)"
        }

        val summary = if (detectedSymbols.isEmpty()) {
            "لم يتم التعرف على رموز قابلة للمطابقة من المعطيات المدخلة. يرجى توضيح العلامات أو تقديم صورة أدق."
        } else {
            val modeLabel = if (isImageMode) "تحليل صورة" else "تحليل وصف نصي"
            "$modeLabel: تم رصد ${detectedSymbols.size} علامة/رمز (${detectedSymbols.joinToString("، ") { it.symbol }})، طوبق منها $knownCount مع الدليل اليدوي، ورُصدت ${conflicts.size} حالة تعدد/تعارض في المعاني."
        }

        return AnalysisReport(
            caseSummary = summary,
            visualContext = context,
            isImageUnclear = false,
            clarityNote = "",
            detectedSymbols = detectedSymbols,
            dictionaryMatches = matches,
            symbolRelationships = relationships,
            interpretations = hypotheses,
            conflicts = conflicts,
            missingInformation = missingInfo,
            confidence = ConfidenceAssessment(
                visualRecognitionLevel = visualLevel,
                dictionaryMatchLevel = dictLevel,
                overallInferenceLevel = inferenceLevel,
                explanation = "يتم فصل درجة وضوح الرمز عن درجة مطابقته للدليل وعن قوة الفرضية العامة دون استخدام نسب مئوية توحي بدقة علمية غير موجودة."
            ),
            warnings = warnings
        )
    }

    /**
     * Audits and merges an AI-generated report against the user's authoritative Room Knowledge Base
     * and enforces all safety/conflict/non-fabrication guardrails.
     */
    fun auditAndEnrichAiReport(
        aiReport: AnalysisReport,
        inputDescription: String,
        activeRules: List<KnowledgeRuleEntity>,
        isImageMode: Boolean
    ): AnalysisReport {
        if (aiReport.isImageUnclear) {
            return buildLocalReport(
                inputDescription = inputDescription,
                detectedSymbols = emptyList(),
                activeRules = activeRules,
                isImageMode = isImageMode,
                isImageUnclear = true,
                clarityNote = aiReport.clarityNote,
                visualContext = aiReport.visualContext
            )
        }

        // Cross-check AI detected symbols with deterministic text extraction when description is provided
        val localExtracted = if (inputDescription.isNotBlank()) {
            extractSymbolsFromText(inputDescription, activeRules)
        } else emptyList()

        val mergedDetected = mutableListOf<DetectedSymbol>()
        mergedDetected.addAll(aiReport.detectedSymbols)
        localExtracted.forEach { localSym ->
            if (mergedDetected.none { it.symbol.equals(localSym.symbol, ignoreCase = true) }) {
                mergedDetected.add(localSym)
            }
        }

        // Authoritative dictionary matching strictly from Room Knowledge Base
        val localAudit = buildLocalReport(
            inputDescription = inputDescription,
            detectedSymbols = mergedDetected,
            activeRules = activeRules,
            isImageMode = isImageMode,
            isImageUnclear = false,
            visualContext = aiReport.visualContext
        )

        // Merge relationships, hypotheses, missing info, and enforce safety warnings
        val mergedRelationships = (aiReport.symbolRelationships + localAudit.symbolRelationships)
            .map { it.trim() }.filter { it.isNotBlank() }.distinct()

        val hasCriticalHazard = localAudit.warnings.any { it.severity == WarningSeverity.CRITICAL_HAZARD } ||
            aiReport.warnings.any { it.severity == WarningSeverity.CRITICAL_HAZARD }

        val mergedInterpretations = if (hasCriticalHazard) {
            // Stop field interpretations if hazard detected
            localAudit.interpretations
        } else {
            val combined = aiReport.interpretations.ifEmpty { localAudit.interpretations }
            combined
        }

        val mergedConflicts = localAudit.conflicts.ifEmpty { aiReport.conflicts }
        val mergedMissing = (aiReport.missingInformation + localAudit.missingInformation)
            .map { it.trim() }.filter { it.isNotBlank() }.distinct().take(6)

        val mergedWarnings = mutableListOf<SafetyWarning>()
        if (hasCriticalHazard) {
            mergedWarnings.add(
                SafetyWarning(
                    severity = WarningSeverity.CRITICAL_HAZARD,
                    title = "تحذير سلامة فوري — خطر ميداني محتمل",
                    message = MANDATORY_HAZARD_MESSAGE
                )
            )
        }
        val hasHeritage = localAudit.warnings.any { it.severity == WarningSeverity.HERITAGE_ADVISORY } ||
            aiReport.warnings.any { it.severity == WarningSeverity.HERITAGE_ADVISORY }
        if (hasHeritage) {
            mergedWarnings.add(
                SafetyWarning(
                    severity = WarningSeverity.HERITAGE_ADVISORY,
                    title = "توصية حماية المواقع الأثرية والقبور",
                    message = MANDATORY_HERITAGE_MESSAGE
                )
            )
        }
        mergedWarnings.add(
            SafetyWarning(
                severity = WarningSeverity.METHODOLOGY_NOTICE,
                title = "تنبيه منهجي وإخلاء مسؤولية",
                message = MANDATORY_METHODOLOGY_DISCLAIMER
            )
        )

        // Strip any accidental percentage signs from confidence levels
        val cleanConfidence = ConfidenceAssessment(
            visualRecognitionLevel = stripPercentages(aiReport.confidence.visualRecognitionLevel).ifBlank { localAudit.confidence.visualRecognitionLevel },
            dictionaryMatchLevel = localAudit.confidence.dictionaryMatchLevel,
            overallInferenceLevel = if (hasCriticalHazard) {
                "معلق للسلامة (توقف عن أي إجراء ميداني)"
            } else {
                stripPercentages(aiReport.confidence.overallInferenceLevel).ifBlank { localAudit.confidence.overallInferenceLevel }
            },
            explanation = localAudit.confidence.explanation
        )

        return aiReport.copy(
            caseSummary = aiReport.caseSummary.ifBlank { localAudit.caseSummary },
            detectedSymbols = mergedDetected,
            dictionaryMatches = localAudit.dictionaryMatches,
            symbolRelationships = mergedRelationships,
            interpretations = mergedInterpretations,
            conflicts = mergedConflicts,
            missingInformation = mergedMissing,
            confidence = cleanConfidence,
            warnings = mergedWarnings
        )
    }

    private fun findBestRule(symbol: String, rules: List<KnowledgeRuleEntity>): KnowledgeRuleEntity? {
        val clean = symbol.trim()
        if (clean.isBlank()) return null

        // 4+ digit numeric rule
        if (Regex("""^\d{4,}$""").matches(clean)) {
            return rules.firstOrNull { it.isEnabled && it.symbolKey == "4_DIGITS_PLUS" }
        }

        // Exact key match (case-insensitive)
        rules.firstOrNull { it.isEnabled && it.symbolKey.equals(clean, ignoreCase = true) }?.let { return it }

        // Display name match
        rules.firstOrNull { it.isEnabled && it.displayName.equals(clean, ignoreCase = true) }?.let { return it }

        // Normalized Arabic definite article match (e.g., "نقطة" vs "النقطة")
        val stripped = clean.removePrefix("ال")
        return rules.firstOrNull {
            it.isEnabled && (
                it.symbolKey.removePrefix("ال").equals(stripped, ignoreCase = true) ||
                    it.displayName.removePrefix("ال").equals(stripped, ignoreCase = true)
                )
        }
    }

    private fun buildConflictResolutionNote(
        symbol: String,
        isInverted: Boolean,
        hasFinancialCue: Boolean,
        hasPathCue: Boolean
    ): String {
        return when (symbol.uppercase()) {
            "S" -> when {
                hasFinancialCue && !hasPathCue ->
                    "وجود قرائن مالية مصاحبة يرجّح دراسة سياق (أموال ورقية)، مع الإبقاء على احتمال (طريق) وارداً دون حسم قاطع."
                hasPathCue && !hasFinancialCue ->
                    "وجود قرائن تضاريسية مصاحبة يرجّح دراسة سياق (طريق)، مع تسجيل المعنى البديل (أموال ورقية) في الدليل."
                else ->
                    "لا يمكن ترجيح تفسير واحد من المعطيات الحالية. يجب التمييز بين حرف S بمعنى طريق واستعماله في قائمة رموز الأموال عبر السياق المحيط."
            }
            "A" -> if (isInverted) {
                "وُصف الحرف بأنه مقلوب مما يتوافق في الدليل مع دلالة (مرتفع)، مع بقاء دلالة التثبيت مسجلة في القاموس."
            } else {
                "لحرف A أربعة أوضاع (أمام، يمين، يسار، مقلوب). لا يمكن ترجيح تفسير واحد من المعطيات الحالية دون تحديد اتجاهه بدقة."
            }
            "V" -> if (isInverted) {
                "وُصف الحرف بأنه مقلوب مما يتوافق في الدليل مع دلالة (طلعة)، مع بقاء دلالة المنعطف والمنظار القريب مسجلة."
            } else {
                "لا يمكن ترجيح تفسير واحد من المعطيات الحالية بين (منعطف) و(منظار قريب على شكل V) دون فحص الهيئة الكاملة."
            }
            "القلب" -> if (isInverted) {
                "وُصف القلب بأنه مقلوب مما يربطه في الدليل بدلالة (تلة)، مع حفظ دلالة المعدن في القاموس."
            } else {
                "لا يمكن ترجيح تفسير واحد من المعطيات الحالية بين القلب العام والقلب داخل الشجرة أو المقلوب دون تأكيد السطح والاتجاه."
            }
            else -> "لا يمكن ترجيح تفسير واحد من المعطيات الحالية. تم عرض جميع المعاني المسجلة في الدليل دون اختيار عشوائي."
        }
    }

    private fun inferContextFromText(
        text: String,
        isImageMode: Boolean,
        isUnclear: Boolean
    ): VisualContext {
        val surface = when {
            text.contains("صخرة") || text.contains("حجر") || text.contains("صخر") -> "صخرة"
            text.contains("شجرة") || text.contains("جذع") || text.contains("غصن") -> "شجرة"
            text.contains("جدار") || text.contains("حائط") -> "جدار / حائط"
            text.contains("ورق") || text.contains("مخطوط") || text.contains("خريطة") -> "رسم على ورق"
            else -> "غير محدد"
        }
        val origin = when {
            text.contains("محفور") || text.contains("نقش") || text.contains("ضربات") -> "موصوف كعلامة محفورة/منقوشة (لا يثبت ذلك قِدمها أو غرضها دون فحص)"
            text.contains("مرسوم") || text.contains("صبغة") || text.contains("بنتورة") -> "موصوف كرسم أو طلاء (تحتاج للتحقق مما إذا كانت حديثة)"
            else -> "لا يمكن الجزم بأن العلامة طبيعية أم مصنوعة أو قديمة من المعطيات الحالية"
        }
        val colors = mutableListOf<String>()
        if (text.contains("أزرق") || text.contains("الأزرق")) colors.add("الأزرق")
        if (text.contains("أصفر") || text.contains("الأصفر")) colors.add("الأصفر")
        if (text.contains("أسود") || text.contains("الأسود")) colors.add("الأسود")

        return VisualContext(
            surfaceType = surface,
            naturalOrManMade = origin,
            imageClarity = when {
                isUnclear -> "غير واضحة"
                isImageMode -> "تعتمد على الفحص البصري"
                else -> "مدخل نصي مباشر"
            },
            colorsObserved = colors,
            isSingleComposition = text.contains("تكوين واحد") || text.contains("متجاورة") || text.contains("بجانبه")
        )
    }

    private fun canonicalizePhrase(phrase: String): String = when (phrase) {
        "قلب مقلوب" -> "القلب المقلوب"
        "الصليب المعقوف" -> "إشارة الصليب المعقوف"
        "فرشات قبايلية" -> "الفرشات القبايلية"
        "الله اكبر" -> "الله أكبر"
        "مثلث به خط" -> "المثلث الذي يحتوي على خط"
        "مربع بخطين متقاطعين" -> "المربع بخطين متقاطعين"
        "هيكل عظمي" -> "الهيكل العظمي"
        "غصن معوج" -> "الغصن المعوج"
        "ثقوب في الشجرة" -> "الثقوب في الأشجار"
        "ضربات الشاقور" -> "الشاقور"
        "إكسبراس", "شاحنة" -> "إكسبراس أو شاحنة"
        "وجه إنسان" -> "وجه إنسان أو حيوان"
        "محراب" -> "المحراب"
        "المنظار القريب على شكل V" -> "المنظار القريب V"
        "السلك الذي يدور ثلاث دورات" -> "السلك ثلاث دورات"
        "السلك الذي يحتوي على عقد فردية" -> "السلك ذو العقد الفردية"
        else -> phrase
    }

    private fun stripPercentages(input: String): String {
        return input.replace(Regex("""\d+\s*%"""), "").replace(Regex("""%\s*\d+"""), "").trim()
    }
}
