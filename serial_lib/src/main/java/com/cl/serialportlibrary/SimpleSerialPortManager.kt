package com.cl.serialportlibrary

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.cl.serialportlibrary.enumerate.ReliableSendFailure
import com.cl.serialportlibrary.enumerate.SerialPortEnum
import com.cl.serialportlibrary.enumerate.SerialStatus
import com.cl.serialportlibrary.listener.OnReliableSendListener
import com.cl.serialportlibrary.listener.OnSerialErrorListener
import com.cl.serialportlibrary.listener.OnSerialPortDataListener
import com.cl.serialportlibrary.listener.SerialResponseMatcher
import com.cl.serialportlibrary.stick.AbsStickPackageHelper
import com.cl.serialportlibrary.stick.BaseStickPackageHelper
import com.cl.serialportlibrary.stick.SpecifiedStickPackageHelper
import com.cl.serialportlibrary.stick.StaticLenStickPackageHelper
import com.cl.serialportlibrary.stick.VariableLenStickPackageHelper
import com.cl.serialportlibrary.utils.SerialPortLogUtil
import java.io.Closeable
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

/** 简洁的单串口门面；支持单例，也支持按生命周期独立创建。 */
class SimpleSerialPortManager private constructor() : Closeable {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val stickyPacketHelpers = mutableListOf<AbsStickPackageHelper>(BaseStickPackageHelper())

    @Volatile
    private var manager: SerialPortManager? = null

    @Volatile
    private var config: SerialConfig? = null

    @Volatile
    private var reliableSendListener: OnReliableSendListener? = null

    @Volatile
    private var errorListener: OnSerialErrorListener? = null

    private var openCallback: OnOpenSerialPortCallback? = null
    private var dataCallback: OnDataReceivedCallback? = null
    private var initialized = false
    private var connectionGeneration = 0L
    private var databits = 8
    private var parity = 0
    private var stopbits = 1
    private var flags = 0

    fun init(): SimpleSerialPortManager = init(true, DEFAULT_LOG_TAG, DEFAULT_READ_INTERVAL_MS)

    @Deprecated("Application 参数未被使用，请改用 init()")
    fun init(@Suppress("UNUSED_PARAMETER") application: Application): SimpleSerialPortManager = init()

    fun init(
        enableLog: Boolean,
        logTag: String,
        intervalSleep: Int,
    ): SimpleSerialPortManager {
        return init(
            SerialConfig.Builder()
                .setEnableLogging(enableLog)
                .setIntervalSleep(intervalSleep)
                .setDatabits(databits)
                .setParity(parity)
                .setStopbits(stopbits)
                .setFlags(flags)
                .setStickyPacketHelpers(*stickyPacketHelpers.toTypedArray())
                .build(),
        ).also {
            SerialPortLogUtil.setDefaultTag(logTag)
        }
    }

    @Deprecated("Application 参数未被使用，请改用 init(enableLog, logTag, intervalSleep)")
    fun init(
        @Suppress("UNUSED_PARAMETER") application: Application,
        enableLog: Boolean,
        logTag: String,
        intervalSleep: Int,
    ): SimpleSerialPortManager = init(enableLog, logTag, intervalSleep)

    @Synchronized
    fun init(config: SerialConfig): SimpleSerialPortManager {
        closeSerialPort()
        this.config = config
        databits = config.databits
        parity = config.parity
        stopbits = config.stopbits
        flags = config.flags
        stickyPacketHelpers.clear()
        stickyPacketHelpers += config.stickyPacketHelpers
        initialized = true
        SerialPortLogUtil.setDebugEnabled(config.isEnableLogging)
        SerialPortLogUtil.i(TAG, "SimpleSerialPortManager 初始化完成")
        return this
    }

    @Deprecated("Application 参数未被使用，请改用 init(config)")
    fun init(
        @Suppress("UNUSED_PARAMETER") application: Application,
        config: SerialConfig,
    ): SimpleSerialPortManager = init(config)

