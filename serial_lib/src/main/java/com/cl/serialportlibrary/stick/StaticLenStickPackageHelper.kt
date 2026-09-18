package com.cl.serialportlibrary.stick

import com.cl.serialportlibrary.utils.SerialPortLogUtil
import java.io.IOException
import java.io.InputStream

/**
 * 固定长度数据包处理器。
 */
open class StaticLenStickPackageHelper(
    private var stackLen: Int,
) : AbsStickPackageHelper {

    init {
        require(stackLen > 0) { "stackLen must be greater than 0" }
        require(stackLen <= DEFAULT_MAX_PACKET_LENGTH) {
            "stackLen must not exceed $DEFAULT_MAX_PACKET_LENGTH"
        }
    }

    constructor() : this(DEFAULT_PACKET_LENGTH)

    override fun execute(inputStream: InputStream): ByteArray? {
        var count = 0
        var len = -1
        val result = ByteArray(stackLen)
        return try {
            while (count < stackLen && inputStream.read().also { len = it } != -1) {
                result[count] = len.toByte()
                count++
            }
            if (len == -1) null else result
        } catch (error: IOException) {
            SerialPortLogUtil.e(TAG, "读取固定长度数据包失败", error)
            throw error
        }
    }

    private companion object {
        private const val TAG = "StaticLenStickPackageHelper"
        private const val DEFAULT_PACKET_LENGTH = 16
        private const val DEFAULT_MAX_PACKET_LENGTH = 1024 * 1024
    }
}
