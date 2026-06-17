/*
 * xdelta3_jni.c
 * JNI bridge: Java/Kotlin -> xdelta3 decode (VCDIFF patch apply)
 *
 * JNI method signature:
 *   com.berry.patchguide.patching.XdeltaNative.applyPatch(
 *       sourceFile: String, patchFile: String, outputFile: String,
 *       progressCallback: XdeltaNative.ProgressCallback
 *   ): Int   (0 = success, non-zero = error code)
 */

#include <jni.h>
#include <android/log.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>

/* xdelta3 single-file build: include the .c directly so we get the
 * full implementation without a separate compilation unit.
 * All compile-time defines (XDELTA3_INTERNAL, XD3_POSIX, etc.) are
 * injected via CMakeLists.txt target_compile_definitions to avoid
 * redefinition warnings. */
#include "xdelta3/xdelta3.c"

#define LOG_TAG "XdeltaNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/* Block size for streaming decode */
#define BLOCK_SIZE (1 << 16)  /* 64 KB */

/* ------------------------------------------------------------------ */
/* Helper: read entire file into a malloc'd buffer                     */
/* ------------------------------------------------------------------ */
static uint8_t* read_file(const char* path, size_t* out_size) {
    FILE* f = fopen(path, "rb");
    if (!f) {
        LOGE("Cannot open file: %s (%s)", path, strerror(errno));
        return NULL;
    }
    fseek(f, 0, SEEK_END);
    long sz = ftell(f);
    fseek(f, 0, SEEK_SET);
    if (sz <= 0) { fclose(f); return NULL; }

    uint8_t* buf = (uint8_t*)malloc((size_t)sz);
    if (!buf) { fclose(f); return NULL; }
    if (fread(buf, 1, (size_t)sz, f) != (size_t)sz) {
        free(buf); fclose(f); return NULL;
    }
    fclose(f);
    *out_size = (size_t)sz;
    return buf;
}

