package com.cl.serialportlibrary.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 线程安全、可关闭且可替换输出器的串口日志入口。 */
object SerialPortLogUtil {

    @Volatile
    private var defaultTag = "SerialPort"

    @Volatile
    private var enabled = false

    @Volatile
    private var logger: SerialPortLogger = AndroidLogger

    private val dateFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        }
    }

    @JvmStatic
    fun setDebugEnabled(enabled: Boolean) {
        this.enabled = enabled
        if (enabled) i(defaultTag, "================== 串口日志系统已启用 ==================")
    }

    @JvmStatic
    fun setDefaultTag(tag: String?) {
        tag?.trim()?.takeIf(String::isNotEmpty)?.let { defaultTag = it }
    }

    @JvmStatic
    fun setLogger(logger: SerialPortLogger?) {
        this.logger = logger ?: AndroidLogger
    }

    @JvmStatic
    fun isDebugEnabled(): Boolean = enabled

    @JvmStatic
    fun d(tag: String?, message: String) = emit(SerialLogLevel.DEBUG, tag, message)

    @JvmStatic
    fun d(message: String) = d(defaultTag, message)

    @JvmStatic
    fun i(tag: String?, message: String) = emit(SerialLogLevel.INFO, tag, message)

    @JvmStatic
    fun i(message: String) = i(defaultTag, message)

    @JvmStatic
    fun w(tag: String?, message: String) = emit(SerialLogLevel.WARN, tag, message)

    @JvmStatic
    fun w(message: String) = w(defaultTag, message)

    @JvmStatic
    fun e(tag: String?, message: String) = emit(SerialLogLevel.ERROR, tag, message)

    @JvmStatic
    fun e(message: String) = e(defaultTag, message)

    @JvmStatic
    fun e(tag: String?, message: String, throwable: Throwable?) {
        emit(SerialLogLevel.ERROR, tag, message, throwable)
    }

    @JvmStatic
    fun e(message: String, throwable: Throwable?) = e(defaultTag, message, throwable)

    @JvmStatic
    fun printData(tag: String?, prefix: String, data: ByteArray?) {
        if (!enabled || data == null) return
        val preview = data.take(MAX_DATA_PREVIEW_BYTES)
        val hex = preview.joinToString(" ") { byte -> "%02X".format(Locale.ROOT, byte.toInt() and 0xFF) }
        val ascii = preview.joinToString("") { byte ->
            val value = byte.toInt() and 0xFF
            if (value in 32 until 127) value.toChar().toString() else "."
        }
        val suffix = if (data.size > MAX_DATA_PREVIEW_BYTES) "..." else ""
        d(tag, "$prefix [${data.size} bytes]: HEX[$hex$suffix] ASCII[$ascii$suffix]")
    }

    @JvmStatic
    fun printData(prefix: String, data: ByteArray?) = printData(defaultTag, prefix, data)

    @JvmStatic
    fun printSerialStatus(tag: String?, devicePath: String, baudRate: Int, isOpen: Boolean) {
        i(tag, "串口状态 - 设备: $devicePath, 波特率: $baudRate, 状态: ${if (isOpen) "已打开" else "已关闭"}")
    }

    @JvmStatic
    fun printSerialConfig(tag: String?, databits: Int, parity: Int, stopbits: Int, flags: Int) {
        val parityText = when (parity) {
            0 -> "无校验"
            1 -> "奇校验"
            2 -> "偶校验"
            3 -> "SPACE"
            4 -> "MARK"
            else -> "未知($parity)"
        }
        i(tag, "串口配置 - 数据位: $databits, 校验位: $parityText, 停止位: $stopbits, 标志位: 0x${flags.toString(16)}")
    }

    @JvmStatic
    fun printPerformance(tag: String?, operation: String, startTime: Long) {
        d(tag, "性能统计 - $operation 耗时: ${System.currentTimeMillis() - startTime}ms")
    }

    @JvmStatic
    fun printSeparator(tag: String?, title: String) {
        i(tag, "==================== $title ====================")
    }

    @JvmStatic
    fun printSeparator(title: String) = printSeparator(defaultTag, title)

    private fun emit(
        level: SerialLogLevel,
        tag: String?,
        message: String,
        throwable: Throwable? = null,
    ) {
        if (!enabled) return
        val actualTag = tag?.takeIf(String::isNotBlank) ?: defaultTag
        val formatted = "${dateFormat.get()!!.format(Date())} ${callerInfo()} $message"
        runCatching { logger.log(level, actualTag, formatted, throwable) }
    }

    private fun callerInfo(): String {
        return Thread.currentThread().stackTrace
            .firstOrNull { element ->
                element.className != SerialPortLogUtil::class.java.name &&
                    !element.className.startsWith("java.lang.Thread")
            }
            ?.let { element -> "[${element.className.substringAfterLast('.')}.${element.methodName}:${element.lineNumber}]" }
            ?: "[Unknown]"
    }

    private object AndroidLogger : SerialPortLogger {
        override fun log(
            level: SerialLogLevel,
            tag: String,
            message: String,
            throwable: Throwable?,
        ) {
            when (level) {
                SerialLogLevel.DEBUG -> Log.d(tag, message, throwable)
                SerialLogLevel.INFO -> Log.i(tag, message, throwable)
                SerialLogLevel.WARN -> Log.w(tag, message, throwable)
                SerialLogLevel.ERROR -> Log.e(tag, message, throwable)
            }
        }
    }

    private const val MAX_DATA_PREVIEW_BYTES = 32
}
