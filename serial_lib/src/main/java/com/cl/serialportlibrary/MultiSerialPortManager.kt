package com.cl.serialportlibrary

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
import com.cl.serialportlibrary.utils.SerialPortLogUtil
import java.io.Closeable
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

/** 线程安全的多串口管理入口，每个串口 ID 拥有独立连接和配置。 */
class MultiSerialPortManager private constructor() : Closeable {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val managers = ConcurrentHashMap<String, SerialPortManager>()
    private val configs = ConcurrentHashMap<String, SerialConfig>()
    private val portEnums = ConcurrentHashMap<String, SerialPortEnum>()
    private val connectionTokens = ConcurrentHashMap<String, Any>()

    @Volatile
    private var errorListener: OnSerialErrorListener? = null

    @Synchronized
    fun openSerialPort(
        serialId: String,
        devicePath: String,
        baudRate: Int,
        config: SerialConfig,
        statusCallback: OnSerialPortStatusCallback?,
        dataCallback: OnSerialPortDataCallback?,
    ): Boolean {
        require(serialId.isNotBlank()) { "serialId must not be blank" }
        require(devicePath.isNotBlank()) { "devicePath must not be blank" }
        require(baudRate >= 0) { "baudRate must not be negative" }

        if (managers.containsKey(serialId)) {
            SerialPortLogUtil.w(TAG, "串口[$serialId] 已存在，先关闭旧连接")
            closeSerialPort(serialId)
        }

        SerialPortLogUtil.printSeparator(TAG, "打开串口 $serialId")
        val token = Any()
        val serialConfig = config.copyForConnection()
        val portEnum = getAvailableSerialPortEnum()
        val manager = SerialPortManager(portEnum)

        connectionTokens[serialId] = token
        configs[serialId] = serialConfig
        portEnums[serialId] = portEnum

        manager.setSerialConfig(serialConfig)
        manager.setOnSerialErrorListener { error ->
            val serialError = error.copy(serialId = serialId)
            mainHandler.post {
                if (isCurrentConnection(serialId, token)) {
                    errorListener?.let { listener ->
                        safely("串口[$serialId] 错误业务回调异常") {
                            listener.onError(serialError)
                        }
                    }
                }
            }
        }
        manager.setOnOpenSerialPortListener { _, device, status ->
            val success = status == SerialStatus.SUCCESS_OPENED
            if (success) {
                SerialPortLogUtil.i(TAG, "串口[$serialId] 状态变化: ${device.path} - $status")
            } else {
                SerialPortLogUtil.e(TAG, "串口[$serialId] 状态变化: ${device.path} - $status")
                synchronized(this) {
                    if (isCurrentConnection(serialId, token)) {
                        managers.remove(serialId, manager)
                        configs.remove(serialId)
                        portEnums.remove(serialId)
                    }
                }
            }
            mainHandler.post {
                if (!isCurrentConnection(serialId, token)) return@post
                statusCallback?.let { callback ->
                    safely("串口[$serialId] 状态业务回调异常") {
                        callback.onStatusChanged(serialId, success, status)
                    }
                }
                if (!success) connectionTokens.remove(serialId, token)
            }
        }
        manager.setOnSerialPortDataListener(object : OnSerialPortDataListener {
            override fun onDataReceived(bytes: ByteArray, serialPortEnum: SerialPortEnum) {
                val snapshot = bytes.copyOf()
                SerialPortLogUtil.printData("${TAG}_$serialId", "接收数据", snapshot)
                mainHandler.post {
                    if (isCurrentConnection(serialId, token) && dataCallback != null) {
                        safely("串口[$serialId] 接收业务回调异常") {
                            dataCallback.onDataReceived(serialId, snapshot)
                        }
                    }
                }
            }

            override fun onDataSent(bytes: ByteArray, serialPortEnum: SerialPortEnum) {
                val snapshot = bytes.copyOf()
                SerialPortLogUtil.printData("${TAG}_$serialId", "发送数据", snapshot)
                mainHandler.post {
                    if (isCurrentConnection(serialId, token) && dataCallback != null) {
                        safely("串口[$serialId] 发送业务回调异常") {
                            dataCallback.onDataSent(serialId, snapshot)
                        }
                    }
                }
            }
        })

        val opened = manager.openSerialPort(devicePath, baudRate)
        if (opened) {
            managers[serialId] = manager
            SerialPortLogUtil.i(TAG, "串口[$serialId] 打开成功")
        } else {
            managers.remove(serialId)
            configs.remove(serialId)
            portEnums.remove(serialId)
            SerialPortLogUtil.e(TAG, "串口[$serialId] 打开失败")
        }
        return opened
    }

    @Deprecated("请直接使用统一的 SerialConfig")
    fun openSerialPort(
        serialId: String,
        devicePath: String,
        baudRate: Int,
        config: SerialPortConfig,
        statusCallback: OnSerialPortStatusCallback?,
        dataCallback: OnSerialPortDataCallback?,
    ): Boolean {
        return openSerialPort(
            serialId,
            devicePath,
            baudRate,
            config.toSerialConfig(),
            statusCallback,
            dataCallback,
        )
    }

