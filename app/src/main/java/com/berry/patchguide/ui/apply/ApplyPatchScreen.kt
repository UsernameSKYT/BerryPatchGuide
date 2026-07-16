package com.berry.patchguide.ui.apply

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.berry.patchguide.patching.ZipApplier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyPatchScreen(
    patchId: String,
    patchTitle: String = "",
    downloadUrl: String? = null,
    sharedPatchUri: Uri? = null,
    onNavigateBack: () -> Unit,
    onNavigateToGuide: (outputPath: String, appliedFormat: String) -> Unit = { _, _ -> },
    viewModel: ApplyPatchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 공유 인텐트 URI가 있으면 자동으로 로드
    LaunchedEffect(sharedPatchUri) {
        if (sharedPatchUri != null) {
            viewModel.loadSharedPatchUri(sharedPatchUri)
        }
    }

    // ROM 파일 선택 런처 (SAF) - StartActivityForResult로 직접 Intent 실행
    val romPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.applyPatch(romUri = uri)
            }
        }
    }

    // 패치 파일 수동 선택 런처 (SAF) - StartActivityForResult로 직접 Intent 실행
    val patchPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.setPatchFileFromUri(uri)
            }
        }
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
                    IdleStep(
                        patchId = patchId,
                        patchTitle = patchTitle,
                        downloadUrl = downloadUrl,
                        onDownloadClick = { url -> viewModel.downloadPatch(url) },
                        onSelectPatchFile = {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "*/*"
                                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                                    "application/octet-stream",
                                    "application/zip",
                                    "application/x-zip-compressed",
                                    "application/x-ips",
                                    "application/x-ups",
                                    "application/x-bps",
                                    "application/x-xdelta",
                                    "application/x-xdelta3",
                                    "application/x-vcdiff"
                                ))
                            }
                            try {
                                patchPickerLauncher.launch(intent)
                            } catch (e: Exception) {
                                context.startActivity(Intent.createChooser(intent, "패치 파일 선택"))
                            }
                        }
                    )
                }

                is ApplyUiState.Downloading -> {
                    ProgressStep(
                        title = "패치 준비 중...",
                        label = state.label,
                        progress = state.progress
                    )
                }

                is ApplyUiState.WaitingForRom -> {
                    WaitingForRomStep(
                        patchFile = state.patchFile,
                        onSelectRom = {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "*/*"
                            }
                            try {
                                romPickerLauncher.launch(intent)
                            } catch (e: Exception) {
                                context.startActivity(Intent.createChooser(intent, "ROM 파일 선택"))
                            }
                        }
                    )
                }

                is ApplyUiState.Applying -> {
                    ProgressStep(
                        title = "패치 적용 중...",
                        label = state.label,
                        progress = state.progress
                    )
                }

                is ApplyUiState.Success -> {
                    SuccessStep(
                        report = state,
                        onDone = onNavigateBack,
                        onViewGuide = {
                            onNavigateToGuide(
                                state.report.outputPath,
                                state.report.appliedFormat.name
                            )
                        }
                    )
                }

                is ApplyUiState.Error -> {
                    ErrorStep(
                        message = state.message,
                        retryable = state.retryable,
                        onRetry = {
                            if (state.retryable) viewModel.retryDownload()
                            else viewModel.resetState()
                        },
                        onBack = onNavigateBack
                    )
                }

                is ApplyUiState.ZipExtracted -> {
                    ZipExtractedStep(
                        destDir = state.destDir,
                        innerPatches = state.innerPatches,
                        onSelectInnerPatch = { file -> viewModel.selectInnerPatch(file) },
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
                    text = "지원 형식: IPS, UPS, BPS, xdelta/VCDIFF",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "ZIP: 압축 해제 후 내부 패치 파일 선택",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "파일 관리자에서 패치 파일을 공유하면 자동으로 열립니다.",
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
            text = "2단계: 원본 ROM 파일 선택",
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
            text = "패치를 적용할 원본 게임 ROM 파일(.gb, .gba, .gbc 등)을 선택하세요.\n(다운로드한 패치 파일이 아닙니다)",
            style = MaterialTheme.typography.bodyMedium
        )

        Button(
            onClick = onSelectRom,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("원본 ROM 파일 선택")
        }

        Text(
            text = "원본 ROM 파일은 변경되지 않습니다. 패치가 적용된 새 파일이 Downloads/BerryPatchGuide/에 저장됩니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProgressStep(title: String, label: String, progress: Float) {
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
        if (label.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
private fun SuccessStep(
    report: ApplyUiState.Success,
    onDone: () -> Unit,
    onViewGuide: () -> Unit
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
            onClick = onViewGuide,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Info, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("다음 단계 가이드 보기")
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
private fun ErrorStep(
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

@Composable
private fun ZipExtractedStep(
    destDir: java.io.File,
    innerPatches: List<java.io.File>,
    onSelectInnerPatch: (java.io.File) -> Unit,
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

        if (innerPatches.isNotEmpty()) {
            Text(
                text = "내부 패치 파일을 선택하면 ROM 적용 단계로 이동합니다.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "내부 패치 파일 (${innerPatches.size}개):",
                style = MaterialTheme.typography.labelMedium
            )
            innerPatches.forEach { file ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectInnerPatch(file) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = file.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "선택",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else {
            Text(
                text = ZipApplier.UNSUPPORTED_MESSAGE,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "압축 해제 위치: ${destDir.absolutePath}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
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
