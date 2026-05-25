package com.berry.patchguide.patching

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * IPS 적용기 라운드트립 단위 테스트
 */
class IpsApplierTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    /**
     * 합성 IPS 패치 생성 헬퍼:
     * PATCH + [offset 3B BE][size 2B BE][data] + EOF
     */
    private fun buildIpsPatch(records: List<Triple<Int, Int, ByteArray>>): ByteArray {
        val buf = mutableListOf<Byte>()
        // Header
        buf.addAll("PATCH".toByteArray().toList())
        for ((offset, _, data) in records) {
            // Offset (3 bytes BE)
            buf.add(((offset shr 16) and 0xFF).toByte())
            buf.add(((offset shr 8) and 0xFF).toByte())
            buf.add((offset and 0xFF).toByte())
            // Size (2 bytes BE)
            buf.add(((data.size shr 8) and 0xFF).toByte())
            buf.add((data.size and 0xFF).toByte())
            // Data
            buf.addAll(data.toList())
        }
        // EOF
        buf.addAll(byteArrayOf(0x45, 0x4F, 0x46).toList())
        return buf.toByteArray()
    }

    @Test
    fun `IPS 단순 레코드 적용 라운드트립`() {
        val rom = ByteArray(16) { it.toByte() }  // 0,1,2,...,15
        val patch = buildIpsPatch(
            listOf(
                Triple(2, 3, byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte()))
            )
        )

        val romIn = tempFolder.newFile("rom.bin").also { it.writeBytes(rom) }
        val patchFile = tempFolder.newFile("patch.ips").also { it.writeBytes(patch) }
        val romOut = File(tempFolder.root, "rom_out.bin")

        val report = IpsApplier.apply(romIn, patchFile, romOut) {}

        val result = romOut.readBytes()
        assertEquals(16, result.size)
        assertEquals(0xAA.toByte(), result[2])
        assertEquals(0xBB.toByte(), result[3])
        assertEquals(0xCC.toByte(), result[4])
        // 나머지는 원본과 동일
        assertEquals(0.toByte(), result[0])
        assertEquals(1.toByte(), result[1])
        assertEquals(5.toByte(), result[5])
        assertEquals(PatchFormat.IPS, report.appliedFormat)
        assertTrue(report.sha256.isNotEmpty())
    }

    @Test
    fun `IPS RLE 레코드 적용`() {
        val rom = ByteArray(16) { 0x00 }
        // RLE: offset=4, size=0(RLE), runLength=5, value=0xFF
        val buf = mutableListOf<Byte>()
        buf.addAll("PATCH".toByteArray().toList())
        // offset=4 (3 bytes BE)
        buf.add(0); buf.add(0); buf.add(4)
        // size=0 (2 bytes BE) → RLE
        buf.add(0); buf.add(0)
        // runLength=5 (2 bytes BE)
        buf.add(0); buf.add(5)
        // value
        buf.add(0xFF.toByte())
        buf.addAll(byteArrayOf(0x45, 0x4F, 0x46).toList())
        val patch = buf.toByteArray()

        val romIn = tempFolder.newFile("rom_rle.bin").also { it.writeBytes(rom) }
        val patchFile = tempFolder.newFile("patch_rle.ips").also { it.writeBytes(patch) }
        val romOut = File(tempFolder.root, "rom_rle_out.bin")

        IpsApplier.apply(romIn, patchFile, romOut) {}

        val result = romOut.readBytes()
        assertEquals(16, result.size)
        for (i in 4 until 9) {
            assertEquals("index $i should be 0xFF", 0xFF.toByte(), result[i])
        }
        // 나머지는 0
        assertEquals(0.toByte(), result[3])
        assertEquals(0.toByte(), result[9])
    }

    @Test
    fun `IPS 포맷 감지`() {
        val magic = byteArrayOf(0x50, 0x41, 0x54, 0x43, 0x48)  // "PATCH"
        assertEquals(PatchFormat.IPS, PatchFormat.detect(magic))
    }

    @Test
    fun `IPS 빈 패치 적용 시 ROM 원본 동일`() {
        val rom = byteArrayOf(1, 2, 3, 4, 5)
        val patch = "PATCH".toByteArray() + byteArrayOf(0x45, 0x4F, 0x46)  // PATCH + EOF

        val romIn = tempFolder.newFile("rom_empty.bin").also { it.writeBytes(rom) }
        val patchFile = tempFolder.newFile("patch_empty.ips").also { it.writeBytes(patch) }
        val romOut = File(tempFolder.root, "rom_empty_out.bin")

        IpsApplier.apply(romIn, patchFile, romOut) {}

        assertArrayEquals(rom, romOut.readBytes())
    }
}
