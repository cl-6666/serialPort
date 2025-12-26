# Android Serial Communication Framework SerialPort

[English](README_EN.md) | [中文](README.md)

[![Version](https://img.shields.io/badge/version-5.0.8-blue.svg)](https://github.com/cl-6666/serialPort)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![License](https://img.shields.io/badge/license-Apache%202-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

> A flexible, efficient, and lightweight Android serial communication framework that makes serial port operations simple. Supports single-port, multi-port, sticky-packet handling, custom configuration, and more.

<img src="https://github.com/cl-6666/serialPort/blob/master/img/multiple_images.png" width="650" height="360" alt="Demo"/>  

## 📱 Demo APK

Want to try it quickly? Download the demo APK and install it on your Android device.

<div align="center">

### 📥 [Download Demo APK](https://www.pgyer.com/XNzY)

[![Download APK](https://img.shields.io/badge/Download-APK%20v5.0.8-brightgreen.svg?style=for-the-badge&logo=android)](https://www.pgyer.com/XNzY)

**Version**: v5.0.8 | **Size**: ~7 MB | **API**: 21+ | **ABIs**: arm64-v8a, armeabi-v7a, x86, x86_64

</div>

### Demo Features

- ✅ Single serial port demo
- ✅ Multi-serial management demo
- ✅ Sticky-packet strategy switching
- ✅ Serial params configuration (data bits, parity, stop bits)
- ✅ Real-time send/receive test
- ✅ Hex/ASCII display
- ✅ Performance test & statistics

> Note: the demo APK must run on an Android device with serial ports (industrial devices, development boards, etc.). If your device has no serial port, check the source code for usage.

## ⭐ Features

- 🚀 **Easy to use** - fluent APIs, configure with one line
- 🔧 **Multi-serial support** - manage multiple ports with independent configs
- 📦 **Smart sticky-packet handling** - multiple strategies, switch at runtime
- ⚡ **High performance** - multithreaded, thread-safe design
- 🛡️ **Stable & reliable** - solid error handling and resource management
- 📝 **Detailed logs** - rich debug information for troubleshooting
- 🎯 **Flexible config** - data bits, parity, stop bits, etc.
- ✨ **Google Play ready** - supports 16 KB page alignment and passes Play requirements

## 📖 Versions

- **Current**: 5.0.8 (recommended) - new architecture, powerful features, supports Google Play 16 KB page alignment
- **Legacy**: [4.1.1 docs](README4.1.1.md) - stable legacy version

### 5.0.8 Changes 🔥 (2025-12-25)

- ✅ **16 KB page alignment**: fully compatible with Google Play 16 KB page size requirements
- ✅ **Android 15 support**: compatible with Android 15
- ✅ **Native library optimization**: `arm64-v8a` native lib meets Google Play checks
- ✅ **Backward compatible**: works on older Android devices without code changes

> Important: since 2024, Google Play requires all `arm64-v8a` native libraries to support 16 KB page size. v5.0.8 fully meets this requirement.

### 5.0.0 Major Update 🎉

- ✅ **Architecture refactor**: removed `SerialUtils` dependency, clearer design
- ✅ **Simplified API**: introduced `SimpleSerialPortManager`, easier usage
- ✅ **Multi-serial management**: new `MultiSerialPortManager` for complex scenarios
- ✅ **Enhanced logging**: built-in logging system for better debugging
- ✅ **Independent config**: each port can use its own sticky-packet strategy
- ✅ **Performance improvements**: reduced ~30% redundant code

## 🚀 Quick Start

### Dependency

Add dependency in your module `build.gradle`:

```gradle
dependencies {
   implementation 'com.github.cl-6666:serialPort:v5.0.8'
}
```

Add JitPack in the root `build.gradle`:

```gradle
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

### Permissions

Add required permissions in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

## 📚 Usage Guide

### 1️⃣ Single Port - Basic Example

#### Minimal usage

```java
public class MainActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Open serial port and receive data with one line
        SimpleSerialPortManager.getInstance()
            .openSerialPort("/dev/ttyS4", 115200, data -> {
                String receivedData = new String(data);
                Log.i("Serial", "Received: " + receivedData);
                // Handle received data
            });
    }
    
    // Send data
    private void sendData() {
        SimpleSerialPortManager.getInstance().sendData("Hello World");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close serial port
        SimpleSerialPortManager.getInstance().closeSerialPort();
    }
}
```

#### Full configuration example

```java
public class App extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Global config (optional)
        new SimpleSerialPortManager.QuickConfig()
            .setIntervalSleep(50)                    // Read interval: 50ms
            .setEnableLog(true)                      // Enable logs
            .setLogTag("SerialPortApp")              // Log tag
            .setDatabits(8)                          // Data bits: 8
            .setParity(0)                            // Parity: none
            .setStopbits(1)                          // Stop bits: 1
            .setStickyPacketStrategy(SimpleSerialPortManager.StickyPacketStrategy.NO_PROCESSING)
            .apply(this);
    }
}
```

### 2️⃣ Data Bits / Parity / Stop Bits

```java
public class SerialConfigExample {
    
