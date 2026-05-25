package com.berry.patchguide.patching

enum class PatchFormat {
    IPS, UPS, BPS, XDELTA, ZIP, UNKNOWN;

    companion object {
        fun detect(bytes: ByteArray): PatchFormat {
            if (bytes.size < 4) return UNKNOWN

            // IPS: "PATCH" (5 bytes)
            if (bytes.size >= 5 &&
                bytes[0] == 0x50.toByte() && // P
                bytes[1] == 0x41.toByte() && // A
                bytes[2] == 0x54.toByte() && // T
                bytes[3] == 0x43.toByte() && // C
                bytes[4] == 0x48.toByte()    // H
            ) return IPS

            // UPS1
            if (bytes[0] == 0x55.toByte() && // U
                bytes[1] == 0x50.toByte() && // P
                bytes[2] == 0x53.toByte() && // S
                bytes[3] == 0x31.toByte()    // 1
            ) return UPS

            // BPS1
            if (bytes[0] == 0x42.toByte() && // B
                bytes[1] == 0x50.toByte() && // P
                bytes[2] == 0x53.toByte() && // S
                bytes[3] == 0x31.toByte()    // 1
            ) return BPS

            // xdelta VCDIFF: 0xD6 0xC3 0xC4 0x00
            if (bytes[0] == 0xD6.toByte() &&
                bytes[1] == 0xC3.toByte() &&
                bytes[2] == 0xC4.toByte() &&
                bytes[3] == 0x00.toByte()
            ) return XDELTA

            // ZIP: 0x50 0x4B 0x03 0x04
            if (bytes[0] == 0x50.toByte() && // P
                bytes[1] == 0x4B.toByte() && // K
                bytes[2] == 0x03.toByte() &&
                bytes[3] == 0x04.toByte()
            ) return ZIP

            return UNKNOWN
        }
    }
}
