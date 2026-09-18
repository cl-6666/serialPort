package com.cl.serialportlibrary.stick

import com.cl.serialportlibrary.utils.SerialPortLogUtil
import java.io.IOException
import java.io.InputStream

/**
 * 不处理黏包，直接返回输入流当前可读取的数据。
 */
open class BaseStickPackageHelper @JvmOverloads constructor(
    private val readIntervalMillis: Long = DEFAULT_READ_INTERVAL_MS,
) : AbsStickPackageHelper {

    init {
        require(readIntervalMillis > 0) { "readIntervalMillis must be greater than 0" }
    }

    override fun execute(inputStream: InputStream): ByteArray? {
        return try {
            val available = inputStream.available()
            if (available > 0) {
                val buffer = ByteArray(available)
                val size = inputStream.read(buffer)
                if (size > 0) {
                    if (size == buffer.size) buffer else buffer.copyOf(size)
                } else {
                    null
                }
            } else {
                Thread.sleep(readIntervalMillis)
                null
            }
        } catch (error: IOException) {
            SerialPortLogUtil.e(TAG, "读取原始串口数据失败", error)
            throw error
        }
    }

    private companion object {
        private const val TAG = "BaseStickPackageHelper"
        private const val DEFAULT_READ_INTERVAL_MS = 50L
    }
}