    public void configureSerialParams() {
        SimpleSerialPortManager manager = SimpleSerialPortManager.getInstance();
        
        // Option 1: use QuickConfig
        new SimpleSerialPortManager.QuickConfig()
            .setDatabits(8)        // Data bits: 5, 6, 7, 8
            .setParity(0)          // Parity: 0=none, 1=odd, 2=even
            .setStopbits(1)        // Stop bits: 1 or 2
            .setFlags(0)           // Flags
            .apply(getApplication());
        
        // Option 2: set dynamically
        manager.setDatabits(8)     // Set data bits
               .setParity(2)       // Set even parity
               .setStopbits(1)     // Set stop bits to 1
               .setFlags(0);       // Set flags
        
        // Open serial port
        manager.openSerialPort("/dev/ttyS4", 115200, data -> {
            Log.i("Serial", "Data: " + new String(data));
        });
    }
    
    // Common configurations
    public void commonConfigurations() {
        SimpleSerialPortManager manager = SimpleSerialPortManager.getInstance();
        
        // Standard 8N1 (8 data bits, no parity, 1 stop bit)
        manager.setDatabits(8).setParity(0).setStopbits(1);
        
        // Modbus RTU 8E1 (8 data bits, even parity, 1 stop bit) 
        manager.setDatabits(8).setParity(2).setStopbits(1);
        
        // Legacy devices 7E2 (7 data bits, even parity, 2 stop bits)
        manager.setDatabits(7).setParity(2).setStopbits(2);
    }
}
```

### 3️⃣ Sticky-Packet Handling

Sticky packets are common in serial communication. Since v5.0.0, multiple strategies are provided:

```java
public class StickyPacketExample {
    
    public void noProcessing() {
        // Strategy 1: no processing - good for simple streams
        new SimpleSerialPortManager.QuickConfig()
            .setStickyPacketStrategy(SimpleSerialPortManager.StickyPacketStrategy.NO_PROCESSING)
            .apply(this);
    }
    
    public void delimiterBased() {
        // Strategy 2: delimiter-based - good for text protocols
        new SimpleSerialPortManager.QuickConfig()
            .setStickyPacketStrategy(SimpleSerialPortManager.StickyPacketStrategy.DELIMITER_BASED)
            .apply(this);
        
        // Custom delimiter
        SimpleSerialPortManager.getInstance()
            .configureStickyPacket(SimpleSerialPortManager.StickyPacketStrategy.DELIMITER_BASED);
    }
    
    public void fixedLength() {
        // Strategy 3: fixed-length - good for fixed-length protocols
        new SimpleSerialPortManager.QuickConfig()
            .setStickyPacketStrategy(SimpleSerialPortManager.StickyPacketStrategy.FIXED_LENGTH)
            .apply(this);
    }
    
    public void variableLength() {
        // Strategy 4: variable-length - good for protocols with length fields
        new SimpleSerialPortManager.QuickConfig()
            .setStickyPacketStrategy(SimpleSerialPortManager.StickyPacketStrategy.VARIABLE_LENGTH)
            .apply(this);
    }
}
```

### 4️⃣ Multi-Serial Management

```java
public class MultiSerialExample {
    
