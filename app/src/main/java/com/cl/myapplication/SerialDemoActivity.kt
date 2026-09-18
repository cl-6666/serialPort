package com.cl.myapplication

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.text.method.ScrollingMovementMethod
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.cl.myapplication.databinding.ActivitySerialDemoBinding
import com.cl.serialportlibrary.MultiSerialPortManager
import com.cl.serialportlibrary.SerialConfig
import com.cl.serialportlibrary.SerialPortFinder
import com.cl.serialportlibrary.SimpleSerialPortManager
import com.cl.serialportlibrary.enumerate.ReliableSendFailure
import com.cl.serialportlibrary.enumerate.SerialStatus
import com.cl.serialportlibrary.listener.OnReliableSendListener
import com.cl.serialportlibrary.listener.SerialResponseMatcher
import com.cl.serialportlibrary.stick.AbsStickPackageHelper
import com.cl.serialportlibrary.stick.BaseStickPackageHelper
import com.cl.serialportlibrary.stick.SpecifiedStickPackageHelper
import com.cl.serialportlibrary.stick.StaticLenStickPackageHelper
import com.cl.serialportlibrary.stick.TimeoutStickPackageHelper
import com.cl.serialportlibrary.stick.VariableLenStickPackageHelper
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 串口 SDK 综合演示页。使用一个页面展示多串口、拆包、自动重连和可靠发送。
 */
class SerialDemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySerialDemoBinding
    private val manager: MultiSerialPortManager = SimpleSerialPortManager.createMulti()
    private val timeFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private var availableDevices: Array<String> = emptyArray()
    private var openedPortIds: List<String> = emptyList()
    private val openedDevicePaths = linkedMapOf<String, String>()
    private var logLineCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySerialDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupStaticOptions()
        setupActions()
        refreshDevices()
        updateFeatureFields()
        updateConnectionSummary()
        appendLog("SDK 控制台已就绪，请选择设备并配置串口参数")
    }

    private fun setupStaticOptions() {
        bindSpinner(
            binding.spinnerBaudrate,
            arrayOf(
                "9600", "19200", "38400", "57600", "115200", "230400",
                "460800", "500000", "576000", "921600", "1000000", "1152000",
                "1500000", "2000000", "2500000", "3000000", "3500000", "4000000",
            ),
            4,
        )
        bindSpinner(binding.spinnerDataBits, arrayOf("8", "7", "6", "5"), 0)
        bindSpinner(binding.spinnerParity, arrayOf("无校验", "奇校验", "偶校验", "SPACE", "MARK"), 0)
        bindSpinner(binding.spinnerStopBits, arrayOf("1", "2"), 0)
        bindSpinner(
            binding.spinnerPacketStrategy,
            arrayOf("原始数据", "换行符 \\n", "AT 命令 \\r\\n", "固定 8 字节", "变长协议", "空闲 50ms", "头尾标识"),
            0,
        )
        bindSpinner(binding.spinnerSendFormat, arrayOf("UTF-8 文本", "HEX 字节"), 0)
        bindSpinner(binding.spinnerResponseMatcher, arrayOf("任意非空响应", "首字节匹配", "完整内容匹配"), 0)

        binding.tvConsole.movementMethod = ScrollingMovementMethod()
    }

    private fun setupActions() {
        binding.btnRefreshDevices.setOnClickListener { refreshDevices() }
        binding.btnOpenPort.setOnClickListener { openConfiguredPort() }
        binding.btnClosePort.setOnClickListener { closeSelectedPort() }
        binding.btnCloseAll.setOnClickListener { closeAllPorts() }
        binding.btnSend.setOnClickListener { sendData() }
        binding.btnSendAll.setOnClickListener { sendDataToAllPorts() }
        binding.btnClearConsole.setOnClickListener {
            binding.tvConsole.text = ""
            logLineCount = 0
            appendLog("日志已清空")
        }
        binding.switchAutoReconnect.setOnCheckedChangeListener { _, _ -> updateFeatureFields() }
        binding.switchReliableSend.setOnCheckedChangeListener { _, _ -> updateFeatureFields() }
        binding.editSendData.doAfterTextChanged { updateSendPreview() }
        binding.spinnerSendFormat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateSendPreview()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        updateSendPreview()
    }

    private fun refreshDevices() {
        val previousDevice = binding.spinnerDevice.selectedItem?.toString()
        availableDevices = SerialPortFinder().allDevicesPath
        val displayDevices = if (availableDevices.isEmpty()) {
            arrayOf(getString(R.string.empty_serial_devices))
        } else {
            availableDevices
        }
        val selectedIndex = availableDevices.indexOf(previousDevice).takeIf { it >= 0 }
            ?: nextUnusedDeviceIndex()
            ?: 0
        bindSpinner(binding.spinnerDevice, displayDevices, selectedIndex)
        updateOpenActionState()
        appendLog("设备扫描完成，共发现 ${availableDevices.size} 个串口")
    }

    private fun openConfiguredPort() {
        val serialId = currentSerialId() ?: return
        if (availableDevices.isEmpty()) {
            appendLog("OPEN_FAIL [$serialId] 没有可用串口设备")
            return
        }
        if (manager.isSerialPortOpened(serialId)) {
            binding.editSerialId.error = getString(R.string.error_serial_id_in_use)
            appendLog("OPEN_FAIL [$serialId] 串口 ID 已被占用，请更换 ID")
            return
        }

        val devicePath = binding.spinnerDevice.selectedItem?.toString().orEmpty()
        val existingSerialId = openedDevicePaths.entries
            .firstOrNull { (_, openedDevicePath) -> openedDevicePath == devicePath }
            ?.key
        if (existingSerialId != null) {
            appendLog("OPEN_FAIL [$serialId] $devicePath 已由 $existingSerialId 打开")
            return
        }
        val baudRate = binding.spinnerBaudrate.selectedItem.toString().toInt()
        val config = try {
            buildSerialConfig()
        } catch (error: IllegalArgumentException) {
            binding.editSerialId.error = null
            appendLog("CONFIG_FAIL [$serialId] ${error.message}")
            return
        }

        appendLog("OPENING [$serialId] $devicePath @ $baudRate")
        val opened = manager.openSerialPort(
            serialId,
            devicePath,
            baudRate,
            config,
            { id: String, success: Boolean, status: SerialStatus ->
                if (!success) openedDevicePaths.remove(id)
                appendLog("STATE [$id] ${if (success) "已连接" else "连接失败"} ($status)")
                updateConnectionSummary()
            },
            object : MultiSerialPortManager.OnSerialPortDataCallback {
                override fun onDataReceived(serialId: String, data: ByteArray) {
                    appendLog("RX [$serialId] ${formatBytes(data)}")
                }

                override fun onDataSent(serialId: String, data: ByteArray) {
                    appendLog("TX [$serialId] 发送成功 ${formatBytes(data)}")
                }
            },
        )

        if (opened) {
            openedDevicePaths[serialId] = devicePath
            manager.setOnReliableSendListener(serialId, createReliableSendListener(serialId))
            prepareNextPortInput()
        }
        updateConnectionSummary()
    }

    private fun buildSerialConfig(): SerialConfig {
        val autoReconnect = binding.switchAutoReconnect.isChecked
        val reliableSend = binding.switchReliableSend.isChecked
        val reconnectInterval = if (autoReconnect) {
            positiveInt(binding.editReconnectInterval.text.toString(), "重连间隔")
        } else {
            DEFAULT_RECONNECT_INTERVAL_MS
        }
        val reconnectAttempts = if (autoReconnect) {
            nonNegativeInt(binding.editMaxReconnectAttempts.text.toString(), "重连次数")
        } else {
            DEFAULT_RECONNECT_ATTEMPTS
        }
        val responseTimeout = if (reliableSend) {
            positiveInt(binding.editResponseTimeout.text.toString(), "响应超时")
        } else {
            DEFAULT_RESPONSE_TIMEOUT_MS
        }
        val sendRetries = if (reliableSend) {
            nonNegativeInt(binding.editMaxSendRetries.text.toString(), "重发次数")
        } else {
            DEFAULT_SEND_RETRIES
        }

        return SerialConfig.Builder()
            .setDatabits(binding.spinnerDataBits.selectedItem.toString().toInt())
            .setParity(binding.spinnerParity.selectedItemPosition)
            .setStopbits(binding.spinnerStopBits.selectedItem.toString().toInt())
            .setAutoReconnect(autoReconnect)
            .setReconnectInterval(reconnectInterval)
            .setMaxReconnectAttempts(reconnectAttempts)
            .setEnableReliableSend(reliableSend)
            .setResponseTimeoutMillis(responseTimeout)
            .setMaxSendRetries(sendRetries)
            .setSendRetryIntervalMillis(DEFAULT_SEND_RETRY_INTERVAL_MS)
            .setResponseMatcher(if (reliableSend) createResponseMatcher() else null)
            .setStickyPacketHelpers(createPacketHelper())
            .build()
    }

    private fun createPacketHelper(): AbsStickPackageHelper {
        return when (binding.spinnerPacketStrategy.selectedItemPosition) {
            1 -> SpecifiedStickPackageHelper("\n")
            2 -> SpecifiedStickPackageHelper("\r\n")
            3 -> StaticLenStickPackageHelper(8)
            4 -> VariableLenStickPackageHelper(ByteOrder.BIG_ENDIAN, 2, 2, 12)
            5 -> TimeoutStickPackageHelper(50)
            6 -> SpecifiedStickPackageHelper("\$\$START\$\$", "\$\$END\$\$")
            else -> BaseStickPackageHelper()
        }
    }

    private fun createResponseMatcher(): SerialResponseMatcher? {
        return when (binding.spinnerResponseMatcher.selectedItemPosition) {
            1 -> SerialResponseMatcher { request, response ->
                request.isNotEmpty() && response.isNotEmpty() && request[0] == response[0]
            }
            2 -> SerialResponseMatcher { request, response -> request.contentEquals(response) }
            else -> null
        }
    }

    private fun createReliableSendListener(serialId: String): OnReliableSendListener {
        return object : OnReliableSendListener {
            override fun onRetry(data: ByteArray, retryCount: Int, maxRetries: Int) {
                appendLog("RETRY [$serialId] 第 $retryCount/$maxRetries 次 ${formatBytes(data)}")
            }

            override fun onSuccess(data: ByteArray, response: ByteArray) {
                appendLog("RELIABLE_OK [$serialId] response=${formatBytes(response)}")
            }

            override fun onFailure(data: ByteArray, reason: ReliableSendFailure) {
                appendLog("RELIABLE_FAIL [$serialId] reason=$reason")
            }
        }
    }

    private fun closeSelectedPort() {
        val serialId = selectedOpenedPortId()
        if (serialId == null) {
            appendLog("CLOSE_FAIL 当前没有已打开串口")
            return
        }
        manager.closeSerialPort(serialId)
        openedDevicePaths.remove(serialId)
        appendLog("CLOSED [$serialId]")
        selectNextUnusedDevice()
        updateConnectionSummary()
    }

    private fun closeAllPorts() {
        manager.closeAllSerialPorts()
        openedDevicePaths.clear()
        binding.editSerialId.setText(R.string.default_serial_id)
        appendLog("所有串口连接已关闭")
        selectNextUnusedDevice()
        updateConnectionSummary()
    }

    private fun sendData() {
        val serialId = selectedTargetPortId()
        if (serialId == null) {
            appendLog("TX_FAIL 当前没有可发送的已打开串口")
            return
        }

        val data = readSendData() ?: return
        sendDataToPort(serialId, data)
    }

    private fun sendDataToAllPorts() {
        val targetPorts = manager.openedSerialPorts.sorted()
        if (targetPorts.isEmpty()) {
            appendLog("TX_ALL_FAIL 当前没有已打开串口")
            updateConnectionSummary()
            return
        }

        val data = readSendData() ?: return
        val acceptedCount = targetPorts.count { serialId -> sendDataToPort(serialId, data) }
        appendLog("TX_ALL 已提交 $acceptedCount/${targetPorts.size} 个串口")
    }

    private fun readSendData(): ByteArray? {
        return try {
            parseSendData(binding.editSendData.text.toString()).also {
                binding.editSendData.error = null
            }
        } catch (error: IllegalArgumentException) {
            binding.editSendData.error = error.message
            null
        }
    }

    private fun sendDataToPort(serialId: String, data: ByteArray): Boolean {
        val accepted = manager.sendData(serialId, data)
        if (!accepted) appendLog("TX_FAIL [$serialId] 连接不可用或发送队列已满")
        return accepted
    }

    private fun parseSendData(value: String): ByteArray {
        if (value.isBlank()) throw IllegalArgumentException("请输入发送数据")
        if (binding.spinnerSendFormat.selectedItemPosition == 0) {
            return value.toByteArray(StandardCharsets.UTF_8)
        }

        val normalized = value.replace(Regex("[^0-9A-Fa-f]"), "")
        if (normalized.isEmpty() || normalized.length % 2 != 0) {
            throw IllegalArgumentException("HEX 数据必须由完整字节组成，例如 01 03 00 00")
        }
        return ByteArray(normalized.length / 2) { index ->
            normalized.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }

    private fun updateSendPreview() {
        val value = binding.editSendData.text.toString()
        binding.tvSendPreview.text = if (value.isBlank()) {
            getString(R.string.send_preview_empty)
        } else {
            try {
                val data = parseSendData(value)
                getString(
                    R.string.send_preview_content,
                    data.size,
                    formatText(data),
                    formatHex(data),
                )
            } catch (error: IllegalArgumentException) {
                getString(R.string.send_preview_error, error.message.orEmpty())
            }
        }
    }

    private fun currentSerialId(): String? {
        val serialId = binding.editSerialId.text.toString().trim()
        if (serialId.isEmpty()) {
            binding.editSerialId.error = "请输入串口 ID"
            return null
        }
        binding.editSerialId.error = null
        return serialId
    }

    private fun updateFeatureFields() {
        val reconnectEnabled = binding.switchAutoReconnect.isChecked
        binding.editReconnectInterval.isEnabled = reconnectEnabled
        binding.editMaxReconnectAttempts.isEnabled = reconnectEnabled

        val reliableEnabled = binding.switchReliableSend.isChecked
        binding.editResponseTimeout.isEnabled = reliableEnabled
        binding.editMaxSendRetries.isEnabled = reliableEnabled
        binding.spinnerResponseMatcher.isEnabled = reliableEnabled
    }

    private fun updateConnectionSummary() {
        runOnUiThread {
            val openedPorts = manager.openedSerialPorts.sorted()
            openedDevicePaths.keys.retainAll(openedPorts.toSet())
            val portItems = openedPorts.map(::portDisplayLabel)
            binding.tvConnectionSummary.text = if (openedPorts.isEmpty()) {
                getString(R.string.no_open_serial_ports)
            } else {
                getString(R.string.open_serial_ports, portItems.joinToString())
            }

            val previousTarget = selectedTargetPortId()
            val previousOpenedPort = selectedOpenedPortId()
            openedPortIds = openedPorts
            val selectorItems = portItems.ifEmpty {
                listOf(getString(R.string.no_available_send_target))
            }
            bindSpinner(binding.spinnerTargetPort, selectorItems.toTypedArray(), 0)
            bindSpinner(binding.spinnerOpenedPort, selectorItems.toTypedArray(), 0)
            restorePortSelection(binding.spinnerTargetPort, previousTarget)
            restorePortSelection(binding.spinnerOpenedPort, previousOpenedPort)

            val hasOpenedPort = openedPorts.isNotEmpty()
            binding.spinnerTargetPort.isEnabled = hasOpenedPort
            binding.spinnerOpenedPort.isEnabled = hasOpenedPort
            binding.btnClosePort.isEnabled = hasOpenedPort
            binding.btnSend.isEnabled = hasOpenedPort
            binding.btnSendAll.isEnabled = hasOpenedPort
            updateOpenActionState()
        }
    }

    private fun selectedTargetPortId(): String? {
        return openedPortIds.getOrNull(binding.spinnerTargetPort.selectedItemPosition)
    }

    private fun selectedOpenedPortId(): String? {
        return openedPortIds.getOrNull(binding.spinnerOpenedPort.selectedItemPosition)
    }

    private fun restorePortSelection(spinner: android.widget.Spinner, serialId: String?) {
        val index = openedPortIds.indexOf(serialId)
        if (index >= 0) spinner.setSelection(index)
    }

    private fun portDisplayLabel(serialId: String): String {
        val devicePath = openedDevicePaths[serialId] ?: getString(R.string.unknown_serial_device)
        return "$serialId  ·  $devicePath"
    }

    private fun prepareNextPortInput() {
        binding.editSerialId.setText(nextAvailableSerialId())
        selectNextUnusedDevice()
    }

    private fun nextAvailableSerialId(): String {
        val openedIds = manager.openedSerialPorts.toSet()
        return generateSequence(1) { it + 1 }
            .map { index -> "PORT_$index" }
            .first { serialId -> serialId !in openedIds }
    }

    private fun selectNextUnusedDevice() {
        nextUnusedDeviceIndex()?.let { index -> binding.spinnerDevice.setSelection(index) }
        updateOpenActionState()
    }

    private fun nextUnusedDeviceIndex(): Int? {
        return availableDevices.indexOfFirst { devicePath ->
            devicePath !in openedDevicePaths.values
        }.takeIf { it >= 0 }
    }

    private fun updateOpenActionState() {
        binding.btnOpenPort.isEnabled = nextUnusedDeviceIndex() != null
    }

    private fun appendLog(message: String) {
        runOnUiThread {
            if (logLineCount >= MAX_LOG_LINES) {
                binding.tvConsole.text = ""
                logLineCount = 0
            }
            val timestamp = timeFormatter.format(Date())
            binding.tvConsole.append("$timestamp  $message\n")
            logLineCount++
            binding.scrollConsole.post { binding.scrollConsole.fullScroll(android.view.View.FOCUS_DOWN) }
        }
    }

    private fun formatBytes(data: ByteArray): String {
        return "len=${data.size} hex=[${formatHex(data)}] text=[${formatText(data)}]"
    }

    private fun formatHex(data: ByteArray): String {
        return data.joinToString(" ") { byte ->
            "%02X".format(Locale.ROOT, byte.toInt() and 0xFF)
        }
    }

    private fun formatText(data: ByteArray): String {
        return data.toString(StandardCharsets.UTF_8)
            .replace("\r", "\\r")
            .replace("\n", "\\n")
            .map { character -> if (character.isISOControl()) '.' else character }
            .joinToString("")
    }

    private fun positiveInt(value: String, label: String): Int {
        val parsed = value.toIntOrNull() ?: throw IllegalArgumentException("$label 必须是整数")
        if (parsed <= 0) throw IllegalArgumentException("$label 必须大于 0")
        return parsed
    }

    private fun nonNegativeInt(value: String, label: String): Int {
        val parsed = value.toIntOrNull() ?: throw IllegalArgumentException("$label 必须是整数")
        if (parsed < 0) throw IllegalArgumentException("$label 不能小于 0")
        return parsed
    }

    private fun bindSpinner(
        spinner: android.widget.Spinner,
        values: Array<String>,
        selection: Int,
    ) {
        spinner.adapter = ArrayAdapter(this, R.layout.spinner_default_item, values).also {
            it.setDropDownViewResource(R.layout.spinner_item)
        }
        if (values.isNotEmpty()) spinner.setSelection(selection.coerceIn(values.indices))
    }

    override fun onDestroy() {
        manager.closeAllSerialPorts()
        super.onDestroy()
    }

    private companion object {
        const val DEFAULT_RECONNECT_INTERVAL_MS = 3000
        const val DEFAULT_RECONNECT_ATTEMPTS = 5
        const val DEFAULT_RESPONSE_TIMEOUT_MS = 1000
        const val DEFAULT_SEND_RETRIES = 2
        const val DEFAULT_SEND_RETRY_INTERVAL_MS = 200
        const val MAX_LOG_LINES = 500
    }
}
