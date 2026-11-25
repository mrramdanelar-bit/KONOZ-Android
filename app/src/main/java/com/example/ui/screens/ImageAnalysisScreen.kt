package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.KonozTopBar
import com.example.ui.components.ManuscriptSectionCard
import com.example.ui.viewmodel.ImageAnalysisUiState
import java.io.File

private val analysisStages = listOf(
    "المرحلة 1: الفحص البصري واستخراج العلامات الظاهرة",
    "المرحلة 2: التعرف على السياق والسطح والألوان",
    "المرحلة 3: مطابقة قاعدة المعرفة والدليل اليدوي",
    "المرحلة 4: التحليل المنطقي ورصد التعارضات",
    "المرحلة 5: إعداد التقرير المفصل وتحذيرات السلامة"
)

@Composable
fun ImageAnalysisScreen(
    state: ImageAnalysisUiState,
    onBack: () -> Unit,
    onOpenCamera: () -> Unit,
    onAddImageUris: (List<android.net.Uri>) -> Unit,
    onSelectImage: (Int) -> Unit,
    onRemoveImage: (Int) -> Unit,
    onToggleCrop: (Boolean) -> Unit,
    onApplyCrop: (Float, Float, Float, Float) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onStartAnalysis: (Boolean) -> Unit
) {
    val singlePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onAddImageUris(listOf(uri))
    }

    val multiPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        if (uris.isNotEmpty()) onAddImageUris(uris)
    }

    // Normalized crop range state (0f..1f)
    var horizontalCrop by remember(state.selectedIndex, state.isCropping) { mutableStateOf(0.1f..0.9f) }
    var verticalCrop by remember(state.selectedIndex, state.isCropping) { mutableStateOf(0.1f..0.9f) }

    Scaffold(
        topBar = {
            KonozTopBar(
                title = "تحليل صورة",
                subtitle = "الوضع الأول — فحص بصري متعدد الوسائط ومطابقة الدليل",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Source Selection Buttons
                item {
                    ManuscriptSectionCard(
                        title = "إدخال صور الحالة",
                        icon = Icons.Default.AddPhotoAlternate
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onOpenCamera,
                                enabled = !state.isAnalyzing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("image_open_camera_button")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("التقاط صورة بالكاميرا (CameraX)")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        singlePicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    enabled = !state.isAnalyzing,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("image_pick_single_button")
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("اختيار صورة")
                                }
                                OutlinedButton(
                                    onClick = {
                                        multiPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    enabled = !state.isAnalyzing,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("image_pick_multiple_button")
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("عدة صور")
                                }
                            }
                        }
                    }
                }

                // 2. Selected Image Preview + Crop Region Selector + Filmstrip
                if (state.imagePaths.isNotEmpty()) {
                    val activePath = state.imagePaths.getOrNull(state.selectedIndex)
                    item {
                        ManuscriptSectionCard(
                            title = "معاينة الصورة (${state.selectedIndex + 1} من ${state.imagePaths.size})",
                            icon = Icons.Default.Crop
                        ) {
                            if (activePath != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(4f / 3f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.Black)
                                ) {
                                    AsyncImage(
                                        model = File(activePath),
                                        contentDescription = "الصورة المختارة للتحليل",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (state.isCropping) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val left = horizontalCrop.start * size.width
                                            val right = horizontalCrop.endInclusive * size.width
                                            val top = verticalCrop.start * size.height
                                            val bottom = verticalCrop.endInclusive * size.height

                                            // Dim outside crop rectangle
                                            drawRect(Color.Black.copy(alpha = 0.55f), Offset.Zero, Size(size.width, top))
                                            drawRect(Color.Black.copy(alpha = 0.55f), Offset(0f, bottom), Size(size.width, size.height - bottom))
                                            drawRect(Color.Black.copy(alpha = 0.55f), Offset(0f, top), Size(left, bottom - top))
                                            drawRect(Color.Black.copy(alpha = 0.55f), Offset(right, top), Size(size.width - right, bottom - top))

                                            // Golden selection border
                                            drawRect(
                                                color = Color(0xFFD9AE5B),
                                                topLeft = Offset(left, top),
                                                size = Size(right - left, bottom - top),
                                                style = Stroke(width = 4f)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (!state.isCropping) {
                                    OutlinedButton(
                                        onClick = { onToggleCrop(true) },
                                        enabled = !state.isAnalyzing,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("image_start_crop_button")
                                    ) {
                                        Icon(Icons.Default.Crop, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("قص الصورة أو تحديد منطقة الرمز")
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "حدود القص الأفقية (يسار – يمين)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        RangeSlider(
                                            value = horizontalCrop,
                                            onValueChange = { range ->
                                                if (range.endInclusive - range.start >= 0.12f) {
                                                    horizontalCrop = range
                                                }
                                            },
                                            valueRange = 0f..1f
                                        )
                                        Text(
                                            text = "حدود القص الرأسية (أعلى – أسفل)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        RangeSlider(
                                            value = verticalCrop,
                                            onValueChange = { range ->
                                                if (range.endInclusive - range.start >= 0.12f) {
                                                    verticalCrop = range
                                                }
                                            },
                                            valueRange = 0f..1f
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    onApplyCrop(
                                                        horizontalCrop.start,
                                                        verticalCrop.start,
                                                        horizontalCrop.endInclusive,
                                                        verticalCrop.endInclusive
                                                    )
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("image_apply_crop_button")
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("اعتماد القص")
                                            }
                                            OutlinedButton(
                                                onClick = { onToggleCrop(false) },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("إلغاء")
                                            }
                                        }
                                    }
                                }
                            }

                            if (state.imagePaths.size > 1) {
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    itemsIndexed(state.imagePaths) { idx, path ->
                                        val selected = idx == state.selectedIndex
                                        Box(
                                            modifier = Modifier
                                                .size(74.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(
                                                    width = if (selected) 2.5.dp else 1.dp,
                                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable { onSelectImage(idx) }
                                        ) {
                                            AsyncImage(
                                                model = File(path),
                                                contentDescription = "صورة رقم ${idx + 1}",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Surface(
                                                shape = CircleShape,
                                                color = Color.Black.copy(alpha = 0.7f),
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(3.dp)
                                                    .size(22.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { onRemoveImage(idx) },
                                                    modifier = Modifier.size(22.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "حذف الصورة",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Optional Description
                item {
                    ManuscriptSectionCard(
                        title = "وصف اختياري مصاحب للصورة",
                        icon = Icons.Default.Search
                    ) {
                        OutlinedTextField(
                            value = state.optionalDescription,
                            onValueChange = onDescriptionChange,
                            enabled = !state.isAnalyzing,
                            placeholder = {
                                Text("مثال: النقش على صخرة كبيرة، الرمز مقلوب، ويوجد حرف بجانبه...")
                            },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("image_optional_description_input")
                        )
                    }
                }

                // 4. Error Banner (Preserving Data!)
                if (!state.errorMessage.isNullOrBlank()) {
                    item {
                        ErrorBannerCard(
                            message = state.errorMessage,
                            onRetry = { onStartAnalysis(false) }
                        )
                    }
                }

                // 5. Multi-stage Progress Indicator
                if (state.isAnalyzing) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "جاري تنفيذ التحليل متعدد المراحل...",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { (state.currentStageIndex + 1) / analysisStages.size.toFloat() },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = analysisStages.getOrElse(state.currentStageIndex) { analysisStages.last() },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // 6. Action Buttons
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onStartAnalysis(false) },
                            enabled = state.imagePaths.isNotEmpty() && !state.isAnalyzing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("image_start_ai_analysis_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (state.isAnalyzing) "جاري التحليل..." else "بدء التحليل الذكي للصور",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        if (state.imagePaths.isNotEmpty() && state.optionalDescription.isNotBlank()) {
                            OutlinedButton(
                                onClick = { onStartAnalysis(true) },
                                enabled = !state.isAnalyzing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("image_local_match_button")
                            ) {
                                Text("مطابقة محلية فورية للوصف (دون إنترنت)")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
