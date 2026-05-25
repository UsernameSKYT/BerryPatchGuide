package com.berry.patchguide.patching

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.CRC32

/**
 * BPS 적용기 라운드트립 단위 테스트
 *
 * 간단한 BPS 패치 벡터를 직접 생성하여 검증합니다.
 */
class BpsApplierTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

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
     * 최소 BPS 패치 생성:
     * Mode 0 (SourceRead): 소스 전체를 그대로 복사
     */
    private fun buildBpsSourceReadPatch(sourceData: ByteArray, targetData: ByteArray): ByteArray {
        val buf = ByteArrayOutputStream()
        buf.write("BPS1".toByteArray())
        buf.write(encodeVli(sourceData.size.toLong()))
        buf.write(encodeVli(targetData.size.toLong()))
        buf.write(encodeVli(0L))  // metadata_size = 0

        // Mode 0 (SourceRead), length = targetData.size
        // action = (length-1) << 2 | mode
        val actionData = ((targetData.size - 1).toLong() shl 2) or 0L
        buf.write(encodeVli(actionData))

        val body = buf.toByteArray()

        val sourceCrc = CRC32().also { it.update(sourceData) }.value
        val targetCrc = CRC32().also { it.update(targetData) }.value

        val patchCrc32 = CRC32()
        patchCrc32.update(body)
        patchCrc32.update(u32LE(sourceCrc))
        patchCrc32.update(u32LE(targetCrc))
        val patchCrc = patchCrc32.value

        val full = ByteArrayOutputStream()
        full.write(body)
        full.write(u32LE(sourceCrc))
        full.write(u32LE(targetCrc))
        full.write(u32LE(patchCrc))
        return full.toByteArray()
    }

    /**
     * Mode 1 (TargetRead): literal bytes로 덮어쓰기
     */
    private fun buildBpsTargetReadPatch(sourceData: ByteArray, targetData: ByteArray): ByteArray {
        val buf = ByteArrayOutputStream()
        buf.write("BPS1".toByteArray())
        buf.write(encodeVli(sourceData.size.toLong()))
        buf.write(encodeVli(targetData.size.toLong()))
        buf.write(encodeVli(0L))  // metadata_size = 0

        // Mode 1 (TargetRead), length = targetData.size
        val actionData = ((targetData.size - 1).toLong() shl 2) or 1L
        buf.write(encodeVli(actionData))
        buf.write(targetData)  // literal data

        val body = buf.toByteArray()

        val sourceCrc = CRC32().also { it.update(sourceData) }.value
        val targetCrc = CRC32().also { it.update(targetData) }.value

        val patchCrc32 = CRC32()
        patchCrc32.update(body)
        patchCrc32.update(u32LE(sourceCrc))
        patchCrc32.update(u32LE(targetCrc))
        val patchCrc = patchCrc32.value

        val full = ByteArrayOutputStream()
        full.write(body)
        full.write(u32LE(sourceCrc))
        full.write(u32LE(targetCrc))
        full.write(u32LE(patchCrc))
        return full.toByteArray()
    }

    @Test
    fun `BPS VLI 인코딩 디코딩 일치`() {
        val testValues = longArrayOf(0, 1, 127, 128, 255, 256, 1000, 65535)
        for (v in testValues) {
            val encoded = BpsApplier.encodeVli(v)
            val decoded = BpsApplier.readVli(encoded, 0)
            assertEquals("BPS VLI $v 인코딩/디코딩 불일치", v, decoded)
        }
    }

    @Test
    fun `BPS Mode 0 SourceRead 라운드트립`() {
        val source = byteArrayOf(1, 2, 3, 4, 5)
        val target = source.copyOf()  // Mode 0: target == source
        val patch = buildBpsSourceReadPatch(source, target)

        val romIn = tempFolder.newFile("rom.bin").also { it.writeBytes(source) }
        val patchFile = tempFolder.newFile("patch.bps").also { it.writeBytes(patch) }
        val romOut = File(tempFolder.root, "rom_out.bin")

        BpsApplier.apply(romIn, patchFile, romOut) {}

        assertArrayEquals("BPS SourceRead: target과 result 불일치", target, romOut.readBytes())
    }

    @Test
    fun `BPS Mode 1 TargetRead 라운드트립`() {
        val source = byteArrayOf(1, 2, 3, 4, 5)
        val target = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte())
        val patch = buildBpsTargetReadPatch(source, target)

        val romIn = tempFolder.newFile("rom2.bin").also { it.writeBytes(source) }
        val patchFile = tempFolder.newFile("patch2.bps").also { it.writeBytes(patch) }
        val romOut = File(tempFolder.root, "rom2_out.bin")

        BpsApplier.apply(romIn, patchFile, romOut) {}

        assertArrayEquals("BPS TargetRead: target과 result 불일치", target, romOut.readBytes())
    }

    @Test
    fun `BPS 포맷 감지`() {
        val magic = byteArrayOf(0x42, 0x50, 0x53, 0x31)  // "BPS1"
        assertEquals(PatchFormat.BPS, PatchFormat.detect(magic))
    }

    @Test
    fun `PatchFormat xdelta 감지`() {
        val magic = byteArrayOf(0xD6.toByte(), 0xC3.toByte(), 0xC4.toByte(), 0x00)
        assertEquals(PatchFormat.XDELTA, PatchFormat.detect(magic))
    }

    @Test
    fun `PatchFormat ZIP 감지`() {
        val magic = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
        assertEquals(PatchFormat.ZIP, PatchFormat.detect(magic))
    }

    @Test
    fun `PatchFormat UNKNOWN 감지`() {
        val magic = byteArrayOf(0x00, 0x00, 0x00, 0x00)
        assertEquals(PatchFormat.UNKNOWN, PatchFormat.detect(magic))
    }
}
