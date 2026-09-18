package com.cl.serialportlibrary

import com.cl.serialportlibrary.enumerate.ReliableSendFailure
import com.cl.serialportlibrary.enumerate.SerialPortEnum
import com.cl.serialportlibrary.enumerate.SerialStatus
import com.cl.serialportlibrary.listener.OnOpenSerialPortListener
import com.cl.serialportlibrary.listener.OnReliableSendListener
import com.cl.serialportlibrary.listener.OnSerialErrorListener
import com.cl.serialportlibrary.listener.OnSerialPortDataListener
import com.cl.serialportlibrary.listener.SerialResponseMatcher
import com.cl.serialportlibrary.stick.AbsStickPackageHelper
import com.cl.serialportlibrary.stick.BaseStickPackageHelper
import com.cl.serialportlibrary.stick.PacketGuardInputStream
import com.cl.serialportlibrary.stick.PacketReadTimeoutException
import com.cl.serialportlibrary.stick.PacketTooLargeException
import com.cl.serialportlibrary.utils.SerialPortLogUtil
import java.io.Closeable
import java.io.EOFException
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 串口管理器，使用协程串行处理收发，并在异常断线后按配置自动重连。
 */
open class SerialPortManager @JvmOverloads constructor(
    private val mSerialPortEnum: SerialPortEnum = SerialPortEnum.SERIAL_ONE,
) : SerialPort() {

    private val lifecycleLock = Any()

    @Volatile
    private var mFileInputStream: FileInputStream? = null

    @Volatile
    private var mFileOutputStream: FileOutputStream? = null

    // JNI 按字段名读取该成员，不能重命名或改为委托属性。
    @Volatile
    private var mFd: FileDescriptor? = null

    @Volatile
    private var mOnOpenSerialPortListener: OnOpenSerialPortListener? = null

    @Volatile
    private var mOnSerialPortDataListener: OnSerialPortDataListener? = null

    @Volatile
    private var mOnReliableSendListener: OnReliableSendListener? = null

    @Volatile
    private var mOnSerialErrorListener: OnSerialErrorListener? = null

    @Volatile
    private var connectionOpen = false

    @Volatile
    private var userClosed = true

    @Volatile
    private var reconnecting = false

    @Volatile
    private var connectionGeneration = 0L

    private var mSerialConfig: SerialConfig? = null
    private var mStickPackageHelpers: List<AbsStickPackageHelper> = listOf(BaseStickPackageHelper())
    private var connectedDevicePath: String? = null
    private var connectedBaudRate = 0

    private var ioScope: CoroutineScope? = null
    private var sendChannel: Channel<OutboundRequest>? = null
    private var sendJob: Job? = null
    private var readJob: Job? = null
    private var reconnectJob: Job? = null
    private var pendingReliableRequest: PendingReliableRequest? = null

    open fun setSerialConfig(config: SerialConfig) {
        synchronized(lifecycleLock) {
            mSerialConfig = config
            val helpers = config.stickyPacketHelpers
            if (config.isEnableStickyPacketProcessing && helpers.isNotEmpty()) {
                mStickPackageHelpers = helpers.toList()
            } else {
                mStickPackageHelpers = listOf(BaseStickPackageHelper(config.intervalSleep.toLong()))
            }
        }
    }

    open fun setStickPackageHelpers(helpers: List<AbsStickPackageHelper>?) {
        if (helpers.isNullOrEmpty()) {
            SerialPortLogUtil.w(TAG, "忽略空的粘包处理器列表")
            return
        }
        synchronized(lifecycleLock) {
            mStickPackageHelpers = helpers.toList()
        }
    }

    @Synchronized
    open fun openSerialPort(devicePath: String, baudRate: Int): Boolean {
        closeSerialPort()
        SerialPortLogUtil.i(TAG, "打开串口 $devicePath，波特率 $baudRate")

        val deviceFile = File(devicePath)
        if (!checkSerialPortPermission(deviceFile, notifyFailure = true)) {
            return false
        }

        val opened = synchronized(lifecycleLock) {
            userClosed = false
            connectedDevicePath = devicePath
            connectedBaudRate = baudRate
            ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

            if (openResourcesLocked(devicePath, baudRate)) {
                connectionOpen = true
                reconnecting = false
                startIoCoroutinesLocked()
                true
            } else {
                userClosed = true
                connectedDevicePath = null
                connectedBaudRate = 0
                ioScope?.cancel()
                ioScope = null
                false
            }
        }

        notifySerialPortOpened(
            deviceFile,
            if (opened) SerialStatus.SUCCESS_OPENED else SerialStatus.OPEN_FAIL,
        )
        return opened
    }

    open fun isOpen(): Boolean {
        return connectionOpen && mFd?.valid() == true
    }

    @Synchronized
    open fun closeSerialPort() {
        val scopeToCancel: CoroutineScope?
        synchronized(lifecycleLock) {
            userClosed = true
            reconnecting = false
            connectionOpen = false
            connectionGeneration++

            reconnectJob?.cancel()
            reconnectJob = null
            stopIoCoroutinesLocked()
            closeResourcesLocked()

            connectedDevicePath = null
            connectedBaudRate = 0
            scopeToCancel = ioScope
            ioScope = null
        }
        scopeToCancel?.cancel()
    }

    open fun setOnOpenSerialPortListener(listener: OnOpenSerialPortListener?): SerialPortManager {
        mOnOpenSerialPortListener = listener
        return this
    }

    open fun setOnSerialPortDataListener(listener: OnSerialPortDataListener?): SerialPortManager {
        mOnSerialPortDataListener = listener
        return this
    }

    open fun setOnReliableSendListener(listener: OnReliableSendListener?): SerialPortManager {
        mOnReliableSendListener = listener
        return this
    }

    open fun setOnSerialErrorListener(listener: OnSerialErrorListener?): SerialPortManager {
        mOnSerialErrorListener = listener
        return this
    }

    open fun sendBytes(sendBytes: ByteArray?): Boolean {
        if (sendBytes == null || sendBytes.isEmpty() || !isOpen()) {
            return false
        }
        return sendChannel?.trySend(OutboundRequest(sendBytes.copyOf()))?.isSuccess == true
    }

    private fun openResourcesLocked(devicePath: String, baudRate: Int): Boolean {
        return try {
            val config = mSerialConfig
            val flags = config?.flags ?: 0
            val databits = config?.databits ?: 8
            val stopbits = config?.stopbits ?: 1
            val parity = config?.parity ?: 0

            SerialPortLogUtil.d(
                TAG,
                "串口参数 - flags: $flags, databits: $databits, stopbits: $stopbits, parity: $parity",
            )

            val fileDescriptor = super.open(
                devicePath,
                baudRate,
                flags,
                databits,
                stopbits,
                parity,
            ) ?: throw IOException("打开串口失败，FileDescriptor 为空")

            mFd = fileDescriptor
            mFileInputStream = FileInputStream(fileDescriptor)
            mFileOutputStream = FileOutputStream(fileDescriptor)
            SerialPortLogUtil.i(TAG, "串口打开成功: $devicePath")
            true
        } catch (error: Exception) {
            SerialPortLogUtil.e(TAG, "打开串口失败: $devicePath", error)
            reportError(SerialErrorCode.OPEN_FAILED, "打开串口失败: $devicePath", error, devicePath)
            closeResourcesLocked()
            false
        }
    }

    private fun startIoCoroutinesLocked() {
        val scope = ioScope ?: return
        val channel = Channel<OutboundRequest>(SEND_QUEUE_CAPACITY)
        val generation = ++connectionGeneration
        val devicePath = connectedDevicePath ?: return

        sendChannel = channel
        startSendCoroutine(scope, channel, generation)
        startReadCoroutine(scope, devicePath, generation)
    }

    private fun startSendCoroutine(
        scope: CoroutineScope,
        channel: Channel<OutboundRequest>,
        generation: Long,
    ) {
        sendJob = scope.launch {
            try {
                for (request in channel) {
                    if (!isCurrentConnection(generation)) return@launch
                    val options = synchronized(lifecycleLock) {
                        mSerialConfig?.takeIf { it.isEnableReliableSend }?.let {
                            ReliableSendOptions(
                                responseTimeoutMillis = it.responseTimeoutMillis.toLong(),
                                maxRetries = it.maxSendRetries,
                                retryIntervalMillis = it.sendRetryIntervalMillis.toLong(),
                                responseMatcher = it.responseMatcher,
                            )
                        }
                    }
                    if (options == null) {
                        writeOnce(request.data, generation)
                    } else {
                        sendReliably(request.data, options, generation)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                reportError(SerialErrorCode.WRITE_FAILED, "发送串口数据失败", error)
                handleConnectionFailure("发送数据", error, generation)
            }
        }
    }

    private suspend fun writeOnce(data: ByteArray, generation: Long) {
        if (!isCurrentConnection(generation)) {
            throw IOException("串口连接不可用")
        }
        val outputStream = mFileOutputStream ?: throw IOException("串口输出流不可用")
        runInterruptible {
            outputStream.write(data)
            outputStream.flush()
        }
        if (isCurrentConnection(generation)) {
            notifyDataSent(data)
        }
    }

    private suspend fun sendReliably(
        data: ByteArray,
        options: ReliableSendOptions,
        generation: Long,
    ) {
        val response = CompletableDeferred<ByteArray>()
        val pending = PendingReliableRequest(
            data = data,
            generation = generation,
            matcher = options.responseMatcher,
            response = response,
        )
        synchronized(lifecycleLock) {
            pendingReliableRequest = pending
        }

        try {
            for (attempt in 0..options.maxRetries) {
                if (attempt > 0) {
                    val lateResponse = if (options.retryIntervalMillis > 0) {
                        withTimeoutOrNull(options.retryIntervalMillis) { response.await() }
                    } else if (response.isCompleted) {
                        response.await()
                    } else {
                        null
                    }
                    if (lateResponse != null) {
                        notifyReliableSendSuccess(data, lateResponse)
                        return
                    }
                    notifyReliableSendRetry(data, attempt, options.maxRetries)
                }

                writeOnce(data, generation)
                val received = withTimeoutOrNull(options.responseTimeoutMillis) {
                    response.await()
                }
                if (received != null) {
                    notifyReliableSendSuccess(data, received)
                    return
                }
            }

            if (response.isCompleted) {
                notifyReliableSendSuccess(data, response.await())
                return
            }

            SerialPortLogUtil.e(
                TAG,
                "等待串口响应超时，已重发 ${options.maxRetries} 次，数据长度: ${data.size}",
            )
            reportError(
                SerialErrorCode.RESPONSE_TIMEOUT,
                "等待串口响应超时，已重发 ${options.maxRetries} 次",
            )
            notifyReliableSendFailure(data, ReliableSendFailure.RESPONSE_TIMEOUT)
        } catch (error: CancellationException) {
            val reason = if (userClosed) {
                ReliableSendFailure.CONNECTION_CLOSED
            } else {
                ReliableSendFailure.CONNECTION_LOST
            }
            notifyReliableSendFailure(data, reason)
            throw error
        } catch (error: IOException) {
            val reason = if (userClosed) {
                ReliableSendFailure.CONNECTION_CLOSED
            } else {
                ReliableSendFailure.CONNECTION_LOST
            }
            notifyReliableSendFailure(data, reason)
            throw error
        } finally {
            synchronized(lifecycleLock) {
                if (pendingReliableRequest === pending) {
                    pendingReliableRequest = null
                }
            }
        }
    }

    private fun startReadCoroutine(
        scope: CoroutineScope,
        devicePath: String,
        generation: Long,
    ) {
        val inputStream = mFileInputStream ?: return
        val monitoredInputStream = DisconnectAwareInputStream(inputStream, devicePath)
        val job = scope.launch(start = CoroutineStart.LAZY) {
            var activeRead: ActivePacketRead? = null
            while (isActive && isCurrentConnection(generation)) {
                try {
                    val packetRead = activeRead ?: synchronized(lifecycleLock) {
                        val options = ReadOptions(
                            helper = selectStickPackageHelper(mStickPackageHelpers)
                                ?: return@synchronized null,
                            packetTimeoutMillis = mSerialConfig?.packetTimeout?.toLong()
                                ?: DEFAULT_PACKET_TIMEOUT_MS,
                            maxPacketSize = mSerialConfig?.maxPacketSize
                                ?: DEFAULT_MAX_PACKET_SIZE,
                        )
                        ActivePacketRead(
                            options = options,
                            inputStream = PacketGuardInputStream(
                                inputStream = monitoredInputStream,
                                packetTimeoutMillis = options.packetTimeoutMillis,
                                maxPacketSize = options.maxPacketSize,
                            ),
                        )
                    }
                    if (packetRead == null) return@launch
                    activeRead = packetRead
                    val buffer = runInterruptible {
                        packetRead.options.helper.execute(packetRead.inputStream)
                    }
                    if (buffer != null && buffer.isNotEmpty() && isCurrentConnection(generation)) {
                        if (buffer.size > packetRead.options.maxPacketSize) {
                            throw PacketTooLargeException(packetRead.options.maxPacketSize)
                        }
                        activeRead = null
                        SerialPortLogUtil.d(TAG, "接收数据，长度: ${buffer.size}")
                        completeReliableResponse(buffer, generation)
                        notifyDataReceived(buffer)
                    } else if (!File(devicePath).exists()) {
                        throw IOException("串口设备节点已断开: $devicePath")
                    } else if (!packetRead.inputStream.hasReadData) {
                        // 本轮没有读到任何数据，不保留配置快照，以便运行时配置可以及时生效。
                        activeRead = null
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: PacketReadTimeoutException) {
                    activeRead = null
                    SerialPortLogUtil.w(TAG, error.message ?: "串口数据包接收超时")
                    reportError(
                        SerialErrorCode.PACKET_TIMEOUT,
                        error.message ?: "串口数据包接收超时",
                        error,
                        devicePath,
                    )
                } catch (error: PacketTooLargeException) {
                    activeRead = null
                    SerialPortLogUtil.e(TAG, error.message ?: "串口数据包过长", error)
                    reportError(
                        SerialErrorCode.PACKET_TOO_LARGE,
                        error.message ?: "串口数据包过长",
                        error,
                        devicePath,
                    )
                    delay(PROCESSING_ERROR_RETRY_DELAY_MS)
                } catch (error: IOException) {
                    reportError(SerialErrorCode.READ_FAILED, "读取串口数据失败", error, devicePath)
                    handleConnectionFailure("读取数据", error, generation)
                    return@launch
                } catch (error: Exception) {
                    activeRead = null
                    SerialPortLogUtil.e(TAG, "拆包处理异常", error)
                    reportError(SerialErrorCode.PACKET_PARSE_FAILED, "拆包处理异常", error, devicePath)
                    delay(PROCESSING_ERROR_RETRY_DELAY_MS)
                }
            }
        }
        job.invokeOnCompletion { cause ->
            if (cause !is CancellationException && isCurrentConnection(generation)) {
                val error = IOException("串口接收任务意外结束", cause)
                reportError(SerialErrorCode.READ_FAILED, error.message.orEmpty(), cause, devicePath)
                handleConnectionFailure("接收任务", error, generation)
            }
        }
        readJob = job
        job.start()
    }

    private fun handleConnectionFailure(
        operation: String,
        error: IOException,
        generation: Long,
    ) {
        var reconnectRequest: ReconnectRequest? = null
        var notifyDisconnected = false
        var disconnectedDevicePath: String? = null
        var scopeToCancel: CoroutineScope? = null

        synchronized(lifecycleLock) {
            if (userClosed || reconnecting || !connectionOpen || connectionGeneration != generation) {
                return
            }

            SerialPortLogUtil.e(TAG, "$operation 异常，串口连接已断开", error)
            connectionOpen = false
            stopIoCoroutinesLocked()
            closeResourcesLocked()

            val devicePath = connectedDevicePath
            val config = mSerialConfig
            val reconnectEnabled = config?.isAutoReconnect == true ||
                config?.isSerialPortReconnection == true
            val maxAttempts = config?.maxReconnectAttempts?.coerceAtLeast(0) ?: 0

            if (reconnectEnabled && maxAttempts > 0 && devicePath != null) {
                reconnecting = true
                reconnectRequest = ReconnectRequest(
                    devicePath = devicePath,
                    baudRate = connectedBaudRate,
                    intervalMillis = config?.reconnectInterval?.toLong()?.coerceAtLeast(0L) ?: 0L,
                    maxAttempts = maxAttempts,
                )
            } else {
                notifyDisconnected = true
                disconnectedDevicePath = devicePath
                userClosed = true
                connectedDevicePath = null
                connectedBaudRate = 0
                scopeToCancel = ioScope
                ioScope = null
            }
        }

        val request = reconnectRequest
        if (request != null) {
            startReconnect(request)
        } else if (notifyDisconnected) {
            disconnectedDevicePath?.let {
                notifySerialPortOpened(File(it), SerialStatus.OPEN_FAIL)
            }
        }
        scopeToCancel?.cancel()
    }

    private fun startReconnect(request: ReconnectRequest) {
        val scope = synchronized(lifecycleLock) { ioScope }
        if (scope == null) {
            synchronized(lifecycleLock) { reconnecting = false }
            return
        }

        val job = scope.launch(start = CoroutineStart.LAZY) {
            for (attempt in 1..request.maxAttempts) {
                delay(request.intervalMillis)
                if (!isActive || userClosed) return@launch

                SerialPortLogUtil.i(
                    TAG,
                    "开始重连串口 ${request.devicePath}，第 $attempt/${request.maxAttempts} 次",
                )

                val opened = synchronized(lifecycleLock) {
                    if (userClosed || !reconnecting) {
                        return@synchronized false
                    }
                    val deviceFile = File(request.devicePath)
                    if (!checkSerialPortPermission(deviceFile, notifyFailure = false)) {
                        return@synchronized false
                    }
                    if (!openResourcesLocked(request.devicePath, request.baudRate)) {
                        return@synchronized false
                    }

                    connectionOpen = true
                    reconnecting = false
                    startIoCoroutinesLocked()
                    true
                }

                if (opened) {
                    SerialPortLogUtil.i(TAG, "串口重连成功: ${request.devicePath}")
                    notifySerialPortOpened(File(request.devicePath), SerialStatus.SUCCESS_OPENED)
                    synchronized(lifecycleLock) {
                        if (reconnectJob === coroutineContext[Job]) {
                            reconnectJob = null
                        }
                    }
                    return@launch
                }
            }

            var scopeToCancel: CoroutineScope? = null
            val shouldNotify = synchronized(lifecycleLock) {
                val isCurrentReconnect = reconnectJob === coroutineContext[Job]
                val notify = isCurrentReconnect && !userClosed && reconnecting
                if (isCurrentReconnect) {
                    reconnecting = false
                    reconnectJob = null
                    userClosed = true
                    connectedDevicePath = null
                    connectedBaudRate = 0
                    scopeToCancel = ioScope
                    ioScope = null
                }
                notify
            }
            if (shouldNotify) {
                SerialPortLogUtil.e(
                    TAG,
                    "串口重连失败，已达到最大次数 ${request.maxAttempts}: ${request.devicePath}",
                )
                reportError(
                    SerialErrorCode.RECONNECT_EXHAUSTED,
                    "串口重连失败，已达到最大次数 ${request.maxAttempts}",
                    devicePath = request.devicePath,
                )
                notifySerialPortOpened(File(request.devicePath), SerialStatus.OPEN_FAIL)
            }
            scopeToCancel?.cancel()
        }

        val shouldStart = synchronized(lifecycleLock) {
            if (userClosed || !reconnecting || ioScope !== scope) {
                false
            } else {
                reconnectJob = job
                true
            }
        }
        if (shouldStart) {
            job.start()
        } else {
            job.cancel()
        }
    }

    private fun stopIoCoroutinesLocked() {
        sendJob?.cancel()
        readJob?.cancel()
        sendJob = null
        readJob = null
        sendChannel?.close()
        sendChannel = null
    }

    private fun completeReliableResponse(response: ByteArray, generation: Long) {
        val pending = synchronized(lifecycleLock) {
            pendingReliableRequest?.takeIf { it.generation == generation }
        } ?: return

        val matched = try {
            pending.matcher?.matches(pending.data.copyOf(), response.copyOf()) ?: true
        } catch (error: Exception) {
            SerialPortLogUtil.e(TAG, "串口响应匹配器执行异常", error)
            false
        }
        if (matched) {
            pending.response.complete(response.copyOf())
        }
    }

    private fun closeResourcesLocked() {
        if (mFd != null) {
            try {
                super.close()
            } catch (error: Exception) {
                SerialPortLogUtil.e(TAG, "关闭底层串口失败", error)
            }
            mFd = null
        }

        closeStream(mFileInputStream)
        closeStream(mFileOutputStream)
        mFileInputStream = null
        mFileOutputStream = null
    }

    private fun isCurrentConnection(generation: Long): Boolean {
        return connectionOpen && !userClosed && connectionGeneration == generation
    }

    private fun selectStickPackageHelper(
        helpers: List<AbsStickPackageHelper>,
    ): AbsStickPackageHelper? {
        if (helpers.size > mSerialPortEnum.ordinal) {
            return helpers[mSerialPortEnum.ordinal]
        }
        if (helpers.isNotEmpty()) {
            return helpers[0]
        }
        SerialPortLogUtil.e(TAG, "没有可用的粘包处理器")
        return null
    }

    private fun checkSerialPortPermission(deviceFile: File, notifyFailure: Boolean): Boolean {
        if (deviceFile.canRead() && deviceFile.canWrite()) {
            return true
        }
        if (chmod777(deviceFile)) {
            return true
        }

        SerialPortLogUtil.e(TAG, "串口不存在或没有读写权限: ${deviceFile.path}")
        reportError(
            if (deviceFile.exists()) SerialErrorCode.PERMISSION_DENIED else SerialErrorCode.DEVICE_NOT_FOUND,
            "串口不存在或没有读写权限: ${deviceFile.path}",
            devicePath = deviceFile.path,
        )
        if (notifyFailure) {
            notifySerialPortOpened(deviceFile, SerialStatus.NO_READ_WRITE_PERMISSION)
        }
        return false
    }

    private fun notifySerialPortOpened(deviceFile: File, status: SerialStatus) {
        try {
            mOnOpenSerialPortListener?.openState(mSerialPortEnum, deviceFile, status)
        } catch (error: Exception) {
            SerialPortLogUtil.e(TAG, "串口状态回调异常", error)
        }
    }

    private fun notifyDataSent(bytes: ByteArray) {
        try {
            mOnSerialPortDataListener?.onDataSent(bytes, mSerialPortEnum)
        } catch (error: Exception) {
            SerialPortLogUtil.e(TAG, "串口发送回调异常", error)
        }
    }

    private fun notifyDataReceived(bytes: ByteArray) {
        try {
            mOnSerialPortDataListener?.onDataReceived(bytes, mSerialPortEnum)
        } catch (error: Exception) {
            SerialPortLogUtil.e(TAG, "串口接收回调异常", error)
        }
    }

    private fun notifyReliableSendRetry(bytes: ByteArray, retryCount: Int, maxRetries: Int) {
        SerialPortLogUtil.w(TAG, "串口响应超时，开始第 $retryCount/$maxRetries 次重发")
        try {
            mOnReliableSendListener?.onRetry(bytes.copyOf(), retryCount, maxRetries)
        } catch (error: Exception) {
            SerialPortLogUtil.e(TAG, "串口重发回调异常", error)
        }
    }

    private fun notifyReliableSendSuccess(bytes: ByteArray, response: ByteArray) {
        try {
            mOnReliableSendListener?.onSuccess(bytes.copyOf(), response.copyOf())
        } catch (error: Exception) {
            SerialPortLogUtil.e(TAG, "可靠发送成功回调异常", error)
        }
    }

    private fun notifyReliableSendFailure(bytes: ByteArray, reason: ReliableSendFailure) {
        try {
            mOnReliableSendListener?.onFailure(bytes.copyOf(), reason)
        } catch (error: Exception) {
            SerialPortLogUtil.e(TAG, "可靠发送失败回调异常", error)
        }
    }

    private fun reportError(
        code: SerialErrorCode,
        message: String,
        cause: Throwable? = null,
        devicePath: String? = connectedDevicePath,
    ) {
        val error = SerialError(code, message, cause, devicePath)
        try {
            mOnSerialErrorListener?.onError(error)
        } catch (callbackError: Exception) {
            SerialPortLogUtil.e(TAG, "串口错误业务回调异常", callbackError)
        }
    }

    private fun closeStream(stream: Closeable?) {
        if (stream == null) return
        try {
            stream.close()
        } catch (error: IOException) {
            SerialPortLogUtil.e(TAG, "关闭串口流异常", error)
        }
    }

    private data class ReconnectRequest(
        val devicePath: String,
        val baudRate: Int,
        val intervalMillis: Long,
        val maxAttempts: Int,
    )

    private data class OutboundRequest(
        val data: ByteArray,
    )

    private data class ReliableSendOptions(
        val responseTimeoutMillis: Long,
        val maxRetries: Int,
        val retryIntervalMillis: Long,
        val responseMatcher: SerialResponseMatcher?,
    )

    private data class PendingReliableRequest(
        val data: ByteArray,
        val generation: Long,
        val matcher: SerialResponseMatcher?,
        val response: CompletableDeferred<ByteArray>,
    )

    private data class ReadOptions(
        val helper: AbsStickPackageHelper,
        val packetTimeoutMillis: Long,
        val maxPacketSize: Int,
    )

    private data class ActivePacketRead(
        val options: ReadOptions,
        val inputStream: PacketGuardInputStream,
    )

    /**
     * 将设备节点消失或底层 EOF 统一转换为 IOException，交给连接生命周期处理。
     */
    private class DisconnectAwareInputStream(
        private val delegate: InputStream,
        private val devicePath: String,
    ) : InputStream() {

        override fun read(): Int {
            ensureDeviceAvailable()
            return delegate.read().also { result ->
                if (result == -1) throw EOFException("串口输入流已结束: $devicePath")
            }
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            ensureDeviceAvailable()
            return delegate.read(buffer, offset, length).also { result ->
                if (result == -1) throw EOFException("串口输入流已结束: $devicePath")
            }
        }

        override fun available(): Int {
            ensureDeviceAvailable()
            return delegate.available()
        }

        private fun ensureDeviceAvailable() {
            if (!File(devicePath).exists()) {
                throw EOFException("串口设备节点已断开: $devicePath")
            }
        }
    }

    private companion object {
        const val TAG = "SerialPortManager"
        const val SEND_QUEUE_CAPACITY = 64
        const val PROCESSING_ERROR_RETRY_DELAY_MS = 100L
        const val DEFAULT_PACKET_TIMEOUT_MS = 1_000L
        const val DEFAULT_MAX_PACKET_SIZE = 1_024
    }
}
