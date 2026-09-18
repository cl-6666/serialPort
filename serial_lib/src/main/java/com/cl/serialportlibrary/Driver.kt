package com.cl.serialportlibrary

import java.io.File
import java.util.ArrayList

/**
 * 串口驱动信息，仅供 [SerialPortFinder] 使用。
 */
open class Driver(
    private val mDriverName: String,
    private val mDeviceRoot: String,
) {

    open fun getName(): String = mDriverName

    open fun getRoot(): String = mDeviceRoot

    open fun getDevices(): ArrayList<File> {
        val devices = ArrayList<File>()
        File("/dev").listFiles()?.sortedBy { it.absolutePath }?.forEach { file ->
            if (file.absolutePath.startsWith(mDeviceRoot)) {
                devices.add(file)
            }
        }
        return devices
    }

    override fun toString(): String {
        return "Driver{name='$mDriverName', root='$mDeviceRoot'}"
    }
}
