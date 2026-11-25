package com.example.domain.model

import org.json.JSONArray
import org.json.JSONObject

object ReportJsonCodec {

    fun encodeAlternativeMeanings(list: List<AlternativeMeaning>): String {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("meaning", item.meaning)
                    put("context", item.context)
                    put("source", item.source)
                }
            )
        }
        return arr.toString()
    }

    fun decodeAlternativeMeanings(json: String): List<AlternativeMeaning> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { idx ->
                val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
                AlternativeMeaning(
                    meaning = obj.optString("meaning", ""),
                    context = obj.optString("context", ""),
                    source = obj.optString("source", "")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeStringList(items: List<String>): String {
        val arr = JSONArray()
        items.forEach { arr.put(it) }
        return arr.toString()
    }

    fun decodeStringList(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeFollowUpMessages(messages: List<FollowUpMessage>): String {
        val arr = JSONArray()
        messages.forEach { msg ->
            arr.put(
                JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                    put("timestamp", msg.timestamp)
                }
            )
        }
        return arr.toString()
    }

    fun decodeFollowUpMessages(json: String): List<FollowUpMessage> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { idx ->
                val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
                FollowUpMessage(
                    role = obj.optString("role", "user"),
                    content = obj.optString("content", ""),
                    timestamp = obj.optLong("timestamp", 0L)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeReport(report: AnalysisReport): String {
        val root = JSONObject()
        root.put("caseSummary", report.caseSummary)
        root.put("isImageUnclear", report.isImageUnclear)
        root.put("clarityNote", report.clarityNote)

        root.put(
            "visualContext",
            JSONObject().apply {
                put("surfaceType", report.visualContext.surfaceType)
                put("naturalOrManMade", report.visualContext.naturalOrManMade)
                put("imageClarity", report.visualContext.imageClarity)
                put("isSingleComposition", report.visualContext.isSingleComposition)
                put("colorsObserved", JSONArray(report.visualContext.colorsObserved))
            }
        )

        val detectedArr = JSONArray()
        report.detectedSymbols.forEach { d ->
            detectedArr.put(
                JSONObject().apply {
                    put("symbol", d.symbol)
                    put("symbolType", d.symbolType)
                    put("positionInImage", d.positionInImage)
                    put("relativeSize", d.relativeSize)
                    put("orientation", d.orientation)
                    put("isInverted", d.isInverted)
                    put("isFramed", d.isFramed)
                    put("repetitionCount", d.repetitionCount)
                    put("recognitionClarity", d.recognitionClarity)
                }
            )
        }
        root.put("detectedSymbols", detectedArr)

        val matchArr = JSONArray()
        report.dictionaryMatches.forEach { m ->
            matchArr.put(
                JSONObject().apply {
                    put("symbol", m.symbol)
                    put("meaningInGuide", m.meaningInGuide)
                    put("ruleSource", m.ruleSource)
                    put("notes", m.notes)
                    put("isFoundInGuide", m.isFoundInGuide)
                }
            )
        }
        root.put("dictionaryMatches", matchArr)

        root.put("symbolRelationships", JSONArray(report.symbolRelationships))

        val hypArr = JSONArray()
        report.interpretations.forEach { h ->
            hypArr.put(
                JSONObject().apply {
                    put("title", h.title)
                    put("observation", h.observation)
                    put("deduction", h.deduction)
                    put("hypothesis", h.hypothesis)
                    put("rationale", h.rationale)
                }
            )
        }
        root.put("interpretations", hypArr)

        val confArr = JSONArray()
        report.conflicts.forEach { c ->
            val altArr = JSONArray()
            c.meanings.forEach { alt ->
                altArr.put(
                    JSONObject().apply {
                        put("meaning", alt.meaning)
                        put("context", alt.context)
                        put("source", alt.source)
                    }
                )
            }
            confArr.put(
                JSONObject().apply {
                    put("symbol", c.symbol)
                    put("meanings", altArr)
                    put("resolutionNote", c.resolutionNote)
                }
            )
        }
        root.put("conflicts", confArr)

        root.put("missingInformation", JSONArray(report.missingInformation))

        root.put(
            "confidence",
            JSONObject().apply {
                put("visualRecognitionLevel", report.confidence.visualRecognitionLevel)
                put("dictionaryMatchLevel", report.confidence.dictionaryMatchLevel)
                put("overallInferenceLevel", report.confidence.overallInferenceLevel)
                put("explanation", report.confidence.explanation)
            }
        )

        val warnArr = JSONArray()
        report.warnings.forEach { w ->
            warnArr.put(
                JSONObject().apply {
                    put("severity", w.severity.name)
                    put("title", w.title)
                    put("message", w.message)
                }
            )
        }
        root.put("warnings", warnArr)

        return root.toString()
    }

    fun decodeReport(json: String): AnalysisReport {
        if (json.isBlank()) {
            return AnalysisReport(caseSummary = "لا يتوفر تقرير محفوظ.")
        }
        return try {
            val root = JSONObject(json)
            val vcObj = root.optJSONObject("visualContext")
            val visualContext = if (vcObj != null) {
                val colorsArr = vcObj.optJSONArray("colorsObserved")
                val colors = if (colorsArr != null) {
                    (0 until colorsArr.length()).mapNotNull { colorsArr.optString(it).takeIf { s -> s.isNotBlank() } }
                } else emptyList()
                VisualContext(
                    surfaceType = vcObj.optString("surfaceType", "غير محدد"),
                    naturalOrManMade = vcObj.optString("naturalOrManMade", "يحتاج إلى فحص سياقي"),
                    imageClarity = vcObj.optString("imageClarity", "غير محددة"),
                    colorsObserved = colors,
                    isSingleComposition = vcObj.optBoolean("isSingleComposition", false)
                )
            } else VisualContext()

            val detected = mutableListOf<DetectedSymbol>()
            val detectedArr = root.optJSONArray("detectedSymbols")
            if (detectedArr != null) {
                for (i in 0 until detectedArr.length()) {
                    val o = detectedArr.optJSONObject(i) ?: continue
                    detected.add(
                        DetectedSymbol(
                            symbol = o.optString("symbol", ""),
                            symbolType = o.optString("symbolType", "رمز"),
                            positionInImage = o.optString("positionInImage", "غير محدد"),
                            relativeSize = o.optString("relativeSize", "متوسط"),
                            orientation = o.optString("orientation", "عادي"),
                            isInverted = o.optBoolean("isInverted", false),
                            isFramed = o.optBoolean("isFramed", false),
                            repetitionCount = o.optInt("repetitionCount", 1),
                            recognitionClarity = o.optString("recognitionClarity", "متوسطة")
                        )
                    )
                }
            }

            val matches = mutableListOf<DictionaryMatch>()
            val matchArr = root.optJSONArray("dictionaryMatches")
            if (matchArr != null) {
                for (i in 0 until matchArr.length()) {
                    val o = matchArr.optJSONObject(i) ?: continue
                    matches.add(
                        DictionaryMatch(
                            symbol = o.optString("symbol", ""),
                            meaningInGuide = o.optString("meaningInGuide", ""),
                            ruleSource = o.optString("ruleSource", ""),
                            notes = o.optString("notes", ""),
                            isFoundInGuide = o.optBoolean("isFoundInGuide", true)
                        )
                    )
                }
            }

            val relations = readStringArray(root.optJSONArray("symbolRelationships"))

            val hypotheses = mutableListOf<InterpretationHypothesis>()
            val hypArr = root.optJSONArray("interpretations")
            if (hypArr != null) {
                for (i in 0 until hypArr.length()) {
                    val o = hypArr.optJSONObject(i) ?: continue
                    hypotheses.add(
                        InterpretationHypothesis(
                            title = o.optString("title", ""),
                            observation = o.optString("observation", ""),
                            deduction = o.optString("deduction", ""),
                            hypothesis = o.optString("hypothesis", ""),
                            rationale = o.optString("rationale", "")
                        )
                    )
                }
            }

            val conflicts = mutableListOf<ConflictItem>()
            val confArr = root.optJSONArray("conflicts")
            if (confArr != null) {
                for (i in 0 until confArr.length()) {
                    val o = confArr.optJSONObject(i) ?: continue
                    val altList = mutableListOf<AlternativeMeaning>()
                    val altArr = o.optJSONArray("meanings")
                    if (altArr != null) {
                        for (j in 0 until altArr.length()) {
                            val ao = altArr.optJSONObject(j) ?: continue
                            altList.add(
                                AlternativeMeaning(
                                    meaning = ao.optString("meaning", ""),
                                    context = ao.optString("context", ""),
                                    source = ao.optString("source", "")
                                )
                            )
                        }
                    }
                    conflicts.add(
                        ConflictItem(
                            symbol = o.optString("symbol", ""),
                            meanings = altList,
                            resolutionNote = o.optString("resolutionNote", "")
                        )
                    )
                }
            }

            val missing = readStringArray(root.optJSONArray("missingInformation"))

            val confObj = root.optJSONObject("confidence")
            val confidence = if (confObj != null) {
                ConfidenceAssessment(
                    visualRecognitionLevel = confObj.optString("visualRecognitionLevel", "متوسط"),
                    dictionaryMatchLevel = confObj.optString("dictionaryMatchLevel", "متوسط"),
                    overallInferenceLevel = confObj.optString("overallInferenceLevel", "منخفض"),
                    explanation = confObj.optString("explanation", "")
                )
            } else {
                ConfidenceAssessment("غير محدد", "غير محدد", "منخفض")
            }

            val warnings = mutableListOf<SafetyWarning>()
            val warnArr = root.optJSONArray("warnings")
            if (warnArr != null) {
                for (i in 0 until warnArr.length()) {
                    val o = warnArr.optJSONObject(i) ?: continue
                    val sev = try {
                        WarningSeverity.valueOf(o.optString("severity", WarningSeverity.METHODOLOGY_NOTICE.name))
                    } catch (_: Exception) {
                        WarningSeverity.METHODOLOGY_NOTICE
                    }
                    warnings.add(
                        SafetyWarning(
                            severity = sev,
                            title = o.optString("title", ""),
                            message = o.optString("message", "")
                        )
                    )
                }
            }

            AnalysisReport(
                caseSummary = root.optString("caseSummary", ""),
                visualContext = visualContext,
                isImageUnclear = root.optBoolean("isImageUnclear", false),
                clarityNote = root.optString("clarityNote", ""),
                detectedSymbols = detected,
                dictionaryMatches = matches,
                symbolRelationships = relations,
                interpretations = hypotheses,
                conflicts = conflicts,
                missingInformation = missing,
                confidence = confidence,
                warnings = warnings
            )
        } catch (_: Exception) {
            AnalysisReport(caseSummary = "تعذر قراءة تفاصيل التقرير المحفوظ.")
        }
    }

    private fun readStringArray(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
    }
}
