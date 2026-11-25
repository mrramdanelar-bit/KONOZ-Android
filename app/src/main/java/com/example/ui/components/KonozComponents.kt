package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.SafetyWarning
import com.example.domain.model.WarningSeverity
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.CautionAmberContainer
import com.example.ui.theme.HazardCrimson
import com.example.ui.theme.HazardCrimsonContainer
import com.example.ui.theme.OnHazardCrimson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KonozTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun ManuscriptSectionCard(
    title: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun SafetyWarningBanner(
    warning: SafetyWarning,
    modifier: Modifier = Modifier
) {
    val (bg, border, iconTint, icon) = when (warning.severity) {
        WarningSeverity.CRITICAL_HAZARD -> Quadruple(
            HazardCrimsonContainer,
            HazardCrimson,
            HazardCrimson,
            Icons.Default.Warning
        )
        WarningSeverity.HERITAGE_ADVISORY -> Quadruple(
            CautionAmberContainer,
            CautionAmber,
            CautionAmber,
            Icons.Default.AccountBalance
        )
        WarningSeverity.METHODOLOGY_NOTICE -> Quadruple(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.outline,
            MaterialTheme.colorScheme.primary,
            Icons.Default.Info
        )
    }

    val textColor = when (warning.severity) {
        WarningSeverity.CRITICAL_HAZARD -> OnHazardCrimson
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = bg,
        border = BorderStroke(1.5.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = warning.title,
                tint = iconTint,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = warning.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = iconTint,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = warning.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun ErrorBannerCard(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = HazardCrimsonContainer,
        border = BorderStroke(1.dp, HazardCrimson)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "خطأ",
                    tint = HazardCrimson,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تنبيه",
                    style = MaterialTheme.typography.titleSmall,
                    color = HazardCrimson,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = OnHazardCrimson
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(10.dp))
                androidx.compose.material3.OutlinedButton(
                    onClick = onRetry,
                    border = BorderStroke(1.dp, HazardCrimson),
                    modifier = Modifier.testTag("error_retry_button")
                ) {
                    Text("إعادة المحاولة", color = OnHazardCrimson)
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
