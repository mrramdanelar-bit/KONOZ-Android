package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AnalysisRecordEntity
import com.example.ui.components.KonozTopBar
import com.example.ui.theme.HazardCrimson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class HistoryFilter(val label: String) {
    ALL("الكل"),
    IMAGES("تحليل الصور"),
    TEXTS("الوصف النصي"),
    HAZARDS("تحذيرات الخطر")
}

@Composable
fun HistoryScreen(
    favoritesOnly: Boolean,
    analyses: List<AnalysisRecordEntity>,
    onBack: () -> Unit,
    onOpenReport: (Long) -> Unit,
    onToggleFavorite: (Long, Boolean) -> Unit,
    onDeleteRecord: (Long) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    var recordPendingDelete by remember { mutableStateOf<AnalysisRecordEntity?>(null) }

    val filtered = remember(analyses, searchQuery, selectedFilter) {
        analyses.filter { rec ->
            val matchesFilter = when (selectedFilter) {
                HistoryFilter.ALL -> true
                HistoryFilter.IMAGES -> rec.mode == "IMAGE"
                HistoryFilter.TEXTS -> rec.mode == "TEXT"
                HistoryFilter.HAZARDS -> rec.hasHazardWarning
            }
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                rec.title.contains(q, ignoreCase = true) ||
                rec.inputDescription.contains(q, ignoreCase = true) ||
                rec.detectedSymbolsSummary.contains(q, ignoreCase = true) ||
                rec.userNotes.contains(q, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            KonozTopBar(
                title = if (favoritesOnly) "المفضلة" else "سجل التحليلات",
                subtitle = "محفوظ محلياً ويعمل دون إنترنت (${filtered.size})",
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث بالرمز أو الوصف أو الملاحظات...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("history_search_input")
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(HistoryFilter.entries) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_empty_archive),
                                contentDescription = "أرشيف فارغ",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 220.dp, height = 165.dp)
                                    .clip(RoundedCornerShape(18.dp))
                            )
                            Text(
                                text = if (favoritesOnly) {
                                    "لا توجد تقارير محفوظة في المفضلة حالياً"
                                } else {
                                    "سجل التحليلات فارغ حالياً"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "تُحفظ جميع تقارير تحليل الصور والأوصاف النصية محلياً في قاعدة البيانات ويمكن الرجوع إليها في أي وقت دون إنترنت.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.id }) { record ->
                            val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                                .format(Date(record.createdAt))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onOpenReport(record.id) }
                                    .testTag("history_record_${record.id}"),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = BorderStroke(
                                    1.dp,
                                    if (record.hasHazardWarning) HazardCrimson
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (record.hasHazardWarning) Icons.Default.Warning
                                        else if (record.mode == "IMAGE") Icons.Default.CameraAlt
                                        else Icons.Default.Description,
                                        contentDescription = null,
                                        tint = if (record.hasHazardWarning) HazardCrimson
                                        else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = record.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = record.inputDescription,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "$dateStr • ${if (record.mode == "IMAGE") "صورة" else "وصف نصي"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { onToggleFavorite(record.id, !record.isFavorite) }) {
                                        Icon(
                                            imageVector = if (record.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = "تبديل المفضلة",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = { recordPendingDelete = record }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف السجل",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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

    recordPendingDelete?.let { pending ->
        AlertDialog(
            onDismissRequest = { recordPendingDelete = null },
            title = { Text("حذف سجل التحليل") },
            text = { Text("هل تريد حذف التقرير «${pending.title}» من السجل المحلي؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteRecord(pending.id)
                        recordPendingDelete = null
                    }
                ) {
                    Text("حذف", color = HazardCrimson)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordPendingDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