    public void basicMultiSerial() {
        MultiSerialPortManager manager = SimpleSerialPortManager.multi();
        
        // Port 1: GPS module, no sticky-packet processing
        manager.openSerialPort("GPS", "/dev/ttyS1", 9600,
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setDatabits(8)
                .setParity(0)
                .setStopbits(1)
                .setStickyPacketHelpers(new BaseStickPackageHelper()) // No processing
                .build(),
            // Status callback
            (serialId, success, status) -> {
                Log.i("GPS", "Status: " + (success ? "Success" : "Failed"));
            },
            // Data callback
            (serialId, data) -> {
                String gpsData = new String(data);
                Log.i("GPS", "Data: " + gpsData);
                handleGpsData(gpsData);
            });
        
        // Port 2: sensor module, split by newline
        manager.openSerialPort("SENSOR", "/dev/ttyS2", 115200,
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setDatabits(8)
                .setParity(0) 
                .setStopbits(1)
                .setStickyPacketHelpers(new SpecifiedStickPackageHelper("\n")) // Newline delimiter
                .build(),
            null, // No status callback needed
            (serialId, data) -> {
                String sensorData = new String(data).trim();
                Log.i("SENSOR", "Data: " + sensorData);
                handleSensorData(sensorData);
            });
        
        // Send to different ports
        manager.sendData("GPS", "AT+GPS?\r\n");
        manager.sendData("SENSOR", "READ_TEMP\n");
    }
    
    // Dynamic management
    public void dynamicManagement() {
        MultiSerialPortManager manager = SimpleSerialPortManager.multi();
        
        // Query status
        List<String> openedPorts = manager.getOpenedSerialPorts();
        boolean isOpened = manager.isSerialPortOpened("GPS");
        manager.printAllSerialStatus();
        
        // Update sticky-packet strategy dynamically
        manager.updateStickyPacketHelpers("GPS", 
            new AbsStickPackageHelper[]{new SpecifiedStickPackageHelper("\r\n")});
        
        // Close one port
        manager.closeSerialPort("GPS");
        
        // Close all ports
        manager.closeAllSerialPorts();
    }
}
```

## 🎯 Real-world Scenarios

### Industrial control
```java
public class IndustrialControlExample {
    
    public void setupIndustrialPorts() {
        MultiSerialPortManager manager = SimpleSerialPortManager.multi();
        
        // PLC - Modbus RTU
        manager.openSerialPort("PLC", "/dev/ttyS1", 9600,
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setDatabits(8).setParity(2).setStopbits(1) // 8E1
                .setStickyPacketHelpers(new StaticLenStickPackageHelper(8))
                .build(),
            null, this::handlePlcData);
        
        // Sensor acquisition - text protocol
        manager.openSerialPort("SENSORS", "/dev/ttyS3", 9600,
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setDatabits(7).setParity(2).setStopbits(1) // 7E1
                .setStickyPacketHelpers(new SpecifiedStickPackageHelper("\r\n"))
                .build(),
            null, this::handleSensorData);
    }
}
```

### Communication gateway
```java
public class GatewayExample {
    
    public void setupGateway() {
        MultiSerialPortManager manager = SimpleSerialPortManager.multi();
        
        // Uplink (to server)
        manager.openSerialPort("UPLINK", "/dev/ttyS1", 115200,
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setStickyPacketHelpers(new SpecifiedStickPackageHelper("\n"))
                .build(),
            null, this::handleUplinkData);
        
        // Downlink device 1 - GPS
        manager.openSerialPort("GPS", "/dev/ttyS2", 9600,
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setStickyPacketHelpers(new SpecifiedStickPackageHelper("\r\n"))
                .build(),
            null, data -> forwardToUplink("GPS", data));
    }
    
    private void forwardToUplink(String deviceId, byte[] data) {
        String message = String.format("[%s]%s\n", deviceId, new String(data));
        SimpleSerialPortManager.multi().sendData("UPLINK", message);
    }
}
```

## 🔧 Advanced

### Logging
```java
// Enable verbose logs
SerialPortLogUtil.setDebugEnabled(true);

// Custom output
SerialPortLogUtil.i("MyTag", "Custom log message");
SerialPortLogUtil.printData("Send", data); // Hex + ASCII
SerialPortLogUtil.printSerialConfig("MySerial", 8, 0, 1, 0); // Config info
```

### Error handling
```java
manager.openSerialPort("TEST", "/dev/ttyS1", 9600,
    (serialId, success, status) -> {
        if (!success) {
            switch (status) {
                case NO_READ_WRITE_PERMISSION:
                    Log.e("Serial", "No permission");
                    break;
                case OPEN_FAIL:
                    Log.e("Serial", "Open failed");
                    break;
            }
        }
    },
    dataCallback);
