package com.cl.serialportlibrary

import com.cl.serialportlibrary.utils.SerialPortLogUtil
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.LineNumberReader

/** 读取 Linux tty 驱动信息并枚举串口设备。 */
class SerialPortFinder {

    init {
        SerialPortLogUtil.d(TAG, "$DRIVERS_PATH canRead=${File(DRIVERS_PATH).canRead()}")
    }

    private fun getDrivers(): List<Driver> {
        return LineNumberReader(FileReader(DRIVERS_PATH)).useLines { lines ->
            lines.mapNotNull { line ->
                val fields = line.trim().split(WHITESPACE_REGEX)
                if (fields.size < MIN_DRIVER_FIELDS || fields.last() != SERIAL_FIELD) {
                    return@mapNotNull null
                }
                val driverName = line.substring(0, minOf(line.length, DRIVER_NAME_END_INDEX)).trim()
                val deviceRoot = fields[fields.lastIndex - DEVICE_ROOT_OFFSET]
                SerialPortLogUtil.d(TAG, "发现串口驱动 $driverName，设备前缀 $deviceRoot")
                Driver(driverName, deviceRoot)
            }.toList()
        }
    }

    val devices: ArrayList<Device>
        get() {
            val result = ArrayList<Device>()
            runCatching { getDrivers() }
                .onSuccess { drivers ->
                    drivers.forEach { driver ->
                        driver.getDevices().forEach { file ->
                            result += Device(file.name, driver.getName(), file)
                        }
                    }
                }
                .onFailure { error ->
                    SerialPortLogUtil.e(TAG, "读取串口设备列表失败", error)
                }
            return result
        }

    val allDevicesPath: Array<String>
        get() {
            return try {
                getDrivers()
                    .flatMap { driver -> driver.getDevices() }
                    .map { file -> file.absolutePath }
                    .sorted()
                    .toTypedArray()
            } catch (error: IOException) {
                SerialPortLogUtil.e(TAG, "读取串口路径列表失败", error)
                emptyArray()
            }
        }

    private companion object {
        private const val TAG = "SerialPortFinder"
        private const val DRIVERS_PATH = "/proc/tty/drivers"
        private const val SERIAL_FIELD = "serial"
        private const val MIN_DRIVER_FIELDS = 5
        private const val DEVICE_ROOT_OFFSET = 3
        private const val DRIVER_NAME_END_INDEX = 0x15
        private val WHITESPACE_REGEX = Regex("\\s+")
    }
}
