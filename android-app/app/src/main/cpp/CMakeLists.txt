cmake_minimum_required(VERSION 3.18)
project(honeynut_mp3 C)

# LAME 3.100 (LGPL, see lame/COPYING): the MP3 encoder.
file(GLOB LAME_SOURCES ${CMAKE_CURRENT_SOURCE_DIR}/lame/libmp3lame/*.c)

add_library(mp3 SHARED mp3_jni.c ${LAME_SOURCES})
target_include_directories(mp3 PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/lame/include
    ${CMAKE_CURRENT_SOURCE_DIR}/lame/libmp3lame)
target_compile_definitions(mp3 PRIVATE STDC_HEADERS HAVE_STDINT_H ieee754_float32_t=float)
target_link_libraries(mp3 m)