```

## 🛠️ Troubleshooting

### Common issues

1. **Failed to open serial port**
   ```java
   // Check device paths
   String[] devices = new SerialPortFinder().getAllDevicesPath();
   
   // Check permission
   File deviceFile = new File("/dev/ttyS4");
   boolean canRead = deviceFile.canRead();
   boolean canWrite = deviceFile.canWrite();
   ```

2. **Incomplete received data**
   ```java
   // Enable logs to inspect raw data
   SerialPortLogUtil.setDebugEnabled(true);
   
   // Try different sticky-packet strategies
   manager.configureStickyPacket(SimpleSerialPortManager.StickyPacketStrategy.NO_PROCESSING);
   ```

## 📖 API Reference

### SimpleSerialPortManager (single port)
| Method | Description |
|------|------|
| `getInstance()` | Get singleton instance |
| `openSerialPort(path, baudRate, callback)` | Open serial port |
| `sendData(data)` | Send data |
| `closeSerialPort()` | Close serial port |
| `setDatabits(databits)` | Set data bits |
| `setParity(parity)` | Set parity |
| `setStopbits(stopbits)` | Set stop bits |

### MultiSerialPortManager (multi port)
| Method | Description |
|------|------|
| `getInstance()` | Get instance |
| `openSerialPort(id, path, baudRate, config, statusCallback, dataCallback)` | Open serial port |
| `sendData(serialId, data)` | Send data to a port |
| `closeSerialPort(serialId)` | Close a port |
| `closeAllSerialPorts()` | Close all ports |
| `isSerialPortOpened(serialId)` | Check port status |

## 🎯 Migration

### From 4.1.1 to 5.0.0

**Old (4.1.1)**:
```java
// Init in Application
SerialUtils.getInstance().init(this, true, "TAG", 50, 8, 0, 1);

// Usage
SerialUtils.getInstance().setmSerialPortDirectorListens(...);
SerialUtils.getInstance().manyOpenSerialPort(list);
```

**New (5.0.0)**:
```java
// Simplified init (optional)
new SimpleSerialPortManager.QuickConfig()
    .setDatabits(8).setParity(0).setStopbits(1)
    .apply(this);

// Direct usage
SimpleSerialPortManager.getInstance()
    .openSerialPort("/dev/ttyS4", 115200, data -> {
        // Handle data
    });
```

## 📞 Contact

- **QQ group**: 458173716
- **Blog**: https://blog.csdn.net/a214024475/article/details/113735085
- **GitHub**: https://github.com/cl-6666/serialPort

### PC serial debugging assistant
<img src="https://github.com/cl-6666/serialPort/blob/master/img/pc_ck.jpg" width="440" height="320" alt="PC tool"/>

**Download**: https://pan.baidu.com/s/1DL2TOHz9bl9RIKIG3oCSWw?pwd=f7sh  

### QQ technical group
<img src="https://github.com/cl-6666/serialPort/blob/master/img/qq2.jpg" width="350" height="560" alt="QQ group"/>

**Group ID**: 458173716

## 🔬 Technical Notes

### 16 KB Page Alignment (v5.0.8)

Since 2024, Google Play requires apps that ship native libraries (`.so`) to support 16 KB page size, to be compatible with newer Android devices. This library fully meets the requirement.

#### Implementation

In CMake configuration, the following linker flags are added for `arm64-v8a`:

```cmake
# CMakeLists.txt
if(ANDROID_ABI STREQUAL "arm64-v8a")
    target_compile_options(SerialPort PRIVATE -fno-emulated-tls)
    target_link_options(SerialPort PRIVATE 
        "LINKER:-z,max-page-size=16384"
        "LINKER:-z,common-page-size=16384")
endif()
```

#### Compatibility

- ✅ **Fully compatible**: supports Android 5.0+ (API 21+)
- ✅ **No code change**: upgrade and use directly
- ✅ **Optimized**: 16 KB alignment can improve memory management on some devices
- ✅ **Google Play ready**: passes 16 KB alignment checks

#### Verification

You can verify alignment with Android Studio APK Analyzer:

1. Build an APK or AAB
2. In Android Studio: `Build` → `Analyze APK...`
3. Check the `Alignment` column for `lib/arm64-v8a/libSerialPort.so`
4. `16 KB` means it is configured correctly

#### Resources

- [Google Play 16 KB page size requirements](https://developer.android.com/guide/practices/page-sizes)
- [CMake `target_link_options` docs](https://cmake.org/cmake/help/latest/command/target_link_options.html)

---
