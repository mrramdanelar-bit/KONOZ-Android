package com.example.data.ai

import android.graphics.Bitmap
import com.example.data.local.KnowledgeRuleEntity
import com.example.domain.model.AnalysisReport
import com.example.domain.model.FollowUpMessage

sealed class AIResult<out T> {
    data class Success<T>(val data: T, val providerLabel: String) : AIResult<T>()
    data class Failure(val userArabicMessage: String, val technicalDetail: String? = null) : AIResult<Nothing>()
}

interface AIService {
    val providerLabel: String

    suspend fun analyzeImages(
        bitmaps: List<Bitmap>,
        optionalDescription: String,
        activeRules: List<KnowledgeRuleEntity>,
        timeoutMs: Long
    ): AIResult<AnalysisReport>

    suspend fun analyzeTextDescription(
        description: String,
        activeRules: List<KnowledgeRuleEntity>,
        timeoutMs: Long
    ): AIResult<AnalysisReport>

    suspend fun continueFollowUp(
        originalDescription: String,
        currentReport: AnalysisReport,
        history: List<FollowUpMessage>,
        newUserInfo: String,
        activeRules: List<KnowledgeRuleEntity>,
        timeoutMs: Long
    ): AIResult<String>
}
