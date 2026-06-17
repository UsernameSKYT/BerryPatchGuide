package com.berry.patchguide.ui.apply

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.berry.patchguide.data.ads.AdManager
import com.berry.patchguide.data.billing.BillingManager
import com.berry.patchguide.data.repository.PatchRepository
import com.berry.patchguide.patching.PatchApplier
import com.berry.patchguide.patching.PatchFormat
import com.berry.patchguide.patching.PatchReport
import com.berry.patchguide.patching.ZipApplier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

sealed class ApplyUiState {
    data object Idle : ApplyUiState()
    // Step 1: 패치 다운로드 중
    data class Downloading(val progress: Float, val label: String = "") : ApplyUiState()
    // Step 1 완료: 패치 다운로드됨 또는 이미 있음 → ROM 선택 대기
    data class WaitingForRom(val patchFile: File, val patchId: String) : ApplyUiState()
    // Step 3: 패치 적용 중
    data class Applying(val progress: Float, val label: String = "") : ApplyUiState()
    // Step 4: 성공
    data class Success(val report: PatchReport) : ApplyUiState()
    // Step 4: 실패 (retryable: 재시도 가능 여부)
    data class Error(val message: String, val retryable: Boolean = true) : ApplyUiState()
    // ZIP 압축 해제 결과 — innerPatches 중 하나를 선택하면 WaitingForRom으로 전환
    data class ZipExtracted(val destDir: File, val innerPatches: List<File>) : ApplyUiState()
}

