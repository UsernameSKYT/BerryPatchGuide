package com.berry.patchguide.patching

import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest

/**
 * IPS (International Patching System) 적용기
 * 표준 IPS 사양: https://zerosoft.zophar.net/ips.php
 *
 * 구조:
 *  - 헤더: "PATCH" (5바이트)
 *  - 레코드: [오프셋 3바이트 BE][크기 2바이트 BE][데이터 or RLE]
 *    - 크기 == 0: RLE 레코드 → [반복횟수 2바이트 BE][값 1바이트]
 *  - EOF 마커: 0x45 0x4F 0x46 ("EOF")
 */
object IpsApplier {

    fun apply(romIn: File, patchFile: File, romOut: File, progress: (Float) -> Unit): PatchReport {
        val startTime = System.currentTimeMillis()

        // 입력 ROM을 출력으로 복사 (원본 불변)
        romIn.copyTo(romOut, overwrite = true)

        val patchData = patchFile.readBytes()
        val patchSize = patchData.size.toFloat()

        var lastProgressTime = System.currentTimeMillis()

        RandomAccessFile(romOut, "rw").use { out ->
            var pos = 5 // "PATCH" 헤더 건너뜀

            while (pos + 3 <= patchData.size) {
                // EOF 마커 확인
                if (patchData[pos] == 0x45.toByte() &&
                    patchData[pos + 1] == 0x4F.toByte() &&
                    patchData[pos + 2] == 0x46.toByte()
                ) break

                if (pos + 5 > patchData.size) break

                // 오프셋 (3바이트 big-endian)
                val offset = ((patchData[pos].toInt() and 0xFF) shl 16) or
                        ((patchData[pos + 1].toInt() and 0xFF) shl 8) or
                        (patchData[pos + 2].toInt() and 0xFF)
                pos += 3

                // 크기 (2바이트 big-endian)
                val size = ((patchData[pos].toInt() and 0xFF) shl 8) or
                        (patchData[pos + 1].toInt() and 0xFF)
                pos += 2

                if (size == 0) {
                    // RLE 레코드
                    if (pos + 3 > patchData.size) break
                    val runLength = ((patchData[pos].toInt() and 0xFF) shl 8) or
                            (patchData[pos + 1].toInt() and 0xFF)
                    val value = patchData[pos + 2]
                    pos += 3

                    out.seek(offset.toLong())
                    val runData = ByteArray(runLength) { value }
                    out.write(runData)
                } else {
                    // 일반 레코드
                    if (pos + size > patchData.size) break
                    out.seek(offset.toLong())
                    out.write(patchData, pos, size)
                    pos += size
                }

                // 100ms 간격으로 진행률 콜백
                val now = System.currentTimeMillis()
                if (now - lastProgressTime >= 100) {
                    progress(pos / patchSize)
                    lastProgressTime = now
                }
            }
        }

        progress(1f)
        val duration = System.currentTimeMillis() - startTime
        val sha256 = computeSha256(romOut)

        return PatchReport(
            outputPath = romOut.absolutePath,
            sha256 = sha256,
            sizeBytes = romOut.length(),
            durationMs = duration,
            appliedFormat = PatchFormat.IPS
        )
    }

    internal fun computeSha256(file: File): String {
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
