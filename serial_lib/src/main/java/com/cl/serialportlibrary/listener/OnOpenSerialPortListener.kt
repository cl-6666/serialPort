package com.cl.serialportlibrary.listener

import com.cl.serialportlibrary.enumerate.SerialPortEnum
import com.cl.serialportlibrary.enumerate.SerialStatus
import java.io.File

fun interface OnOpenSerialPortListener {
    fun openState(serialPortEnum: SerialPortEnum, device: File, status: SerialStatus)
}
