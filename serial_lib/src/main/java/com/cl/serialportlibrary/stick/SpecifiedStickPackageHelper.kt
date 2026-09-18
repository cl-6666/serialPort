package com.cl.serialportlibrary.stick

import com.cl.serialportlibrary.utils.SerialPortLogUtil
import java.io.IOException
import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
 * 根据开始、结束标识解析完整数据包。
 */
open class SpecifiedStickPackageHelper(
    head: ByteArray?,
    tail: ByteArray?,
) : AbsStickPackageHelper {

    private val head: ByteArray = head ?: throw IllegalStateException(" head or tail ==null")
    private val tail: ByteArray = tail ?: throw IllegalStateException(" head or tail ==null")
    private val headLen = this.head.size
    private val tailLen = this.tail.size

    init {
        if (headLen == 0 && tailLen == 0) {
            throw IllegalStateException(" head and tail length==0")
        }
    }

    constructor(tail: ByteArray?) : this(ByteArray(0), tail)

    constructor(head: String?, tail: String?) : this(
        head?.toByteArray(StandardCharsets.UTF_8) ?: ByteArray(0),
        tail?.toByteArray(StandardCharsets.UTF_8) ?: ByteArray(0),
    )

    constructor(tail: String?) : this(
        ByteArray(0),
        tail?.toByteArray(StandardCharsets.UTF_8) ?: ByteArray(0),
    )

    override fun execute(inputStream: InputStream): ByteArray? {
        try {
            if (headLen == 0 || tailLen == 0) {
                return readUntilDelimiter(inputStream, if (tailLen > 0) tail else head)
            }

            val searchWindow = ArrayDeque<Byte>(headLen)
            val tailWindow = ArrayDeque<Byte>(tailLen)
            val packet = ByteArrayOutputStream()
            var started = false
            while (true) {
                val value = inputStream.read()
                if (value == -1) return null

                if (!started) {
                    searchWindow.addLast(value.toByte())
                    if (searchWindow.size > headLen) searchWindow.removeFirst()
                    if (searchWindow.size == headLen && searchWindow.matches(head)) {
                        started = true
                        packet.write(head)
                    }
                    continue
                }

                packet.write(value)
                ensurePacketSize(packet.size())
                tailWindow.addLast(value.toByte())
                if (tailWindow.size > tailLen) tailWindow.removeFirst()
                if (tailWindow.size == tailLen && tailWindow.matches(tail)) {
                    return packet.toByteArray()
                }
            }
        } catch (error: IOException) {
            SerialPortLogUtil.e(TAG, "按标识拆包读取失败", error)
            throw error
        }
    }

    private fun readUntilDelimiter(inputStream: InputStream, delimiter: ByteArray): ByteArray? {
        val packet = ByteArrayOutputStream()
        val delimiterWindow = ArrayDeque<Byte>(delimiter.size)
        while (true) {
            val value = inputStream.read()
            if (value == -1) return null
            packet.write(value)
            ensurePacketSize(packet.size())
            delimiterWindow.addLast(value.toByte())
            if (delimiterWindow.size > delimiter.size) delimiterWindow.removeFirst()
            if (delimiterWindow.size == delimiter.size && delimiterWindow.matches(delimiter)) {
                return packet.toByteArray()
            }
        }
    }

    private fun ensurePacketSize(size: Int) {
        if (size > DEFAULT_MAX_PACKET_SIZE) {
            throw IllegalStateException("数据包超过最大长度 $DEFAULT_MAX_PACKET_SIZE")
        }
    }

    private fun ArrayDeque<Byte>.matches(target: ByteArray): Boolean {
        if (size != target.size) return false
        return withIndex().all { (index, value) -> value == target[index] }
    }

    private companion object {
        private const val TAG = "SpecifiedStickPackageHelper"
        private const val DEFAULT_MAX_PACKET_SIZE = 1024 * 1024
    }
}
