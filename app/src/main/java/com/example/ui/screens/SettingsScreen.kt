package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.AIProviderType
import com.example.domain.model.AppSettings
import com.example.ui.components.KonozTopBar
import com.example.ui.components.ManuscriptSectionCard
import com.example.ui.theme.CautionAmber

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onProviderChange: (AIProviderType) -> Unit,
    onGeminiModelChange: (String) -> Unit,
    onSaveProxy: (String, String) -> Unit,
    onTimeoutChange: (Int) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit
) {
    var proxyUrl by remember(settings.customProxyUrl) { mutableStateOf(settings.customProxyUrl) }
    var proxyToken by remember(settings.customProxyToken) { mutableStateOf(settings.customProxyToken) }
    var proxySavedNotice by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            KonozTopBar(
                title = "إعدادات التطبيق",
                subtitle = "تخصيص محرك الذكاء الاصطناعي والمظهر والاتصال",
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
                // 1. AI Provider Selection
                item {
                    ManuscriptSectionCard(
                        title = "مزود الذكاء الاصطناعي متعدد الوسائط",
                        icon = Icons.Default.Cloud
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            AIProviderType.entries.forEach { type ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    RadioButton(
                                        selected = settings.providerType == type,
                                        onClick = { onProviderChange(type) },
                                        modifier = Modifier.testTag("settings_provider_${type.name}")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = type.arabicName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (type == AIProviderType.FIREBASE_GEMINI) {
                                                "الافتراضي الآمن: يعمل عبر Firebase App Check دون الحاجة لتضمين مفتاح API في التطبيق."
                                            } else {
                                                "يتصل بخادم وسيط HTTPS خاص بك لحماية المفاتيح خارج ملف APK."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (settings.providerType == AIProviderType.FIREBASE_GEMINI) {
                                Text(
                                    text = "نموذج Gemini المستخدم:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = settings.geminiModel == "gemini-3.8-flash",
                                        onClick = { onGeminiModelChange("gemini-3.8-flash") },
                                        label = { Text("Gemini 3.8 Flash (موصى به)") }
                                    )
                                    FilterChip(
                                        selected = settings.geminiModel == "gemini-3.5-flash-lite",
                                        onClick = { onGeminiModelChange("gemini-3.5-flash-lite") },
                                        label = { Text("Gemini 3.5 Flash-Lite") }
                                    )
                                }
                            } else {
                                OutlinedTextField(
                                    value = proxyUrl,
                                    onValueChange = {
                                        proxyUrl = it
                                        proxySavedNotice = false
                                    },
                                    label = { Text("رابط الخادم الوسيط (HTTPS)") },
                                    placeholder = { Text("https://your-secure-proxy.example.com/analyze") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = proxyToken,
                                    onValueChange = {
                                        proxyToken = it
                                        proxySavedNotice = false
                                    },
                                    label = { Text("رمز الوصول الاختياري (Bearer Token)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        onSaveProxy(proxyUrl, proxyToken)
                                        proxySavedNotice = true
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("حفظ إعدادات الخادم الوسيط")
                                }
                                if (proxySavedNotice) {
                                    Text(
                                        text = "تم حفظ إعدادات الخادم الوسيط محلياً.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Security & Offline Transparency
                item {
                    ManuscriptSectionCard(
                        title = "أمان المفاتيح والعمل دون إنترنت",
                        icon = Icons.Default.Security,
                        accentColor = CautionAmber
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "• لا يضمّن التطبيق أي مفتاح API ثابت داخل الكود المصدري أو مستودع GitHub العام تجنباً لمخاطر تسريب المفاتيح عند فك حزمة APK.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "• القاموس المحلي (إدارة الدليل) وسجل التحليلات والمفضلة والمطابقة المحلية تعمل بالكامل دون اتصال بالإنترنت.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "• التحليل الذكي متعدد الوسائط للصور والنصوص يتطلب اتصالاً بالإنترنت.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CautionAmber,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 3. Timeout Configuration
                item {
                    ManuscriptSectionCard(
                        title = "مهلة انتظار التحليل (${settings.timeoutSeconds} ثانية)",
                        icon = Icons.Default.Timer
                    ) {
                        Slider(
                            value = settings.timeoutSeconds.toFloat(),
                            onValueChange = { onTimeoutChange(it.toInt()) },
                            valueRange = 30f..60f,
                            steps = 5
                        )
                        Text(
                            text = "نطاق المهلة بين 30 و60 ثانية لضمان تحليل الصور عالية التفاصيل دون تجميد الواجهة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 4. Appearance & Language Readiness
                item {
                    ManuscriptSectionCard(
                        title = "المظهر واللغة",
                        icon = Icons.Default.Palette
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "المظهر الداكن (المخطوطات القديمة)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "أو التبديل إلى مظهر الرق الفاتح الدافئ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.darkTheme,
                                onCheckedChange = onDarkThemeChange,
                                modifier = Modifier.testTag("settings_dark_theme_switch")
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "اللغة الأساسية: العربية (دعم كامل لاتجاه RTL)",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "بنية موارد النصوص مهيأة لإضافة الفرنسية والإنجليزية مستقبلاً.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
