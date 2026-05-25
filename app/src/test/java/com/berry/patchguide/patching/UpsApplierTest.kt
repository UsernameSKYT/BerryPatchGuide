package com.berry.patchguide.patching

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.CRC32

/**
 * UPS 적용기 라운드트립 단위 테스트
 */
class UpsApplierTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    /**
     * VLI (byuu 인코딩) 인코딩 헬퍼
     */
    private fun encodeVli(value: Long): ByteArray {
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

    private fun u32LE(value: Long): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }

    /**
     * UPS 패치 생성:
     * patches: List of (relativeOffset, xorData)
     */
    private fun buildUpsPatch(
        sourceData: ByteArray,
        targetData: ByteArray,
        patches: List<Pair<Long, ByteArray>>
    ): ByteArray {
        val buf = ByteArrayOutputStream()
        buf.write("UPS1".toByteArray())
        buf.write(encodeVli(sourceData.size.toLong()))
        buf.write(encodeVli(targetData.size.toLong()))

        for ((relOffset, xorData) in patches) {
            buf.write(encodeVli(relOffset))
            buf.write(xorData)
            buf.write(0)  // null terminator
        }

        val bodyBytes = buf.toByteArray()

        // CRC32 계산
        val sourceCrc = CRC32().also { it.update(sourceData) }.value
        val targetCrc = CRC32().also { it.update(targetData) }.value
        val patchCrc = CRC32().also { it.update(bodyBytes) }.also {
            it.update(u32LE(sourceCrc))
            it.update(u32LE(targetCrc))
        }.value

        val fullBuf = ByteArrayOutputStream()
        fullBuf.write(bodyBytes)
        fullBuf.write(u32LE(sourceCrc))
        fullBuf.write(u32LE(targetCrc))
        fullBuf.write(u32LE(patchCrc))
        return fullBuf.toByteArray()
    }

    @Test
    fun `UPS VLI 인코딩 디코딩 일치`() {
        val testValues = longArrayOf(0, 1, 127, 128, 255, 256, 1000, 65535, 100000)
        for (v in testValues) {
            val encoded = encodeVli(v)
            val decoded = UpsApplier.readVli(encoded, 0)
            assertEquals("VLI $v 인코딩/디코딩 불일치", v, decoded)
        }
    }

    @Test
    fun `UPS 단순 XOR 패치 라운드트립`() {
        val source = byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07)
        val target = source.copyOf()
        target[2] = (target[2].toInt() xor 0xFF).toByte()
        target[3] = (target[3].toInt() xor 0xAA).toByte()

        // 패치: offset 2에서 2바이트 XOR
        // relOffset = 2 - 0 - 1 = 1 (첫 번째 패치이므로, outputOffset 시작 0, +relOffset+1 = offset 2)
        val xorData = byteArrayOf(0xFF.toByte(), 0xAA.toByte())
        val patch = buildUpsPatch(source, target, listOf(Pair(1L, xorData)))

        val romIn = tempFolder.newFile("rom.bin").also { it.writeBytes(source) }
        val patchFile = tempFolder.newFile("patch.ups").also { it.writeBytes(patch) }
        val romOut = File(tempFolder.root, "rom_out.bin")

        UpsApplier.apply(romIn, patchFile, romOut) {}

        assertArrayEquals("UPS 적용 결과가 예상 target과 다름", target, romOut.readBytes())
    }

    @Test
    fun `UPS 포맷 감지`() {
        val magic = byteArrayOf(0x55, 0x50, 0x53, 0x31)  // "UPS1"
        assertEquals(PatchFormat.UPS, PatchFormat.detect(magic))
    }

    @Test
    fun `UPS CRC32 검증 유틸리티`() {
        val data = "Hello, World!".toByteArray()
        val crc = UpsApplier.crc32(data, 0, data.size)
        assertTrue("CRC32는 0보다 커야 함", crc > 0)
    }
}
