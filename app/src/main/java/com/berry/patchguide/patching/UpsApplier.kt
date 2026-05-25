package com.berry.patchguide.patching

import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.zip.CRC32

/**
 * UPS (Universal Patching System) 적용기
 * 사양: byuu의 UPS 형식
 *
 * 구조:
 *  - 매직: "UPS1" (4바이트)
 *  - input_size (가변 정수 VLI)
 *  - output_size (VLI)
 *  - 패치 레코드: [상대 오프셋 VLI] [XOR 데이터 ... 0x00 종료자]
 *  - 푸터 (마지막 12바이트): input_crc32, output_crc32, patch_crc32 (각 4바이트 LE)
 *
 * VLI 인코딩: 비트 7이 1이면 종료, 0이면 계속 읽음
 */
object UpsApplier {

    fun apply(romIn: File, patchFile: File, romOut: File, progress: (Float) -> Unit): PatchReport {
        val startTime = System.currentTimeMillis()

        val patchData = patchFile.readBytes()
        require(patchData.size >= 4) { "패치 파일이 너무 작습니다" }
        require(
            patchData[0] == 0x55.toByte() &&
                    patchData[1] == 0x50.toByte() &&
                    patchData[2] == 0x53.toByte() &&
                    patchData[3] == 0x31.toByte()
        ) { "UPS 매직바이트가 올바르지 않습니다" }

        // 패치 파일 자체의 CRC32 검증 (마지막 4바이트 제외)
        val patchCrc32Expected = readU32LE(patchData, patchData.size - 4)
        val patchCrc32Actual = crc32(patchData, 0, patchData.size - 4)
        require(patchCrc32Actual == patchCrc32Expected) {
            "패치 파일 CRC32 불일치: expected=${"0x%08X".format(patchCrc32Expected)} actual=${"0x%08X".format(patchCrc32Actual)}"
        }

        var pos = 4 // "UPS1" 건너뜀
        val inputSize = readVli(patchData, pos).also { pos += vliSize(patchData, pos) }
        val outputSize = readVli(patchData, pos).also { pos += vliSize(patchData, pos) }

        // 입력 ROM CRC32 확인
        val inputCrc32Expected = readU32LE(patchData, patchData.size - 12)
        val inputCrc32Actual = crc32File(romIn)
        require(inputCrc32Actual == inputCrc32Expected) {
            "입력 ROM CRC32 불일치: expected=${"0x%08X".format(inputCrc32Expected)} actual=${"0x%08X".format(inputCrc32Actual)}"
        }

        val inputData = romIn.readBytes()
        val outputData = ByteArray(outputSize.toInt()) { index ->
            if (index < inputData.size) inputData[index] else 0
        }

        val patchEnd = patchData.size - 12 // 마지막 12바이트는 CRC32 푸터
        var outputOffset = 0L
        val patchSizeFloat = (patchEnd - pos).toFloat()
        var lastProgressTime = System.currentTimeMillis()

        while (pos < patchEnd) {
            // 상대 오프셋 읽기
            val relOffset = readVli(patchData, pos)
            pos += vliSize(patchData, pos)
            outputOffset += relOffset + 1

            // XOR 데이터 적용 (0x00까지)
            while (pos < patchEnd) {
                val xorByte = patchData[pos++]
                if (xorByte == 0x00.toByte()) break
                val outIdx = outputOffset.toInt()
                if (outIdx < outputData.size) {
                    outputData[outIdx] = (outputData[outIdx].toInt() xor xorByte.toInt()).toByte()
                }
                outputOffset++
            }

            val now = System.currentTimeMillis()
            if (now - lastProgressTime >= 100) {
                val processed = (pos - (patchData.size - patchEnd - 12)).toFloat()
                progress((processed / patchSizeFloat).coerceIn(0f, 0.99f))
                lastProgressTime = now
            }
        }

        romOut.writeBytes(outputData)
        progress(1f)

        // 출력 ROM CRC32 검증
        val outputCrc32Expected = readU32LE(patchData, patchData.size - 8)
        val outputCrc32Actual = crc32(outputData, 0, outputData.size)
        require(outputCrc32Actual == outputCrc32Expected) {
            "출력 ROM CRC32 불일치: expected=${"0x%08X".format(outputCrc32Expected)} actual=${"0x%08X".format(outputCrc32Actual)}"
        }

        val duration = System.currentTimeMillis() - startTime
        val sha256 = computeSha256(romOut)

        return PatchReport(
            outputPath = romOut.absolutePath,
            sha256 = sha256,
            sizeBytes = romOut.length(),
            durationMs = duration,
            appliedFormat = PatchFormat.UPS
        )
    }

    // VLI 값 읽기 (byuu 인코딩)
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

    // VLI가 몇 바이트인지 계산
    internal fun vliSize(data: ByteArray, startPos: Int): Int {
        var pos = startPos
        while (pos < data.size) {
            val x = data[pos++].toInt() and 0xFF
            if (x and 0x80 != 0) return pos - startPos
        }
        return pos - startPos
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