    fun openSerialPort(
        serialId: String,
        devicePath: String,
        baudRate: Int,
        dataCallback: OnSerialPortDataCallback?,
    ): Boolean {
        return openSerialPort(
            serialId,
            devicePath,
            baudRate,
            SerialConfig.Builder().build(),
            null,
            dataCallback,
        )
    }

    fun sendData(serialId: String, data: ByteArray?): Boolean {
        if (data == null || data.isEmpty()) {
            SerialPortLogUtil.w(TAG, "串口[$serialId] 尝试发送空数据")
            return false
        }
        val manager = managers[serialId] ?: run {
            SerialPortLogUtil.e(TAG, "串口[$serialId] 未打开，无法发送数据")
            return false
        }
        val startTime = System.currentTimeMillis()
        val accepted = manager.sendBytes(data)
        SerialPortLogUtil.printPerformance("${TAG}_$serialId", "提交发送数据", startTime)
        if (!accepted) SerialPortLogUtil.e(TAG, "串口[$serialId] 数据发送失败")
        return accepted
    }

    fun sendData(serialId: String, data: String?): Boolean {
        return data != null && sendData(serialId, data.toByteArray(StandardCharsets.UTF_8))
    }

    @Synchronized
    fun closeSerialPort(serialId: String) {
        connectionTokens.remove(serialId)
        managers.remove(serialId)?.let { manager ->
            SerialPortLogUtil.i(TAG, "关闭串口[$serialId]")
            manager.closeSerialPort()
        }
        configs.remove(serialId)
        portEnums.remove(serialId)
    }

    @Synchronized
    fun closeAllSerialPorts() {
        managers.keys.toList().forEach(::closeSerialPort)
    }

    override fun close() = closeAllSerialPorts()

    fun isSerialPortOpened(serialId: String): Boolean = managers[serialId]?.isOpen() == true

    val openedSerialPorts: List<String>
        get() = managers.entries
            .filter { (_, manager) -> manager.isOpen() }
            .map { (serialId, _) -> serialId }
            .sorted()

    fun getSerialConfig(serialId: String): SerialConfig? = configs[serialId]

    fun setOnReliableSendListener(serialId: String, listener: OnReliableSendListener?): Boolean {
        val manager = managers[serialId] ?: return false
        val token = connectionTokens[serialId] ?: return false
        if (listener == null) {
            manager.setOnReliableSendListener(null)
            return true
        }
        manager.setOnReliableSendListener(object : OnReliableSendListener {
            override fun onRetry(data: ByteArray, retryCount: Int, maxRetries: Int) {
                mainHandler.post {
                    if (isCurrentConnection(serialId, token)) {
                        safely("串口[$serialId] 可靠发送重试业务回调异常") {
                            listener.onRetry(data.copyOf(), retryCount, maxRetries)
                        }
                    }
                }
            }

            override fun onSuccess(data: ByteArray, response: ByteArray) {
                mainHandler.post {
                    if (isCurrentConnection(serialId, token)) {
                        safely("串口[$serialId] 可靠发送成功业务回调异常") {
                            listener.onSuccess(data.copyOf(), response.copyOf())
                        }
                    }
                }
            }

            override fun onFailure(data: ByteArray, reason: ReliableSendFailure) {
                mainHandler.post {
                    if (isCurrentConnection(serialId, token)) {
                        safely("串口[$serialId] 可靠发送失败业务回调异常") {
                            listener.onFailure(data.copyOf(), reason)
                        }
                    }
                }
            }
        })
        return true
    }

    fun setOnSerialErrorListener(serialId: String, listener: OnSerialErrorListener?): Boolean {
        val manager = managers[serialId] ?: return false
        val token = connectionTokens[serialId] ?: return false
        manager.setOnSerialErrorListener(
            if (listener == null) {
                null
            } else {
                OnSerialErrorListener { error ->
                    mainHandler.post {
                        if (isCurrentConnection(serialId, token)) {
                            safely("串口[$serialId] 错误业务回调异常") {
                                listener.onError(error.copy(serialId = serialId))
                            }
                        }
                    }
                }
            },
        )
        return true
    }

    /** 设置所有串口的统一错误监听；应在打开串口前注册。 */
    fun setOnSerialErrorListener(listener: OnSerialErrorListener?): MultiSerialPortManager = apply {
        errorListener = listener
    }

    @Synchronized
    fun updateStickyPacketHelpers(
        serialId: String,
        helpers: Array<AbsStickPackageHelper>?,
    ): Boolean {
        if (helpers.isNullOrEmpty()) return false
        val manager = managers[serialId] ?: return false
        val config = configs[serialId] ?: return false
        config.stickyPacketHelpers = helpers
        manager.setStickPackageHelpers(helpers.toList())
        return true
    }

