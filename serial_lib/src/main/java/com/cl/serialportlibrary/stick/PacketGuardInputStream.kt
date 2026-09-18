package com.cl.serialportlibrary.stick

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import kotlin.math.min

/**
 * 为同步拆包器提供统一的半包超时和单包长度保护。
 *
 * 首字节到达前不会计时；收到首字节后，超过 [packetTimeoutMillis] 没有新数据即放弃当前半包。
 */
internal class PacketGuardInputStream(
    inputStream: InputStream,
    private val packetTimeoutMillis: Long,
    private val maxPacketSize: Int,
    private val pollIntervalMillis: Long = DEFAULT_POLL_INTERVAL_MS,
) : FilterInputStream(inputStream) {

    private var bytesRead = 0
    private var lastDataNanos = 0L

    internal val hasReadData: Boolean
        get() = bytesRead > 0

    init {
        require(packetTimeoutMillis > 0) { "packetTimeoutMillis must be greater than 0" }
        require(maxPacketSize > 0) { "maxPacketSize must be greater than 0" }
        require(pollIntervalMillis > 0) { "pollIntervalMillis must be greater than 0" }
    }

    override fun read(): Int {
        waitUntilReadable()
        val value = super.read()
        if (value >= 0) {
            recordRead(1)
        }
        return value
    }

    override fun read(buffer: ByteArray): Int = read(buffer, 0, buffer.size)

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        waitUntilReadable()

        // 最多多读取一个字节，用于及时发现超长数据包，同时避免按异常 available 值分配大数组。
        val remainingWithOverflowByte = maxPacketSize - bytesRead + 1
        if (remainingWithOverflowByte <= 0) {
            throw PacketTooLargeException(maxPacketSize)
        }
        val readLength = min(length, remainingWithOverflowByte)
        val count = super.read(buffer, offset, readLength)
        if (count > 0) {
            recordRead(count)
        }
        return count
    }

    override fun available(): Int {
        val available = super.available()
        if (available == 0) {
            throwIfPacketTimedOut()
            return 0
        }
        val remainingWithOverflowByte = maxPacketSize - bytesRead + 1
        return min(available, remainingWithOverflowByte.coerceAtLeast(1))
    }

    private fun waitUntilReadable() {
        while (super.available() == 0) {
            throwIfPacketTimedOut()
            Thread.sleep(pollIntervalMillis)
        }
    }

    private fun recordRead(count: Int) {
        bytesRead += count
        if (bytesRead > maxPacketSize) {
            throw PacketTooLargeException(maxPacketSize)
        }
        lastDataNanos = System.nanoTime()
    }

    private fun throwIfPacketTimedOut() {
        if (bytesRead == 0) return
        val idleMillis = (System.nanoTime() - lastDataNanos) / NANOS_PER_MILLISECOND
        if (idleMillis >= packetTimeoutMillis) {
            throw PacketReadTimeoutException(packetTimeoutMillis)
        }
    }

    private companion object {
        const val DEFAULT_POLL_INTERVAL_MS = 10L
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}

internal class PacketReadTimeoutException(
    timeoutMillis: Long,
) : IOException("串口数据包在 ${timeoutMillis}ms 内未接收完整")

internal class PacketTooLargeException(
    maxPacketSize: Int,
) : IOException("串口数据包超过最大长度 $maxPacketSize")
