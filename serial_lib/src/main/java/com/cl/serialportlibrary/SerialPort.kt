package com.cl.serialportlibrary

import com.cl.serialportlibrary.utils.SerialPortLogUtil
import java.io.File
import java.io.FileDescriptor
import java.io.IOException
import java.nio.charset.StandardCharsets

/** JNI 串口访问层。 */
open class SerialPort {

    internal fun chmod777(file: File?): Boolean {
        if (file == null || !file.exists()) return false

        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec("/system/bin/su")
            val escapedPath = "'${file.absolutePath.replace("'", "'\\''")}'"
            process.outputStream.use { output ->
                output.write("chmod 666 $escapedPath\nexit\n".toByteArray(StandardCharsets.UTF_8))
                output.flush()
            }

            val deadline = System.currentTimeMillis() + PERMISSION_TIMEOUT_MS
            while (System.currentTimeMillis() < deadline) {
                try {
                    return process.exitValue() == 0 && file.canRead() && file.canWrite()
                } catch (_: IllegalThreadStateException) {
                    Thread.sleep(PERMISSION_POLL_INTERVAL_MS)
                }
            }
            SerialPortLogUtil.e(TAG, "修改串口权限超时: ${file.absolutePath}")
            false
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            SerialPortLogUtil.e(TAG, "修改串口权限被中断", error)
            false
        } catch (error: IOException) {
            SerialPortLogUtil.e(TAG, "无法通过 su 修改串口权限", error)
            false
        } finally {
            process?.destroy()
        }
    }

    protected external fun open(
        path: String,
        baudrate: Int,
        flags: Int,
        databits: Int,
        stopbits: Int,
        parity: Int,
    ): FileDescriptor?

    protected external fun close()

    private companion object {
        private const val TAG = "SerialPort"
        private const val PERMISSION_TIMEOUT_MS = 2_000L
        private const val PERMISSION_POLL_INTERVAL_MS = 20L

        init {
            System.loadLibrary("SerialPort")
        }
    }
}