    fun printAllSerialStatus() {
        SerialPortLogUtil.printSeparator(TAG, "所有串口状态")
        if (managers.isEmpty()) {
            SerialPortLogUtil.i(TAG, "当前没有打开的串口")
            return
        }
        managers.forEach { (serialId, manager) ->
            SerialPortLogUtil.i(TAG, "串口[$serialId] - 状态: ${if (manager.isOpen()) "已打开" else "已关闭"}")
            configs[serialId]?.let { config ->
                SerialPortLogUtil.printSerialConfig(
                    "${TAG}_$serialId",
                    config.databits,
                    config.parity,
                    config.stopbits,
                    config.flags,
                )
            }
        }
    }

    private fun isCurrentConnection(serialId: String, token: Any): Boolean {
        return connectionTokens[serialId] === token
    }

    private fun getAvailableSerialPortEnum(): SerialPortEnum {
        return SerialPortEnum.entries.firstOrNull { candidate -> candidate !in portEnums.values }
            ?: SerialPortEnum.SERIAL_ONE.also {
                SerialPortLogUtil.w(TAG, "已超过 6 个并发串口，回调枚举将复用 SERIAL_ONE")
            }
    }

    private inline fun safely(message: String, action: () -> Unit) {
        try {
            action()
        } catch (error: Exception) {
            SerialPortLogUtil.e(TAG, message, error)
        }
    }

    fun interface OnSerialPortStatusCallback {
        fun onStatusChanged(serialId: String, success: Boolean, status: SerialStatus)
    }

    interface OnSerialPortDataCallback {
        fun onDataReceived(serialId: String, data: ByteArray)

        fun onDataSent(serialId: String, data: ByteArray) = Unit
    }

    @Deprecated("请直接使用统一的 SerialConfig")
    class SerialPortConfig private constructor(builder: Builder) {
        internal val enableLogging = builder.enableLogging
        internal val intervalSleep = builder.intervalSleep
        internal val databits = builder.databits
        internal val parity = builder.parity
        internal val stopbits = builder.stopbits
        internal val flags = builder.flags
        internal val maxPacketSize = builder.maxPacketSize
        internal val packetTimeout = builder.packetTimeout
        internal val autoReconnect = builder.autoReconnect
        internal val reconnectInterval = builder.reconnectInterval
        internal val maxReconnectAttempts = builder.maxReconnectAttempts
        internal val enableReliableSend = builder.enableReliableSend
        internal val responseTimeoutMillis = builder.responseTimeoutMillis
        internal val maxSendRetries = builder.maxSendRetries
        internal val sendRetryIntervalMillis = builder.sendRetryIntervalMillis
        internal val responseMatcher = builder.responseMatcher
        internal val stickyPacketHelpers = builder.stickyPacketHelpers?.copyOf()

        init {
            toSerialConfig()
        }

        internal fun toSerialConfig(): SerialConfig {
            return SerialConfig.Builder()
                .setEnableLogging(enableLogging)
                .setIntervalSleep(intervalSleep)
                .setDatabits(databits)
                .setParity(parity)
                .setStopbits(stopbits)
                .setFlags(flags)
                .setEnableStickyPacketProcessing(!stickyPacketHelpers.isNullOrEmpty())
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
                .setStickyPacketHelpers(*(stickyPacketHelpers ?: arrayOf(BaseStickPackageHelper())))
                .build()
        }

        class Builder {
            internal var enableLogging = true
            internal var intervalSleep = 50
            internal var databits = 8
            internal var parity = 0
            internal var stopbits = 1
            internal var flags = 0
            internal var maxPacketSize = 1024
            internal var packetTimeout = 1000
            internal var autoReconnect = false
            internal var reconnectInterval = 5000
            internal var maxReconnectAttempts = 3
            internal var enableReliableSend = false
            internal var responseTimeoutMillis = 1000
            internal var maxSendRetries = 2
            internal var sendRetryIntervalMillis = 100
            internal var responseMatcher: SerialResponseMatcher? = null
            internal var stickyPacketHelpers: Array<out AbsStickPackageHelper>? = null

            fun setEnableLogging(value: Boolean) = apply { enableLogging = value }
            fun setIntervalSleep(value: Int) = apply { intervalSleep = value }
            fun setDatabits(value: Int) = apply { databits = value }
            fun setParity(value: Int) = apply { parity = value }
            fun setStopbits(value: Int) = apply { stopbits = value }
            fun setFlags(value: Int) = apply { flags = value }
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
            fun setStickyPacketHelpers(vararg value: AbsStickPackageHelper) = apply {
                stickyPacketHelpers = value.map { helper -> helper }.toTypedArray()
            }

            fun build(): SerialPortConfig = SerialPortConfig(this)
        }
    }

    companion object {
        private const val TAG = "MultiSerialPortManager"

        @Volatile
        private var instance: MultiSerialPortManager? = null

        @JvmStatic
        fun getInstance(): MultiSerialPortManager {
            return instance ?: synchronized(this) {
                instance ?: MultiSerialPortManager().also { instance = it }
            }
        }

        /** 创建生命周期互不影响的管理器，页面或业务组件持有时优先使用。 */
        @JvmStatic
        fun create(): MultiSerialPortManager = MultiSerialPortManager()
    }
}
