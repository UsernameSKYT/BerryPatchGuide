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
import com.berry.patchguide.data.repository.PatchRepository
import com.berry.patchguide.patching.PatchApplier
import com.berry.patchguide.patching.PatchFormat
import com.berry.patchguide.patching.PatchReport
import com.berry.patchguide.patching.XdeltaApplier
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
    data class Downloading(val progress: Float) : ApplyUiState()
    // Step 1 완료: 패치 다운로드됨 또는 이미 있음 → ROM 선택 대기
    data class WaitingForRom(val patchFile: File, val patchId: String) : ApplyUiState()
    // Step 3: 패치 적용 중
    data class Applying(val progress: Float) : ApplyUiState()
    // Step 4: 성공
    data class Success(val report: PatchReport) : ApplyUiState()
    // Step 4: 실패
    data class Error(val message: String) : ApplyUiState()
    // xdelta 미지원 안내
    data class XdeltaUnsupported(val patchId: String) : ApplyUiState()
    // ZIP 압축 해제 결과
    data class ZipExtracted(val destDir: File, val innerPatches: List<File>) : ApplyUiState()
}

@HiltViewModel
class ApplyPatchViewModel @Inject constructor(
    application: Application,
    private val patchRepository: PatchRepository,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val TAG = "ApplyPatchViewModel"

    private val _uiState = MutableStateFlow<ApplyUiState>(ApplyUiState.Idle)
    val uiState: StateFlow<ApplyUiState> = _uiState.asStateFlow()

    private val patchId: String = savedStateHandle.get<String>("patchId") ?: ""

    // 현재 선택된 패치 파일 (다운로드 완료 또는 사용자 지정)
    private var currentPatchFile: File? = null
    // 현재 선택된 ROM Uri
    private var selectedRomUri: Uri? = null

    /**
     * 패치 다운로드 URL로 다운로드를 시작합니다.
     */
    fun downloadPatch(url: String) {
        viewModelScope.launch {
            _uiState.value = ApplyUiState.Downloading(0f)
            try {
                val destFile = File(
                    getApplication<Application>().cacheDir,
                    "patch_${patchId}_${System.currentTimeMillis()}"
                )
                withContext(Dispatchers.IO) {
                    patchRepository.downloadPatch(url, destFile) { p ->
                        _uiState.value = ApplyUiState.Downloading(p)
                    }
                }

                // 포맷 감지
                val magicBytes = destFile.inputStream().use { it.readNBytes(8) }
                val format = PatchFormat.detect(magicBytes)

                when (format) {
                    PatchFormat.XDELTA -> {
                        _uiState.value = ApplyUiState.XdeltaUnsupported(patchId)
                    }
                    PatchFormat.ZIP -> {
                        // ZIP 압축 해제
                        val extractDir = File(
                            getApplication<Application>().cacheDir,
                            "zip_extract_${patchId}"
                        )
                        withContext(Dispatchers.IO) {
                            val extracted = ZipApplier.extract(destFile, extractDir) { p ->
                                _uiState.value = ApplyUiState.Applying(p)
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
            } catch (e: Exception) {
                Log.e(TAG, "다운로드 실패", e)
                _uiState.value = ApplyUiState.Error("다운로드 실패: ${e.message}")
            }
        }
    }

    /**
     * 사용자가 직접 선택한 패치 파일을 설정합니다.
     */
    fun setPatchFileFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val destFile = File(context.cacheDir, "patch_manual_${System.currentTimeMillis()}")
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                currentPatchFile = destFile

                val magicBytes = destFile.inputStream().use { it.readNBytes(8) }
                val format = PatchFormat.detect(magicBytes)
                when (format) {
                    PatchFormat.XDELTA -> _uiState.value = ApplyUiState.XdeltaUnsupported(patchId)
                    PatchFormat.ZIP -> {
                        val extractDir = File(context.cacheDir, "zip_extract_manual_${System.currentTimeMillis()}")
                        withContext(Dispatchers.IO) {
                            val extracted = ZipApplier.extract(destFile, extractDir) { p ->
                                _uiState.value = ApplyUiState.Applying(p)
                            }
                            val innerPatches = ZipApplier.findPatchFiles(extracted)
                            _uiState.value = ApplyUiState.ZipExtracted(extractDir, innerPatches)
                        }
                    }
                    else -> _uiState.value = ApplyUiState.WaitingForRom(destFile, patchId)
                }
            } catch (e: Exception) {
                _uiState.value = ApplyUiState.Error("패치 파일 읽기 실패: ${e.message}")
            }
        }
    }

    /**
     * SAF로 선택한 ROM Uri로 패치 적용을 시작합니다.
     */
    fun applyPatch(romUri: Uri) {
        val patchFile = currentPatchFile ?: run {
            _uiState.value = ApplyUiState.Error("패치 파일이 준비되지 않았습니다. 먼저 패치를 다운로드하세요.")
            return
        }

        viewModelScope.launch {
            _uiState.value = ApplyUiState.Applying(0f)
            try {
                val context = getApplication<Application>()

                // ROM을 임시 캐시 파일로 복사
                val romCacheFile = File(context.cacheDir, "rom_input_${System.currentTimeMillis()}")
                val romOutputFile = File(context.cacheDir, "rom_output_${System.currentTimeMillis()}.bin")

                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(romUri)?.use { input ->
                        romCacheFile.outputStream().use { output -> input.copyTo(output) }
                    } ?: throw IllegalStateException("ROM 파일을 열 수 없습니다")

                    val result = PatchApplier.apply(
                        romIn = romCacheFile,
                        patch = patchFile,
                        romOut = romOutputFile,
                        progress = { p -> _uiState.value = ApplyUiState.Applying(p) }
                    )

                    result.onSuccess { report ->
                        // 다운로드 폴더에 저장
                        val savedPath = saveToDownloads(context, romOutputFile, "patched_rom_${patchId}.bin")
                        val finalReport = report.copy(outputPath = savedPath ?: report.outputPath)
                        _uiState.value = ApplyUiState.Success(finalReport)
                    }.onFailure { e ->
                        _uiState.value = ApplyUiState.Error(e.message ?: "패치 적용 실패")
                    }

                    // 임시 파일 정리
                    romCacheFile.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "패치 적용 오류", e)
                _uiState.value = ApplyUiState.Error(e.message ?: "알 수 없는 오류가 발생했습니다")
            }
        }
    }

    /**
     * 출력 ROM을 Downloads/BerryPatchGuide/ 폴더에 저장합니다.
     * @return 저장된 파일 경로 (실패 시 null)
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

    fun resetState() {
        _uiState.value = ApplyUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        // 임시 파일 정리
        currentPatchFile?.delete()
    }
}
