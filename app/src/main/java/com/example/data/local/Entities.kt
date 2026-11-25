package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_rules")
data class KnowledgeRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val symbolKey: String,
    val displayName: String,
    val category: String, // RuleCategory.name
    val meaning: String,
    val alternativeMeaningsJson: String = "[]",
    val contextNotes: String = "",
    val ruleSource: String = "الدليل اليدوي لخرائط فكّ الرموز التكنيزية",
    val examples: String = "",
    val isHazard: Boolean = false,
    val isHeritage: Boolean = false,
    val isEnabled: Boolean = true,
    val isUserModified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "analysis_records")
data class AnalysisRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val mode: String, // "IMAGE" or "TEXT"
    val inputDescription: String,
    val imagePathsJson: String = "[]",
    val detectedSymbolsSummary: String = "",
    val reportJson: String,
    val followUpMessagesJson: String = "[]",
    val userNotes: String = "",
    val hasHazardWarning: Boolean = false,
    val isFavorite: Boolean = false,
    val providerUsed: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
