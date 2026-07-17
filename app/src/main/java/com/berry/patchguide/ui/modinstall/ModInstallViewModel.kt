package com.berry.patchguide.ui.modinstall

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.berry.patchguide.patching.ModInstaller
import com.berry.patchguide.patching.PatchFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

sealed class ModInstallUiState {
    data object Idle : ModInstallUiState()
    data class Extracting(val progress: Float) : ModInstallUiState()
    data class Ready(
        val extractDir: File,
        val modName: String,
        val fileCount: Int,
        val totalSizeBytes: Long
    ) : ModInstallUiState()
    data class Installing(val progress: Float) : ModInstallUiState()
    data class Success(val destLabel: String) : ModInstallUiState()
    data class Error(val message: String, val retryable: Boolean = true) : ModInstallUiState()
}

@HiltViewModel
class ModInstallViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val TAG = "ModInstallViewModel"

    private val _uiState = MutableStateFlow<ModInstallUiState>(ModInstallUiState.Idle)
    val uiState: StateFlow<ModInstallUiState> = _uiState.asStateFlow()

    // 압축 해제된 파일 (설치 단계에서 사용)
    private var extractedDir: File? = null

    private val tempFiles = mutableListOf<File>()

    /**
     * 사용자가 선택한 ZIP 파일 Uri를 받아 압축을 해제하고 요약 정보를 표시합니다.
     */
    fun onZipSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ModInstallUiState.Extracting(0f)
            try {
                val context = getApplication<Application>()
                val zipFile = File(context.cacheDir, "mod_zip_${System.currentTimeMillis()}.zip")
                tempFiles.add(zipFile)

                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        zipFile.outputStream().use { output -> input.copyTo(output) }
                    } ?: throw IllegalStateException("파일을 열 수 없습니다.")

                    val headBytes = zipFile.inputStream().use { it.readNBytes(512) }
                    if (PatchFormat.detect(headBytes) != PatchFormat.ZIP) {
                        throw IllegalArgumentException(
                            "ZIP 파일이 아닙니다. 모드 설치는 ZIP 압축 파일만 지원합니다."
                        )
                    }

                    val extractDir = File(context.cacheDir, "mod_extract_${System.currentTimeMillis()}")
                    tempFiles.add(extractDir)

                    val extracted = ModInstaller.extractZip(zipFile, extractDir) { p ->
                        _uiState.value = ModInstallUiState.Extracting(p)
                    }

                    if (extracted.isEmpty()) {
                        throw IllegalStateException("압축 파일 안에 내용이 없습니다.")
                    }

                    extractedDir = extractDir
                    val fileList = extracted.filter { it.isFile }
                    val modName = extractDir.listFiles()
                        ?.firstOrNull { it.isDirectory }?.name
                        ?: extractDir.listFiles()?.firstOrNull()?.name
                        ?: "모드"

                    _uiState.value = ModInstallUiState.Ready(
                        extractDir = extractDir,
                        modName = modName,
                        fileCount = fileList.size,
                        totalSizeBytes = ModInstaller.totalSize(fileList)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "ZIP 처리 실패", e)
                _uiState.value = ModInstallUiState.Error(
                    message = e.message ?: "ZIP 파일 처리 중 오류가 발생했습니다.",
                    retryable = true
                )
            }
        }
    }

    /**
     * 사용자가 선택한 설치 위치(SAF 트리 Uri)로 압축 해제된 파일들을 복사합니다.
     */
    fun onDestinationSelected(treeUri: Uri) {
        val srcDir = extractedDir ?: run {
            _uiState.value = ModInstallUiState.Error("먼저 모드 ZIP 파일을 선택해주세요.", retryable = false)
            return
        }

        viewModelScope.launch {
            _uiState.value = ModInstallUiState.Installing(0f)
            try {
                val context = getApplication<Application>()
                withContext(Dispatchers.IO) {
                    ModInstaller.copyToTree(context, srcDir, treeUri) { p ->
                        _uiState.value = ModInstallUiState.Installing(p)
                    }
                }
                val destLabel = treeUri.lastPathSegment ?: "선택한 폴더"
                _uiState.value = ModInstallUiState.Success(destLabel)
                cleanupTempFiles()
            } catch (e: Exception) {
                Log.e(TAG, "모드 설치 실패", e)
                _uiState.value = ModInstallUiState.Error(
                    message = e.message ?: "모드 설치 중 오류가 발생했습니다.",
                    retryable = true
                )
            }
        }
    }

    fun resetState() {
        cleanupTempFiles()
        extractedDir = null
        _uiState.value = ModInstallUiState.Idle
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
