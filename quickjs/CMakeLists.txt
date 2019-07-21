cmake_minimum_required(VERSION 3.4.1)

project(quickjs C)

file(STRINGS "quickjs/VERSION" CONFIG_VERSION)

include_directories(quickjs)

if (CMAKE_CXX_COMPILER_ID MATCHES "Clang")
    set(CONFIG_CC clang)
else()
    set(CONFIG_CC gcc)
endif()

set(COMMON_FLAGS -D_GNU_SOURCE -DCONFIG_VERSION=\"${CONFIG_VERSION}\" -DCONFIG_CC=\"${CONFIG_CC}\" -DCONFIG_PREFIX=\"/usr/local\")

set(BN_FLAGS -DCONFIG_BIGNUM)

set(QJS_LIB_SOURCES
        patched/quickjs.c
        patched/libregexp.c
        quickjs/libunicode.c
        quickjs/cutils.c
        quickjs/quickjs-libc.c
        patch.c
)

set(QJSBN_LIB_SOURCES
        quickjs/libbf.c
        ${QJS_LIB_SOURCES}
)

set(QJS_SOURCES
        quickjs/qjs.c
        repl.c
        ${QJS_LIB_SOURCES}
)

set(QJSBN_SOURCES
        quickjs/qjs.c
        repl.c
        qjscalc.c
        ${QJSBN_LIB_SOURCES}
)

add_executable(patch_test patch_test.c patch.c)
# Camouflage the executable as a shared library to make apk include it
set_target_properties(patch_test PROPERTIES OUTPUT_NAME libpatch_test.so)

add_executable(qjs ${QJS_SOURCES})
target_compile_options(qjs PRIVATE ${COMMON_FLAGS})
# Camouflage the executable as a shared library to make apk include it
set_target_properties(qjs PROPERTIES OUTPUT_NAME libqjs.so)

add_executable(qjsbn ${QJSBN_SOURCES})
target_compile_options(qjsbn PRIVATE ${COMMON_FLAGS} ${BN_FLAGS})
# Camouflage the executable as a shared library to make apk include it
set_target_properties(qjsbn PROPERTIES OUTPUT_NAME libqjsbn.so)
