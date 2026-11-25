package com.example.domain.model

import com.example.data.local.KnowledgeRuleEntity
import org.json.JSONArray
import org.json.JSONObject

sealed class KnowledgeImportResult {
    data class Success(val rules: List<KnowledgeRuleEntity>) : KnowledgeImportResult()
    data class Error(val arabicMessage: String) : KnowledgeImportResult()
}

object KnowledgeBackupCodec {

    private const val SCHEMA_VERSION = 1

    fun exportToJson(rules: List<KnowledgeRuleEntity>): String {
        val root = JSONObject()
        root.put("app", "KONOZ")
        root.put("schemaVersion", SCHEMA_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("count", rules.size)

        val arr = JSONArray()
        rules.forEach { r ->
            arr.put(
                JSONObject().apply {
                    put("symbolKey", r.symbolKey)
                    put("displayName", r.displayName)
                    put("category", r.category)
                    put("meaning", r.meaning)
                    put("alternativeMeaningsJson", r.alternativeMeaningsJson)
                    put("contextNotes", r.contextNotes)
                    put("ruleSource", r.ruleSource)
                    put("examples", r.examples)
                    put("isHazard", r.isHazard)
                    put("isHeritage", r.isHeritage)
                    put("isEnabled", r.isEnabled)
                    put("isUserModified", r.isUserModified)
                }
            )
        }
        root.put("rules", arr)
        return root.toString(2)
    }

    fun importFromJson(jsonText: String): KnowledgeImportResult {
        if (jsonText.isBlank()) {
            return KnowledgeImportResult.Error("ملف النسخة الاحتياطية فارغ.")
        }
        return try {
            val root = JSONObject(jsonText)
            val rulesArr = root.optJSONArray("rules")
                ?: return KnowledgeImportResult.Error("ملف JSON لا يحتوي على مصفوفة القواعد (rules) المتوقعة لدليل كنوز.")

            val parsed = mutableListOf<KnowledgeRuleEntity>()
            for (i in 0 until rulesArr.length()) {
                val obj = rulesArr.optJSONObject(i) ?: continue
                val key = obj.optString("symbolKey", "").trim()
                val meaning = obj.optString("meaning", "").trim()
                if (key.isBlank() || meaning.isBlank()) continue

                val categoryRaw = obj.optString("category", RuleCategory.GENERAL_RULES.name)
                val validCategory = try {
                    RuleCategory.valueOf(categoryRaw).name
                } catch (_: Exception) {
                    RuleCategory.GENERAL_RULES.name
                }

                parsed.add(
                    KnowledgeRuleEntity(
                        symbolKey = key,
                        displayName = obj.optString("displayName", key).ifBlank { key },
                        category = validCategory,
                        meaning = meaning,
                        alternativeMeaningsJson = obj.optString("alternativeMeaningsJson", "[]").ifBlank { "[]" },
                        contextNotes = obj.optString("contextNotes", ""),
                        ruleSource = obj.optString("ruleSource", "نسخة مستوردة (JSON)"),
                        examples = obj.optString("examples", ""),
                        isHazard = obj.optBoolean("isHazard", false),
                        isHeritage = obj.optBoolean("isHeritage", false),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        isUserModified = obj.optBoolean("isUserModified", true),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            if (parsed.isEmpty()) {
                KnowledgeImportResult.Error("لم يتم العثور على قواعد صالحة داخل ملف JSON المستورد.")
            } else {
                KnowledgeImportResult.Success(parsed)
            }
        } catch (_: Exception) {
            KnowledgeImportResult.Error("تعذر استعادة النسخة الاحتياطية؛ تأكد من أن ملف JSON سليم ومتوافق مع دليل كنوز.")
        }
    }
}
