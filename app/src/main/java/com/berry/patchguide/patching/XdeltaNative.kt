package com.berry.patchguide.patching

/**
 * xdelta3 NDK 네이티브 바인딩
 *
 * 네이티브 라이브러리 libxdelta3_jni.so 를 로드하고
 * VCDIFF 패치 적용 함수를 노출합니다.
 */
object XdeltaNative {

    init {
        System.loadLibrary("xdelta3_jni")
    }

    /**
     * 진행률 콜백 인터페이스 (C 코드에서 onProgress(Float) 를 호출)
     */
    fun interface ProgressCallback {
        fun onProgress(fraction: Float)
    }

    /**
     * VCDIFF 패치를 적용합니다.
     *
     * @param sourcePath  원본 ROM 파일 절대 경로
     * @param patchPath   .xdelta / .vcdiff 패치 파일 절대 경로
     * @param outputPath  출력 ROM 파일 절대 경로
     * @param callback    진행률 콜백 (0f ~ 1f), null 허용
     * @return 0 = 성공, 그 외 = xdelta3 오류 코드
     */
    external fun applyPatch(
        sourcePath: String,
        patchPath: String,
        outputPath: String,
        callback: ProgressCallback?
    ): Int
}