    @Synchronized
    fun configureStickyPacket(strategy: StickyPacketStrategy): SimpleSerialPortManager {
        val helper = when (strategy) {
            StickyPacketStrategy.DELIMITER_BASED -> SpecifiedStickPackageHelper("\n")
            StickyPacketStrategy.FIXED_LENGTH -> StaticLenStickPackageHelper()
            StickyPacketStrategy.VARIABLE_LENGTH -> {
                VariableLenStickPackageHelper(ByteOrder.BIG_ENDIAN, 2, 2, 12)
            }
            StickyPacketStrategy.NO_PROCESSING -> {
                BaseStickPackageHelper((config?.intervalSleep ?: DEFAULT_READ_INTERVAL_MS).toLong())
            }
        }
        return setStickyPacketHelpers(helper)
    }

    @Synchronized
    fun setStickyPacketHelpers(vararg helpers: AbsStickPackageHelper): SimpleSerialPortManager {
        require(helpers.isNotEmpty()) { "helpers must not be empty" }
        stickyPacketHelpers.clear()
        stickyPacketHelpers += helpers
        config?.stickyPacketHelpers = helpers.map { helper -> helper }.toTypedArray()
        manager?.setStickPackageHelpers(stickyPacketHelpers.toList())
        return this
    }

    fun openSerialPort(
        devicePath: String,
        baudRate: Int,
        callback: OnDataReceivedCallback?,
    ): Boolean = openSerialPort(devicePath, baudRate, null, callback)

    @Synchronized
    fun openSerialPort(
        devicePath: String,
        baudRate: Int,
        openCallback: OnOpenSerialPortCallback?,
        dataCallback: OnDataReceivedCallback?,
    ): Boolean {
        if (!initialized) {
            SerialPortLogUtil.e(TAG, "SimpleSerialPortManager 未初始化，请先调用 init()")
            return false
        }
        require(devicePath.isNotBlank()) { "devicePath must not be blank" }

        closeSerialPort()
        val generation = ++connectionGeneration
        this.openCallback = openCallback
        this.dataCallback = dataCallback
        updateSerialConfig()

        val activeConfig = requireNotNull(config)
        val newManager = SerialPortManager()
            .setSerialConfigAndReturn(activeConfig)
            .setStickPackageHelpersAndReturn(stickyPacketHelpers)
        manager = newManager

        newManager.setOnOpenSerialPortListener { _, device, status ->
            val success = status == SerialStatus.SUCCESS_OPENED
            mainHandler.post {
                if (isCurrentGeneration(generation)) {
                    this.openCallback?.let { callback ->
                        safely("串口状态业务回调异常") {
                            callback.onStatusChanged(success, status)
                        }
                    }
                }
            }
            SerialPortLogUtil.i(TAG, "串口状态: ${device.path} - $status")
        }
        newManager.setOnSerialPortDataListener(object : OnSerialPortDataListener {
            override fun onDataReceived(bytes: ByteArray, serialPortEnum: SerialPortEnum) {
                val snapshot = bytes.copyOf()
                mainHandler.post {
                    if (isCurrentGeneration(generation)) {
                        this@SimpleSerialPortManager.dataCallback?.let { callback ->
                            safely("串口接收业务回调异常") { callback.onDataReceived(snapshot) }
                        }
                    }
                }
            }

            override fun onDataSent(bytes: ByteArray, serialPortEnum: SerialPortEnum) {
                val snapshot = bytes.copyOf()
                mainHandler.post {
                    if (isCurrentGeneration(generation)) {
                        this@SimpleSerialPortManager.dataCallback?.let { callback ->
                            safely("串口发送业务回调异常") { callback.onDataSent(snapshot) }
                        }
                    }
                }
            }
        })
        newManager.setOnReliableSendListener(object : OnReliableSendListener {
            override fun onRetry(data: ByteArray, retryCount: Int, maxRetries: Int) {
                mainHandler.post {
                    if (isCurrentGeneration(generation)) {
                        reliableSendListener?.let { listener ->
                            safely("可靠发送重试业务回调异常") {
                                listener.onRetry(data.copyOf(), retryCount, maxRetries)
                            }
                        }
                    }
                }
            }

            override fun onSuccess(data: ByteArray, response: ByteArray) {
                mainHandler.post {
                    if (isCurrentGeneration(generation)) {
                        reliableSendListener?.let { listener ->
                            safely("可靠发送成功业务回调异常") {
                                listener.onSuccess(data.copyOf(), response.copyOf())
                            }
                        }
                    }
                }
            }

            override fun onFailure(data: ByteArray, reason: ReliableSendFailure) {
                mainHandler.post {
                    if (isCurrentGeneration(generation)) {
                        reliableSendListener?.let { listener ->
                            safely("可靠发送失败业务回调异常") {
                                listener.onFailure(data.copyOf(), reason)
                            }
                        }
                    }
                }
            }
        })
        newManager.setOnSerialErrorListener { error ->
            mainHandler.post {
                if (isCurrentGeneration(generation)) {
                    errorListener?.let { listener ->
                        safely("串口错误业务回调异常") { listener.onError(error) }
                    }
                }
            }
        }

        val opened = newManager.openSerialPort(devicePath, baudRate)
        if (!opened && manager === newManager) manager = null
        return opened
    }

