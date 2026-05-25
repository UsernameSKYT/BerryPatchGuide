package com.berry.patchguide.data.remote.api

import com.berry.patchguide.data.model.PatchItem
import com.berry.patchguide.data.model.SearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface BerryPatchApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<SearchResponse>

    @GET("featured")
    suspend fun getFeatured(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Response<SearchResponse>
}
