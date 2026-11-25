package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AnalysisRecordEntity
import com.example.data.local.KnowledgeRuleEntity
import com.example.data.media.ImageLoadResult
import com.example.data.repository.AnalysisExecutionResult
import com.example.data.repository.KonozRepository
import com.example.domain.engine.SymbolComparisonEngine
import com.example.domain.model.AIProviderType
import com.example.domain.model.AppSettings
import com.example.domain.model.DetectedSymbol
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImageAnalysisUiState(
    val imagePaths: List<String> = emptyList(),
    val selectedIndex: Int = 0,
    val optionalDescription: String = "",
    val isCropping: Boolean = false,
    val isAnalyzing: Boolean = false,
    val currentStageIndex: Int = 0,
    val errorMessage: String? = null
)

data class TextAnalysisUiState(
    val description: String = "",
    val liveDetectedPreview: List<DetectedSymbol> = emptyList(),
    val isAnalyzing: Boolean = false,
    val errorMessage: String? = null
)

data class FollowUpUiState(
    val isSending: Boolean = false,
    val errorMessage: String? = null
)

class KonozViewModel(
    val repository: KonozRepository
) : ViewModel() {

    val allRules: StateFlow<List<KnowledgeRuleEntity>> = repository.allRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAnalyses: StateFlow<List<AnalysisRecordEntity>> = repository.allAnalyses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteAnalyses: StateFlow<List<AnalysisRecordEntity>> = repository.favoriteAnalyses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _imageState = MutableStateFlow(ImageAnalysisUiState())
    val imageState: StateFlow<ImageAnalysisUiState> = _imageState.asStateFlow()

    private val _textState = MutableStateFlow(TextAnalysisUiState())
    val textState: StateFlow<TextAnalysisUiState> = _textState.asStateFlow()

    private val _followUpState = MutableStateFlow(FollowUpUiState())
    val followUpState: StateFlow<FollowUpUiState> = _followUpState.asStateFlow()

    private val _knowledgeFeedback = MutableStateFlow<String?>(null)
    val knowledgeFeedback: StateFlow<String?> = _knowledgeFeedback.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureKnowledgeBaseSeeded()
        }
    }

    fun observeAnalysis(id: Long): Flow<AnalysisRecordEntity?> = repository.observeAnalysis(id)

    // ==================== Image Session Actions ====================

    fun addImagesFromUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            var firstError: String? = null
            val addedPaths = mutableListOf<String>()
            for (uri in uris) {
                when (val res = repository.imageStorageManager.importUriToLocalFile(uri)) {
                    is ImageLoadResult.Success -> addedPaths.add(res.filePath)
                    is ImageLoadResult.Error -> if (firstError == null) firstError = res.arabicMessage
                }
            }
            _imageState.update { state ->
                val merged = state.imagePaths + addedPaths
                state.copy(
                    imagePaths = merged,
                    selectedIndex = if (addedPaths.isNotEmpty()) merged.lastIndex else state.selectedIndex,
                    errorMessage = firstError
                )
            }
        }
    }

    fun addCapturedImagePath(path: String) {
        _imageState.update { state ->
            val merged = state.imagePaths + path
            state.copy(
                imagePaths = merged,
                selectedIndex = merged.lastIndex,
                errorMessage = null
            )
        }
    }

    fun selectImageIndex(index: Int) {
        _imageState.update { state ->
            if (index in state.imagePaths.indices) state.copy(selectedIndex = index, isCropping = false)
            else state
        }
    }

    fun removeImageAt(index: Int) {
        _imageState.update { state ->
            if (index !in state.imagePaths.indices) return@update state
            val updated = state.imagePaths.toMutableList().apply { removeAt(index) }
            state.copy(
                imagePaths = updated,
                selectedIndex = updated.lastIndex.coerceAtLeast(0),
                isCropping = false
            )
        }
    }

    fun toggleCropMode(enabled: Boolean) {
        _imageState.update { it.copy(isCropping = enabled, errorMessage = null) }
    }

    fun applyCropToSelectedImage(left: Float, top: Float, right: Float, bottom: Float) {
        val state = _imageState.value
        val currentPath = state.imagePaths.getOrNull(state.selectedIndex) ?: return
        viewModelScope.launch {
            when (val res = repository.imageStorageManager.cropNormalizedRegion(currentPath, left, top, right, bottom)) {
                is ImageLoadResult.Success -> {
                    _imageState.update { cur ->
                        val updated = cur.imagePaths.toMutableList()
                        if (cur.selectedIndex in updated.indices) {
                            updated[cur.selectedIndex] = res.filePath
                        }
                        cur.copy(imagePaths = updated, isCropping = false, errorMessage = null)
                    }
                }
                is ImageLoadResult.Error -> {
                    _imageState.update { it.copy(isCropping = false, errorMessage = res.arabicMessage) }
                }
            }
        }
    }

    fun updateImageDescription(desc: String) {
        _imageState.update { it.copy(optionalDescription = desc, errorMessage = null) }
    }

    fun clearImageError() {
        _imageState.update { it.copy(errorMessage = null) }
    }

    fun startImageAnalysis(
        useLocalOfflineOnly: Boolean = false,
        onSavedNavigate: (Long) -> Unit
    ) {
        val current = _imageState.value
        if (current.isAnalyzing) return

        viewModelScope.launch {
            _imageState.update { it.copy(isAnalyzing = true, currentStageIndex = 0, errorMessage = null) }

            val stageJob = launch {
                for (stage in 1..4) {
                    delay(1400L)
                    _imageState.update { s -> if (s.isAnalyzing) s.copy(currentStageIndex = stage) else s }
                }
            }

            val result = repository.runImageAnalysis(
                imagePaths = current.imagePaths,
                optionalDescription = current.optionalDescription,
                useLocalOfflineOnly = useLocalOfflineOnly
            )
            stageJob.cancel()

            when (result) {
                is AnalysisExecutionResult.Saved -> {
                    _imageState.update {
                        ImageAnalysisUiState() // Clean state after successful save
                    }
                    onSavedNavigate(result.recordId)
                }
                is AnalysisExecutionResult.Error -> {
                    // Preserve user images & description on failure!
                    _imageState.update {
                        it.copy(isAnalyzing = false, errorMessage = result.arabicMessage)
                    }
                }
            }
        }
    }

    // ==================== Text Description Actions ====================

    fun updateTextDescription(desc: String) {
        val rules = allRules.value
        val preview = if (desc.isBlank()) emptyList()
        else SymbolComparisonEngine.extractSymbolsFromText(desc, rules)
        _textState.update {
            it.copy(
                description = desc,
                liveDetectedPreview = preview,
                errorMessage = null
            )
        }
    }

    fun appendContextChipToText(chipText: String) {
        val cur = _textState.value.description.trim()
        val next = if (cur.isEmpty()) chipText else "$cur، $chipText"
        updateTextDescription(next)
    }

    fun startTextAnalysis(
        useLocalOfflineOnly: Boolean = false,
        onSavedNavigate: (Long) -> Unit
    ) {
        val current = _textState.value
        if (current.isAnalyzing) return

        viewModelScope.launch {
            _textState.update { it.copy(isAnalyzing = true, errorMessage = null) }
            when (val result = repository.runTextAnalysis(current.description, useLocalOfflineOnly)) {
                is AnalysisExecutionResult.Saved -> {
                    _textState.update { TextAnalysisUiState() }
                    onSavedNavigate(result.recordId)
                }
                is AnalysisExecutionResult.Error -> {
                    // Preserve user description on failure!
                    _textState.update { it.copy(isAnalyzing = false, errorMessage = result.arabicMessage) }
                }
            }
        }
    }

    // ==================== Report Follow-Up & Management ====================

    fun sendFollowUp(recordId: Long, newInfo: String, onDone: () -> Unit) {
        if (_followUpState.value.isSending) return
        viewModelScope.launch {
            _followUpState.update { it.copy(isSending = true, errorMessage = null) }
            val result = repository.submitFollowUp(recordId, newInfo)
            result.fold(
                onSuccess = {
                    _followUpState.update { FollowUpUiState(isSending = false) }
                    onDone()
                },
                onFailure = { err ->
                    _followUpState.update {
                        it.copy(
                            isSending = false,
                            errorMessage = err.message ?: "تعذر إرسال معلومة المتابعة."
                        )
                    }
                }
            )
        }
    }

    fun toggleFavorite(recordId: Long, favorite: Boolean) {
        viewModelScope.launch { repository.toggleFavorite(recordId, favorite) }
    }

    fun updateUserNotes(recordId: Long, notes: String) {
        viewModelScope.launch { repository.updateNotes(recordId, notes) }
    }

    fun deleteAnalysis(recordId: Long) {
        viewModelScope.launch { repository.deleteAnalysis(recordId) }
    }

    // ==================== Knowledge Base CRUD & JSON ====================

    fun saveKnowledgeRule(rule: KnowledgeRuleEntity) {
        viewModelScope.launch {
            repository.saveRule(rule)
            _knowledgeFeedback.value = "تم حفظ القاعدة (${rule.symbolKey}) في الدليل المحلي."
        }
    }

    fun toggleRuleEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            repository.setRuleEnabled(id, enabled)
            _knowledgeFeedback.value = if (enabled) "تم تفعيل القاعدة." else "تم تعطيل القاعدة مؤقتاً دون حذفها."
        }
    }

    fun exportKnowledgeJson(onJsonReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportKnowledgeBaseJson()
            onJsonReady(json)
        }
    }

    fun importKnowledgeJson(jsonText: String) {
        viewModelScope.launch {
            val res = repository.importKnowledgeBaseJson(jsonText)
            _knowledgeFeedback.value = res.fold(
                onSuccess = { it.message },
                onFailure = { it.message ?: "تعذر استعادة النسخة الاحتياطية." }
            )
        }
    }

    fun consumeKnowledgeFeedback() {
        _knowledgeFeedback.value = null
    }

    // ==================== Settings ====================

    fun updateProviderType(type: AIProviderType) = viewModelScope.launch { repository.updateProviderType(type) }
    fun updateGeminiModel(model: String) = viewModelScope.launch { repository.updateGeminiModel(model) }
    fun updateCustomProxy(url: String, token: String) = viewModelScope.launch { repository.updateCustomProxy(url, token) }
    fun updateTimeoutSeconds(sec: Int) = viewModelScope.launch { repository.updateTimeoutSeconds(sec) }
    fun updateDarkTheme(dark: Boolean) = viewModelScope.launch { repository.updateDarkTheme(dark) }
    fun updateLanguageCode(lang: String) = viewModelScope.launch { repository.updateLanguageCode(lang) }

    class Factory(private val repository: KonozRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return KonozViewModel(repository) as T
        }
    }
}
