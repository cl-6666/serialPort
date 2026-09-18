package com.cl.serialportlibrary.listener

import com.cl.serialportlibrary.SerialError

fun interface OnSerialErrorListener {
    fun onError(error: SerialError)
}
