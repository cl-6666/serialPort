package com.cl.serialportlibrary

import com.cl.serialportlibrary.listener.SerialResponseMatcher
import com.cl.serialportlibrary.stick.AbsStickPackageHelper
import com.cl.serialportlibrary.stick.BaseStickPackageHelper

/** 单串口运行配置；Builder 负责在连接创建前完成参数校验。 */
class SerialConfig(builder: Builder) {

    @Volatile
    @set:JvmName("setEnableLoggingProperty")
    var isEnableLogging: Boolean = builder.enableLogging

    @Volatile
    @set:JvmName("setIntervalSleepProperty")
    var intervalSleep: Int = builder.intervalSleep
        set(value) {
            requirePositive("intervalSleep", value)
            field = value
        }

    @Volatile
    private var reconnectEnabled: Boolean = builder.autoReconnect || builder.serialPortReconnection

    @Volatile
    @set:JvmName("setFlagsProperty")
    var flags: Int = builder.flags

    @Volatile
    @set:JvmName("setDatabitsProperty")
    var databits: Int = builder.databits
        set(value) {
            validateSerialParameters(value, stopbits, parity)
            field = value
        }

    @Volatile
    @set:JvmName("setStopbitsProperty")
    var stopbits: Int = builder.stopbits
        set(value) {
            validateSerialParameters(databits, value, parity)
            field = value
        }

    @Volatile
    @set:JvmName("setParityProperty")
    var parity: Int = builder.parity
        set(value) {
            validateSerialParameters(databits, stopbits, value)
            field = value
        }

    @Volatile
    @set:JvmName("setEnableStickyPacketProcessingProperty")
    var isEnableStickyPacketProcessing: Boolean = builder.enableStickyPacketProcessing

    @Volatile
    @set:JvmName("setMaxPacketSizeProperty")
    var maxPacketSize: Int = builder.maxPacketSize
        set(value) {
            requirePositive("maxPacketSize", value)
            field = value
        }

    @Volatile
    @set:JvmName("setPacketTimeoutProperty")
    var packetTimeout: Int = builder.packetTimeout
        set(value) {
            requirePositive("packetTimeout", value)
            field = value
        }

    @Volatile
    private var helperSnapshot: Array<AbsStickPackageHelper> = builder.stickyPacketHelpers.copyOf()

    @set:JvmName("setStickyPacketHelpersProperty")
    var stickyPacketHelpers: Array<AbsStickPackageHelper>
        get() = helperSnapshot.copyOf()
        set(value) {
            validateHelpers(value)
            helperSnapshot = value.copyOf()
        }

    @set:JvmName("setSerialPortReconnectionProperty")
    var isSerialPortReconnection: Boolean
        get() = reconnectEnabled
        set(value) {
            reconnectEnabled = value
        }

    @set:JvmName("setAutoReconnectProperty")
    var isAutoReconnect: Boolean
        get() = reconnectEnabled
        set(value) {
            reconnectEnabled = value
        }

    @Volatile
    @set:JvmName("setReconnectIntervalProperty")
    var reconnectInterval: Int = builder.reconnectInterval
        set(value) {
            requireNonNegative("reconnectInterval", value)
            field = value
        }

    @Volatile
    @set:JvmName("setMaxReconnectAttemptsProperty")
    var maxReconnectAttempts: Int = builder.maxReconnectAttempts
        set(value) {
            requireNonNegative("maxReconnectAttempts", value)
            field = value
        }

    @Volatile
    @set:JvmName("setEnableReliableSendProperty")
    var isEnableReliableSend: Boolean = builder.enableReliableSend

    @Volatile
    @set:JvmName("setResponseTimeoutMillisProperty")
    var responseTimeoutMillis: Int = builder.responseTimeoutMillis
        set(value) {
            requirePositive("responseTimeoutMillis", value)
            field = value
        }

    @Volatile
    @set:JvmName("setMaxSendRetriesProperty")
    var maxSendRetries: Int = builder.maxSendRetries
        set(value) {
            requireNonNegative("maxSendRetries", value)
            field = value
        }

    @Volatile
    @set:JvmName("setSendRetryIntervalMillisProperty")
    var sendRetryIntervalMillis: Int = builder.sendRetryIntervalMillis
        set(value) {
            requireNonNegative("sendRetryIntervalMillis", value)
            field = value
        }

    @Volatile
    @set:JvmName("setResponseMatcherProperty")
    var responseMatcher: SerialResponseMatcher? = builder.responseMatcher

    init {
        validateSerialParameters(databits, stopbits, parity)
        requirePositive("intervalSleep", intervalSleep)
        requirePositive("maxPacketSize", maxPacketSize)
        requirePositive("packetTimeout", packetTimeout)
        requireNonNegative("reconnectInterval", reconnectInterval)
        requireNonNegative("maxReconnectAttempts", maxReconnectAttempts)
        requirePositive("responseTimeoutMillis", responseTimeoutMillis)
        requireNonNegative("maxSendRetries", maxSendRetries)
        requireNonNegative("sendRetryIntervalMillis", sendRetryIntervalMillis)
        validateHelpers(helperSnapshot)
    }

    fun setEnableLogging(value: Boolean) {
        isEnableLogging = value
    }

    fun setIntervalSleep(value: Int) {
        intervalSleep = value
    }

    fun setSerialPortReconnection(value: Boolean) {
        isSerialPortReconnection = value
    }

    fun setFlags(value: Int) {
        flags = value
    }

    fun setDatabits(value: Int) {
        databits = value
    }

    fun setStopbits(value: Int) {
        stopbits = value
    }

    fun setParity(value: Int) {
        parity = value
    }