    fun sendData(data: ByteArray?): Boolean {
        if (data == null || data.isEmpty()) return false
        return manager?.sendBytes(data) == true
    }

    fun sendData(data: String?): Boolean {
        return data != null && sendData(data.toByteArray(StandardCharsets.UTF_8))
    }

    @Synchronized
    fun closeSerialPort() {
        connectionGeneration++
        manager?.closeSerialPort()
        manager = null
        openCallback = null
        dataCallback = null
    }

    override fun close() = closeSerialPort()

    @Synchronized
    private fun isCurrentGeneration(generation: Long): Boolean = connectionGeneration == generation

    fun setOnReliableSendListener(listener: OnReliableSendListener?): SimpleSerialPortManager = apply {
        reliableSendListener = listener
    }

    fun setOnSerialErrorListener(listener: OnSerialErrorListener?): SimpleSerialPortManager = apply {
        errorListener = listener
    }

    fun isSerialPortOpened(): Boolean = manager?.isOpen() == true

    fun setDatabits(value: Int): SimpleSerialPortManager = apply {
        require(value in 5..8) { "databits must be between 5 and 8" }
        databits = value
    }

    fun setParity(value: Int): SimpleSerialPortManager = apply {
        require(value in 0..4) { "parity must be between 0 and 4" }
        parity = value
    }

    fun setStopbits(value: Int): SimpleSerialPortManager = apply {
        require(value == 1 || value == 2) { "stopbits must be 1 or 2" }
        stopbits = value
    }

    fun setFlags(value: Int): SimpleSerialPortManager = apply { flags = value }

    fun getDatabits(): Int = databits
    fun getParity(): Int = parity
    fun getStopbits(): Int = stopbits
    fun getFlags(): Int = flags
    fun getSerialConfig(): SerialConfig? = config

    private fun updateSerialConfig() {
        config?.let { current ->
            current.databits = databits
            current.parity = parity
            current.stopbits = stopbits
            current.flags = flags
            current.stickyPacketHelpers = stickyPacketHelpers.toTypedArray()
        }
    }

    private fun SerialPortManager.setSerialConfigAndReturn(config: SerialConfig): SerialPortManager {
        setSerialConfig(config)
        return this
    }

    private fun SerialPortManager.setStickPackageHelpersAndReturn(
        helpers: List<AbsStickPackageHelper>,
    ): SerialPortManager {
        setStickPackageHelpers(helpers)
        return this
    }

    private inline fun safely(message: String, action: () -> Unit) {
        try {
            action()
        } catch (error: Exception) {
            SerialPortLogUtil.e(TAG, message, error)
        }
    }

    enum class StickyPacketStrategy {
        NO_PROCESSING,
        DELIMITER_BASED,
        FIXED_LENGTH,
        VARIABLE_LENGTH,
    }

    fun interface OnOpenSerialPortCallback {
        fun onStatusChanged(success: Boolean, status: SerialStatus)
    }

    interface OnDataReceivedCallback {
        fun onDataReceived(data: ByteArray)

        fun onDataSent(data: ByteArray) = Unit
    }

