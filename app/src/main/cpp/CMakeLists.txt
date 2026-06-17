cmake_minimum_required(VERSION 3.22.1)
project(xdelta3_jni C)

# ---------------------------------------------------------------
# xdelta3 JNI shared library
# xdelta3.c is included directly from xdelta3_jni.c via #include,
# so we only compile the bridge file.
# ---------------------------------------------------------------
add_library(
    xdelta3_jni
    SHARED
    xdelta3_jni.c
)

# Include path so xdelta3_jni.c can find xdelta3/xdelta3.c and headers
target_include_directories(xdelta3_jni PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

# Compiler flags
target_compile_options(xdelta3_jni PRIVATE
    -Wall
    -Wno-unused-function
    -Wno-unused-variable
    -O2
    # xdelta3 uses POSIX types; suppress pedantic warnings on Android
    -Wno-implicit-function-declaration
)

# Definitions required by xdelta3 for Android (no POSIX file I/O layer)
target_compile_definitions(xdelta3_jni PRIVATE
    XD3_POSIX=0
    XD3_STDIO=1
    XD3_WIN32=0
    SECONDARY_DJW=0
    SECONDARY_FGK=0
    SECONDARY_LZMA=0
    XDELTA3_INTERNAL=1
)

# Link against Android log library
find_library(log-lib log)
target_link_libraries(xdelta3_jni ${log-lib})