@HiltViewModel
class ApplyPatchViewModel @Inject constructor(
    application: Application,
    private val patchRepository: PatchRepository,
    private val adManager: AdManager,
    private val billingManager: BillingManager,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val TAG = "ApplyPatchViewModel"

    private val _uiState = MutableStateFlow<ApplyUiState>(ApplyUiState.Idle)
    val uiState: StateFlow<ApplyUiState> = _uiState.asStateFlow()

    fun isAdFree(): Boolean = billingManager.isAdFree()

    fun showInterstitialAd(activity: android.app.Activity, onDismissed: () -> Unit = {}) {
        if (!isAdFree()) {
            adManager.showInterstitial(activity, onDismissed)
        } else {
            onDismissed()
        }
    }

    private val patchId: String = savedStateHandle.get<String>("patchId") ?: ""

    // 현재 선택된 패치 파일 (다운로드 완료 또는 사용자 지정)
    private var currentPatchFile: File? = null

    // 복구용: 마지막 다운로드 URL 저장
    private var lastDownloadUrl: String? = null

    // 복구용: 임시 파일 목록 (onCleared 또는 복구 시 정리)
    private val tempFiles = mutableListOf<File>()

    /**
     * 외부 공유 인텐트 URI로 패치 파일을 즉시 로드합니다.
     */
    fun loadSharedPatchUri(uri: Uri) {
        if (_uiState.value !is ApplyUiState.Idle) return
        setPatchFileFromUri(uri)
    }

    /**
     * 패치 다운로드 URL로 다운로드를 시작합니다.
     */
    fun downloadPatch(url: String) {
        lastDownloadUrl = url
        viewModelScope.launch {
            _uiState.value = ApplyUiState.Downloading(0f, "서버에서 패치 다운로드 중...")
            try {
                val destFile = File(
                    getApplication<Application>().cacheDir,
                    "patch_${patchId}_${System.currentTimeMillis()}"
                )
                tempFiles.add(destFile)
                withContext(Dispatchers.IO) {
                    patchRepository.downloadPatch(url, destFile) { p ->
                        _uiState.value = ApplyUiState.Downloading(p, "다운로드 중... ${(p * 100).toInt()}%")
                    }
                }

                processDownloadedFile(destFile)
            } catch (e: Exception) {
                Log.e(TAG, "다운로드 실패", e)
                _uiState.value = ApplyUiState.Error(
                    message = "다운로드 실패: ${e.message}",
                    retryable = true
                )
            }
        }
    }

    /**
     * 사용자가 직접 선택한 패치 파일을 설정합니다.
     */
    fun setPatchFileFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ApplyUiState.Downloading(0f, "파일 읽는 중...")
            try {
                val context = getApplication<Application>()
                val destFile = File(context.cacheDir, "patch_manual_${System.currentTimeMillis()}")
                tempFiles.add(destFile)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                _uiState.value = ApplyUiState.Downloading(1f, "파일 읽기 완료")
                processDownloadedFile(destFile)
            } catch (e: Exception) {
                _uiState.value = ApplyUiState.Error(
                    message = "패치 파일 읽기 실패: ${e.message}",
                    retryable = false
                )
            }
        }
    }

    /**
     * 다운로드/복사된 파일의 포맷을 감지하고 다음 상태로 전환합니다.
     */
    private suspend fun processDownloadedFile(destFile: File) {
        val magicBytes = destFile.inputStream().use { it.readNBytes(8) }
        val format = PatchFormat.detect(magicBytes)

        when (format) {
            PatchFormat.ZIP -> {
                val extractDir = File(
                    getApplication<Application>().cacheDir,
                    "zip_extract_${patchId}_${System.currentTimeMillis()}"
                )
                tempFiles.add(extractDir)
                withContext(Dispatchers.IO) {
                    val extracted = ZipApplier.extract(destFile, extractDir) { p ->
                        _uiState.value = ApplyUiState.Applying(p, "ZIP 압축 해제 중... ${(p * 100).toInt()}%")
                    }
                    val innerPatches = ZipApplier.findPatchFiles(extracted)
                    _uiState.value = ApplyUiState.ZipExtracted(extractDir, innerPatches)
                }
            }
            else -> {
                currentPatchFile = destFile
                _uiState.value = ApplyUiState.WaitingForRom(destFile, patchId)
            }
        }
    }

    /**
     * SAF로 선택한 ROM Uri로 패치 적용을 시작합니다.
     */
    fun applyPatch(romUri: Uri) {
        val patchFile = currentPatchFile ?: run {
            _uiState.value = ApplyUiState.Error(
                message = "패치 파일이 준비되지 않았습니다. 먼저 패치를 다운로드하세요.",
                retryable = false
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = ApplyUiState.Applying(0f, "ROM 파일 읽는 중...")
            try {
                val context = getApplication<Application>()

                val romCacheFile = File(context.cacheDir, "rom_input_${System.currentTimeMillis()}")
                val romOutputFile = File(context.cacheDir, "rom_output_${System.currentTimeMillis()}.bin")
                tempFiles.add(romCacheFile)
                tempFiles.add(romOutputFile)

                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(romUri)?.use { input ->
                        romCacheFile.outputStream().use { output -> input.copyTo(output) }
                    } ?: throw IllegalStateException("ROM 파일을 열 수 없습니다")

                    _uiState.value = ApplyUiState.Applying(0.05f, "패치 적용 중...")

                    val result = PatchApplier.apply(
                        romIn = romCacheFile,
                        patch = patchFile,
                        romOut = romOutputFile,
                        progress = { p ->
                            _uiState.value = ApplyUiState.Applying(
                                progress = 0.05f + p * 0.85f,
                                label = "패치 적용 중... ${(p * 100).toInt()}%"
                            )
                        }
                    )

                    result.onSuccess { report ->
                        _uiState.value = ApplyUiState.Applying(0.95f, "결과 파일 저장 중...")
                        val savedPath = saveToDownloads(context, romOutputFile, "patched_rom_${patchId}.bin")
                        val finalReport = report.copy(outputPath = savedPath ?: report.outputPath)
                        _uiState.value = ApplyUiState.Success(finalReport)
                    }.onFailure { e ->
                        _uiState.value = ApplyUiState.Error(
                            message = e.message ?: "패치 적용 실패",
                            retryable = true
                        )
                    }

                    // 임시 ROM 입력 파일 정리 (출력 파일은 Downloads에 저장됨)
                    romCacheFile.delete()
                    tempFiles.remove(romCacheFile)
                }
            } catch (e: Exception) {
                Log.e(TAG, "패치 적용 오류", e)
                _uiState.value = ApplyUiState.Error(
                    message = e.message ?: "알 수 없는 오류가 발생했습니다",
                    retryable = true
                )
            }
        }
    }

    /**
     * 출력 ROM을 Downloads/BerryPatchGuide/ 폴더에 저장합니다.
     */
    private fun saveToDownloads(context: Context, srcFile: File, fileName: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                    put(MediaStore.Downloads.RELATIVE_PATH, "Download/BerryPatchGuide")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return null
                resolver.openOutputStream(uri)?.use { out ->
                    srcFile.inputStream().use { input -> input.copyTo(out) }
                }
                "Downloads/BerryPatchGuide/$fileName"
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
                val destDir = File(downloadsDir, "BerryPatchGuide")
                destDir.mkdirs()
                val destFile = File(destDir, fileName)
                srcFile.copyTo(destFile, overwrite = true)
                destFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e(TAG, "Downloads 저장 실패", e)
            null
        }
    }

    /**
     * 상태를 Idle로 초기화합니다 (재시도 또는 처음부터 다시 시작).
     * 임시 파일도 정리합니다.
     */
    fun resetState() {
        cleanupTempFiles()
        currentPatchFile = null
        _uiState.value = ApplyUiState.Idle
    }

    /**
     * 마지막 다운로드 URL로 재시도합니다.
     */
    fun retryDownload() {
        val url = lastDownloadUrl
        if (url != null) {
            cleanupTempFiles()
            currentPatchFile = null
            downloadPatch(url)
        } else {
            resetState()
        }
    }

    /**
     * ZIP 압축 해제 결과 화면에서 내부 패치 파일을 선택합니다.
     */
    fun selectInnerPatch(patchFile: File) {
        currentPatchFile = patchFile
        _uiState.value = ApplyUiState.WaitingForRom(patchFile, patchId)
    }

    private fun cleanupTempFiles() {
        tempFiles.forEach { file ->
            try {
                if (file.isDirectory) file.deleteRecursively() else file.delete()
            } catch (e: Exception) {
                Log.w(TAG, "임시 파일 삭제 실패: ${file.name}", e)
            }
        }
        tempFiles.clear()
    }

    override fun onCleared() {
        super.onCleared()
        cleanupTempFiles()
    }
}
