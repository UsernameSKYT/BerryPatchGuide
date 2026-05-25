package com.berry.patchguide.patching

import java.io.File

/**
 * xdelta3 (VCDIFF) 적용기 — Phase 4-B에서는 미구현 (골격만)
 *
 * xdelta3 적용은 네이티브 라이브러리(NDK) 통합이 필요합니다.
 * 향후 Phase에서 NDK xdelta3 연동으로 구현 예정입니다.
 */
object XdeltaApplier {

    const val UNSUPPORTED_MESSAGE =
        "xdelta/VCDIFF 형식은 현재 지원되지 않습니다.\n" +
                "이 형식의 패치는 공식 xdelta3 도구를 사용하여 PC에서 적용하거나,\n" +
                "향후 업데이트에서 앱 내 지원 예정입니다."

    @Suppress("UNUSED_PARAMETER")
    fun apply(romIn: File, patchFile: File, romOut: File, progress: (Float) -> Unit): PatchReport {
        throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    }
}
