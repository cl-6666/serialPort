package com.cl.serialportlibrary.stick

import com.cl.serialportlibrary.utils.SerialPortLogUtil
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.util.ArrayDeque

/**
 * 优先使用主处理器，无法解析时再尝试备用处理器。
 */
open class CompositeStickPackageHelper(
    primaryHelper: AbsStickPackageHelper?,
    fallbackHelper: AbsStickPackageHelper?,
) : AbsStickPackageHelper {

    private val primaryHelper = requireNotNull(primaryHelper) { "primaryHelper must not be null" }
    private val fallbackHelper = requireNotNull(fallbackHelper) { "fallbackHelper must not be null" }
    private val buffer = ArrayDeque<Byte>()

    override fun execute(inputStream: InputStream): ByteArray? {
        return try {
            val available = inputStream.available()
            if (available > 0) {
                val temporaryBuffer = ByteArray(available)
                val readBytes = inputStream.read(temporaryBuffer)
                if (readBytes > 0) {
                    for (index in 0 until readBytes) {
                        buffer.addLast(temporaryBuffer[index])
                    }
                    if (buffer.size > DEFAULT_MAX_PACKET_SIZE) {
                        buffer.clear()
                        throw IllegalStateException("组合拆包缓存超过最大长度 $DEFAULT_MAX_PACKET_SIZE")
                    }
                }
            }

            if (buffer.isEmpty()) {
                return null
            }

            val bufferData = buffer.toByteArray()
            val primaryResult = primaryHelper.execute(ByteArrayInputStream(bufferData))
            if (primaryResult != null && primaryResult.isNotEmpty()) {
                removeProcessedBytes(bufferData.consumedLength(primaryResult))
                return primaryResult
            }

            val fallbackResult = fallbackHelper.execute(ByteArrayInputStream(bufferData))
            if (fallbackResult != null && fallbackResult.isNotEmpty()) {
                removeProcessedBytes(bufferData.consumedLength(fallbackResult))
                return fallbackResult
            }
            null
        } catch (error: IOException) {
            buffer.clear()
            SerialPortLogUtil.e(TAG, "组合拆包读取失败", error)
            throw error
        } catch (error: RuntimeException) {
            buffer.clear()
            throw error
        }
    }

    private fun removeProcessedBytes(count: Int) {
        if (count <= buffer.size) {
            repeat(count) {
                buffer.removeFirst()
            }
        }
    }

    private fun Collection<Byte>.toByteArray(): ByteArray {
        return ByteArray(size).also { result ->
            forEachIndexed { index, value -> result[index] = value }
        }
    }

    private fun ByteArray.consumedLength(result: ByteArray): Int {
        if (result.size > size) return result.size
        for (start in 0..size - result.size) {
            if (result.indices.all { offset -> this[start + offset] == result[offset] }) {
                return start + result.size
            }
        }
        return result.size
    }

    private companion object {
        private const val TAG = "CompositeStickPackageHelper"
        private const val DEFAULT_MAX_PACKET_SIZE = 1024 * 1024
    }
}
