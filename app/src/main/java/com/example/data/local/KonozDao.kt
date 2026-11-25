package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KonozDao {

    // --- Knowledge Base Rules ---

    @Query("SELECT * FROM knowledge_rules ORDER BY isEnabled DESC, category ASC, symbolKey ASC")
    fun observeAllRules(): Flow<List<KnowledgeRuleEntity>>

    @Query("SELECT * FROM knowledge_rules WHERE isEnabled = 1 ORDER BY category ASC, symbolKey ASC")
    suspend fun getEnabledRulesOnce(): List<KnowledgeRuleEntity>

    @Query("SELECT * FROM knowledge_rules ORDER BY id ASC")
    suspend fun getAllRulesOnce(): List<KnowledgeRuleEntity>

    @Query("SELECT COUNT(*) FROM knowledge_rules")
    suspend fun countRules(): Int

    @Query("SELECT * FROM knowledge_rules WHERE LOWER(symbolKey) = LOWER(:symbolKey) LIMIT 1")
    suspend fun findRuleBySymbolKey(symbolKey: String): KnowledgeRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: KnowledgeRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<KnowledgeRuleEntity>)

    @Update
    suspend fun updateRule(rule: KnowledgeRuleEntity)

    @Query("UPDATE knowledge_rules SET isEnabled = :enabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setRuleEnabled(id: Long, enabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    // --- Analysis History Records ---

    @Query("SELECT * FROM analysis_records ORDER BY createdAt DESC")
    fun observeAllAnalyses(): Flow<List<AnalysisRecordEntity>>

    @Query("SELECT * FROM analysis_records WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun observeFavoriteAnalyses(): Flow<List<AnalysisRecordEntity>>

    @Query("SELECT * FROM analysis_records WHERE id = :id")
    fun observeAnalysisById(id: Long): Flow<AnalysisRecordEntity?>

    @Query("SELECT * FROM analysis_records WHERE id = :id")
    suspend fun getAnalysisByIdOnce(id: Long): AnalysisRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(record: AnalysisRecordEntity): Long

    @Update
    suspend fun updateAnalysis(record: AnalysisRecordEntity)

    @Query("UPDATE analysis_records SET isFavorite = :favorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setAnalysisFavorite(id: Long, favorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE analysis_records SET userNotes = :notes, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateAnalysisNotes(id: Long, notes: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM analysis_records WHERE id = :id")
    suspend fun deleteAnalysisById(id: Long)
}