    class QuickConfig {
        private var intervalSleep = DEFAULT_READ_INTERVAL_MS
        private var enableLog = true
        private var logTag = DEFAULT_LOG_TAG
        private var stickyPacketStrategy = StickyPacketStrategy.NO_PROCESSING
        private var maxPacketSize = 1024
        private var packetTimeout = 1000
        private var autoReconnect = false
        private var reconnectInterval = 5000
        private var maxReconnectAttempts = 3
        private var enableReliableSend = false
        private var responseTimeoutMillis = 1000
        private var maxSendRetries = 2
        private var sendRetryIntervalMillis = 100
        private var responseMatcher: SerialResponseMatcher? = null
        private var databits = 8
        private var parity = 0
        private var stopbits = 1
        private var flags = 0

        fun setIntervalSleep(value: Int) = apply { intervalSleep = value }
        fun setEnableLog(value: Boolean) = apply { enableLog = value }
        fun setLogTag(value: String) = apply { logTag = value }
        fun setStickyPacketStrategy(value: StickyPacketStrategy) = apply { stickyPacketStrategy = value }
        fun setMaxPacketSize(value: Int) = apply { maxPacketSize = value }
        fun setPacketTimeout(value: Int) = apply { packetTimeout = value }
        fun setAutoReconnect(value: Boolean) = apply { autoReconnect = value }
        fun setReconnectInterval(value: Int) = apply { reconnectInterval = value }
        fun setMaxReconnectAttempts(value: Int) = apply { maxReconnectAttempts = value }
        fun setEnableReliableSend(value: Boolean) = apply { enableReliableSend = value }
        fun setResponseTimeoutMillis(value: Int) = apply { responseTimeoutMillis = value }
        fun setMaxSendRetries(value: Int) = apply { maxSendRetries = value }
        fun setSendRetryIntervalMillis(value: Int) = apply { sendRetryIntervalMillis = value }
        fun setResponseMatcher(value: SerialResponseMatcher?) = apply { responseMatcher = value }
        fun setDatabits(value: Int) = apply { databits = value }
        fun setParity(value: Int) = apply { parity = value }
        fun setStopbits(value: Int) = apply { stopbits = value }
        fun setFlags(value: Int) = apply { flags = value }

        fun apply(): SimpleSerialPortManager {
            SerialPortLogUtil.setDefaultTag(logTag)
            val config = SerialConfig.Builder()
                .setIntervalSleep(intervalSleep)
                .setEnableLogging(enableLog)
                .setEnableStickyPacketProcessing(stickyPacketStrategy != StickyPacketStrategy.NO_PROCESSING)
                .setMaxPacketSize(maxPacketSize)
                .setPacketTimeout(packetTimeout)
                .setAutoReconnect(autoReconnect)
                .setReconnectInterval(reconnectInterval)
                .setMaxReconnectAttempts(maxReconnectAttempts)
                .setEnableReliableSend(enableReliableSend)
                .setResponseTimeoutMillis(responseTimeoutMillis)
                .setMaxSendRetries(maxSendRetries)
                .setSendRetryIntervalMillis(sendRetryIntervalMillis)
                .setResponseMatcher(responseMatcher)
                .setDatabits(databits)
                .setParity(parity)
                .setStopbits(stopbits)
                .setFlags(flags)
                .build()
            return getInstance()
                .init(config)
                .configureStickyPacket(stickyPacketStrategy)
                .setDatabits(databits)
                .setParity(parity)
                .setStopbits(stopbits)
                .setFlags(flags)
        }

        @Deprecated("Application 参数未被使用，请改用 apply()")
        fun apply(@Suppress("UNUSED_PARAMETER") application: Application): SimpleSerialPortManager = apply()
    }

    companion object {
        private const val TAG = "SimpleSerialPortManager"
        private const val DEFAULT_LOG_TAG = "SerialPort"
        private const val DEFAULT_READ_INTERVAL_MS = 50

        @Volatile
        private var instance: SimpleSerialPortManager? = null

        @JvmStatic
        fun getInstance(): SimpleSerialPortManager {
            return instance ?: synchronized(this) {
                instance ?: SimpleSerialPortManager().also { instance = it }
            }
        }

        /** 创建生命周期互不影响的单串口管理器。 */
        @JvmStatic
        fun create(): SimpleSerialPortManager = SimpleSerialPortManager()

        @JvmStatic
        fun multi(): MultiSerialPortManager = MultiSerialPortManager.getInstance()

        @JvmStatic
        fun createMulti(): MultiSerialPortManager = MultiSerialPortManager.create()
    }
}
