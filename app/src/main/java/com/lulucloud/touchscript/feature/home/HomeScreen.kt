package com.lulucloud.touchscript.feature.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding
import com.lulucloud.touchscript.common.AutomationLauncher
import com.lulucloud.touchscript.data.repository.LocalScriptFile
import com.lulucloud.touchscript.ui.theme.WorkshopSuccess

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    context: Context
) {
    val uiState by viewModel.uiState.collectAsState()
    val readyColor = if (uiState.isScriptReady) WorkshopSuccess else MaterialTheme.colorScheme.error
    val openScriptLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        val uri = data?.data ?: return@rememberLauncherForActivityResult
        if (result.resultCode == Activity.RESULT_OK) {
            val flags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }
            viewModel.loadScript(uri.toString())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Text(
            text = "触灵工坊",
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 32.sp,
                letterSpacing = 2.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 24.dp, top = 28.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.selectedScriptName.isBlank()) "未选择脚本" else uiState.selectedScriptName,
                    style = MaterialTheme.typography.titleLarge
                )
                Icon(
                    imageVector = if (uiState.isScriptReady) {
                        Icons.Rounded.CheckCircle
                    } else {
                        Icons.Rounded.Close
                    },
                    contentDescription = if (uiState.isScriptReady) "脚本可用" else "脚本不可用",
                    tint = readyColor
                )
            }
            Text(
                text = uiState.validationMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        viewModel.clearDebugDraft()
                        AutomationLauncher.showOverlay(context)
                    },
                    enabled = uiState.isScriptReady,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(min = 160.dp)
                ) {
                    Text("启用悬浮窗")
                }

                OutlinedButton(
                    onClick = {
                        openScriptLauncher.launch(buildOpenScriptIntent(uiState.scriptWorkspaceUri))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("加载脚本")
                }
            }
        }
    }
}

@Composable
fun ScriptFilePickerDialog(
    title: String,
    files: List<LocalScriptFile>,
    onDismiss: () -> Unit,
    onSelect: (LocalScriptFile) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (files.isEmpty()) {
                    Text("本地还没有脚本文件")
                } else {
                    files.forEach { file ->
                        TextButton(
                            onClick = { onSelect(file) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = file.name)
                                if (file.isTemplate) {
                                    Text(
                                        text = "模板",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
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

private fun buildDefaultTouchScriptDocumentUri(): Uri {
    return DocumentsContract.buildDocumentUri(
        EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY,
        "primary:Documents/TouchScript"
    )
}

private const val EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY = "com.android.externalstorage.documents"
