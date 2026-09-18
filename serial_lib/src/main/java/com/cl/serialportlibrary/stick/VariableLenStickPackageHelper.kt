package com.cl.serialportlibrary.stick

import com.cl.serialportlibrary.utils.SerialPortLogUtil
import java.io.IOException
import java.io.InputStream
import java.nio.ByteOrder
import java.util.ArrayList

/**
 * 根据协议中的长度字段读取变长数据包。
 */
open class VariableLenStickPackageHelper(
    private var byteOrder: ByteOrder?,
    private var lenSize: Int,
    private var lenIndex: Int,
    private var offset: Int,
) : AbsStickPackageHelper {

    private val mBytes = ArrayList<Byte>()
    private val lenStartIndex = lenIndex
    private val lenEndIndex = lenIndex + lenSize - 1

    init {
        require(byteOrder != null) { "byteOrder must not be null" }
        require(lenSize in 1..4) { "lenSize must be between 1 and 4" }
        require(lenIndex >= 0) { "lenIndex must not be negative" }
    }

    override fun execute(inputStream: InputStream): ByteArray? {
        mBytes.clear()
        var count = 0
        var messageLength = -1
        val lengthField = ByteArray(lenSize)

        try {
            while (true) {
                val readByte = inputStream.read()
                if (readByte == -1) {
                    return null
                }
                val value = readByte.toByte()
                if (count in lenStartIndex..lenEndIndex) {
                    lengthField[count - lenStartIndex] = value
                    if (count == lenEndIndex) {
                        messageLength = getLength(lengthField, byteOrder)
                        val packetLength = messageLength.toLong() + offset.toLong()
                        require(packetLength in 1..DEFAULT_MAX_PACKET_SIZE.toLong()) {
                            "解析出的数据包长度非法: $packetLength"
                        }
                    }
                }
                count++
                mBytes.add(value)
                if (messageLength != -1) {
                    if (count == messageLength + offset) {
                        return mBytes.toByteArray()
                    } else if (count > messageLength + offset) {
                        return null
                    }
                }
            }
        } catch (error: IOException) {
            SerialPortLogUtil.e(TAG, "读取变长数据包失败", error)
            throw error
        }
    }

    private fun getLength(source: ByteArray, order: ByteOrder?): Int {
        var result = 0
        if (order == ByteOrder.BIG_ENDIAN) {
            for (value in source) {
                result = result shl 8 or (value.toInt() and 0xff)
            }
        } else {
            for (index in source.indices.reversed()) {
                result = result shl 8 or (source[index].toInt() and 0xff)
            }
        }
        return result
    }

    private fun List<Byte>.toByteArray(): ByteArray {
        return ByteArray(size) { index -> this[index] }
    }

    private companion object {
        private const val TAG = "VariableLenStickPackageHelper"
        private const val DEFAULT_MAX_PACKET_SIZE = 1024 * 1024
    }
}
