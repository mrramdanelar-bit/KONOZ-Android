package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.domain.engine.SymbolComparisonEngine
import com.example.domain.model.SafetyWarning
import com.example.domain.model.WarningSeverity
import com.example.ui.components.KonozTopBar
import com.example.ui.components.ManuscriptSectionCard
import com.example.ui.components.SafetyWarningBanner
import com.example.ui.theme.HazardCrimson

@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            KonozTopBar(
                title = "معلومات التطبيق",
                subtitle = "KONOZ – كنوز • ميثاق السلامة والمنهجية",
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
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_konoz_emblem),
                            contentDescription = "شعار KONOZ",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(86.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "KONOZ – كنوز",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "الإصدار ${BuildConfig.VERSION_NAME} • الحزمة ${BuildConfig.APPLICATION_ID}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                item {
                    ManuscriptSectionCard(
                        title = "فكرة التطبيق ومنهجية التحليل",
                        icon = Icons.Default.Info
                    ) {
                        Text(
                            text = "تطبيق KONOZ – كنوز هو مساعد ذكي لتوثيق الصور والأوصاف النصية للرموز والنقوش ومقارنتها بقاعدة المعرفة المحلية (الدليل اليدوي لخرائط فكّ الرموز التكنيزية).\n\n" +
                                "• يميّز التقرير بوضوح بين الملاحظة الفعلية، والاستنتاج، والفرضية.\n" +
                                "• لا يختلق التطبيق رمزاً غير ظاهر، ولا يخمن الحروف في الصور غير الواضحة.\n" +
                                "• يعرض المعاني المتعارضة والبديلة دون اختيار عشوائي.\n" +
                                "• يفصل مستويات الثقة بالكلمات دون نسب مئوية توحي بدقة علمية غير موجودة.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                item {
                    ManuscriptSectionCard(
                        title = "ميثاق الأمان والمسؤولية (إلزامي)",
                        icon = Icons.Default.Shield,
                        accentColor = HazardCrimson
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "هذا التطبيق مخصص للتعرف على العلامات وتوثيقها ودراسة الفرضيات الواردة في مرجع المستخدم فقط، ولا يقدم أبداً:\n" +
                                    "• إحداثيات لمواقع دفائن.\n" +
                                    "• توجيهات ميدانية للحفر أو إزالة الصخور.\n" +
                                    "• حساب مسافات أو اتجاهات للوصول إلى أشياء مدفونة.\n" +
                                    "• تعليمات فتح القبور أو الغرف المدفونة.\n" +
                                    "• تعليمات تجاوز الرصد أو الفخاخ أو لمس الأسلاك والأجسام المشبوهة أو التعامل مع الألغام.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                            SafetyWarningBanner(
                                warning = SafetyWarning(
                                    severity = WarningSeverity.CRITICAL_HAZARD,
                                    title = "قاعدة التعامل مع علامات الخطر والألغام",
                                    message = SymbolComparisonEngine.MANDATORY_HAZARD_MESSAGE
                                )
                            )
                        }
                    }
                }

                item {
                    ManuscriptSectionCard(
                        title = "حماية التراث والمواقع الأثرية",
                        icon = Icons.Default.AccountBalance
                    ) {
                        SafetyWarningBanner(
                            warning = SafetyWarning(
                                severity = WarningSeverity.HERITAGE_ADVISORY,
                                title = "توصية التوثيق المسؤول",
                                message = SymbolComparisonEngine.MANDATORY_HERITAGE_MESSAGE
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SafetyWarningBanner(
                            warning = SafetyWarning(
                                severity = WarningSeverity.METHODOLOGY_NOTICE,
                                title = "إخلاء المسؤولية العلمية",
                                message = SymbolComparisonEngine.MANDATORY_METHODOLOGY_DISCLAIMER
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
