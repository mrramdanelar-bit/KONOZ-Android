package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.AnalysisRecordEntity
import com.example.domain.model.AnalysisReport
import com.example.domain.model.ReportJsonCodec
import com.example.domain.model.WarningSeverity
import com.example.ui.components.KonozTopBar
import com.example.ui.components.ManuscriptSectionCard
import com.example.ui.components.SafetyWarningBanner
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.HazardCrimson
import com.example.ui.theme.SandstoneSage
import com.example.ui.viewmodel.FollowUpUiState
import java.io.File

private val quickFollowUps = listOf(
    "الرمز مقلوب",
    "توجد شجرة بجانبه",
    "الحروف متباعدة",
    "العلامة محفورة داخل مربع"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportDetailScreen(
    record: AnalysisRecordEntity?,
    followUpState: FollowUpUiState,
    onBack: () -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit,
    onSaveNotes: (Long, String) -> Unit,
    onSendFollowUp: (Long, String, () -> Unit) -> Unit
) {
    val context = LocalContext.current

    if (record == null) {
        Scaffold(
            topBar = { KonozTopBar(title = "تقرير التحليل", onBack = onBack) }
        ) { pad ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pad),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val report = remember(record.reportJson) { ReportJsonCodec.decodeReport(record.reportJson) }
    val imagePaths = remember(record.imagePathsJson) { ReportJsonCodec.decodeStringList(record.imagePathsJson) }
    val followUpHistory = remember(record.followUpMessagesJson) { ReportJsonCodec.decodeFollowUpMessages(record.followUpMessagesJson) }

    var notesText by remember(record.id, record.userNotes) { mutableStateOf(record.userNotes) }
    var followUpInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            KonozTopBar(
                title = "تقرير التحليل",
                subtitle = record.providerUsed,
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { onToggleFavorite(record.id, !record.isFavorite) },
                        modifier = Modifier.testTag("report_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (record.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "المفضلة",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, record.title)
                                putExtra(Intent.EXTRA_TEXT, buildPlainTextReport(record, report))
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة تقرير KONOZ"))
                        },
                        modifier = Modifier.testTag("report_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "مشاركة التقرير",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
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
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Immediate Top Critical Hazard Warning if present!
                val criticalWarnings = report.warnings.filter { it.severity == WarningSeverity.CRITICAL_HAZARD }
                items(criticalWarnings) { warn ->
                    SafetyWarningBanner(
                        warning = warn,
                        modifier = Modifier.testTag("report_critical_hazard_banner")
                    )
                }

                // Attached Images Strip
                if (imagePaths.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(imagePaths) { path ->
                                AsyncImage(
                                    model = File(path),
                                    contentDescription = "صورة الحالة",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(width = 180.dp, height = 135.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                )
                            }
                        }
                    }
                }

                // Section 1: ملخص الحالة + السياق البصري
                item {
                    ManuscriptSectionCard(
                        title = "1. ملخص الحالة والسياق",
                        icon = Icons.Default.Visibility
                    ) {
                        Text(
                            text = report.caseSummary,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (report.isImageUnclear && report.clarityNote.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = report.clarityNote,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CautionAmber,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ContextMetaRow("السطح:", report.visualContext.surfaceType)
                                ContextMetaRow("طبيعة العلامة:", report.visualContext.naturalOrManMade)
                                ContextMetaRow("وضوح الرؤية:", report.visualContext.imageClarity)
                                if (report.visualContext.colorsObserved.isNotEmpty()) {
                                    ContextMetaRow("الألوان المرصودة:", report.visualContext.colorsObserved.joinToString("، "))
                                }
                            }
                        }
                    }
                }

                // Section 2: الرموز المكتشفة (Table View)
                item {
                    ManuscriptSectionCard(
                        title = "2. الرموز المكتشفة (${report.detectedSymbols.size})",
                        icon = Icons.Default.FactCheck
                    ) {
                        if (report.detectedSymbols.isEmpty()) {
                            Text(
                                text = "لم يتم رصد رموز مقروءة بوضوح كافٍ. لم يتم تخمين أي رمز.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                report.detectedSymbols.forEach { sym ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = sym.symbol,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = sym.symbolType,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                            ContextMetaRow("الموضع:", sym.positionInImage)
                                            ContextMetaRow("الحجم النسبي:", sym.relativeSize)
                                            ContextMetaRow(
                                                "الاتجاه والوضع:",
                                                buildString {
                                                    append(sym.orientation)
                                                    if (sym.isInverted && !sym.orientation.contains("مقلوب")) append(" (مقلوب)")
                                                    if (sym.isFramed) append(" • داخل مربع/إطار")
                                                    if (sym.repetitionCount > 1) append(" • مكرر ×${sym.repetitionCount}")
                                                }
                                            )
                                            ContextMetaRow("درجة وضوح التعرف:", sym.recognitionClarity)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 3: المطابقة مع الدليل (Table View)
                item {
                    ManuscriptSectionCard(
                        title = "3. المطابقة مع الدليل اليدوي",
                        icon = Icons.Default.MenuBook
                    ) {
                        if (report.dictionaryMatches.isEmpty()) {
                            Text(
                                text = "لا توجد مطابقات مسجلة في هذه الحالة.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                report.dictionaryMatches.forEach { m ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        border = BorderStroke(
                                            1.dp,
                                            if (m.isFoundInGuide) SandstoneSage.copy(alpha = 0.6f)
                                            else CautionAmber.copy(alpha = 0.6f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "الرمز: ${m.symbol}",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (m.isFoundInGuide) MaterialTheme.colorScheme.primary else CautionAmber,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "المعنى في الدليل: ${m.meaningInGuide}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "المصدر: ${m.ruleSource}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (m.notes.isNotBlank()) {
                                                Text(
                                                    text = "ملاحظات: ${m.notes}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 4: العلاقات بين الرموز
                item {
                    ManuscriptSectionCard(
                        title = "4. العلاقات بين الرموز",
                        icon = Icons.Default.Hub
                    ) {
                        if (report.symbolRelationships.isEmpty()) {
                            Text(
                                text = "لا توجد علاقات مركبة بارزة بين الرموز في المعطيات الحالية.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                report.symbolRelationships.forEach { rel ->
                                    Text(
                                        text = "• $rel",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 5: الاحتمالات التفسيرية (Observation vs Deduction vs Hypothesis)
                item {
                    ManuscriptSectionCard(
                        title = "5. الاحتمالات التفسيرية",
                        icon = Icons.Default.Lightbulb
                    ) {
                        if (report.interpretations.isEmpty()) {
                            Text(
                                text = "تم تعليق الاحتمالات التفسيرية لعدم كفاية الوضوح أو المعطيات.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                report.interpretations.forEach { h ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = h.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (h.observation.isNotBlank()) {
                                                LabeledParagraph("الملاحظة الفعلية:", h.observation)
                                            }
                                            if (h.deduction.isNotBlank()) {
                                                LabeledParagraph("الاستنتاج:", h.deduction)
                                            }
                                            if (h.hypothesis.isNotBlank()) {
                                                LabeledParagraph("الفرضية:", h.hypothesis)
                                            }
                                            if (h.rationale.isNotBlank()) {
                                                LabeledParagraph("سبب الاحتمال:", h.rationale)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 6: التعارضات والبدائل
                item {
                    ManuscriptSectionCard(
                        title = "6. التعارضات وتعدد المعاني",
                        icon = Icons.Default.CompareArrows,
                        accentColor = if (report.conflicts.isNotEmpty()) CautionAmber else MaterialTheme.colorScheme.primary
                    ) {
                        if (report.conflicts.isEmpty()) {
                            Text(
                                text = "لم يُرصد تعارض مباشر بين معاني الرموز المكتشفة في الدليل الحالي.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                report.conflicts.forEach { c ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        border = BorderStroke(1.dp, CautionAmber.copy(alpha = 0.6f))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "الرمز متعدد المعاني: ${c.symbol}",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = CautionAmber,
                                                fontWeight = FontWeight.Bold
                                            )
                                            c.meanings.forEachIndexed { idx, alt ->
                                                Text(
                                                    text = "${idx + 1}. ${alt.meaning} — السياق: ${alt.context} (${alt.source})",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                            Text(
                                                text = c.resolutionNote,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = CautionAmber,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 7: المعلومات الناقصة
                item {
                    ManuscriptSectionCard(
                        title = "7. المعلومات الناقصة والأسئلة المقترحة",
                        icon = Icons.Default.HelpOutline
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            report.missingInformation.forEach { q ->
                                Text(
                                    text = "• $q",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Section 8: مستوى الثقة المنفصل (بدون نسب مئوية)
                item {
                    ManuscriptSectionCard(
                        title = "8. مستوى الثقة (دون نسب مئوية)",
                        icon = Icons.Default.Shield
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ConfidenceRow("التعرف البصري على الرمز:", report.confidence.visualRecognitionLevel)
                            ConfidenceRow("المطابقة مع قاموس الدليل:", report.confidence.dictionaryMatchLevel)
                            ConfidenceRow("الثقة في الاستنتاج العام:", report.confidence.overallInferenceLevel)
                            if (report.confidence.explanation.isNotBlank()) {
                                Text(
                                    text = report.confidence.explanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Section 9: التحذيرات وإخلاء المسؤولية
                item {
                    ManuscriptSectionCard(
                        title = "9. التحذيرات والمسؤولية",
                        icon = Icons.Default.Shield,
                        accentColor = if (record.hasHazardWarning) HazardCrimson else MaterialTheme.colorScheme.primary
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            report.warnings.forEach { w ->
                                SafetyWarningBanner(warning = w)
                            }
                        }
                    }
                }

                // Interactive Follow-Up Conversation (Section 5)
                item {
                    ManuscriptSectionCard(
                        title = "محادثة المتابعة وإعادة التقييم",
                        icon = Icons.Default.ChatBubbleOutline
                    ) {
                        Text(
                            text = "أضف معلومات جديدة (مثل اتجاه الرمز أو وجود شجرة أو مربع) ليعيد التطبيق تقييم التفسير دون تجاهل المعطيات السابقة:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (followUpHistory.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                followUpHistory.forEach { msg ->
                                    val isUser = msg.role == "user"
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isUser) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = if (isUser) "إضافة من المستخدم" else "إعادة تقييم KONOZ",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = msg.content,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            quickFollowUps.forEach { chip ->
                                SuggestionChip(
                                    onClick = {
                                        followUpInput = if (followUpInput.isBlank()) chip else "$followUpInput، $chip"
                                    },
                                    label = { Text(chip, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = followUpInput,
                                onValueChange = { followUpInput = it },
                                placeholder = { Text("أضف معلومة جديدة...") },
                                enabled = !followUpState.isSending,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("report_followup_input")
                            )
                            Button(
                                onClick = {
                                    val payload = followUpInput.trim()
                                    if (payload.isNotBlank()) {
                                        onSendFollowUp(record.id, payload) {
                                            followUpInput = ""
                                        }
                                    }
                                },
                                enabled = followUpInput.isNotBlank() && !followUpState.isSending,
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("report_followup_send_button")
                            ) {
                                if (followUpState.isSending) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال")
                                }
                            }
                        }
                        if (!followUpState.errorMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = followUpState.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = HazardCrimson
                            )
                        }
                    }
                }

                // Editable User Notes persisted in Room
                item {
                    ManuscriptSectionCard(
                        title = "ملاحظاتك الشخصية على الحالة",
                        icon = Icons.Default.NoteAlt
                    ) {
                        OutlinedTextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            placeholder = { Text("سجّل ملاحظاتك التوثيقية الخاصة بهذه الحالة...") },
                            minLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("report_notes_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { onSaveNotes(record.id, notesText) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("report_save_notes_button")
                        ) {
                            Text("حفظ الملاحظات")
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun ContextMetaRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun LabeledParagraph(label: String, body: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ConfidenceRow(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.45f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(0.55f)
            )
        }
    }
}

private fun buildPlainTextReport(record: AnalysisRecordEntity, report: AnalysisReport): String = buildString {
    appendLine("=== تقرير KONOZ – كنوز ===")
    appendLine("العنوان: ${record.title}")
    appendLine("1. ملخص الحالة: ${report.caseSummary}")
    appendLine()
    appendLine("2. الرموز المكتشفة:")
    report.detectedSymbols.forEach {
        appendLine("- ${it.symbol} (${it.symbolType}) | الاتجاه: ${it.orientation} | الوضوح: ${it.recognitionClarity}")
    }
    appendLine()
    appendLine("3. المطابقة مع الدليل:")
    report.dictionaryMatches.forEach {
        appendLine("- ${it.symbol}: ${it.meaningInGuide} [${it.ruleSource}]")
    }
    if (report.conflicts.isNotEmpty()) {
        appendLine()
        appendLine("6. التعارضات:")
        report.conflicts.forEach { appendLine("- ${it.symbol}: ${it.resolutionNote}") }
    }
    appendLine()
    appendLine("8. مستوى الثقة:")
    appendLine("- التعرف البصري: ${report.confidence.visualRecognitionLevel}")
    appendLine("- مطابقة الدليل: ${report.confidence.dictionaryMatchLevel}")
    appendLine("- الاستنتاج العام: ${report.confidence.overallInferenceLevel}")
    appendLine()
    appendLine("9. التحذيرات:")
    report.warnings.forEach { appendLine("! ${it.title}: ${it.message}") }
}
