package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.KonozTopBar
import com.example.ui.components.ManuscriptSectionCard
import com.example.ui.viewmodel.TextAnalysisUiState

private val exampleDescriptions = listOf(
    "مثال الدليل (L, I, 4, نقطة)" to "وجدت حرف L على صخرة، بجانبه حرف I، وتحته رقم 4، وهناك نقطة كبيرة قرب الحروف.",
    "تركيب L + I + B" to "تظهر على صخرة علامات مركبة: L + I + B متجاورة.",
    "تركيب تدريبي E + 5 + T" to "نقش يحتوي على E + 5 + T على سطح صخري.",
    "تعارض حرف S" to "يوجد حرف S محفور بوضوح على صخرة دون علامات مالية مرافقة.",
    "رمز مقلوب وخطر" to "حرف A مقلوب وبجانبه مثلث به خط وأسلاك معدنية."
)

private val contextModifiers = listOf(
    "الرمز مقلوب",
    "العلامة محفورة داخل مربع",
    "توجد شجرة بجانبه",
    "الحروف متباعدة",
    "على صخرة كبيرة",
    "على جدار قديم",
    "يوجد لون أزرق"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextAnalysisScreen(
    state: TextAnalysisUiState,
    onBack: () -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAppendModifier: (String) -> Unit,
    onStartAnalysis: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            KonozTopBar(
                title = "التحليل بالوصف النصي",
                subtitle = "الوضع الثاني — استخراج الرموز والمطابقة مع الدليل",
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
                // 1. Description Input
                item {
                    ManuscriptSectionCard(
                        title = "صف العلامات والرموز التي شاهدتها",
                        icon = Icons.Default.Description
                    ) {
                        OutlinedTextField(
                            value = state.description,
                            onValueChange = onDescriptionChange,
                            enabled = !state.isAnalyzing,
                            placeholder = {
                                Text("مثال: وجدت حرف L على صخرة، بجانبه حرف I، وتحته رقم 4، وهناك نقطة كبيرة قرب الحروف.")
                            },
                            minLines = 5,
                            maxLines = 10,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("text_description_input")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "إضافة تفاصيل سياقية سريعة:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            contextModifiers.forEach { mod ->
                                SuggestionChip(
                                    onClick = { onAppendModifier(mod) },
                                    label = { Text(mod, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }

                // 2. Live Local Extracted Symbols Preview
                if (state.liveDetectedPreview.isNotEmpty()) {
                    item {
                        ManuscriptSectionCard(
                            title = "معاينة فورية للرموز المستخرجة (${state.liveDetectedPreview.size})",
                            icon = Icons.Default.MenuBook
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.liveDetectedPreview.forEach { sym ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = sym.symbol,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = sym.symbolType,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Example Prompts
                item {
                    ManuscriptSectionCard(
                        title = "أمثلة سريعة للتجربة والتدريب",
                        icon = Icons.Default.Tune
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            exampleDescriptions.forEach { (label, text) ->
                                AssistChip(
                                    onClick = { onDescriptionChange(text) },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }

                // 4. Error Banner
                if (!state.errorMessage.isNullOrBlank()) {
                    item {
                        ErrorBannerCard(
                            message = state.errorMessage,
                            onRetry = { onStartAnalysis(false) }
                        )
                    }
                }

                // 5. Submit Buttons
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onStartAnalysis(false) },
                            enabled = state.description.isNotBlank() && !state.isAnalyzing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("text_start_ai_analysis_button")
                        ) {
                            if (state.isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("جاري التحليل ومطابقة الدليل...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تحليل ذكي شامل (AI + الدليل)",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { onStartAnalysis(true) },
                            enabled = state.description.isNotBlank() && !state.isAnalyzing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("text_local_match_button")
                        ) {
                            Text("مطابقة محلية فورية عبر القاموس (دون إنترنت)")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
