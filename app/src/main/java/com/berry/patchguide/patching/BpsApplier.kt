package com.berry.patchguide.patching

import java.io.File
import java.security.MessageDigest
import java.util.zip.CRC32

/**
 * BPS (Binary Patching System) 적용기
 * 사양: byuu의 BPS 형식
 *
 * 구조:
 *  - 매직: "BPS1" (4바이트)
 *  - source_size (VLI)
 *  - target_size (VLI)
 *  - metadata_size (VLI)
 *  - metadata (metadata_size 바이트)
 *  - 액션 레코드들: [data VLI] + 추가 데이터
 *    - mode = data & 3
 *    - length = (data >> 2) + 1
 *    - Mode 0 (SourceRead): source[outputOffset..outputOffset+length] 복사
 *    - Mode 1 (TargetRead): 패치에서 literal 읽기
 *    - Mode 2 (SourceCopy): 오프셋 VLI + source에서 복사
 *    - Mode 3 (TargetCopy): 오프셋 VLI + target에서 복사
 *  - 푸터 (마지막 12바이트): source_crc32, target_crc32, patch_crc32 (4바이트 LE 각)
 */
object BpsApplier {

    fun apply(romIn: File, patchFile: File, romOut: File, progress: (Float) -> Unit): PatchReport {
        val startTime = System.currentTimeMillis()

        val patchData = patchFile.readBytes()
        require(patchData.size >= 4) { "패치 파일이 너무 작습니다" }
        require(
            patchData[0] == 0x42.toByte() &&
                    patchData[1] == 0x50.toByte() &&
                    patchData[2] == 0x53.toByte() &&
                    patchData[3] == 0x31.toByte()
        ) { "BPS 매직바이트가 올바르지 않습니다" }

        // 패치 파일 CRC32 검증
        val patchCrc32Expected = readU32LE(patchData, patchData.size - 4)
        val patchCrc32Actual = crc32(patchData, 0, patchData.size - 4)
        require(patchCrc32Actual == patchCrc32Expected) {
            "패치 파일 CRC32 불일치: expected=${"0x%08X".format(patchCrc32Expected)} actual=${"0x%08X".format(patchCrc32Actual)}"
        }

        var pos = 4 // "BPS1" 건너뜀

        val sourceSize = readVli(patchData, pos).also { pos += vliSize(patchData, pos) }
        val targetSize = readVli(patchData, pos).also { pos += vliSize(patchData, pos) }
        val metadataSize = readVli(patchData, pos).also { pos += vliSize(patchData, pos) }
        pos += metadataSize.toInt() // 메타데이터 건너뜀

        // 소스 CRC32 검증
        val sourceCrc32Expected = readU32LE(patchData, patchData.size - 12)
        val sourceCrc32Actual = crc32File(romIn)
        require(sourceCrc32Actual == sourceCrc32Expected) {
            "소스 ROM CRC32 불일치: expected=${"0x%08X".format(sourceCrc32Expected)} actual=${"0x%08X".format(sourceCrc32Actual)}"
        }

        val sourceData = romIn.readBytes()
        val targetData = ByteArray(targetSize.toInt())

        val patchEnd = patchData.size - 12
        var outputOffset = 0
        var sourceRelativeOffset = 0L
        var targetRelativeOffset = 0L
        var lastProgressTime = System.currentTimeMillis()

        while (pos < patchEnd) {
            val actionData = readVli(patchData, pos)
            pos += vliSize(patchData, pos)

            val mode = (actionData and 3).toInt()
            val length = ((actionData shr 2) + 1).toInt()

            when (mode) {
                0 -> {
                    // SourceRead: source[outputOffset..] → target
                    for (i in 0 until length) {
                        val srcIdx = outputOffset + i
                        targetData[outputOffset + i] = if (srcIdx < sourceData.size) sourceData[srcIdx] else 0
                    }
                    outputOffset += length
                }
                1 -> {
                    // TargetRead: literal bytes from patch
                    for (i in 0 until length) {
                        if (pos < patchEnd) {
                            targetData[outputOffset + i] = patchData[pos++]
                        }
                    }
                    outputOffset += length
                }
                2 -> {
                    // SourceCopy: read signed offset VLI
                    val offsetData = readVli(patchData, pos)
                    pos += vliSize(patchData, pos)
                    val sign = if (offsetData and 1L != 0L) -1L else 1L
                    sourceRelativeOffset += sign * (offsetData shr 1).toLong()
                    for (i in 0 until length) {
                        val srcIdx = sourceRelativeOffset.toInt()
                        targetData[outputOffset + i] = if (srcIdx < sourceData.size) sourceData[srcIdx] else 0
                        sourceRelativeOffset++
                    }
                    outputOffset += length
                }
                3 -> {
                    // TargetCopy: read signed offset VLI
                    val offsetData = readVli(patchData, pos)
                    pos += vliSize(patchData, pos)
                    val sign = if (offsetData and 1L != 0L) -1L else 1L
                    targetRelativeOffset += sign * (offsetData shr 1).toLong()
                    for (i in 0 until length) {
                        val tgtIdx = targetRelativeOffset.toInt()
                        targetData[outputOffset + i] = if (tgtIdx < targetData.size) targetData[tgtIdx] else 0
                        targetRelativeOffset++
                    }
                    outputOffset += length
                }
            }

            val now = System.currentTimeMillis()
            if (now - lastProgressTime >= 100) {
                progress((pos.toFloat() / patchEnd).coerceIn(0f, 0.99f))
                lastProgressTime = now
            }
        }

        romOut.writeBytes(targetData)
        progress(1f)

        // 출력 CRC32 검증
        val targetCrc32Expected = readU32LE(patchData, patchData.size - 8)
        val targetCrc32Actual = crc32(targetData, 0, targetData.size)
        require(targetCrc32Actual == targetCrc32Expected) {
            "출력 ROM CRC32 불일치: expected=${"0x%08X".format(targetCrc32Expected)} actual=${"0x%08X".format(targetCrc32Actual)}"
        }

        val duration = System.currentTimeMillis() - startTime
        val sha256 = computeSha256(romOut)

        return PatchReport(
            outputPath = romOut.absolutePath,
            sha256 = sha256,
            sizeBytes = romOut.length(),
            durationMs = duration,
            appliedFormat = PatchFormat.BPS
        )
    }

