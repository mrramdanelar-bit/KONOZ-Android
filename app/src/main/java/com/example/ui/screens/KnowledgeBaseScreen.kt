package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.KnowledgeRuleEntity
import com.example.domain.model.AlternativeMeaning
import com.example.domain.model.ReportJsonCodec
import com.example.domain.model.RuleCategory
import com.example.ui.components.KonozTopBar
import com.example.ui.theme.HazardCrimson

@Composable
fun KnowledgeBaseScreen(
    rules: List<KnowledgeRuleEntity>,
    feedbackMessage: String?,
    onConsumeFeedback: () -> Unit,
    onBack: () -> Unit,
    onSaveRule: (KnowledgeRuleEntity) -> Unit,
    onToggleEnabled: (Long, Boolean) -> Unit,
    onExportJson: ((String) -> Unit) -> Unit,
    onImportJson: (String) -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<RuleCategory?>(null) }
    var editingRule by remember { mutableStateOf<KnowledgeRuleEntity?>(null) }
    var pendingExportJson by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(feedbackMessage) {
        if (!feedbackMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(feedbackMessage)
            onConsumeFeedback()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val payload = pendingExportJson
        if (uri != null && payload != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(payload.toByteArray(Charsets.UTF_8))
                }
            } catch (_: Exception) {
            }
        }
        pendingExportJson = null
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                if (text != null) {
                    onImportJson(text)
                }
            } catch (_: Exception) {
                onImportJson("")
            }
        }
    }

    val filteredRules = remember(rules, searchQuery, selectedCategory) {
        rules.filter { r ->
            val catOk = selectedCategory == null || r.category == selectedCategory!!.name
            val q = searchQuery.trim()
            val qOk = q.isEmpty() ||
                r.symbolKey.contains(q, ignoreCase = true) ||
                r.displayName.contains(q, ignoreCase = true) ||
                r.meaning.contains(q, ignoreCase = true) ||
                r.contextNotes.contains(q, ignoreCase = true)
            catOk && qOk
        }
    }

    Scaffold(
        topBar = {
            KonozTopBar(
                title = "إدارة الدليل",
                subtitle = "قاعدة المعرفة المحلية (${rules.count { it.isEnabled }} مفعلة / ${rules.size} إجمالاً)",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = {
                            onExportJson { json ->
                                pendingExportJson = json
                                exportLauncher.launch("konoz_knowledge_backup.json")
                            }
                        },
                        modifier = Modifier.testTag("kb_export_json_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = "تصدير JSON",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) },
                        modifier = Modifier.testTag("kb_import_json_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "استيراد JSON",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingRule = KnowledgeRuleEntity(
                        id = 0L,
                        symbolKey = "",
                        displayName = "",
                        category = (selectedCategory ?: RuleCategory.LETTERS).name,
                        meaning = "",
                        ruleSource = "إضافة المستخدم"
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("kb_add_rule_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة رمز جديد")
            }
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
                    .widthIn(max = 700.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن رمز أو حرف أو رقم أو معنى في الدليل...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("kb_search_input")
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("الكل (${rules.size})") }
                        )
                    }
                    items(RuleCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.arabicLabel) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredRules, key = { it.id }) { rule ->
                        val catLabel = try {
                            RuleCategory.valueOf(rule.category).arabicLabel
                        } catch (_: Exception) {
                            rule.category
                        }
                        val alts = remember(rule.alternativeMeaningsJson) {
                            ReportJsonCodec.decodeAlternativeMeanings(rule.alternativeMeaningsJson)
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editingRule = rule }
                                .testTag("kb_rule_card_${rule.symbolKey}"),
                            shape = RoundedCornerShape(16.dp),
                            color = if (rule.isEnabled) {
                                MaterialTheme.colorScheme.surfaceContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            border = BorderStroke(
                                1.dp,
                                if (rule.isHazard) HazardCrimson.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = rule.symbolKey,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = rule.displayName,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = catLabel + if (!rule.isEnabled) " • معطّلة" else "",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (rule.isHazard) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = "مؤشر خطر",
                                                tint = HazardCrimson,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        IconButton(onClick = { editingRule = rule }) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "تعديل القاعدة",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Switch(
                                            checked = rule.isEnabled,
                                            onCheckedChange = { onToggleEnabled(rule.id, it) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = rule.meaning,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (rule.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (alts.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "معانٍ بديلة مسجلة (${alts.size}): ${alts.joinToString(" | ") { it.meaning }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                if (rule.contextNotes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "ملاحظة: ${rule.contextNotes}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (rule.examples.isNotBlank()) {
                                    Text(
                                        text = "أمثلة: ${rule.examples}",
                                        style = MaterialTheme.typography.labelSmall,
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

    editingRule?.let { current ->
        RuleEditorDialog(
            initialRule = current,
            onDismiss = { editingRule = null },
            onSave = { updated ->
                onSaveRule(updated)
                editingRule = null
            }
        )
    }
}

@Composable
private fun RuleEditorDialog(
    initialRule: KnowledgeRuleEntity,
    onDismiss: () -> Unit,
    onSave: (KnowledgeRuleEntity) -> Unit
) {
    var symbolKey by remember { mutableStateOf(initialRule.symbolKey) }
    var displayName by remember { mutableStateOf(initialRule.displayName) }
    var meaning by remember { mutableStateOf(initialRule.meaning) }
    var contextNotes by remember { mutableStateOf(initialRule.contextNotes) }
    var examples by remember { mutableStateOf(initialRule.examples) }
    var ruleSource by remember { mutableStateOf(initialRule.ruleSource) }
    var isHazard by remember { mutableStateOf(initialRule.isHazard) }
    var isHeritage by remember { mutableStateOf(initialRule.isHeritage) }
    var selectedCategory by remember {
        mutableStateOf(
            try {
                RuleCategory.valueOf(initialRule.category)
            } catch (_: Exception) {
                RuleCategory.GENERAL_RULES
            }
        )
    }

    var alternatives by remember {
        mutableStateOf(ReportJsonCodec.decodeAlternativeMeanings(initialRule.alternativeMeaningsJson))
    }
    var newAltMeaning by remember { mutableStateOf("") }
    var newAltContext by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialRule.id == 0L) "إضافة رمز جديد للدليل" else "تعديل قاعدة الدليل")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = symbolKey,
                    onValueChange = { symbolKey = it },
                    label = { Text("الرمز أو المفتاح (مثال: Q أو 41)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("اسم العرض") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = meaning,
                    onValueChange = { meaning = it },
                    label = { Text("المعنى الوارد في الدليل") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contextNotes,
                    onValueChange = { contextNotes = it },
                    label = { Text("ملاحظات السياق") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = examples,
                    onValueChange = { examples = it },
                    label = { Text("أمثلة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ruleSource,
                    onValueChange = { ruleSource = it },
                    label = { Text("مصدر القاعدة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("التصنيف:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(RuleCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.arabicLabel, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                HorizontalDivider()
                Text(
                    text = "المعاني البديلة (${alternatives.size}):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                alternatives.forEach { alt ->
                    Text(
                        text = "• ${alt.meaning} (${alt.context})",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedTextField(
                    value = newAltMeaning,
                    onValueChange = { newAltMeaning = it },
                    label = { Text("إضافة معنى بديل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newAltContext,
                    onValueChange = { newAltContext = it },
                    label = { Text("سياق المعنى البديل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = {
                        if (newAltMeaning.isNotBlank()) {
                            alternatives = alternatives + AlternativeMeaning(
                                meaning = newAltMeaning.trim(),
                                context = newAltContext.trim().ifBlank { "سياق مضاف من المستخدم" },
                                source = ruleSource.ifBlank { "إضافة المستخدم" }
                            )
                            newAltMeaning = ""
                            newAltContext = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إدراج المعنى البديل")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isHazard, onCheckedChange = { isHazard = it })
                    Text("مؤشر خطر ميداني محتمل (يُظهر تحذير سلامة)", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isHeritage, onCheckedChange = { isHeritage = it })
                    Text("معلم ديني أو قبر أو أثر محتمل", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (symbolKey.isNotBlank() && meaning.isNotBlank()) {
                        onSave(
                            initialRule.copy(
                                symbolKey = symbolKey.trim(),
                                displayName = displayName.trim().ifBlank { symbolKey.trim() },
                                category = selectedCategory.name,
                                meaning = meaning.trim(),
                                alternativeMeaningsJson = ReportJsonCodec.encodeAlternativeMeanings(alternatives),
                                contextNotes = contextNotes.trim(),
                                examples = examples.trim(),
                                ruleSource = ruleSource.trim().ifBlank { "إضافة المستخدم" },
                                isHazard = isHazard,
                                isHeritage = isHeritage
                            )
                        )
                    }
                },
                enabled = symbolKey.isNotBlank() && meaning.isNotBlank()
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
