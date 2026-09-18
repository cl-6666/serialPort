cmake_minimum_required(VERSION 3.18.1)

project(SerialPort C)

add_library(SerialPort SHARED
            SerialPort.c)

# 为所有 64 位 ABI 添加 16KB 页面对齐支持。
if(ANDROID_ABI STREQUAL "arm64-v8a" OR ANDROID_ABI STREQUAL "x86_64")
    target_compile_options(SerialPort PRIVATE -fno-emulated-tls)
    target_link_options(SerialPort PRIVATE 
        "LINKER:-z,max-page-size=16384"
        "LINKER:-z,common-page-size=16384")
endif()

# Include libraries needed for libserial_port lib
target_link_libraries(SerialPort
                      android
                      log)
