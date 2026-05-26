package com.lulucloud.touchscript.feature.editor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lulucloud.touchscript.common.AutomationLauncher
import com.lulucloud.touchscript.common.canDrawOverlay
import com.lulucloud.touchscript.common.isAccessibilityServiceEnabled
import com.lulucloud.touchscript.common.openAccessibilitySettings
import com.lulucloud.touchscript.common.openOverlaySettings
import com.lulucloud.touchscript.core.automation.CoordinateCaptureManager
import com.lulucloud.touchscript.core.automation.CoordinateCaptureService

@Composable
fun EditorScreen(viewModel: EditorViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current
    var editorValue by remember { mutableStateOf(TextFieldValue(uiState.currentSource)) }
    val editorScroll = rememberScrollState()
    val resultScroll = rememberScrollState()
    val menuScroll = rememberScrollState()
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showFileMenu by remember { mutableStateOf(false) }
    var showInsertMenu by remember { mutableStateOf(false) }
    var selectedInsertGroup by remember { mutableStateOf<InsertOperationGroup?>(null) }
    var showEditMenu by remember { mutableStateOf(false) }
    var resultExpanded by remember { mutableStateOf(false) }
    var pendingSaveName by remember { mutableStateOf<String?>(null) }
    var selectedInsertOperation by remember { mutableStateOf<InsertOperationType?>(null) }
    val lineCount = editorValue.text.lines().size.coerceAtLeast(1)
    val charCount = editorValue.text.length
    val editorBackground = Color(0xFF11161D)
    val gutterBackground = Color(0xFF171D26)
    val chromeBackground = MaterialTheme.colorScheme.surface
    val subtleChrome = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    val resultLabel = when {
        uiState.compileError != null -> "编译失败"
        uiState.generatedLua.isNotBlank() -> "编译成功"
        else -> "未编译"
    }
    val resultSummary = when {
        uiState.compileError != null -> uiState.compileError ?: "编译失败"
        uiState.generatedLua.isNotBlank() -> uiState.compileMessage
        else -> ""
    }
    val resultColor = when {
        uiState.compileError != null -> MaterialTheme.colorScheme.error
        uiState.generatedLua.isNotBlank() -> Color(0xFF69C08A)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    LaunchedEffect(uiState.currentSource, uiState.currentFilePath) {
        if (uiState.currentSource != editorValue.text) {
            editorValue = TextFieldValue(
                text = uiState.currentSource,
                selection = TextRange(uiState.currentSource.length)
            )
        }
    }

    val openFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        val uri = data?.data ?: return@rememberLauncherForActivityResult
        if (result.resultCode == Activity.RESULT_OK) {
            val flags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }
            viewModel.openFile(uri.toString())
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        val uri = data?.data
        val targetName = pendingSaveName
        pendingSaveName = null
        if (result.resultCode == Activity.RESULT_OK && uri != null && targetName != null) {
            val flags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }
            viewModel.saveAsToLocation(targetName, uri.toString())
        }
    }

    val pickWorkspaceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        val uri = data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            val flags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }
            viewModel.configureScriptWorkspace(uri.toString()) { workspaceUri ->
                pendingSaveName?.let { saveName ->
                    createDocumentLauncher.launch(buildCreateScriptIntent(saveName, workspaceUri))
                }
            }
        } else {
            pendingSaveName = null
        }
    }

    fun requestCreateDocument(fileName: String) {
        pendingSaveName = fileName
        val workspaceUri = uiState.scriptWorkspaceUri
        if (workspaceUri.isNullOrBlank()) {
            pickWorkspaceLauncher.launch(buildOpenScriptTreeIntent())
        } else {
            createDocumentLauncher.launch(buildCreateScriptIntent(fileName, workspaceUri))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(editorBackground)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = chromeBackground,
            tonalElevation = 0.dp,
            shadowElevation = 2.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        EditorFileTab(fileName = uiState.currentScriptName)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(onClick = viewModel::compileCurrentScript) {
                        Text("编译")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            when {
                                !context.canDrawOverlay() -> {
                                    Toast.makeText(context, "请先开启悬浮窗权限", Toast.LENGTH_SHORT).show()
                                    context.openOverlaySettings()
                                }

                                !context.isAccessibilityServiceEnabled() -> {
                                    Toast.makeText(context, "请先开启无障碍服务", Toast.LENGTH_SHORT).show()
                                    context.openAccessibilitySettings()
                                }

                                else -> {
                                    viewModel.prepareDebugDraft {
                                        AutomationLauncher.showOverlay(context)
                                    }
                                }
                            }
                        }
                    ) {
                        Text("调试")
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(menuScroll)
                        .background(subtleChrome)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorDropdownMenu(
                        text = "文件",
                        expanded = showFileMenu,
                        onExpandedChange = { showFileMenu = it }
                    ) {
                        DropdownMenuItem(
                            text = { Text("新建") },
                            onClick = {
                                showFileMenu = false
                                showNewFileDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("打开文件") },
                            onClick = {
                                showFileMenu = false
                                openFileLauncher.launch(buildOpenScriptIntent(uiState.scriptWorkspaceUri))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("保存") },
                            onClick = {
                                showFileMenu = false
                                val currentPath = uiState.currentFilePath
                                if (currentPath.isNullOrBlank()) {
                                    requestCreateDocument(uiState.currentScriptName)
                                } else {
                                    viewModel.saveCurrentToLocation(currentPath)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("另存为") },
                            onClick = {
                                showFileMenu = false
                                showSaveAsDialog = true
                            }
                        )
                    }
                    EditorDropdownMenu(
                        text = "编辑",
                        expanded = showEditMenu,
                        onExpandedChange = { showEditMenu = it }
                    ) {
                        DropdownMenuItem(
                            text = { Text("撤销") },
                            onClick = {
                                showEditMenu = false
                                viewModel.undo()
                            }
                        )
                    }
                    EditorDropdownMenu(
                        text = "插入操作",
                        expanded = showInsertMenu,
                        onExpandedChange = {
                            showInsertMenu = it
                            if (!it) {
                                selectedInsertGroup = null
                            }
                        }
                    ) {
                        InsertOperationGroup.entries.forEach { group ->
                            Box {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(group.label)
                                            Text(">")
                                        }
                                    },
                                    onClick = {
                                        selectedInsertGroup = if (selectedInsertGroup == group) null else group
                                    }
                                )
                                DropdownMenu(
                                    expanded = selectedInsertGroup == group,
                                    onDismissRequest = {
                                        selectedInsertGroup = null
                                    },
                                    offset = DpOffset(x = 124.dp, y = (-14).dp)
                                ) {
                                    group.operations.forEach { operation ->
                                        DropdownMenuItem(
                                            text = { Text(operation.label) },
                                            onClick = {
                                                showInsertMenu = false
                                                selectedInsertGroup = null
                                                selectedInsertOperation = operation
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF0D1117),
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${normalizeEditorScriptName(uiState.currentScriptName)}.tscript",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8EA0B5)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "$lineCount 行  $charCount 字",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8EA0B5)
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val keyboardBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
            val scrollContentMinHeight = maxHeight + keyboardBottomPadding + 320.dp
            val cursorLineIndex = remember(editorValue.text, editorValue.selection.start) {
                cursorLineIndex(
                    text = editorValue.text,
                    cursor = editorValue.selection.start
                )
            }

            LaunchedEffect(cursorLineIndex, keyboardBottomPadding, editorScroll.maxValue) {
                if (keyboardBottomPadding > 0.dp) {
                    val target = with(density) {
                        (cursorLineIndex * EDITOR_LINE_HEIGHT_SP.sp.toPx() - 140.dp.toPx()).toInt()
                    }.coerceIn(0, editorScroll.maxValue)
                    editorScroll.animateScrollTo(target)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(editorScroll)
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(scrollContentMinHeight)
                        .background(gutterBackground)
                        .padding(horizontal = 2.dp, vertical = 12.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Text(
                        text = buildLineNumbers(uiState.currentSource),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF627287),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(scrollContentMinHeight)
                        .background(editorBackground)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (editorValue.text.isBlank()) {
                        Text(
                            text = "记录 \"开始执行\"\n点击 540 1600",
                            style = TextStyle(
                                color = Color(0xFF5A6E83),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                lineHeight = 22.sp
                            )
                        )
                    }

                    BasicTextField(
                        value = editorValue,
                        onValueChange = { value ->
                            val nextValue = applyEditorInputBehaviors(
                                previousValue = editorValue,
                                nextValue = value
                            )
                            editorValue = nextValue
                            viewModel.updateSource(nextValue.text)
                        },
                        textStyle = TextStyle(
                            color = Color(0xFFF1F4F8),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        ),
                        visualTransformation = DslSyntaxHighlightTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = scrollContentMinHeight)
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = chromeBackground,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { resultExpanded = !resultExpanded }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("编译结果", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = resultLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = resultColor
                    )
                    if (resultSummary.isNotBlank()) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = resultSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (resultExpanded) "收起" else "展开",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (resultExpanded) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (resultSummary.isNotBlank()) {
                            Text(
                                text = resultSummary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        uiState.compileError?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (uiState.generatedLua.isNotBlank()) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 180.dp),
                                color = subtleChrome,
                                tonalElevation = 0.dp
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = uiState.generatedLua,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 20.sp
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .verticalScroll(resultScroll)
                                            .padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewFileDialog) {
        FileNameDialog(
            title = "新建脚本",
            initialName = "",
            confirmText = "创建",
            onDismiss = { showNewFileDialog = false },
            onConfirm = {
                viewModel.createNewFile(it)
                showNewFileDialog = false
            }
        )
    }

    if (showSaveAsDialog) {
        FileNameDialog(
            title = "另存为",
            initialName = uiState.currentScriptName,
            confirmText = "保存",
            onDismiss = { showSaveAsDialog = false },
            onConfirm = {
                showSaveAsDialog = false
                requestCreateDocument(it)
            }
        )
    }

    selectedInsertOperation?.let { operation ->
        InsertOperationDialog(
            operation = operation,
            onImportImage = { uri, fileName, onResult ->
                viewModel.importRecognitionImage(uri, fileName, onResult)
            },
            onEnsureImagesDirectory = { onResult ->
                viewModel.ensureRecognitionImagesDirectory(onResult)
            },
            onDismiss = { selectedInsertOperation = null },
            onInsert = { insertion ->
                val updated = insertSnippetAtSelection(
                    currentValue = editorValue,
                    insertion = insertion
                )
                editorValue = updated
                viewModel.updateSource(updated.text)
                selectedInsertOperation = null
            }
        )
    }
}

@Composable
private fun EditorFileTab(
    fileName: String
) {
    Text(
        text = "${normalizeEditorScriptName(fileName)}.tscript",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun EditorDropdownMenu(
    text: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Box {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .clickable { onExpandedChange(!expanded) }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            content()
        }
    }
}

@Composable
private fun InsertOperationDialog(
    operation: InsertOperationType,
    onImportImage: (Uri, String, (Result<String>) -> Unit) -> Unit,
    onEnsureImagesDirectory: ((Result<String>) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onInsert: (InsertOperationPayload) -> Unit
) {
    val context = LocalContext.current
    var fieldA by remember(operation) { mutableStateOf(operation.defaultFieldA) }
    var fieldB by remember(operation) { mutableStateOf(operation.defaultFieldB) }
    var fieldC by remember(operation) { mutableStateOf(operation.defaultFieldC) }
    var fieldD by remember(operation) { mutableStateOf(operation.defaultFieldD) }
    var fieldE by remember(operation) { mutableStateOf(operation.defaultFieldE) }
    var findTextUseRegion by remember(operation) { mutableStateOf(false) }
    var activeCaptureRequestId by remember(operation) { mutableStateOf<Long?>(null) }
    var pendingPreprocessUri by remember(operation) { mutableStateOf<Uri?>(null) }
    var pendingPreprocessName by remember(operation) { mutableStateOf("") }
    val imagePreprocessLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            pendingPreprocessUri = uri
            pendingPreprocessName = context.queryDisplayName(uri)
                ?: "识图_${System.currentTimeMillis()}.jpg"
        }
    }
    val imageFilePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            val fileName = context.queryDisplayName(uri)
            if (fileName.isNullOrBlank()) {
                Toast.makeText(context, "无法读取图片文件名", Toast.LENGTH_SHORT).show()
            } else {
                fieldA = fileName
            }
        }
    }

    LaunchedEffect(operation, activeCaptureRequestId) {
        if (activeCaptureRequestId == null) {
            return@LaunchedEffect
        }
        CoordinateCaptureManager.results.collect { result ->
            if (result.requestId != activeCaptureRequestId) {
                return@collect
            }
            when (operation) {
                InsertOperationType.CLICK,
                InsertOperationType.LONG_PRESS -> {
                    val point = result.points.firstOrNull() ?: return@collect
                    fieldA = point.x.toString()
                    fieldB = point.y.toString()
                }

                InsertOperationType.SWIPE -> {
                    val start = result.points.getOrNull(0) ?: return@collect
                    val end = result.points.getOrNull(1) ?: return@collect
                    fieldA = start.x.toString()
                    fieldB = start.y.toString()
                    fieldC = end.x.toString()
                    fieldD = end.y.toString()
                }

                InsertOperationType.FIND_TEXT -> {
                    val rect = result.rect ?: return@collect
                    findTextUseRegion = true
                    fieldB = rect.left.toString()
                    fieldC = rect.top.toString()
                    fieldD = rect.right.toString()
                    fieldE = rect.bottom.toString()
                }

                InsertOperationType.RECOGNIZE_REGION_TEXT -> {
                    val rect = result.rect ?: return@collect
                    fieldA = rect.left.toString()
                    fieldB = rect.top.toString()
                    fieldC = rect.right.toString()
                    fieldD = rect.bottom.toString()
                }

                else -> Unit
            }
            activeCaptureRequestId = null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(min = 300.dp, max = 420.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = operation.dialogTitle,
                    style = MaterialTheme.typography.titleLarge
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    operation.fieldLabelA?.let {
                        if (operation.supportsImagePicker) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = fieldA,
                                    onValueChange = { value -> fieldA = value },
                                    label = { Text(it) },
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = {
                                        onEnsureImagesDirectory { result ->
                                            result
                                                .onSuccess { imagesUri ->
                                                    imageFilePickerLauncher.launch(buildOpenImageFileIntent(imagesUri))
                                                }
                                                .onFailure { throwable ->
                                                    Toast.makeText(
                                                        context,
                                                        throwable.message ?: "无法打开 Images 文件夹",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                        }
                                    }
                                ) {
                                    Text("选择")
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = fieldA,
                                onValueChange = { value -> fieldA = value },
                                label = { Text(it) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    if (operation == InsertOperationType.FIND_TEXT) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "查找范围",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { findTextUseRegion = false },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = !findTextUseRegion,
                                    onClick = { findTextUseRegion = false }
                                )
                                Text("全屏查找")
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { findTextUseRegion = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = findTextUseRegion,
                                    onClick = { findTextUseRegion = true }
                                )
                                Text("区域查找")
                            }
                        }
                    }
                    if (operation != InsertOperationType.FIND_TEXT || findTextUseRegion) {
                        operation.fieldLabelB?.let {
                            OutlinedTextField(
                                value = fieldB,
                                onValueChange = { value -> fieldB = value },
                                label = { Text(it) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        operation.fieldLabelC?.let {
                            OutlinedTextField(
                                value = fieldC,
                                onValueChange = { value -> fieldC = value },
                                label = { Text(it) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        operation.fieldLabelD?.let {
                            OutlinedTextField(
                                value = fieldD,
                                onValueChange = { value -> fieldD = value },
                                label = { Text(it) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        operation.fieldLabelE?.let {
                            OutlinedTextField(
                                value = fieldE,
                                onValueChange = { value -> fieldE = value },
                                label = { Text(it) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (operation.supportsImagePicker) {
                        TextButton(
                            onClick = {
                                imagePreprocessLauncher.launch(buildPickImageFromGalleryIntent())
                            }
                        ) {
                            Text("图片预处理")
                        }
                    } else if (operation.supportsCoordinateCapture || operation.supportsRegionCapture && (operation != InsertOperationType.FIND_TEXT || findTextUseRegion)) {
                        TextButton(
                            onClick = {
                                val request = operation.createCaptureRequest() ?: return@TextButton
                                activeCaptureRequestId = request.requestId
                                CoordinateCaptureService.launch(context, request)
                            }
                        ) {
                            Text(if (operation.supportsRegionCapture) "抓区域" else "抓抓")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    TextButton(
                        onClick = {
                            val useRegionFields = operation != InsertOperationType.FIND_TEXT || findTextUseRegion
                            onInsert(
                                operation.buildInsertion(
                                    fieldA = fieldA,
                                    fieldB = if (useRegionFields) fieldB else "",
                                    fieldC = if (useRegionFields) fieldC else "",
                                    fieldD = if (useRegionFields) fieldD else "",
                                    fieldE = if (useRegionFields) fieldE else ""
                                )
                            )
                        }
                    ) {
                        Text("插入")
                    }
                }
            }
        }
    }

    pendingPreprocessUri?.let { sourceUri ->
        ImageFileNameDialog(
            title = "保存识图图片",
            initialName = pendingPreprocessName,
            onDismiss = {
                pendingPreprocessUri = null
                pendingPreprocessName = ""
            },
            onConfirm = { targetName ->
                onImportImage(sourceUri, targetName) { result ->
                    result
                        .onSuccess { fileName ->
                            fieldA = fileName
                            Toast.makeText(context, "已保存到 Images：$fileName", Toast.LENGTH_SHORT).show()
                        }
                        .onFailure { throwable ->
                            Toast.makeText(context, throwable.message ?: "保存识图图片失败", Toast.LENGTH_SHORT).show()
                        }
                }
                pendingPreprocessUri = null
                pendingPreprocessName = ""
            }
        )
    }
}

@Composable
private fun ImageFileNameDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var fileName by remember(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = fileName,
                onValueChange = { fileName = it },
                label = { Text("图片文件名") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(fileName.ifBlank { "未命名图片.jpg" }) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun FileNameDialog(
    title: String,
    initialName: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var fileName by remember(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = fileName,
                onValueChange = { fileName = it },
        label = { Text("文件名") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(normalizeEditorScriptName(fileName)) }) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun buildLineNumbers(source: String): String {
    val lineCount = source.lines().size.coerceAtLeast(1)
    return (1..lineCount).joinToString(separator = "\n")
}

private fun cursorLineIndex(text: String, cursor: Int): Int {
    val safeCursor = cursor.coerceIn(0, text.length)
    return text.take(safeCursor).count { it == '\n' }
}

private fun insertSnippetAtSelection(
    currentValue: TextFieldValue,
    insertion: InsertOperationPayload
): TextFieldValue {
    val lineIndent = currentLineIndent(
        text = currentValue.text,
        cursor = currentValue.selection.min
    )
    val formattedInsertion = formatInsertionSnippet(
        snippet = insertion.snippet.trimEnd(),
        indent = lineIndent,
        placeCursorAtMarker = insertion.placeCursorAtMarker
    )
    if (formattedInsertion.text.isBlank()) {
        return currentValue
    }

    val text = currentValue.text
    val selectionStart = currentValue.selection.min
    val selectionEnd = currentValue.selection.max
    val prefix = text.substring(0, selectionStart)
    val suffix = text.substring(selectionEnd)
    val needsLeadingNewline = prefix.isNotEmpty() && !prefix.endsWith("\n")
    val needsTrailingNewline = suffix.isNotEmpty() && !suffix.startsWith("\n")
    val insertedText = buildString {
        if (needsLeadingNewline) {
            append('\n')
        }
        append(formattedInsertion.text)
        if (needsTrailingNewline) {
            append('\n')
        }
    }
    val nextText = prefix + insertedText + suffix
    val defaultCursor = prefix.length + insertedText.length
    val nextCursor = formattedInsertion.cursorOffset?.let { cursorOffset ->
        prefix.length + if (needsLeadingNewline) 1 else 0 + cursorOffset
    } ?: defaultCursor
    return TextFieldValue(
        text = nextText,
        selection = TextRange(nextCursor)
    )
}

private fun currentLineIndent(text: String, cursor: Int): String {
    val safeCursor = cursor.coerceIn(0, text.length)
    val lineStart = text.lastIndexOf('\n', startIndex = (safeCursor - 1).coerceAtLeast(0))
        .let { if (it == -1) 0 else it + 1 }
    val line = text.substring(lineStart, safeCursor)
    return line.takeWhile { it == ' ' || it == '\t' }
}

private fun formatInsertionSnippet(
    snippet: String,
    indent: String,
    placeCursorAtMarker: Boolean
): FormattedInsertion {
    if (snippet.isBlank()) {
        return FormattedInsertion(text = "")
    }

    val builder = StringBuilder()
    var cursorOffset: Int? = null
    snippet.lines().forEachIndexed { index, line ->
        if (index > 0) {
            builder.append('\n')
        }
        when {
            placeCursorAtMarker && line == INSERT_CURSOR_MARKER -> {
                builder.append(indent)
                builder.append(INDENT_UNIT)
                cursorOffset = builder.length
            }
            line == INSERT_INDENTED_EMPTY_LINE_MARKER -> {
                builder.append(indent)
                builder.append(INDENT_UNIT)
            }
            line.isBlank() -> builder.append(line)
            else -> {
                builder.append(indent)
                builder.append(line)
            }
        }
    }
    return FormattedInsertion(
        text = builder.toString(),
        cursorOffset = cursorOffset
    )
}

private data class FormattedInsertion(
    val text: String,
    val cursorOffset: Int? = null
)

private fun applyEditorInputBehaviors(
    previousValue: TextFieldValue,
    nextValue: TextFieldValue
): TextFieldValue {
    applyAutoUnindentOnClosingLine(
        previousValue = previousValue,
        nextValue = nextValue
    )?.let { return it }

    return applyAutoIndentOnNewLine(
        previousValue = previousValue,
        nextValue = nextValue
    )
}

private fun applyAutoIndentOnNewLine(
    previousValue: TextFieldValue,
    nextValue: TextFieldValue
): TextFieldValue {
    val previousSelection = previousValue.selection
    val nextSelection = nextValue.selection
    if (!previousSelection.collapsed || !nextSelection.collapsed) {
        return nextValue
    }

    val insertedLength = nextValue.text.length - previousValue.text.length
    if (insertedLength != 1) {
        return nextValue
    }

    val cursor = nextSelection.start
    if (cursor <= 0 || cursor > nextValue.text.length) {
        return nextValue
    }

    if (nextValue.text[cursor - 1] != '\n') {
        return nextValue
    }

    val insertedIndent = currentLineIndent(
        text = previousValue.text,
        cursor = previousSelection.start
    )
    val extraIndent = if (shouldIncreaseIndentAfterLine(previousValue.text, previousSelection.start)) {
        INDENT_UNIT
    } else {
        ""
    }
    val targetIndent = insertedIndent + extraIndent
    if (targetIndent.isEmpty()) {
        return nextValue
    }

    val textWithIndent = buildString {
        append(nextValue.text, 0, cursor)
        append(targetIndent)
        append(nextValue.text.substring(cursor))
    }
    val nextCursor = cursor + targetIndent.length
    return nextValue.copy(
        text = textWithIndent,
        selection = TextRange(nextCursor)
    )
}

private fun applyAutoUnindentOnClosingLine(
    previousValue: TextFieldValue,
    nextValue: TextFieldValue
): TextFieldValue? {
    val previousSelection = previousValue.selection
    val nextSelection = nextValue.selection
    if (!previousSelection.collapsed || !nextSelection.collapsed) {
        return null
    }

    val deletedLength = previousValue.text.length - nextValue.text.length
    if (deletedLength != 1) {
        return null
    }

    val nextCursor = nextSelection.start
    val previousCursor = previousSelection.start
    if (previousCursor <= 0 || nextCursor < 0) {
        return null
    }

    val previousLineStart = previousValue.text.lastIndexOf('\n', startIndex = (previousCursor - 1).coerceAtLeast(0))
        .let { if (it == -1) 0 else it + 1 }
    val previousLineEnd = previousValue.text.indexOf('\n', startIndex = previousCursor)
        .let { if (it == -1) previousValue.text.length else it }
    val previousLineText = previousValue.text.substring(previousLineStart, previousLineEnd)
    val previousLineIndent = previousLineText.takeWhile { it == ' ' || it == '\t' }

    val lineStart = nextValue.text.lastIndexOf('\n', startIndex = (nextCursor - 1).coerceAtLeast(0))
        .let { if (it == -1) 0 else it + 1 }
    val lineEnd = nextValue.text.indexOf('\n', startIndex = nextCursor)
        .let { if (it == -1) nextValue.text.length else it }
    val lineText = nextValue.text.substring(lineStart, lineEnd)
    val lineIndent = lineText.takeWhile { it == ' ' || it == '\t' }
    val trimmedLine = lineText.trimStart()
    val cursorInIndent = nextCursor <= lineStart + lineIndent.length
    val deletedCharWasIndent = previousValue.text[previousCursor - 1] == ' ' || previousValue.text[previousCursor - 1] == '\t'

    if (!cursorInIndent || !deletedCharWasIndent || !isClosingDslLine(trimmedLine)) {
        return null
    }

    val targetIndentLength = when {
        previousLineIndent.endsWith(INDENT_UNIT) -> previousLineIndent.length - INDENT_UNIT.length
        previousLineIndent.endsWith("\t") -> previousLineIndent.length - 1
        else -> previousLineIndent.length - 1
    }.coerceAtLeast(0)
    if (lineIndent.length <= targetIndentLength) {
        return null
    }

    val restoredDeletionStart = lineStart + targetIndentLength
    val currentIndentEnd = lineStart + lineIndent.length
    val correctedText = buildString {
        append(nextValue.text, 0, restoredDeletionStart)
        append(nextValue.text.substring(currentIndentEnd))
    }
    val correctedCursor = restoredDeletionStart
    return nextValue.copy(
        text = correctedText,
        selection = TextRange(correctedCursor)
    )
}

private fun isClosingDslLine(line: String): Boolean {
    return line == "结束循环" || line == "结束如果"
}

private fun shouldIncreaseIndentAfterLine(text: String, cursor: Int): Boolean {
    val safeCursor = cursor.coerceIn(0, text.length)
    val lineStart = text.lastIndexOf('\n', startIndex = (safeCursor - 1).coerceAtLeast(0))
        .let { if (it == -1) 0 else it + 1 }
    val line = text.substring(lineStart, safeCursor).trim()
    if (line.isEmpty()) {
        return false
    }
    return when {
        line.startsWith("循环 ") && line.endsWith("次") -> true
        line == "无限循环" -> true
        line.startsWith("如果 ") -> true
        line == "否则" -> true
        else -> false
    }
}

private fun buildOpenScriptIntent(initialUri: String?): Intent {
    return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "*/*"
        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/plain", "application/octet-stream", "*/*"))
        putExtra(
            DocumentsContract.EXTRA_INITIAL_URI,
            initialUri?.let(Uri::parse) ?: buildDefaultTouchScriptDocumentUri()
        )
    }
}

private fun buildCreateScriptIntent(fileName: String, initialUri: String?): Intent {
    return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "text/plain"
        putExtra(Intent.EXTRA_TITLE, "${normalizeEditorScriptName(fileName)}.tscript")
        putExtra(
            DocumentsContract.EXTRA_INITIAL_URI,
            initialUri?.let(Uri::parse) ?: buildDefaultTouchScriptDocumentUri()
        )
    }
}

private fun buildOpenScriptTreeIntent(): Intent {
    return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
}

private fun buildPickImageFromGalleryIntent(): Intent {
    return Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
        type = "image/*"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

private fun buildOpenImageFileIntent(initialUri: String?): Intent {
    return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "image/*"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        initialUri?.let {
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(it))
        }
    }
}

private fun android.content.Context.queryDisplayName(uri: Uri): String? {
    return contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }
}

private fun buildDefaultTouchScriptDocumentUri(): Uri {
    return DocumentsContract.buildDocumentUri(
        EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY,
        "primary:Documents/TouchScript"
    )
}

private const val EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY = "com.android.externalstorage.documents"

private fun normalizeEditorScriptName(name: String): String {
    var result = name.trim()
    while (result.endsWith(".tscript", ignoreCase = true)) {
        result = result.dropLast(".tscript".length)
    }
    return result.ifBlank { "未命名脚本" }
}

private enum class InsertOperationGroup(
    val label: String,
    val operations: List<InsertOperationType>
) {
    SIMULATION(
        label = "模拟操作",
        operations = listOf(
            InsertOperationType.CLICK,
            InsertOperationType.LONG_PRESS,
            InsertOperationType.SWIPE,
            InsertOperationType.KEYBOARD_INPUT
        )
    ),
    RECOGNITION(
        label = "识别",
        operations = listOf(
            InsertOperationType.IMAGE_FIND,
            InsertOperationType.FIND_TEXT,
            InsertOperationType.RECOGNIZE_REGION_TEXT
        )
    ),
    LOGIC(
        label = "逻辑",
        operations = listOf(
            InsertOperationType.ASSIGN,
            InsertOperationType.REPEAT,
            InsertOperationType.FOREVER,
            InsertOperationType.IF,
            InsertOperationType.SLEEP
        )
    ),
    SYSTEM(
        label = "系统操作",
        operations = listOf(
            InsertOperationType.LAUNCH_APP,
            InsertOperationType.LOG,
            InsertOperationType.BACK,
            InsertOperationType.HOME,
            InsertOperationType.STOP_RUNNING
        )
    )
}

private enum class InsertOperationType(
    val label: String,
    val dialogTitle: String,
    val fieldLabelA: String? = null,
    val fieldLabelB: String? = null,
    val fieldLabelC: String? = null,
    val fieldLabelD: String? = null,
    val fieldLabelE: String? = null,
    val defaultFieldA: String = "",
    val defaultFieldB: String = "",
    val defaultFieldC: String = "",
    val defaultFieldD: String = "",
    val defaultFieldE: String = ""
) {
    CLICK(
        label = "点击",
        dialogTitle = "插入点击",
        fieldLabelA = "X",
        fieldLabelB = "Y",
        defaultFieldA = "540",
        defaultFieldB = "1600"
    ),
    LONG_PRESS(
        label = "长按",
        dialogTitle = "插入长按",
        fieldLabelA = "X",
        fieldLabelB = "Y",
        fieldLabelC = "时长(ms)",
        defaultFieldA = "540",
        defaultFieldB = "1600",
        defaultFieldC = "800"
    ),
    SWIPE(
        label = "滑动",
        dialogTitle = "插入滑动",
        fieldLabelA = "从 X",
        fieldLabelB = "从 Y",
        fieldLabelC = "滑到 X",
        fieldLabelD = "滑到 Y",
        fieldLabelE = "时长(ms)",
        defaultFieldA = "540",
        defaultFieldB = "1500",
        defaultFieldC = "540",
        defaultFieldD = "500",
        defaultFieldE = "260"
    ),
    KEYBOARD_INPUT(
        label = "键盘输入",
        dialogTitle = "插入键盘输入",
        fieldLabelA = "输入文本",
        defaultFieldA = "预设文字"
    ),
    SLEEP(
        label = "等待",
        dialogTitle = "插入等待",
        fieldLabelA = "时长(ms)",
        defaultFieldA = "500"
    ),
    LAUNCH_APP(
        label = "启动应用",
        dialogTitle = "插入启动应用",
        fieldLabelA = "包名",
        defaultFieldA = "com.android.settings"
    ),
    LOG(
        label = "记录",
        dialogTitle = "插入记录",
        fieldLabelA = "日志文本",
        defaultFieldA = "开始执行"
    ),
    IMAGE_FIND(
        label = "识图",
        dialogTitle = "插入识图",
        fieldLabelA = "图片文件名",
        fieldLabelB = "置信度(0-1)",
        defaultFieldB = "0.85"
    ),
    FIND_TEXT(
        label = "查找文字",
        dialogTitle = "插入查找文字",
        fieldLabelA = "目标文字",
        fieldLabelB = "左(可空)",
        fieldLabelC = "上(可空)",
        fieldLabelD = "右(可空)",
        fieldLabelE = "下(可空)",
        defaultFieldA = "设置"
    ),
    RECOGNIZE_REGION_TEXT(
        label = "识别区域文字",
        dialogTitle = "插入识别区域文字",
        fieldLabelA = "左",
        fieldLabelB = "上",
        fieldLabelC = "右",
        fieldLabelD = "下",
        defaultFieldA = "0",
        defaultFieldB = "0",
        defaultFieldC = "1080",
        defaultFieldD = "600"
    ),
    ASSIGN(
        label = "设变量",
        dialogTitle = "插入设变量",
        fieldLabelA = "变量名",
        fieldLabelB = "变量值",
        defaultFieldA = "次数",
        defaultFieldB = "3"
    ),
    REPEAT(
        label = "循环",
        dialogTitle = "插入循环",
        fieldLabelA = "循环次数",
        defaultFieldA = "3"
    ),
    FOREVER(
        label = "无限循环",
        dialogTitle = "插入无限循环"
    ),
    IF(
        label = "如果",
        dialogTitle = "插入如果",
        fieldLabelA = "条件表达式",
        defaultFieldA = "次数 > 0"
    ),
    BACK(
        label = "返回",
        dialogTitle = "插入返回"
    ),
    HOME(
        label = "主页",
        dialogTitle = "插入主页"
    ),
    STOP_RUNNING(
        label = "停止运行",
        dialogTitle = "插入停止运行"
    );

    val supportsCoordinateCapture: Boolean
        get() = this == CLICK || this == LONG_PRESS || this == SWIPE

    val supportsRegionCapture: Boolean
        get() = this == FIND_TEXT || this == RECOGNIZE_REGION_TEXT

    val supportsImagePicker: Boolean
        get() = this == IMAGE_FIND

    fun buildInsertion(
        fieldA: String,
        fieldB: String,
        fieldC: String,
        fieldD: String,
        fieldE: String
    ): InsertOperationPayload {
        return when (this) {
            CLICK -> InsertOperationPayload("点击 ${fieldA.ifBlank { "540" }} ${fieldB.ifBlank { "1600" }}")
            LONG_PRESS -> InsertOperationPayload("长按 ${fieldA.ifBlank { "540" }} ${fieldB.ifBlank { "1600" }} ${fieldC.ifBlank { "800" }}")
            SWIPE -> InsertOperationPayload("滑动 ${fieldA.ifBlank { "540" }} ${fieldB.ifBlank { "1500" }} ${fieldC.ifBlank { "540" }} ${fieldD.ifBlank { "500" }} ${fieldE.ifBlank { "260" }}")
            KEYBOARD_INPUT -> InsertOperationPayload("键盘输入 \"${sanitizeQuotedText(fieldA.ifBlank { "预设文字" })}\"")
            SLEEP -> InsertOperationPayload("等待 ${fieldA.ifBlank { "500" }}")
            LAUNCH_APP -> InsertOperationPayload("启动应用 \"${sanitizeQuotedText(fieldA.ifBlank { "com.android.settings" })}\"")
            LOG -> InsertOperationPayload("记录 \"${sanitizeQuotedText(fieldA.ifBlank { "开始执行" })}\"")
            IMAGE_FIND -> {
                val imageName = sanitizeQuotedText(fieldA.ifBlank { "请选择图片.png" })
                InsertOperationPayload(
                    """
                    设 结果1 = 识图 "$imageName" ${fieldB.ifBlank { "0.85" }}
                    如果 结果1.找到
                        点击 结果1.x 结果1.y
                    否则
                        记录 "未找到：$imageName"
                    结束如果
                    """.trimIndent()
                )
            }
            FIND_TEXT -> {
                val target = sanitizeQuotedText(fieldA.ifBlank { "设置" })
                val hasRegion = listOf(fieldB, fieldC, fieldD, fieldE).all { it.isNotBlank() }
                val regionArgs = if (hasRegion) {
                    " ${fieldB} ${fieldC} ${fieldD} ${fieldE}"
                } else {
                    ""
                }
                InsertOperationPayload(
                    """
                    设 文字结果1 = 查找文字 "$target"$regionArgs
                    如果 文字结果1.找到
                        点击 文字结果1.x 文字结果1.y
                    否则
                        记录 "未找到文字：$target"
                    结束如果
                    """.trimIndent()
                )
            }
            RECOGNIZE_REGION_TEXT -> InsertOperationPayload(
                """
                设 文字1 = 识别文字 ${fieldA.ifBlank { "0" }} ${fieldB.ifBlank { "0" }} ${fieldC.ifBlank { "1080" }} ${fieldD.ifBlank { "600" }}
                如果 文字1.找到
                    记录 文字1.文本
                否则
                    记录 "未识别到文字"
                结束如果
                """.trimIndent()
            )
            ASSIGN -> InsertOperationPayload("设 ${fieldA.ifBlank { "变量" }} = ${fieldB.ifBlank { "0" }}")
            REPEAT -> InsertOperationPayload(
                snippet = "循环 ${fieldA.ifBlank { "3" }} 次\n$INSERT_CURSOR_MARKER\n结束循环",
                placeCursorAtMarker = true
            )
            FOREVER -> InsertOperationPayload(
                snippet = "无限循环\n$INSERT_CURSOR_MARKER\n结束循环",
                placeCursorAtMarker = true
            )
            IF -> InsertOperationPayload(
                snippet = "如果 ${fieldA.ifBlank { "条件" }}\n$INSERT_CURSOR_MARKER\n否则\n$INSERT_INDENTED_EMPTY_LINE_MARKER\n结束如果",
                placeCursorAtMarker = true
            )
            BACK -> InsertOperationPayload("返回")
            HOME -> InsertOperationPayload("主页")
            STOP_RUNNING -> InsertOperationPayload("停止运行")
        }
    }

    fun createCaptureRequest() = when (this) {
        CLICK -> CoordinateCaptureManager.createRequest(
            pointCount = 1,
            stepLabels = listOf("点击位置")
        )

        LONG_PRESS -> CoordinateCaptureManager.createRequest(
            pointCount = 1,
            stepLabels = listOf("长按位置")
        )

        SWIPE -> CoordinateCaptureManager.createRequest(
            pointCount = 2,
            stepLabels = listOf("起点", "终点")
        )

        FIND_TEXT -> CoordinateCaptureManager.createRectangleRequest("文字查找区域")

        RECOGNIZE_REGION_TEXT -> CoordinateCaptureManager.createRectangleRequest("文字识别区域")

        else -> null
    }
}

private fun sanitizeQuotedText(value: String): String {
    return value.replace("\"", "'")
}

private data class InsertOperationPayload(
    val snippet: String,
    val placeCursorAtMarker: Boolean = false
)

private const val INSERT_CURSOR_MARKER = "__TOUCHSCRIPT_CURSOR_MARKER__"
private const val INSERT_INDENTED_EMPTY_LINE_MARKER = "__TOUCHSCRIPT_INDENTED_EMPTY_LINE__"
private const val INDENT_UNIT = "    "
private const val EDITOR_LINE_HEIGHT_SP = 22
