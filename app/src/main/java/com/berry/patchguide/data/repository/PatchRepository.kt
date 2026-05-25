package com.berry.patchguide.data.repository

import android.util.Log
import com.berry.patchguide.data.local.datastore.SettingsDataStore
import com.berry.patchguide.data.model.PatchItem
import com.berry.patchguide.data.remote.api.BerryPatchApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class PatchRepository @Inject constructor(
    @Named("local") private val localApi: BerryPatchApi,
    @Named("cloud") private val cloudApi: BerryPatchApi,
    private val settingsDataStore: SettingsDataStore,
    private val okHttpClient: OkHttpClient
) {
    private val TAG = "PatchRepository"
    private val REQUEST_TIMEOUT_MS = 8_000L

    /**
     * 설정에 따라 우선 서버 선택 + 실패 시 자동 fallback
     */
    private suspend fun <T> callWithFallback(
        block: suspend (BerryPatchApi) -> T
    ): Result<T> {
        val useCloud = settingsDataStore.useCloudServer.first()
        val primary = if (useCloud) cloudApi else localApi
        val secondary = if (useCloud) localApi else cloudApi
        val primaryName = if (useCloud) "cloud" else "local"
        val secondaryName = if (useCloud) "local" else "cloud"

        // 1) Primary 시도
        val primaryResult = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
            runCatching { block(primary) }
        } ?: runCatching { throw java.util.concurrent.TimeoutException("$primaryName timeout") }

        if (primaryResult.isSuccess) {
            Log.d(TAG, "API success via $primaryName")
            return primaryResult
        }

        Log.w(TAG, "API failed via $primaryName: ${primaryResult.exceptionOrNull()?.message}, trying $secondaryName...")

        // 2) Secondary fallback
        val secondaryResult = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
            runCatching { block(secondary) }
        } ?: runCatching { throw java.util.concurrent.TimeoutException("$secondaryName timeout") }

        return secondaryResult.onFailure {
            Log.e(TAG, "API failed via $secondaryName: ${it.message}")
        }
    }

    suspend fun searchAll(query: String, page: Int = 1, limit: Int = 20): Result<List<PatchItem>> {
        return callWithFallback { api ->
            val response = api.search(query, page, limit)
            if (response.isSuccessful) {
                response.body()?.results ?: emptyList()
            } else {
                throw Exception("검색 실패: ${response.code()}")
            }
        }
    }

    suspend fun getFeatured(): Result<List<PatchItem>> {
        return callWithFallback { api ->
            val response = api.getFeatured()
            if (response.isSuccessful) {
                response.body()?.results ?: emptyList()
            } else {
                throw Exception("추천 패치 불러오기 실패: ${response.code()}")
            }
        }
    }

    /**
     * 패치 파일을 다운로드합니다.
     */
    fun downloadPatch(url: String, dest: File, progress: (Float) -> Unit): File {
        val safeUrl = if (url.startsWith("http://")) url.replace("http://", "https://") else url

        Log.d(TAG, "패치 다운로드 시작: $safeUrl")
        dest.parentFile?.mkdirs()

        val request = Request.Builder().url(safeUrl).build()
        val response = okHttpClient.newCall(request).execute()

        check(response.isSuccessful) { "다운로드 실패: HTTP ${response.code}" }

        val body = checkNotNull(response.body) { "응답 본문이 없습니다" }
        val contentLength = body.contentLength()

        body.byteStream().buffered().use { input ->
            dest.outputStream().buffered().use { output ->
                val buffer = ByteArray(8192)
                var downloaded = 0L
                var lastProgressTime = System.currentTimeMillis()
                var bytesRead = input.read(buffer)
                while (bytesRead != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloaded += bytesRead
                    val now = System.currentTimeMillis()
                    if (now - lastProgressTime >= 100 && contentLength > 0) {
                        progress((downloaded.toFloat() / contentLength).coerceIn(0f, 0.99f))
                        lastProgressTime = now
                    }
                    bytesRead = input.read(buffer)
                }
            }
        }

        progress(1f)

        val sha256 = computeSha256(dest)
        Log.d(TAG, "다운로드 완료: ${dest.name}, SHA-256: $sha256")

        return dest
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
