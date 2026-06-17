package com.berry.patchguide.patching

import java.io.File
import java.security.MessageDigest

/**
 * xdelta3 (VCDIFF) 패치 적용기 — NDK 구현
 *
 * libxdelta3_jni.so 를 통해 네이티브 xdelta3 디코더를 호출합니다.
 */
object XdeltaApplier {

    @Suppress("unused")
    const val UNSUPPORTED_MESSAGE =
        "xdelta/VCDIFF 형식은 현재 지원되지 않습니다.\n" +
                "이 형식의 패치는 공식 xdelta3 도구를 사용하여 PC에서 적용하거나,\n" +
                "향후 업데이트에서 앱 내 지원 예정입니다."

    fun apply(romIn: File, patchFile: File, romOut: File, progress: (Float) -> Unit): PatchReport {
        val startMs = System.currentTimeMillis()

        val result = XdeltaNative.applyPatch(
            sourcePath = romIn.absolutePath,
            patchPath  = patchFile.absolutePath,
            outputPath = romOut.absolutePath,
            callback   = XdeltaNative.ProgressCallback { fraction -> progress(fraction) }
        )

        if (result != 0) {
            throw RuntimeException("xdelta3 패치 적용 실패 (오류 코드: $result)")
        }

        val sha256 = computeSha256(romOut)
        val durationMs = System.currentTimeMillis() - startMs

        return PatchReport(
            outputPath    = romOut.absolutePath,
            sha256        = sha256,
            sizeBytes     = romOut.length(),
            durationMs    = durationMs,
            appliedFormat = PatchFormat.XDELTA
        )
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
