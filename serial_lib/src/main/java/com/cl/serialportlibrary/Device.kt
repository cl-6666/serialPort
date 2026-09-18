package com.cl.serialportlibrary

import java.io.File
import java.io.Serializable

open class Device(
    open var name: String,
    open var root: String,
    open var file: File?,
) : Serializable {
    private companion object {
        private const val serialVersionUID = 1967121468038781131L
        private const val TAG = "Device"
    }
}
