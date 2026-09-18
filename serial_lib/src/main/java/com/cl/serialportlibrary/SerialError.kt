package com.cl.serialportlibrary

enum class SerialErrorCode {
    DEVICE_NOT_FOUND,
    PERMISSION_DENIED,
    OPEN_FAILED,
    READ_FAILED,
    WRITE_FAILED,
    PACKET_PARSE_FAILED,
    CONNECTION_LOST,
    RECONNECT_EXHAUSTED,
    RESPONSE_TIMEOUT,
    CALLBACK_FAILED,
    PACKET_TIMEOUT,
    PACKET_TOO_LARGE,
}

/** SDK 内部错误的统一、可定位描述。 */
data class SerialError @JvmOverloads constructor(
    val code: SerialErrorCode,
    val message: String,
    val cause: Throwable? = null,
    val devicePath: String? = null,
    val serialId: String? = null,
)
