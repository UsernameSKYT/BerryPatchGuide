package com.berry.patchguide.patching

data class PatchReport(
    val outputPath: String,
    val sha256: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val appliedFormat: PatchFormat
)
