package com.cl.serialportlibrary.listener

import com.cl.serialportlibrary.enumerate.SerialPortEnum

interface OnSerialPortDataListener {
    fun onDataReceived(bytes: ByteArray, serialPortEnum: SerialPortEnum)

    fun onDataSent(bytes: ByteArray, serialPortEnum: SerialPortEnum)
}
