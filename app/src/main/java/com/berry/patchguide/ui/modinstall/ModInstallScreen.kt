package com.berry.patchguide.ui.modinstall

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModInstallScreen(
    onNavigateBack: () -> Unit,
    viewModel: ModInstallViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 모드 ZIP 파일 선택 런처 (SAF)
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> viewModel.onZipSelected(uri) }
        }
    }

    // 설치 위치 폴더 선택 런처 (SAF 폴더 트리 선택)
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { treeUri ->
                viewModel.onDestinationSelected(treeUri)
            }
        }
    }

    val launchZipPicker: () -> Unit = {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/zip",
                "application/x-zip-compressed",
                "application/octet-stream"
            ))
        }
        zipPickerLauncher.launch(intent)
    }

    val launchFolderPicker: () -> Unit = {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        folderPickerLauncher.launch(intent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("모드 설치") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is ModInstallUiState.Idle -> {
                    IdleModStep(onSelectZip = launchZipPicker)
                }

                is ModInstallUiState.Extracting -> {
                    ModProgressStep(
                        title = "모드 압축 해제 중...",
                        progress = state.progress
                    )
                }

                is ModInstallUiState.Ready -> {
                    ReadyModStep(
                        modName = state.modName,
                        fileCount = state.fileCount,
                        totalSizeBytes = state.totalSizeBytes,
                        onSelectDestination = launchFolderPicker
                    )
                }

                is ModInstallUiState.Installing -> {
                    ModProgressStep(
                        title = "모드 설치 중...",
                        progress = state.progress
                    )
                }

                is ModInstallUiState.Success -> {
                    ModSuccessStep(
                        destLabel = state.destLabel,
                        onDone = {
                            viewModel.resetState()
                            onNavigateBack()
                        },
                        onInstallAnother = { viewModel.resetState() }
                    )
                }

                is ModInstallUiState.Error -> {
                    ModErrorStep(
                        message = state.message,
                        retryable = state.retryable,
                        onRetry = {
                            if (state.retryable) launchZipPicker() else viewModel.resetState()
                        },
                        onBack = onNavigateBack
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleModStep(onSelectZip: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "모드 설치",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Polymod 등 폴더 통째로 복사해서 쓰는 모드 ZIP을 원하는 위치에 설치합니다.\n" +
                        "(IPS/UPS/BPS/xdelta ROM 패치는 \"패치 적용\" 메뉴를 이용하세요)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = "1단계: 모드 ZIP 파일 선택",
            style = MaterialTheme.typography.titleSmall
        )

        Button(
            onClick = onSelectZip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("모드 ZIP 파일 선택")
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "선택한 ZIP을 압축 해제한 뒤, 다음 단계에서 설치할 폴더를 직접 선택할 수 있습니다.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun ReadyModStep(
    modName: String,
    fileCount: Int,
    totalSizeBytes: Long,
    onSelectDestination: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "2단계: 설치 위치 폴더 선택",
            style = MaterialTheme.typography.titleMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "압축 해제 완료",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = modName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "파일 ${fileCount}개 · ${formatModBytes(totalSizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Text(
            text = "모드를 설치할 폴더를 선택하세요. (예: 게임의 mods 폴더)",
            style = MaterialTheme.typography.bodyMedium
        )

        Button(
            onClick = onSelectDestination,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CreateNewFolder, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("설치 위치 폴더 선택")
        }
    }
}

@Composable
private fun ModProgressStep(title: String, progress: Float) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ModSuccessStep(
    destLabel: String,
    onDone: () -> Unit,
    onInstallAnother: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = "모드 설치 완료!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "설치 위치: $destLabel",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Button(
            onClick = onInstallAnother,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("다른 모드 설치하기")
        }

        OutlinedButton(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("완료")
        }
    }
}

@Composable
private fun ModErrorStep(
    message: String,
    retryable: Boolean,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "오류 발생",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text(if (retryable) "다시 시도" else "처음으로")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("뒤로가기")
        }
    }
}

private fun formatModBytes(bytes: Long): String {
    return when {
        bytes >= 1_048_576 -> "${"%.1f".format(bytes / 1_048_576.0)} MB"
        bytes >= 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
        else -> "$bytes B"
    }
}