    fun setEnableStickyPacketProcessing(value: Boolean) {
        isEnableStickyPacketProcessing = value
    }

    fun setMaxPacketSize(value: Int) {
        maxPacketSize = value
    }

    fun setPacketTimeout(value: Int) {
        packetTimeout = value
    }

    fun setStickyPacketHelpers(value: Array<AbsStickPackageHelper>) {
        stickyPacketHelpers = value
    }

    fun setAutoReconnect(value: Boolean) {
        isAutoReconnect = value
    }

    fun setReconnectInterval(value: Int) {
        reconnectInterval = value
    }

    fun setMaxReconnectAttempts(value: Int) {
        maxReconnectAttempts = value
    }

    fun setEnableReliableSend(value: Boolean) {
        isEnableReliableSend = value
    }

    fun setResponseTimeoutMillis(value: Int) {
        responseTimeoutMillis = value
    }

    fun setMaxSendRetries(value: Int) {
        maxSendRetries = value
    }

    fun setSendRetryIntervalMillis(value: Int) {
        sendRetryIntervalMillis = value
    }

    fun setResponseMatcher(value: SerialResponseMatcher?) {
        responseMatcher = value
    }

    internal fun copyForConnection(): SerialConfig {
        return Builder()
            .setEnableLogging(isEnableLogging)
            .setIntervalSleep(intervalSleep)
            .setSerialPortReconnection(isSerialPortReconnection)
            .setFlags(flags)
            .setDatabits(databits)
            .setStopbits(stopbits)
            .setParity(parity)
            .setEnableStickyPacketProcessing(isEnableStickyPacketProcessing)
            .setMaxPacketSize(maxPacketSize)
            .setPacketTimeout(packetTimeout)
            .setStickyPacketHelpers(*stickyPacketHelpers)
            .setAutoReconnect(isAutoReconnect)
            .setReconnectInterval(reconnectInterval)
            .setMaxReconnectAttempts(maxReconnectAttempts)
            .setEnableReliableSend(isEnableReliableSend)
            .setResponseTimeoutMillis(responseTimeoutMillis)
            .setMaxSendRetries(maxSendRetries)
            .setSendRetryIntervalMillis(sendRetryIntervalMillis)
            .setResponseMatcher(responseMatcher)
            .build()
    }

    class Builder {
        internal var enableLogging = true
        internal var intervalSleep = 50
        internal var serialPortReconnection = false
        internal var flags = 0
        internal var databits = 8
        internal var stopbits = 1
        internal var parity = 0
        internal var enableStickyPacketProcessing = true
        internal var maxPacketSize = 1024
        internal var packetTimeout = 1000
        internal var stickyPacketHelpers: Array<AbsStickPackageHelper> = arrayOf(BaseStickPackageHelper())
        internal var autoReconnect = false
        internal var reconnectInterval = 5000
        internal var maxReconnectAttempts = 3
        internal var enableReliableSend = false
        internal var responseTimeoutMillis = 1000
        internal var maxSendRetries = 2
        internal var sendRetryIntervalMillis = 100
        internal var responseMatcher: SerialResponseMatcher? = null

        fun setEnableLogging(value: Boolean) = apply { enableLogging = value }

        fun setIntervalSleep(value: Int) = apply { intervalSleep = value }

        fun setSerialPortReconnection(value: Boolean) = apply { serialPortReconnection = value }

        fun setFlags(value: Int) = apply { flags = value }

        fun setDatabits(value: Int) = apply { databits = value }

        fun setStopbits(value: Int) = apply { stopbits = value }

        fun setParity(value: Int) = apply { parity = value }

        fun setEnableStickyPacketProcessing(value: Boolean) = apply {
            enableStickyPacketProcessing = value
        }

        fun setMaxPacketSize(value: Int) = apply { maxPacketSize = value }

        fun setPacketTimeout(value: Int) = apply { packetTimeout = value }

        fun setStickyPacketHelpers(vararg value: AbsStickPackageHelper) = apply {
            validateHelpers(value)
            stickyPacketHelpers = value.map { helper -> helper }.toTypedArray()
        }

        fun setAutoReconnect(value: Boolean) = apply { autoReconnect = value }

        fun setReconnectInterval(value: Int) = apply { reconnectInterval = value }

        fun setMaxReconnectAttempts(value: Int) = apply { maxReconnectAttempts = value }

        fun setEnableReliableSend(value: Boolean) = apply { enableReliableSend = value }

        fun setResponseTimeoutMillis(value: Int) = apply { responseTimeoutMillis = value }

        fun setMaxSendRetries(value: Int) = apply { maxSendRetries = value }

        fun setSendRetryIntervalMillis(value: Int) = apply { sendRetryIntervalMillis = value }

        fun setResponseMatcher(value: SerialResponseMatcher?) = apply { responseMatcher = value }

        fun build(): SerialConfig = SerialConfig(this)
    }

    private companion object {
        private fun validateSerialParameters(databits: Int, stopbits: Int, parity: Int) {
            require(databits in 5..8) { "databits must be between 5 and 8" }
            require(stopbits == 1 || stopbits == 2) { "stopbits must be 1 or 2" }
            require(parity in 0..4) { "parity must be between 0 and 4" }
        }

        private fun requirePositive(name: String, value: Int) {
            require(value > 0) { "$name must be greater than 0" }
        }

        private fun requireNonNegative(name: String, value: Int) {
            require(value >= 0) { "$name must not be negative" }
        }

        private fun validateHelpers(helpers: Array<out AbsStickPackageHelper>) {
            require(helpers.isNotEmpty()) { "stickyPacketHelpers must not be empty" }
        }
    }
}
