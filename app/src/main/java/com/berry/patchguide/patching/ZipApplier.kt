package com.berry.patchguide.patching

import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * ZIP 압축 해제기
 *
 * ZIP 형식 패치는 자동 ROM 적용이 불가능합니다.
 * 압축을 해제하여 사용자가 내용물을 확인하고 직접 적용할 수 있도록 폴더를 보여줍니다.
 */
object ZipApplier {

    const val UNSUPPORTED_MESSAGE =
        "ZIP 형식은 자동 ROM 패치 적용을 지원하지 않습니다.\n" +
                "압축을 해제하여 파일을 확인한 후, 내부의 IPS/UPS/BPS 패치 파일을 선택하여 적용하세요."

    /**
     * ZIP 파일을 destDir에 압축 해제합니다.
     * @return 압축 해제된 파일 목록
     */
    fun extract(zipFile: File, destDir: File, progress: (Float) -> Unit): List<File> {
        destDir.mkdirs()
        val extractedFiles = mutableListOf<File>()

        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(destDir, entry.name)
                // ZIP slip 방지
                val canonicalDest = destDir.canonicalPath
                val canonicalOut = outFile.canonicalPath
                require(canonicalOut.startsWith(canonicalDest + File.separator) ||
                        canonicalOut == canonicalDest) {
                    "ZIP slip 방지: 허용되지 않은 경로 - ${entry.name}"
                }

                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().buffered().use { out ->
                        zis.copyTo(out)
                    }
                    extractedFiles.add(outFile)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        progress(1f)
        return extractedFiles
    }

    /**
     * ZIP 내 ROM 패치 파일(IPS/UPS/BPS) 목록을 반환합니다.
     */
    fun findPatchFiles(extractedFiles: List<File>): List<File> {
        return extractedFiles.filter { file ->
            val name = file.name.lowercase()
            name.endsWith(".ips") || name.endsWith(".ups") || name.endsWith(".bps")
        }
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
