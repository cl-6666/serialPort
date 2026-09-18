package com.cl.serialportlibrary.utils

enum class SerialLogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

/** 可替换的 SDK 日志输出器。 */
fun interface SerialPortLogger {
    fun log(
        level: SerialLogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
    )
}
