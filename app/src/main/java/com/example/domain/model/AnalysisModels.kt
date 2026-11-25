package com.example.domain.model

enum class RuleCategory(val arabicLabel: String) {
    LETTERS("الحروف"),
    EXTRA_SYMBOLS("رموز إضافية"),
    NUMBERS("الأرقام"),
    NUMBER_COMBINATIONS("التراكيب العددية"),
    SPECIAL_SYMBOLS("الرموز الخاصة"),
    RELIGIOUS("الرموز الدينية"),
    NAMES("الأسماء والقصص"),
    COMBINATIONS("التركيبات المركبة"),
    MONITORING_WIRES("الرصد والمسامير والأسلاك"),
    SCOPES("المناظير"),
    ANCHORING_GUIDANCE("التثبيت والتوجيه"),
    TREES_ROCKS("الأشجار والصخور"),
    GEOMETRIC_HAZARDS("الأشكال الهندسية وعلامات الخطر"),
    COLORS_DRAWINGS("الألوان والرسومات"),
    GENERAL_RULES("قواعد عامة")
}

data class AlternativeMeaning(
    val meaning: String,
    val context: String,
    val source: String
)

data class VisualContext(
    val surfaceType: String = "غير محدد",
    val naturalOrManMade: String = "يحتاج إلى فحص سياقي أوضح",
    val imageClarity: String = "واضحة",
    val colorsObserved: List<String> = emptyList(),
    val isSingleComposition: Boolean = false
)

data class DetectedSymbol(
    val symbol: String,
    val symbolType: String,
    val positionInImage: String = "غير محدد",
    val relativeSize: String = "متوسط",
    val orientation: String = "عادي",
    val isInverted: Boolean = false,
    val isFramed: Boolean = false,
    val repetitionCount: Int = 1,
    val recognitionClarity: String = "متوسطة"
)

data class DictionaryMatch(
    val symbol: String,
    val meaningInGuide: String,
    val ruleSource: String,
    val notes: String = "",
    val isFoundInGuide: Boolean = true
)

data class InterpretationHypothesis(
    val title: String,
    val observation: String,
    val deduction: String,
    val hypothesis: String,
    val rationale: String
)

data class ConflictItem(
    val symbol: String,
    val meanings: List<AlternativeMeaning>,
    val resolutionNote: String
)

data class ConfidenceAssessment(
    val visualRecognitionLevel: String,
    val dictionaryMatchLevel: String,
    val overallInferenceLevel: String,
    val explanation: String = ""
)

enum class WarningSeverity {
    CRITICAL_HAZARD,
    HERITAGE_ADVISORY,
    METHODOLOGY_NOTICE
}

data class SafetyWarning(
    val severity: WarningSeverity,
    val title: String,
    val message: String
)

data class AnalysisReport(
    val caseSummary: String,
    val visualContext: VisualContext = VisualContext(),
    val isImageUnclear: Boolean = false,
    val clarityNote: String = "",
    val detectedSymbols: List<DetectedSymbol> = emptyList(),
    val dictionaryMatches: List<DictionaryMatch> = emptyList(),
    val symbolRelationships: List<String> = emptyList(),
    val interpretations: List<InterpretationHypothesis> = emptyList(),
    val conflicts: List<ConflictItem> = emptyList(),
    val missingInformation: List<String> = emptyList(),
    val confidence: ConfidenceAssessment = ConfidenceAssessment(
        visualRecognitionLevel = "غير محدد",
        dictionaryMatchLevel = "غير محدد",
        overallInferenceLevel = "منخفض"
    ),
    val warnings: List<SafetyWarning> = emptyList()
)

data class FollowUpMessage(
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AIProviderType(val arabicName: String) {
    FIREBASE_GEMINI("Firebase AI Logic (Gemini Vision)"),
    CUSTOM_PROXY("خادم وسيط مخصص (Proxy)")
}

data class AppSettings(
    val providerType: AIProviderType = AIProviderType.FIREBASE_GEMINI,
    val geminiModel: String = "gemini-3.8-flash",
    val customProxyUrl: String = "",
    val customProxyToken: String = "",
    val timeoutSeconds: Int = 45,
    val darkTheme: Boolean = true,
    val languageCode: String = "ar"
)