/* ------------------------------------------------------------------ */
/* JNI entry point                                                     */
/* ------------------------------------------------------------------ */
JNIEXPORT jint JNICALL
Java_com_berry_patchguide_patching_XdeltaNative_applyPatch(
        JNIEnv* env,
        jclass  clazz,
        jstring jSourcePath,
        jstring jPatchPath,
        jstring jOutputPath,
        jobject jCallback)
{
    (void)clazz;

    const char* sourcePath = (*env)->GetStringUTFChars(env, jSourcePath, NULL);
    const char* patchPath  = (*env)->GetStringUTFChars(env, jPatchPath,  NULL);
    const char* outputPath = (*env)->GetStringUTFChars(env, jOutputPath, NULL);

    int ret = -1;

    /* Load source (original ROM) into memory */
    size_t src_size = 0;
    uint8_t* src_buf = read_file(sourcePath, &src_size);
    if (!src_buf) {
        LOGE("Failed to read source: %s", sourcePath);
        goto cleanup_strings;
    }

    /* Open patch file */
    FILE* patch_fp = fopen(patchPath, "rb");
    if (!patch_fp) {
        LOGE("Failed to open patch: %s (%s)", patchPath, strerror(errno));
        free(src_buf);
        goto cleanup_strings;
    }

    /* Open output file */
    FILE* out_fp = fopen(outputPath, "wb");
    if (!out_fp) {
        LOGE("Failed to open output: %s (%s)", outputPath, strerror(errno));
        fclose(patch_fp);
        free(src_buf);
        goto cleanup_strings;
    }

    /* Get patch file size for progress reporting */
    fseek(patch_fp, 0, SEEK_END);
    long patch_size = ftell(patch_fp);
    fseek(patch_fp, 0, SEEK_SET);

    /* Set up xdelta3 stream */
    xd3_stream stream;
    xd3_config config;
    xd3_source source;

    memset(&stream, 0, sizeof(stream));
    memset(&config, 0, sizeof(config));
    memset(&source, 0, sizeof(source));

    xd3_init_config(&config, XD3_ADLER32);
    config.winsize = BLOCK_SIZE;

    if (xd3_config_stream(&stream, &config) != 0) {
        LOGE("xd3_config_stream failed");
        fclose(out_fp);
        fclose(patch_fp);
        free(src_buf);
        goto cleanup_strings;
    }

    /* Set source (the original ROM as a single block) */
    source.size      = (xoff_t)src_size;
    source.blksize   = (usize_t)src_size;
    source.onblk     = (usize_t)src_size;
    source.curblk    = src_buf;
    source.curblkno  = 0;

    if (xd3_set_source_and_size(&stream, &source, (xoff_t)src_size) != 0) {
        LOGE("xd3_set_source_and_size failed");
        xd3_close_stream(&stream);
        xd3_free_stream(&stream);
        fclose(out_fp);
        fclose(patch_fp);
        free(src_buf);
        goto cleanup_strings;
    }

    /* JNI callback method lookup */
    jclass cbClass = NULL;
    jmethodID cbMethod = NULL;
    if (jCallback != NULL) {
        cbClass  = (*env)->GetObjectClass(env, jCallback);
        cbMethod = (*env)->GetMethodID(env, cbClass, "onProgress", "(F)V");
    }

    /* Streaming decode loop */
    uint8_t* input_buf = (uint8_t*)malloc(BLOCK_SIZE);
    if (!input_buf) {
        LOGE("malloc failed for input buffer");
        xd3_close_stream(&stream);
        xd3_free_stream(&stream);
        fclose(out_fp);
        fclose(patch_fp);
        free(src_buf);
        goto cleanup_strings;
    }

    long bytes_read_total = 0;
    int done = 0;

    while (!done) {
        size_t nread = fread(input_buf, 1, BLOCK_SIZE, patch_fp);
        bytes_read_total += (long)nread;

        if (nread < BLOCK_SIZE) {
            xd3_set_flags(&stream, XD3_FLUSH | stream.flags);
        }

        xd3_avail_input(&stream, input_buf, (usize_t)nread);

process:
        {
            int r = xd3_decode_input(&stream);
            switch (r) {
                case XD3_INPUT:
                    /* Need more input */
                    if (nread == 0) { done = 1; }
                    break;

                case XD3_OUTPUT:
                    /* Write decoded output */
                    if (fwrite(stream.next_out, 1, stream.avail_out, out_fp)
                            != stream.avail_out) {
                        LOGE("fwrite failed");
                        done = 1;
                        ret = -2;
                    }
                    xd3_consume_output(&stream);
                    goto process;

                case XD3_GETSRCBLK:
                    /* Single-block source: already set, should not happen */
                    LOGE("XD3_GETSRCBLK unexpected");
                    done = 1;
                    ret = -3;
                    break;

                case XD3_GOTHEADER:
                case XD3_WINSTART:
                case XD3_WINFINISH:
                    /* Report progress */
                    if (cbMethod != NULL && patch_size > 0) {
                        float progress = (float)bytes_read_total / (float)patch_size;
                        if (progress > 1.0f) progress = 1.0f;
                        (*env)->CallVoidMethod(env, jCallback, cbMethod, (jfloat)progress);
                    }
                    goto process;

                case 0:
                    /* Success / stream finished */
                    done = 1;
                    ret = 0;
                    break;

                default:
                    LOGE("xd3_decode_input error: %d (%s)", r,
                         stream.msg ? stream.msg : "unknown");
                    done = 1;
                    ret = r;
                    break;
            }
        }
    }

    free(input_buf);
    xd3_close_stream(&stream);
    xd3_free_stream(&stream);
    fclose(out_fp);
    fclose(patch_fp);
    free(src_buf);

    if (ret == 0) {
        LOGI("xdelta3 decode success -> %s", outputPath);
        /* Final progress callback */
        if (cbMethod != NULL) {
            (*env)->CallVoidMethod(env, jCallback, cbMethod, (jfloat)1.0f);
        }
    } else {
        LOGE("xdelta3 decode failed, code=%d", ret);
        /* Remove partial output on failure */
        remove(outputPath);
    }

cleanup_strings:
    (*env)->ReleaseStringUTFChars(env, jSourcePath, sourcePath);
    (*env)->ReleaseStringUTFChars(env, jPatchPath,  patchPath);
    (*env)->ReleaseStringUTFChars(env, jOutputPath, outputPath);

    return (jint)ret;
}
