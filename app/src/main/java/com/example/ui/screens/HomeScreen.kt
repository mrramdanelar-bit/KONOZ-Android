package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AnalysisRecordEntity
import com.example.ui.theme.HazardCrimson
import com.example.ui.theme.ObsidianManuscript
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    rulesCount: Int,
    analyses: List<AnalysisRecordEntity>,
    favoritesCount: Int,
    onOpenImageAnalysis: () -> Unit,
    onOpenTextAnalysis: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenKnowledgeBase: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenReport: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Logo Header — KONOZ – كنوز
            item {
                KonozHeroHeader(
                    rulesCount = rulesCount,
                    analysesCount = analyses.size,
                    favoritesCount = favoritesCount
                )
            }

            // 2 & 3. Primary Modes: Image Analysis & Text Description Analysis
            item {
                Text(
                    text = "أوضاع التحليل الأساسية",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PrimaryModeCard(
                        title = "تحليل صورة",
                        subtitle = "كاميرا أو معرض، قص وتحديد، وفحص بصري ذكي",
                        icon = Icons.Default.CameraAlt,
                        isPrimaryGold = true,
                        onClick = onOpenImageAnalysis,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_image_analysis_card")
                    )
                    PrimaryModeCard(
                        title = "التحليل بالوصف",
                        subtitle = "صف الرموز نصياً مع محادثة متابعة تفاعلية",
                        icon = Icons.Default.Description,
                        isPrimaryGold = false,
                        onClick = onOpenTextAnalysis,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_text_analysis_card")
                    )
                }
            }

            // 4, 5, 6, 7, 8. Secondary Quick Actions Grid
            item {
                Text(
                    text = "الأرشيف والدليل والإعدادات",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SecondaryActionTile(
                            title = "سجل التحليلات",
                            badge = "${analyses.size}",
                            icon = Icons.Default.History,
                            onClick = onOpenHistory,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_history_button")
                        )
                        SecondaryActionTile(
                            title = "قاعدة الرموز",
                            badge = "$rulesCount",
                            icon = Icons.Default.MenuBook,
                            onClick = onOpenKnowledgeBase,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_knowledge_button")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SecondaryActionTile(
                            title = "المفضلة",
                            badge = "$favoritesCount",
                            icon = Icons.Default.Bookmark,
                            onClick = onOpenFavorites,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_favorites_button")
                        )
                        SecondaryActionTile(
                            title = "الإعدادات",
                            badge = null,
                            icon = Icons.Default.Settings,
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_settings_button")
                        )
                    }
                    // 8. معلومات التطبيق
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onOpenAbout)
                            .testTag("home_about_button"),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "معلومات التطبيق",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "معلومات التطبيق وميثاق السلامة",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "المنهجية العلمية، قواعد السلامة، والفرق بين الملاحظة والفرضية",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Recent Analyses Strip
            if (analyses.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "أحدث التحليلات المحفوظة",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "عرض الكل",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onOpenHistory)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                items(analyses.take(3), key = { it.id }) { record ->
                    RecentAnalysisItemCard(
                        record = record,
                        onClick = { onOpenReport(record.id) }
                    )
                }
            }

            // Methodological Disclaimer Footer
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = "تنبيه: القواعد المحفوظة في الدليل اليدوي هي فرضيات مرجعية خاصة بالمستخدم لأغراض التوثيق والدراسة، ولا تُعد مرجعاً أثرياً علمياً مثبتاً أو توجيهاً للحفر.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun KonozHeroHeader(
    rulesCount: Int,
    analysesCount: Int,
    favoritesCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_konoz_logo_card"),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_manuscript_banner),
                contentDescription = "خلفية مخطوطة كنوز",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                ObsidianManuscript.copy(alpha = 0.45f),
                                ObsidianManuscript.copy(alpha = 0.90f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_konoz_emblem),
                        contentDescription = "شعار KONOZ",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                    )
                    Column {
                        Text(
                            text = "KONOZ – كنوز",
                            style = MaterialTheme.typography.displayMedium,
                            color = Color(0xFFF4EBD9)
                        )
                        Text(
                            text = "مساعد تحليل الرموز والنقوش ومطابقة الدليل اليدوي",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD9AE5B)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroStatChip(label = "قواعد الدليل", value = "$rulesCount", modifier = Modifier.weight(1f))
                    HeroStatChip(label = "التحليلات", value = "$analysesCount", modifier = Modifier.weight(1f))
                    HeroStatChip(label = "المفضلة", value = "$favoritesCount", modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HeroStatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E1711).copy(alpha = 0.85f),
        border = BorderStroke(1.dp, Color(0xFF6B553B))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFD9AE5B),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFC2B5A0),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PrimaryModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isPrimaryGold: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isPrimaryGold) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val borderColor = if (isPrimaryGold) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondary
    }
    Card(
        modifier = modifier
            .height(166.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.2.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.35f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SecondaryActionTile(
    title: String,
    badge: String?,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentAnalysisItemCard(
    record: AnalysisRecordEntity,
    onClick: () -> Unit
) {
    val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(record.createdAt))
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            1.dp,
            if (record.hasHazardWarning) HazardCrimson else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (record.hasHazardWarning) Icons.Default.Warning
                else if (record.mode == "IMAGE") Icons.Default.CameraAlt
                else Icons.Default.Description,
                contentDescription = null,
                tint = if (record.hasHazardWarning) HazardCrimson else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$dateStr • ${if (record.mode == "IMAGE") "صورة" else "وصف نصي"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
