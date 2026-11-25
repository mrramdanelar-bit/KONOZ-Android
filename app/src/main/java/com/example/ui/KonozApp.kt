package com.example.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.CameraCaptureScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImageAnalysisScreen
import com.example.ui.screens.KnowledgeBaseScreen
import com.example.ui.screens.ReportDetailScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TextAnalysisScreen
import com.example.ui.theme.KonozTheme
import com.example.ui.viewmodel.KonozViewModel

object KonozRoutes {
    const val HOME = "home"
    const val IMAGE_ANALYSIS = "image_analysis"
    const val CAMERA_CAPTURE = "camera_capture"
    const val TEXT_ANALYSIS = "text_analysis"
    const val HISTORY = "history"
    const val FAVORITES = "favorites"
    const val KNOWLEDGE_BASE = "knowledge_base"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val REPORT_DETAIL = "report_detail/{recordId}"

    fun reportDetail(recordId: Long) = "report_detail/$recordId"
}

@Composable
fun KonozApp(viewModel: KonozViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val rules by viewModel.allRules.collectAsStateWithLifecycle()
    val analyses by viewModel.allAnalyses.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteAnalyses.collectAsStateWithLifecycle()
    val imageState by viewModel.imageState.collectAsStateWithLifecycle()
    val textState by viewModel.textState.collectAsStateWithLifecycle()
    val followUpState by viewModel.followUpState.collectAsStateWithLifecycle()
    val kbFeedback by viewModel.knowledgeFeedback.collectAsStateWithLifecycle()

    KonozTheme(
        darkTheme = settings.darkTheme,
        isRtl = settings.languageCode == "ar"
    ) {
        val navController = rememberNavController()

        Surface(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = KonozRoutes.HOME
            ) {
                composable(KonozRoutes.HOME) {
                    HomeScreen(
                        rulesCount = rules.count { it.isEnabled },
                        analyses = analyses,
                        favoritesCount = favorites.size,
                        onOpenImageAnalysis = { navController.navigate(KonozRoutes.IMAGE_ANALYSIS) },
                        onOpenTextAnalysis = { navController.navigate(KonozRoutes.TEXT_ANALYSIS) },
                        onOpenHistory = { navController.navigate(KonozRoutes.HISTORY) },
                        onOpenKnowledgeBase = { navController.navigate(KonozRoutes.KNOWLEDGE_BASE) },
                        onOpenFavorites = { navController.navigate(KonozRoutes.FAVORITES) },
                        onOpenSettings = { navController.navigate(KonozRoutes.SETTINGS) },
                        onOpenAbout = { navController.navigate(KonozRoutes.ABOUT) },
                        onOpenReport = { id -> navController.navigate(KonozRoutes.reportDetail(id)) },
                        modifier = Modifier.systemBarsPadding()
                    )
                }

                composable(KonozRoutes.IMAGE_ANALYSIS) {
                    ImageAnalysisScreen(
                        state = imageState,
                        onBack = { navController.popBackStack() },
                        onOpenCamera = { navController.navigate(KonozRoutes.CAMERA_CAPTURE) },
                        onAddImageUris = { uris -> viewModel.addImagesFromUris(uris) },
                        onSelectImage = { idx -> viewModel.selectImageIndex(idx) },
                        onRemoveImage = { idx -> viewModel.removeImageAt(idx) },
                        onToggleCrop = { enabled -> viewModel.toggleCropMode(enabled) },
                        onApplyCrop = { l, t, r, b -> viewModel.applyCropToSelectedImage(l, t, r, b) },
                        onDescriptionChange = { desc -> viewModel.updateImageDescription(desc) },
                        onStartAnalysis = { localOnly ->
                            viewModel.startImageAnalysis(useLocalOfflineOnly = localOnly) { savedId ->
                                navController.navigate(KonozRoutes.reportDetail(savedId))
                            }
                        }
                    )
                }

                composable(KonozRoutes.CAMERA_CAPTURE) {
                    CameraCaptureScreen(
                        imageStorageManager = viewModel.repository.imageStorageManager,
                        onImageCaptured = { path ->
                            viewModel.addCapturedImagePath(path)
                            navController.popBackStack()
                        },
                        onClose = { navController.popBackStack() }
                    )
                }

                composable(KonozRoutes.TEXT_ANALYSIS) {
                    TextAnalysisScreen(
                        state = textState,
                        onBack = { navController.popBackStack() },
                        onDescriptionChange = { viewModel.updateTextDescription(it) },
                        onAppendModifier = { viewModel.appendContextChipToText(it) },
                        onStartAnalysis = { localOnly ->
                            viewModel.startTextAnalysis(useLocalOfflineOnly = localOnly) { savedId ->
                                navController.navigate(KonozRoutes.reportDetail(savedId))
                            }
                        }
                    )
                }

                composable(
                    route = KonozRoutes.REPORT_DETAIL,
                    arguments = listOf(navArgument("recordId") { type = NavType.LongType })
                ) { entry ->
                    val recordId = entry.arguments?.getLong("recordId") ?: 0L
                    val record by viewModel.observeAnalysis(recordId).collectAsStateWithLifecycle(initialValue = null)
                    ReportDetailScreen(
                        record = record,
                        followUpState = followUpState,
                        onBack = { navController.popBackStack() },
                        onToggleFavorite = { id, fav -> viewModel.toggleFavorite(id, fav) },
                        onSaveNotes = { id, notes -> viewModel.updateUserNotes(id, notes) },
                        onSendFollowUp = { id, info, onDone -> viewModel.sendFollowUp(id, info, onDone) }
                    )
                }

                composable(KonozRoutes.HISTORY) {
                    HistoryScreen(
                        favoritesOnly = false,
                        analyses = analyses,
                        onBack = { navController.popBackStack() },
                        onOpenReport = { id -> navController.navigate(KonozRoutes.reportDetail(id)) },
                        onToggleFavorite = { id, fav -> viewModel.toggleFavorite(id, fav) },
                        onDeleteRecord = { id -> viewModel.deleteAnalysis(id) }
                    )
                }

                composable(KonozRoutes.FAVORITES) {
                    HistoryScreen(
                        favoritesOnly = true,
                        analyses = favorites,
                        onBack = { navController.popBackStack() },
                        onOpenReport = { id -> navController.navigate(KonozRoutes.reportDetail(id)) },
                        onToggleFavorite = { id, fav -> viewModel.toggleFavorite(id, fav) },
                        onDeleteRecord = { id -> viewModel.deleteAnalysis(id) }
                    )
                }

                composable(KonozRoutes.KNOWLEDGE_BASE) {
                    KnowledgeBaseScreen(
                        rules = rules,
                        feedbackMessage = kbFeedback,
                        onConsumeFeedback = { viewModel.consumeKnowledgeFeedback() },
                        onBack = { navController.popBackStack() },
                        onSaveRule = { viewModel.saveKnowledgeRule(it) },
                        onToggleEnabled = { id, en -> viewModel.toggleRuleEnabled(id, en) },
                        onExportJson = { cb -> viewModel.exportKnowledgeJson(cb) },
                        onImportJson = { json -> viewModel.importKnowledgeJson(json) }
                    )
                }

                composable(KonozRoutes.SETTINGS) {
                    SettingsScreen(
                        settings = settings,
                        onBack = { navController.popBackStack() },
                        onProviderChange = { viewModel.updateProviderType(it) },
                        onGeminiModelChange = { viewModel.updateGeminiModel(it) },
                        onSaveProxy = { u, t -> viewModel.updateCustomProxy(u, t) },
                        onTimeoutChange = { viewModel.updateTimeoutSeconds(it) },
                        onDarkThemeChange = { viewModel.updateDarkTheme(it) }
                    )
                }

                composable(KonozRoutes.ABOUT) {
                    AboutScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
