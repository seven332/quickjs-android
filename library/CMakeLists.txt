cmake_minimum_required(VERSION 3.4.1)

project(quickjs-android)

add_subdirectory(../quickjs ${CMAKE_CURRENT_BINARY_DIR}/quickjs)

set(QUICKJS_ANDROID_SOURCES
        src/main/c/quickjs-jni.c
)

add_library(quickjs-android SHARED ${QUICKJS_ANDROID_SOURCES})
target_link_libraries(quickjs-android PRIVATE quickjs)
