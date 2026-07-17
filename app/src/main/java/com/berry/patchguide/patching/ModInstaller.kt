package com.berry.patchguide.patching

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

/**
 * ROM 바이너리 패치(IPS/UPS/BPS/xdelta)가 아니라, Polymod 등 통째로 복사해서 쓰는
 * "폴더형 모드" ZIP을 사용자가 지정한 폴더(SAF 트리 URI)에 그대로 설치하는 유틸리티.
 */
object ModInstaller {

    /**
     * ZIP 파일을 destDir에 압축 해제합니다. (ZipApplier.extract 재사용)
     */
    fun extractZip(zipFile: File, destDir: File, progress: (Float) -> Unit): List<File> {
        return ZipApplier.extract(zipFile, destDir, progress)
    }

    /**
     * 압축 해제된 파일들의 총 용량(바이트)을 계산합니다. (설치 진행률 표시용)
     */
    fun totalSize(files: List<File>): Long {
        return files.filter { it.isFile }.sumOf { it.length() }
    }

    /**
     * 압축 해제된 로컬 폴더(sourceRootDir)의 내용을 사용자가 선택한 대상 폴더(destTreeUri)에
     * 그대로 재귀 복사합니다. 폴더 구조를 유지하며, 동일한 이름의 파일/폴더가 있으면 덮어씁니다.
     *
     * @return 대상 폴더에 복사된 최상위 항목 개수
     */
    fun copyToTree(
        context: Context,
        sourceRootDir: File,
        destTreeUri: Uri,
        progress: (Float) -> Unit
    ) {
        val destRoot = DocumentFile.fromTreeUri(context, destTreeUri)
            ?: throw IllegalStateException("설치 위치 폴더를 열 수 없습니다.")
        if (!destRoot.isDirectory) {
            throw IllegalStateException("선택한 위치가 폴더가 아닙니다.")
        }

        val allFiles = sourceRootDir.walkTopDown().filter { it.isFile }.toList()
        val totalBytes = allFiles.sumOf { it.length() }.coerceAtLeast(1L)
        var copiedBytes = 0L

        fun copyRecursive(src: File, destDir: DocumentFile) {
            if (src.isDirectory) {
                val childDir = destDir.findFile(src.name)?.takeIf { it.isDirectory }
                    ?: destDir.createDirectory(src.name)
                    ?: throw IllegalStateException("폴더 생성 실패: ${src.name}")
                src.listFiles()?.forEach { child -> copyRecursive(child, childDir) }
            } else {
                // 기존 동일 이름 파일이 있으면 삭제 후 재생성 (덮어쓰기)
                destDir.findFile(src.name)?.delete()
                val mimeType = mimeTypeFor(src.name)
                val newFile = destDir.createFile(mimeType, src.name)
                    ?: throw IllegalStateException("파일 생성 실패: ${src.name}")
                context.contentResolver.openOutputStream(newFile.uri)?.use { out ->
                    src.inputStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } >= 0) {
                            out.write(buffer, 0, read)
                            copiedBytes += read
                            progress(copiedBytes.toFloat() / totalBytes.toFloat())
                        }
                    }
                } ?: throw IllegalStateException("파일을 쓸 수 없습니다: ${src.name}")
            }
        }

        sourceRootDir.listFiles()?.forEach { child -> copyRecursive(child, destRoot) }
    }

    private fun mimeTypeFor(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "json" -> "application/json"
            "ogg" -> "audio/ogg"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "txt" -> "text/plain"
            "xml" -> "application/xml"
            else -> "application/octet-stream"
        }
    }
}
