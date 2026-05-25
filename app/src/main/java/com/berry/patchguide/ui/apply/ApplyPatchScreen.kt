package com.berry.patchguide.ui.apply

import android.net.Uri
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
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berry.patchguide.patching.XdeltaApplier
import com.berry.patchguide.patching.ZipApplier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyPatchScreen(
    patchId: String,
    patchTitle: String = "",
    downloadUrl: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: ApplyPatchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ROM 파일 선택 런처 (SAF)
    val romPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.applyPatch(it) }
    }

    // 패치 파일 수동 선택 런처 (SAF)
    val patchPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.setPatchFileFromUri(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("패치 적용") },
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
                is ApplyUiState.Idle -> {
                    // Step 1: 패치 정보 표시 + 다운로드 / 파일 선택
                    IdleStep(
                        patchId = patchId,
                        patchTitle = patchTitle,
                        downloadUrl = downloadUrl,
                        onDownloadClick = { url -> viewModel.downloadPatch(url) },
                        onSelectPatchFile = {
                            patchPickerLauncher.launch(arrayOf("*/*"))
                        }
                    )
                }

                is ApplyUiState.Downloading -> {
                    ProgressStep(
                        title = "패치 다운로드 중...",
                        progress = state.progress
                    )
                }

                is ApplyUiState.WaitingForRom -> {
                    // Step 2: ROM 파일 선택
                    WaitingForRomStep(
                        patchFile = state.patchFile,
                        onSelectRom = {
                            romPickerLauncher.launch(arrayOf("*/*"))
                        }
                    )
                }

                is ApplyUiState.Applying -> {
                    ProgressStep(
                        title = "패치 적용 중...",
                        progress = state.progress
                    )
                }

                is ApplyUiState.Success -> {
                    SuccessStep(
                        report = state,
                        onDone = onNavigateBack
                    )
                }

                is ApplyUiState.Error -> {
                    ErrorStep(
                        message = state.message,
                        onRetry = { viewModel.resetState() },
                        onBack = onNavigateBack
                    )
                }

                is ApplyUiState.XdeltaUnsupported -> {
                    XdeltaUnsupportedStep(onBack = onNavigateBack)
                }

                is ApplyUiState.ZipExtracted -> {
                    ZipExtractedStep(
                        destDir = state.destDir,
                        innerPatches = state.innerPatches,
                        onBack = onNavigateBack
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleStep(
    patchId: String,
    patchTitle: String,
    downloadUrl: String?,
    onDownloadClick: (String) -> Unit,
    onSelectPatchFile: () -> Unit
) {
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
                    text = if (patchTitle.isNotBlank()) patchTitle else "패치 ID: $patchId",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ROM 파일에 패치를 적용합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = "1단계: 패치 파일 준비",
            style = MaterialTheme.typography.titleSmall
        )

        if (!downloadUrl.isNullOrBlank()) {
            Button(
                onClick = { onDownloadClick(downloadUrl) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("패치 다운로드")
            }
        }

        OutlinedButton(
            onClick = onSelectPatchFile,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("이미 다운로드한 패치 파일 선택")
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "지원 형식: IPS, UPS, BPS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "xdelta/VCDIFF: 지원 예정",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun WaitingForRomStep(
    patchFile: java.io.File,
    onSelectRom: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "2단계: ROM 파일 선택",
            style = MaterialTheme.typography.titleMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "패치 준비 완료",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = patchFile.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Text(
            text = "패치를 적용할 원본 ROM 파일을 선택하세요.",
            style = MaterialTheme.typography.bodyMedium
        )

        Button(
            onClick = onSelectRom,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("ROM 파일 선택")
        }

        Text(
            text = "원본 ROM 파일은 변경되지 않습니다. 패치가 적용된 새 파일이 Downloads/BerryPatchGuide/에 저장됩니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProgressStep(title: String, progress: Float) {
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
private fun SuccessStep(report: ApplyUiState.Success, onDone: () -> Unit) {
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
            text = "패치 적용 완료!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoRow("형식", report.report.appliedFormat.name)
                InfoRow("저장 위치", report.report.outputPath)
                InfoRow("파일 크기", formatBytes(report.report.sizeBytes))
                InfoRow("소요 시간", "${report.report.durationMs}ms")
                InfoRow("SHA-256", report.report.sha256.take(16) + "...")
            }
        }

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("완료")
        }
    }
}

@Composable
private fun ErrorStep(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
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
            Text("다시 시도")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("뒤로가기")
        }
    }
}

@Composable
private fun XdeltaUnsupportedStep(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "xdelta 형식 — 지원 예정",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = XdeltaApplier.UNSUPPORTED_MESSAGE,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("돌아가기")
        }
    }
}

@Composable
private fun ZipExtractedStep(
    destDir: java.io.File,
    innerPatches: List<java.io.File>,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "ZIP 압축 해제 완료",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = ZipApplier.UNSUPPORTED_MESSAGE,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "압축 해제 위치: ${destDir.absolutePath}",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )

        if (innerPatches.isNotEmpty()) {
            Text(
                text = "내부 패치 파일 발견 (${innerPatches.size}개):",
                style = MaterialTheme.typography.labelMedium
            )
            innerPatches.forEach { file ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = file.name,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        FilledTonalButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("돌아가기")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = if (label == "SHA-256" || label == "저장 위치") FontFamily.Monospace else FontFamily.Default
        )
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_048_576 -> "${"%.1f".format(bytes / 1_048_576.0)} MB"
        bytes >= 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
        else -> "$bytes B"
    }
}
