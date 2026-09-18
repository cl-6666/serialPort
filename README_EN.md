# Android Serial Communication Framework SerialPort

[中文](README.md) | [English](README_EN.md)

[![Version](https://img.shields.io/badge/version-5.0.8-blue.svg)](https://github.com/cl-6666/serialPort)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)](https://developer.android.com/tools/releases/platforms)
[![License](https://img.shields.io/badge/license-Apache%202-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

A lightweight serial-port SDK for Android devices. It supports single and multiple ports, packet framing, reconnect, response-timeout retries, structured errors, and replaceable logging.

<p align="center">
  <img src="img/introduce1.png" width="100%" alt="Serial SDK feature console" />
</p>

## Features

- Written in Kotlin and callable from Java.
- Single- and multi-port managers with independent configuration and send queues.
- 5–8 data bits, odd/even/SPACE/MARK parity, and 1/2 stop bits.
- Raw, delimiter, fixed-length, variable-length, idle-timeout, and custom framing.
- Reconnect after an unexpected disconnect, with configurable interval and attempt limit.
- Response-timeout retries with a custom response matcher.
- Coroutine-based IO with ordered writes per connection.
- Structured errors, replaceable logging, and an option to disable detailed logs.
- `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64` ABIs.
- Android 5.0 (API 21) and later; 64-bit native libraries are linked for 16 KB page sizes.

## Installation

Add JitPack to the root project repositories:

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

Add the dependency to the application module:

```groovy
dependencies {
    implementation 'com.github.cl-6666:serialPort:v5.1.0'
}
```

When copying the AAR directly, the consuming app must also provide the Kotlin standard library and Coroutines Core:

```groovy
implementation files('libs/serial_lib-release.aar')
implementation 'org.jetbrains.kotlin:kotlin-stdlib:2.0.21'
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1'
```

The SDK does not require an `Application`, `Context`, or manifest permission. The app process must have read/write access to the target `/dev/tty*` node. A normal app commonly needs a system signature, vendor authorization, SELinux configuration, or a rooted environment.

## Quick Start: Single Port

Use `create()` for a manager owned by a screen or business component. Always call `init(config)` before opening a port:

```kotlin
private val serialManager = SimpleSerialPortManager.create()

private fun openSerialPort() {
    val config = SerialConfig.Builder()
        .setDatabits(8)
        .setParity(0)
        .setStopbits(1)
        .setEnableLogging(BuildConfig.DEBUG)
        .build()

    serialManager
        .init(config)
        .setOnSerialErrorListener { error ->
            Log.e("Serial", "${error.code}: ${error.message}", error.cause)
        }

    val opened = serialManager.openSerialPort(
        devicePath = "/dev/ttyS4",
        baudRate = 115200,
        openCallback = { success, status ->
            Log.i("Serial", "opened=$success, status=$status")
        },
        dataCallback = object : SimpleSerialPortManager.OnDataReceivedCallback {
            override fun onDataReceived(data: ByteArray) {
                Log.d("Serial", "RX=${data.toHexString()}")
            }

            override fun onDataSent(data: ByteArray) {
                Log.d("Serial", "TX=${data.toHexString()}")
            }
        },
    )

    if (!opened) Log.e("Serial", "Open request failed")
}

private fun sendCommand() {
    val accepted = serialManager.sendData(byteArrayOf(0x01, 0x03, 0x00, 0x00))
    if (!accepted) Log.e("Serial", "Connection unavailable or send queue full")
}

override fun onDestroy() {
    serialManager.close()
    super.onDestroy()
}

private fun ByteArray.toHexString(): String =
    joinToString(" ") { byte -> "%02X".format(byte.toInt() and 0xFF) }
```

`openSerialPort()` is synchronous. Permission probing can be slow, so production apps should call it from a worker thread or `Dispatchers.IO`. Status, data, reliable-send, and error callbacks from the facade managers are dispatched to the main thread.

## Multiple Ports

Use `createMulti()` to create an independently owned multi-port manager. Give each connection a unique ID and its own `SerialConfig`:

```kotlin
private val serialPorts = SimpleSerialPortManager.createMulti()

private fun openPort(serialId: String, path: String, baudRate: Int) {
    val config = SerialConfig.Builder()
        .setDatabits(8)
        .setParity(0)
        .setStopbits(1)
        .setStickyPacketHelpers(SpecifiedStickPackageHelper("\r\n"))
        .build()

    val opened = serialPorts.openSerialPort(
        serialId,
        path,
        baudRate,
        config,
        { id, success, status ->
            Log.i("Serial", "[$id] opened=$success, status=$status")
        },
        object : MultiSerialPortManager.OnSerialPortDataCallback {
            override fun onDataReceived(serialId: String, data: ByteArray) {
                Log.d("Serial", "RX [$serialId] ${data.size} bytes")
            }

            override fun onDataSent(serialId: String, data: ByteArray) {
                Log.d("Serial", "TX [$serialId] ${data.size} bytes")
            }
        },
    )

    if (!opened) Log.e("Serial", "[$serialId] open failed")
}

private fun usePorts() {
    openPort("GPS", "/dev/ttyS1", 9600)
    openPort("SENSOR", "/dev/ttyS2", 115200)

    serialPorts.sendData("GPS", "AT+GPS?\r\n")
    serialPorts.sendData("SENSOR", byteArrayOf(0x01, 0x03))

    val openedIds = serialPorts.openedSerialPorts
    val gpsOpened = serialPorts.isSerialPortOpened("GPS")

    serialPorts.closeSerialPort("GPS")
    serialPorts.closeAllSerialPorts()
}
```

Opening an existing ID closes that ID's previous connection first. Keep both IDs and device paths unique in the application layer to avoid accidental replacement or opening one device twice.

## Serial Configuration

Use the same `SerialConfig` for single- and multi-port connections:

```kotlin
val config = SerialConfig.Builder()
    .setDatabits(8)                 // 5, 6, 7, 8
    .setParity(0)                   // 0 none, 1 odd, 2 even, 3 SPACE, 4 MARK
    .setStopbits(1)                 // 1, 2
    .setFlags(0)
    .setIntervalSleep(50)           // Raw-mode polling delay when no data is available
    .setMaxPacketSize(1_024)        // Maximum bytes in one packet
    .setPacketTimeout(1_000)        // Discard an incomplete packet after 1 second of idle time
    .setAutoReconnect(true)
    .setReconnectInterval(3_000)
    .setMaxReconnectAttempts(5)
    .build()
```

`SerialConfig` validates data bits, stop bits, parity, and timeout values when built. Invalid values throw `IllegalArgumentException`.

`packetTimeout` starts after the first byte and measures inter-byte idle time, so an idle serial port does not trigger it. Timeout and oversized-packet errors are reported through `OnSerialErrorListener` as `PACKET_TIMEOUT` and `PACKET_TOO_LARGE`; the receive loop then continues.

## Packet Framing

| Helper | Use case | Example |
|---|---|---|
| `BaseStickPackageHelper` | Raw byte stream | `BaseStickPackageHelper(50)` |
| `SpecifiedStickPackageHelper` | Newline, AT, or head/tail markers | `SpecifiedStickPackageHelper("\r\n")` |
| `StaticLenStickPackageHelper` | Fixed-length protocol | `StaticLenStickPackageHelper(8)` |
| `VariableLenStickPackageHelper` | Length field embedded in the packet | `VariableLenStickPackageHelper(ByteOrder.BIG_ENDIAN, 2, 2, 12)` |
| `TimeoutStickPackageHelper` | An idle period terminates a packet | `TimeoutStickPackageHelper(50)` |
| `CompositeStickPackageHelper` | Primary and fallback strategies | `CompositeStickPackageHelper(primary, fallback)` |

```kotlin
val config = SerialConfig.Builder()
    .setEnableStickyPacketProcessing(true)
    .setStickyPacketHelpers(SpecifiedStickPackageHelper("\n"))
    .build()
```

Implement `AbsStickPackageHelper` for a custom protocol:

```kotlin
val customHelper = AbsStickPackageHelper { inputStream ->
    // Block until one complete frame is available, then return a ByteArray.
    null
}
```

## Automatic Reconnect

```kotlin
val config = SerialConfig.Builder()
    .setAutoReconnect(true)
    .setReconnectInterval(3_000)
    .setMaxReconnectAttempts(5)
    .build()
```

- Only an IO failure or a missing device node triggers reconnect.
- Calling `closeSerialPort()` or `close()` does not reconnect.
- A successful reconnect reports `SUCCESS_OPENED` again.
- Exhausting all attempts reports `OPEN_FAIL` and `RECONNECT_EXHAUSTED`.

## Response Timeout and Retry

```kotlin
val config = SerialConfig.Builder()
    .setEnableReliableSend(true)
    .setResponseTimeoutMillis(1_000)
    .setMaxSendRetries(2)
    .setSendRetryIntervalMillis(200)
    .setResponseMatcher { request, response ->
        request.isNotEmpty() && response.isNotEmpty() && request[0] == response[0]
    }
    .build()
```

```kotlin
serialManager.setOnReliableSendListener(object : OnReliableSendListener {
    override fun onRetry(data: ByteArray, retryCount: Int, maxRetries: Int) {
        Log.w("Serial", "retry=$retryCount/$maxRetries")
    }

    override fun onSuccess(data: ByteArray, response: ByteArray) {
        Log.i("Serial", "Matched response received")
    }

    override fun onFailure(data: ByteArray, reason: ReliableSendFailure) {
        Log.e("Serial", "Reliable send failed: $reason")
    }
})
```

For multiple ports, call `setOnReliableSendListener(serialId, listener)` after the corresponding connection opens successfully.

Without a `responseMatcher`, any non-empty packet is treated as the current request's response. Protocols with unsolicited or concurrent messages must match an address, command, or sequence number.

A `true` result from `sendData()` only means the data entered the send queue. `onDataSent` means one physical write completed. Reliable-send `onSuccess` means a matching response was received.

## Errors and Logging

The structured `SerialError` contains `code`, `message`, `cause`, `devicePath`, and the multi-port `serialId`:

```kotlin
serialPorts.setOnSerialErrorListener { error ->
    Log.e(
        "Serial",
        "id=${error.serialId}, device=${error.devicePath}, code=${error.code}, ${error.message}",
        error.cause,
    )
}
```

Disable detailed logs in production or bridge the SDK to the application's logger:

```kotlin
SerialPortLogUtil.setDebugEnabled(false)

SerialPortLogUtil.setLogger { level, tag, message, throwable ->
    // Forward to the app logger. Avoid recording sensitive business data.
}
```

## Lifecycle and Threads

- Screen- or component-scoped connection: use `create()` / `createMulti()` and call `close()` from `onDestroy()`, `onCleared()`, or the owner's release method.
- Long-lived cross-screen connection: let a Repository, Service, or foreground Service own and release the manager.
- Initialization in `Application` is not required. Legacy `init(application)` and `QuickConfig.apply(application)` overloads remain only for compatibility and are deprecated.
- `getInstance()` / `multi()` return global singletons. Use them only for deliberately global connections, and prevent screens from closing each other's instance.
- `sendData()` queues without blocking; each connection has a queue capacity of 64.
- Business callbacks from `SimpleSerialPortManager` and `MultiSerialPortManager` are dispatched to the main thread.

## Device Discovery and Permission Troubleshooting

```kotlin
val paths: Array<String> = SerialPortFinder().allDevicesPath
paths.forEach { path -> Log.d("Serial", path) }
```

When opening fails, check the following in order:

1. The `/dev/tty*` node exists.
2. The app process has read/write access.
3. SELinux allows access.
4. The baud rate is supported by the SDK.
5. No other process or connection owns the device.

When permission is missing, the SDK attempts `chmod 666` through `su`. This can take time. Production devices should preferably grant stable access through system permissions, vendor configuration, or SELinux policy.

## Java Usage

The public builders, callbacks, and managers are Java-compatible. Java callers must also invoke `init(config)` first and do not need to pass an `Application`. See:

[JavaApiCompatibilityTest.java](serial_lib/src/test/java/com/cl/serialportlibrary/JavaApiCompatibilityTest.java)

## Build and Verification

```bash
./gradlew :serial_lib:testDebugUnitTest
./gradlew :serial_lib:lintDebug
./gradlew :serial_lib:assembleRelease
```

Release AAR output:

```text
serial_lib/build/outputs/aar/serial_lib-release.aar
```

Use Android Studio APK Analyzer to inspect 16 KB page alignment for `lib/arm64-v8a/libSerialPort.so` and `lib/x86_64/libSerialPort.so`.

## Demo Application

The repository's `app` module demonstrates:

- A landscape phone/tablet layout.
- Device discovery, adding one connection, and managing multiple ports.
- Data bits, parity, stop bits, and packet-framing strategies.
- Automatic reconnect and response-timeout retry switches.
- Per-port send, send-to-all, UTF-8/HEX input, and conversion preview.
- Connection, receive, send, and error logs.

```bash
./gradlew :app:assembleDebug
```

## Contact

- QQ group: 458173716
- [Blog](https://blog.csdn.net/a214024475/article/details/113735085)
- [GitHub](https://github.com/cl-6666/serialPort)

### PC Serial Debugging Tool

<img src="img/pc_ck.jpg" width="440" height="320" alt="PC serial debugging tool" />

[Download](https://pan.baidu.com/s/1DL2TOHz9bl9RIKIG3oCSWw?pwd=f7sh)

### QQ Technical Group

<img src="img/qq2.jpg" width="350" height="560" alt="QQ technical group" />

Group ID: 458173716

---

Apache License 2.0
