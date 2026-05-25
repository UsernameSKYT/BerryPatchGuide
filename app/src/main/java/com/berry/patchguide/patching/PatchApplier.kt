package com.berry.patchguide.patching

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * 패치 적용 디스패처
 * 포맷 자동 감지 후 적절한 Applier로 라우팅합니다.
 */
object PatchApplier {

    private const val TAG = "PatchApplier"
    private const val MAGIC_READ_BYTES = 8

    /**
     * ROM에 패치를 적용합니다.
     *
     * @param romIn 원본 ROM 파일 (read-only, 변경되지 않음)
     * @param patch 패치 파일
     * @param romOut 출력 ROM 파일 경로
     * @param progress 진행률 콜백 (0f ~ 1f)
     * @return 성공 시 PatchReport, 실패 시 Exception 포함 Result
     */
    suspend fun apply(
        romIn: File,
        patch: File,
        romOut: File,
        progress: (Float) -> Unit
    ): Result<PatchReport> = withContext(Dispatchers.IO) {
        try {
            require(romIn.exists()) { "ROM 파일을 찾을 수 없습니다: ${romIn.name}" }
            require(patch.exists()) { "패치 파일을 찾을 수 없습니다: ${patch.name}" }

            romOut.parentFile?.mkdirs()

            val magicBytes = patch.inputStream().use { it.readNBytes(MAGIC_READ_BYTES) }
            val format = PatchFormat.detect(magicBytes)

            Log.d(TAG, "감지된 포맷: $format, 패치: ${patch.name}, ROM: ${romIn.name}")

            // 입력 ROM SHA-256 로그
            val inputSha256 = computeSha256(romIn)
            Log.d(TAG, "입력 ROM SHA-256: $inputSha256")

            val report = when (format) {
                PatchFormat.IPS -> IpsApplier.apply(romIn, patch, romOut, progress)
                PatchFormat.UPS -> UpsApplier.apply(romIn, patch, romOut, progress)
                PatchFormat.BPS -> BpsApplier.apply(romIn, patch, romOut, progress)
                PatchFormat.XDELTA -> throw UnsupportedOperationException(XdeltaApplier.UNSUPPORTED_MESSAGE)
                PatchFormat.ZIP -> throw UnsupportedOperationException(ZipApplier.UNSUPPORTED_MESSAGE)
                PatchFormat.UNKNOWN -> throw IllegalArgumentException(
                    "지원하지 않는 패치 형식입니다. IPS, UPS, BPS 패치만 자동 적용 가능합니다."
                )
            }

            Log.d(TAG, "패치 적용 완료: ${report.outputPath}, SHA-256: ${report.sha256}")
            Result.success(report)
        } catch (e: Exception) {
            Log.e(TAG, "패치 적용 실패", e)
            Result.failure(e)
        }
    }

    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered(8192).use { stream ->
            val buffer = ByteArray(8192)
            var bytesRead = stream.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = stream.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