    // VLI 값 읽기 (byuu 인코딩, UPS와 동일)
    internal fun readVli(data: ByteArray, startPos: Int): Long {
        var result = 0L
        var shift = 1L
        var pos = startPos
        while (pos < data.size) {
            val x = data[pos++].toInt() and 0xFF
            result += (x and 0x7F).toLong() * shift
            if (x and 0x80 != 0) break
            shift = shift shl 7
            result += shift
        }
        return result
    }

    internal fun vliSize(data: ByteArray, startPos: Int): Int {
        var pos = startPos
        while (pos < data.size) {
            val x = data[pos++].toInt() and 0xFF
            if (x and 0x80 != 0) return pos - startPos
        }
        return pos - startPos
    }

    internal fun encodeVli(value: Long): ByteArray {
        val bytes = mutableListOf<Byte>()
        var v = value
        var shift = 1L
        while (true) {
            val x = (v and 0x7F).toInt()
            v = v shr 7
            if (v == 0L) {
                bytes.add((x or 0x80).toByte())
                break
            }
            bytes.add(x.toByte())
            v -= 1
        }
        return bytes.toByteArray()
    }

    private fun readU32LE(data: ByteArray, offset: Int): Long {
        return ((data[offset].toLong() and 0xFF)) or
                ((data[offset + 1].toLong() and 0xFF) shl 8) or
                ((data[offset + 2].toLong() and 0xFF) shl 16) or
                ((data[offset + 3].toLong() and 0xFF) shl 24)
    }

    internal fun crc32(data: ByteArray, offset: Int, length: Int): Long {
        val crc = CRC32()
        crc.update(data, offset, length)
        return crc.value
    }

    private fun crc32File(file: File): Long {
        val crc = CRC32()
        file.inputStream().buffered(8192).use { stream ->
            val buffer = ByteArray(8192)
            var bytesRead = stream.read(buffer)
            while (bytesRead != -1) {
                crc.update(buffer, 0, bytesRead)
                bytesRead = stream.read(buffer)
            }
        }
        return crc.value
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
