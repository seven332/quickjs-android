cmake_minimum_required(VERSION 3.4.1)

project(quickjs-android)

if (CMAKE_BUILD_TYPE STREQUAL "Debug")
    option(LEAK_TRIGGER "Add a leak trigger" ON)
else ()
    option(LEAK_TRIGGER "Add a leak trigger" OFF)
endif (CMAKE_BUILD_TYPE STREQUAL "Debug")

add_subdirectory(../quickjs ${CMAKE_CURRENT_BINARY_DIR}/quickjs)

set(QUICKJS_ANDROID_SOURCES
        src/main/c/quickjs-jni.c
        src/main/c/bit-source.c
        src/main/c/bit-sink.c
        src/main/c/pickle.c
        src/main/c/unpickle.c
        src/main/c/java-exception.c
        src/main/c/java-method.c
        src/main/c/java-object.c
)

if (LEAK_TRIGGER)
    set(COMMON_FLAGS -DLEAK_TRIGGER)
else ()
    set(COMMON_FLAGS)
endif (LEAK_TRIGGER)

add_library(quickjs-android SHARED ${QUICKJS_ANDROID_SOURCES})
target_compile_options(quickjs-android PRIVATE ${COMMON_FLAGS})
target_link_libraries(quickjs-android PRIVATE quickjs log)
