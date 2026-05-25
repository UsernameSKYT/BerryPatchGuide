package com.berry.patchguide.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PatchItem(
    val id: String,
    val title: String,
    val description: String? = null,
    val author: String? = null,
    @Json(name = "thumbnail_url")
    val thumbnailUrl: String? = null,
    @Json(name = "download_url")
    val downloadUrl: String? = null,
    val source: String,  // 서버에서 문자열로 받음
    @Json(name = "created_at")
    val createdAt: String? = null,
    @Json(name = "updated_at")
    val updatedAt: String? = null,
    val tags: List<String> = emptyList()
) {
    val sourceEnum: PatchSource
        get() = when (source) {
            "ModDB" -> PatchSource.MODDB
            "GameBanana" -> PatchSource.GAMEBANANA
            "RHDN" -> PatchSource.RHDN
            else -> PatchSource.UNKNOWN
        }
}

enum class PatchSource(val displayName: String) {
    MODDB("ModDB"),
    GAMEBANANA("GameBanana"),
    RHDN("ROMhacking.net"),
    UNKNOWN("Unknown")
}

@JsonClass(generateAdapter = true)
data class SearchResponse(
    val results: List<PatchItem>,
    val total: Int,
    val page: Int,
    @Json(name = "per_page")
    val perPage: Int
)
