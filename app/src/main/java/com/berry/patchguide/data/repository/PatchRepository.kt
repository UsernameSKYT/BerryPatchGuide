package com.berry.patchguide.data.repository

import android.util.Log
import com.berry.patchguide.data.model.PatchItem
import com.berry.patchguide.data.remote.api.BerryPatchApi
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatchRepository @Inject constructor(
    private val api: BerryPatchApi,
    private val okHttpClient: OkHttpClient
) {
    private val TAG = "PatchRepository"

    suspend fun searchAll(query: String, page: Int = 1, limit: Int = 20): Result<List<PatchItem>> {
        return try {
            val response = api.search(query, page, limit)
            if (response.isSuccessful) {
                val body = response.body()
                Result.success(body?.results ?: emptyList())
            } else {
                Result.failure(Exception("검색 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFeatured(): Result<List<PatchItem>> {
        return try {
            val response = api.getFeatured()
            if (response.isSuccessful) {
                val body = response.body()
                Result.success(body?.results ?: emptyList())
            } else {
                Result.failure(Exception("추천 패치 불러오기 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 패치 파일을 다운로드합니다.
     *
     * @param url 다운로드 URL (HTTPS 강제)
     * @param dest 저장할 로컬 파일
     * @param progress 진행률 콜백 (0f ~ 1f)
     * @return 다운로드된 파일 (SHA-256 계산 후 반환)
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
