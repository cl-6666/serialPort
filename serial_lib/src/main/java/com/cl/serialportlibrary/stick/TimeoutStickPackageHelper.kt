package com.cl.serialportlibrary.stick

import com.cl.serialportlibrary.utils.SerialPortLogUtil
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList

/**
 * 在指定时间没有新数据时，将缓存内容作为一个完整数据包返回。
 */
open class TimeoutStickPackageHelper(
    private val timeout: Int,
) : AbsStickPackageHelper {

    init {
        require(timeout > 0) { "timeout must be greater than 0" }
    }

    private val buffer = ArrayList<Byte>()

    override fun execute(inputStream: InputStream): ByteArray? {
        buffer.clear()
        var lastDataTime = System.currentTimeMillis()
        try {
            while (true) {
                if (Thread.currentThread().isInterrupted) {
                    return null
                }
                val available = inputStream.available()
                if (available > 0) {
                    val temporaryBuffer = ByteArray(available)
                    val readBytes = inputStream.read(temporaryBuffer)
                    if (readBytes > 0) {
                        for (index in 0 until readBytes) {
                            buffer.add(temporaryBuffer[index])
                        }
                        if (buffer.size > DEFAULT_MAX_PACKET_SIZE) {
                            buffer.clear()
                            throw IllegalStateException("数据包超过最大长度 $DEFAULT_MAX_PACKET_SIZE")
                        }
                        lastDataTime = System.currentTimeMillis()
                    }
                } else {
                    if (buffer.isNotEmpty() && System.currentTimeMillis() - lastDataTime >= timeout) {
                        return buffer.toByteArray()
                    }
                    Thread.sleep(CHECK_INTERVAL_MS)
                }
            }
        } catch (error: IOException) {
            SerialPortLogUtil.e(TAG, "按超时拆包读取失败", error)
            throw error
        }
    }

    private fun List<Byte>.toByteArray(): ByteArray {
        return ByteArray(size) { index -> this[index] }
    }

    private companion object {
        private const val TAG = "TimeoutStickPackageHelper"
        private const val CHECK_INTERVAL_MS = 10L
        private const val DEFAULT_MAX_PACKET_SIZE = 1024 * 1024
    }
}
