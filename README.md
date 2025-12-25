# Android串口通信框架 SerialPort

[![Version](https://img.shields.io/badge/version-5.0.8-blue.svg)](https://github.com/cl-6666/serialPort)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![License](https://img.shields.io/badge/license-Apache%202-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

> 一个灵活、高效并且轻量的Android串口通信框架，让串口操作变得简单易用。支持单串口、多串口、粘包处理、自定义配置等功能。

<img src="https://github.com/cl-6666/serialPort/blob/master/img/multiple_images.png" width="650" height="360" alt="演示"/>  

## 📱 体验演示

想要快速体验串口通信框架的强大功能？直接下载演示 APK 安装到您的 Android 设备上试试吧！

<div align="center">

### 📥 [点击下载演示 APK](https://www.pgyer.com/XNzY)

[![Download APK](https://img.shields.io/badge/Download-APK%20v5.0.8-brightgreen.svg?style=for-the-badge&logo=android)](https://github.com/cl-6666/serialPort/releases/download/v5.0.8/serialport-demo-v5.0.8.apk)

**版本**: v5.0.8 | **大小**: ~7 MB | **API**: 21+ | **架构**: arm64-v8a, armeabi-v7a, x86, x86_64

</div>

### 演示 APK 功能

- ✅ 单串口通信演示
- ✅ 多串口管理演示
- ✅ 粘包处理策略切换
- ✅ 串口参数配置（数据位、校验位、停止位）
- ✅ 实时数据收发测试
- ✅ 十六进制/ASCII 数据显示
- ✅ 性能测试与统计

> **提示**: 演示 APK 需要在具有串口的 Android 设备上运行（如工控设备、开发板等）。如果您的设备没有串口，可以查看源码了解使用方法。

## ⭐ 特性

- 🚀 **简单易用** - 链式调用，一行代码完成配置
- 🔧 **多串口支持** - 同时管理多个串口，独立配置
- 📦 **智能粘包处理** - 支持多种粘包策略，可动态切换
- ⚡ **高性能** - 多线程处理，线程安全设计
- 🛡️ **稳定可靠** - 完善的错误处理和资源管理
- 📝 **详细日志** - 丰富的调试信息，方便排查问题
- 🎯 **灵活配置** - 支持数据位、校验位、停止位等参数配置
- ✨ **Google Play 认证** - 支持 16KB 页面对齐，完全符合 Google Play 上架要求

## 📖 版本说明

- **当前版本**: 5.0.8 (推荐) - 全新架构，功能强大，支持 Google Play 16KB 页面对齐
- **历史版本**: [4.1.1版本文档](README4.1.1.md) - 稳定版本

### 5.0.8 版本更新 🔥 (2025-12-25)

- ✅ **16KB 页面对齐**: 完全适配 Google Play 16KB 页面大小要求
- ✅ **Android 15 支持**: 兼容最新 Android 15 系统
- ✅ **原生库优化**: arm64-v8a 架构原生库已通过 Google Play 审核标准
- ✅ **向后兼容**: 完全兼容旧版本 Android 设备，无需修改代码

> **重要提示**: 从 2024 年开始，Google Play 要求所有 arm64-v8a 原生库必须支持 16KB 页面大小。5.0.8 版本已完全适配此要求，可放心上架 Google Play。

### 5.0.0 版本重大更新 🎉

- ✅ **架构重构**: 移除SerialUtils依赖，架构更清晰
- ✅ **API简化**: 新增SimpleSerialPortManager，使用更简单
- ✅ **多串口管理**: 全新MultiSerialPortManager，支持复杂场景
- ✅ **增强日志**: 自研日志系统，调试信息更丰富
- ✅ **独立配置**: 每个串口可独立配置粘包处理策略
- ✅ **性能优化**: 减少30%冗余代码，性能提升显著

## 🚀 快速开始

### 依赖集成

在项目的 `build.gradle` 中添加依赖：

```gradle
dependencies {
   implementation 'com.github.cl-6666:serialPort:v5.0.8'
}
```

在项目根目录的 `build.gradle` 中添加：

```gradle
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

### 权限配置

在 `AndroidManifest.xml` 中添加必要权限：

```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

## 📚 使用指南

### 1️⃣ 单串口使用 - 基础示例

#### 最简单的使用方式

```java
public class MainActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 一行代码打开串口并接收数据
        SimpleSerialPortManager.getInstance()
            .openSerialPort("/dev/ttyS4", 115200, data -> {
                String receivedData = new String(data);
                Log.i("Serial", "收到数据: " + receivedData);
                // 处理接收到的数据
            });
    }
    
    // 发送数据
    private void sendData() {
        SimpleSerialPortManager.getInstance().sendData("Hello World");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 关闭串口
        SimpleSerialPortManager.getInstance().closeSerialPort();
    }
}
```

#### 完整配置示例

```java
public class App extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 全局配置（可选）
        new SimpleSerialPortManager.QuickConfig()
            .setIntervalSleep(50)                    // 读取间隔50ms
            .setEnableLog(true)                      // 启用日志
            .setLogTag("SerialPortApp")              // 设置日志标签
            .setDatabits(8)                          // 数据位8
            .setParity(0)                            // 无校验
            .setStopbits(1)                          // 停止位1
            .setStickyPacketStrategy(SimpleSerialPortManager.StickyPacketStrategy.NO_PROCESSING)
            .apply(this);
    }
}
```

### 2️⃣ 数据位、校验位、停止位配置

```java
public class SerialConfigExample {
    
    public void configureSerialParams() {
        SimpleSerialPortManager manager = SimpleSerialPortManager.getInstance();
        
        // 方式1：使用QuickConfig配置
        new SimpleSerialPortManager.QuickConfig()
            .setDatabits(8)        // 数据位：5, 6, 7, 8
            .setParity(0)          // 校验位：0=无校验, 1=奇校验, 2=偶校验
            .setStopbits(1)        // 停止位：1 或 2
            .setFlags(0)           // 标志位
            .apply(getApplication());
        
        // 方式2：动态设置
        manager.setDatabits(8)     // 设置数据位
               .setParity(2)       // 设置偶校验
               .setStopbits(1)     // 设置停止位1
               .setFlags(0);       // 设置标志位
        
        // 打开串口
        manager.openSerialPort("/dev/ttyS4", 115200, data -> {
            Log.i("Serial", "数据: " + new String(data));
        });
    }
    
    // 常用配置组合
    public void commonConfigurations() {
        SimpleSerialPortManager manager = SimpleSerialPortManager.getInstance();
        
        // 标准配置 8N1 (8数据位, 无校验, 1停止位)
        manager.setDatabits(8).setParity(0).setStopbits(1);
        
        // Modbus RTU 8E1 (8数据位, 偶校验, 1停止位) 
        manager.setDatabits(8).setParity(2).setStopbits(1);
        
        // 老式设备 7E2 (7数据位, 偶校验, 2停止位)
        manager.setDatabits(7).setParity(2).setStopbits(2);
    }
}
```

### 3️⃣ 粘包处理详解

粘包是串口通信中常见的问题，5.0.0版本提供了多种处理策略：

```java
public class StickyPacketExample {
    
    public void noProcessing() {
        // 策略1：不处理粘包 - 适用于简单数据流
        new SimpleSerialPortManager.QuickConfig()
            .setStickyPacketStrategy(SimpleSerialPortManager.StickyPacketStrategy.NO_PROCESSING)
            .apply(this);
    }
    
    public void delimiterBased() {
        // 策略2：基于分隔符 - 适用于文本协议
        new SimpleSerialPortManager.QuickConfig()
            .setStickyPacketStrategy(SimpleSerialPortManager.StickyPacketStrategy.DELIMITER_BASED)
            .apply(this);
        
        // 自定义分隔符
        SimpleSerialPortManager.getInstance()
            .configureStickyPacket(SimpleSerialPortManager.StickyPacketStrategy.DELIMITER_BASED);
    }
    
    public void fixedLength() {
        // 策略3：固定长度 - 适用于固定长度协议
        new SimpleSerialPortManager.QuickConfig()
            .setStickyPacketStrategy(SimpleSerialPortManager.StickyPacketStrategy.FIXED_LENGTH)
            .apply(this);
    }
    
    public void variableLength() {
        // 策略4：可变长度 - 适用于带长度字段的协议
        new SimpleSerialPortManager.QuickConfig()
            .setStickyPacketStrategy(SimpleSerialPortManager.StickyPacketStrategy.VARIABLE_LENGTH)
            .apply(this);
    }
}
```

### 4️⃣ 多串口管理 - 强大功能

```java
public class MultiSerialExample {
    
    public void basicMultiSerial() {
        MultiSerialPortManager manager = SimpleSerialPortManager.multi();
        
        // 串口1：GPS模块，不需要粘包处理
        manager.openSerialPort("GPS", "/dev/ttyS1", 9600,
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setDatabits(8)
                .setParity(0)
                .setStopbits(1)
                .setStickyPacketHelpers(new BaseStickPackageHelper()) // 不处理粘包
                .build(),
            // 状态回调
            (serialId, success, status) -> {
                Log.i("GPS", "状态: " + (success ? "成功" : "失败"));
            },
            // 数据回调
            (serialId, data) -> {
                String gpsData = new String(data);
                Log.i("GPS", "数据: " + gpsData);
                handleGpsData(gpsData);
            });
        
        // 串口2：传感器模块，需要换行符分包
        manager.openSerialPort("SENSOR", "/dev/ttyS2", 115200,
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setDatabits(8)
                .setParity(0) 
                .setStopbits(1)
                .setStickyPacketHelpers(new SpecifiedStickPackageHelper("\n")) // 换行符分包
                .build(),
            null, // 不需要状态回调
            (serialId, data) -> {
                String sensorData = new String(data).trim();
                Log.i("SENSOR", "数据: " + sensorData);
                handleSensorData(sensorData);
            });
        
        // 发送数据到不同串口
        manager.sendData("GPS", "AT+GPS?\r\n");
        manager.sendData("SENSOR", "READ_TEMP\n");
    }
    
    // 动态管理串口
    public void dynamicManagement() {
        MultiSerialPortManager manager = SimpleSerialPortManager.multi();
        
        // 查看串口状态
        List<String> openedPorts = manager.getOpenedSerialPorts();
        boolean isOpened = manager.isSerialPortOpened("GPS");
        manager.printAllSerialStatus();
        
        // 动态更新粘包策略
        manager.updateStickyPacketHelpers("GPS", 
            new AbsStickPackageHelper[]{new SpecifiedStickPackageHelper("\r\n")});
        
        // 关闭特定串口
        manager.closeSerialPort("GPS");
        
        // 关闭所有串口
        manager.closeAllSerialPorts();
    }
}
```

## 🎯 实际应用场景

### 工业控制场景
```java
public class IndustrialControlExample {
    
    public void setupIndustrialPorts() {
        MultiSerialPortManager manager = SimpleSerialPortManager.multi();
        
        // PLC通信 - Modbus RTU
        manager.openSerialPort("PLC", "/dev/ttyS1", 9600,
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setDatabits(8).setParity(2).setStopbits(1) // 8E1
                .setStickyPacketHelpers(new StaticLenStickPackageHelper(8))
                .build(),
            null, this::handlePlcData);
        
        // 传感器数据采集 - 文本协议
        manager.openSerialPort("SENSORS", "/dev/ttyS3", 9600,
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setDatabits(7).setParity(2).setStopbits(1) // 7E1
                .setStickyPacketHelpers(new SpecifiedStickPackageHelper("\r\n"))
                .build(),
            null, this::handleSensorData);
    }
}
```

### 通信网关场景
```java
public class GatewayExample {
    
    public void setupGateway() {
        MultiSerialPortManager manager = SimpleSerialPortManager.multi();
        
        // 上行通信（与服务器）
        manager.openSerialPort("UPLINK", "/dev/ttyS1", 115200,
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setStickyPacketHelpers(new SpecifiedStickPackageHelper("\n"))
                .build(),
            null, this::handleUplinkData);
        
        // 下行设备1 - GPS
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

## 🔧 高级功能

### 日志系统
```java
// 启用详细日志
SerialPortLogUtil.setDebugEnabled(true);

// 自定义日志输出
SerialPortLogUtil.i("MyTag", "自定义日志信息");
SerialPortLogUtil.printData("发送", data); // 十六进制+ASCII显示
SerialPortLogUtil.printSerialConfig("MySerial", 8, 0, 1, 0); // 配置信息
```

### 错误处理
```java
manager.openSerialPort("TEST", "/dev/ttyS1", 9600,
    (serialId, success, status) -> {
        if (!success) {
            switch (status) {
                case NO_READ_WRITE_PERMISSION:
                    Log.e("Serial", "权限不足");
                    break;
                case OPEN_FAIL:
                    Log.e("Serial", "打开失败");
                    break;
            }
        }
    },
    dataCallback);
```

## 🛠️ 故障排查

### 常见问题

1. **串口打开失败**
   ```java
   // 检查设备路径
   String[] devices = new SerialPortFinder().getAllDevicesPath();
   
   // 检查权限
   File deviceFile = new File("/dev/ttyS4");
   boolean canRead = deviceFile.canRead();
   boolean canWrite = deviceFile.canWrite();
   ```

2. **数据接收不完整**
   ```java
   // 启用日志查看原始数据
   SerialPortLogUtil.setDebugEnabled(true);
   
   // 尝试不同的粘包策略
   manager.configureStickyPacket(SimpleSerialPortManager.StickyPacketStrategy.NO_PROCESSING);
   ```

## 📖 API参考

### SimpleSerialPortManager (单串口)
| 方法 | 说明 |
|------|------|
| `getInstance()` | 获取单例实例 |
| `openSerialPort(path, baudRate, callback)` | 打开串口 |
| `sendData(data)` | 发送数据 |
| `closeSerialPort()` | 关闭串口 |
| `setDatabits(databits)` | 设置数据位 |
| `setParity(parity)` | 设置校验位 |
| `setStopbits(stopbits)` | 设置停止位 |

### MultiSerialPortManager (多串口)
| 方法 | 说明 |
|------|------|
| `getInstance()` | 获取实例 |
| `openSerialPort(id, path, baudRate, config, statusCallback, dataCallback)` | 打开串口 |
| `sendData(serialId, data)` | 发送数据到指定串口 |
| `closeSerialPort(serialId)` | 关闭指定串口 |
| `closeAllSerialPorts()` | 关闭所有串口 |
| `isSerialPortOpened(serialId)` | 检查串口状态 |

## 🎯 版本迁移

### 从4.1.1迁移到5.0.0

**旧版本 (4.1.1)**:
```java
// 在Application中初始化
SerialUtils.getInstance().init(this, true, "TAG", 50, 8, 0, 1);

// 使用
SerialUtils.getInstance().setmSerialPortDirectorListens(...);
SerialUtils.getInstance().manyOpenSerialPort(list);
```

**新版本 (5.0.0)**:
```java
// 简化的初始化（可选）
new SimpleSerialPortManager.QuickConfig()
    .setDatabits(8).setParity(0).setStopbits(1)
    .apply(this);

// 直接使用
SimpleSerialPortManager.getInstance()
    .openSerialPort("/dev/ttyS4", 115200, data -> {
        // 处理数据
    });
```

## 📞 联系我们

- **QQ群**: 458173716
- **博客**: https://blog.csdn.net/a214024475/article/details/113735085
- **GitHub**: https://github.com/cl-6666/serialPort


### PC端串口调试助手
<img src="https://github.com/cl-6666/serialPort/blob/master/img/pc_ck.jpg" width="440" height="320" alt="PC调试助手"/>

**下载链接**: https://pan.baidu.com/s/1DL2TOHz9bl9RIKIG3oCSWw?pwd=f7sh  

### QQ技术交流群
<img src="https://github.com/cl-6666/serialPort/blob/master/img/qq2.jpg" width="350" height="560" alt="QQ群"/>

**QQ群号**: 458173716

## 🔬 技术说明

### 16KB 页面对齐适配 (v5.0.8)

从 2024 年开始，Google Play 要求所有使用原生库（.so 文件）的应用必须支持 16KB 页面大小，以适配最新的 Android 设备。本库已完全适配此要求。

#### 技术实现

我们在 CMake 构建配置中针对 arm64-v8a 架构添加了以下链接器标志：

```cmake
# CMakeLists.txt
if(ANDROID_ABI STREQUAL "arm64-v8a")
    target_compile_options(SerialPort PRIVATE -fno-emulated-tls)
    target_link_options(SerialPort PRIVATE 
        "LINKER:-z,max-page-size=16384"
        "LINKER:-z,common-page-size=16384")
endif()
```

#### 兼容性说明

- ✅ **完全兼容**: 支持所有 Android 5.0+ (API 21+) 设备
- ✅ **无需修改**: 开发者无需修改任何代码，直接升级即可
- ✅ **性能优化**: 16KB 页面对齐可提升部分设备的内存管理效率
- ✅ **Google Play 认证**: 已通过 Google Play 的 16KB 页面对齐检测

#### 验证方法

使用 Android Studio 的 APK Analyzer 工具可以验证原生库是否支持 16KB 页面对齐：

1. 构建 APK 或 AAB 文件
2. 在 Android Studio 中选择 `Build` → `Analyze APK...`
3. 查看 `lib/arm64-v8a/libSerialPort.so` 的 `Alignment` 列
4. 显示 `16 KB` 表示已正确配置

#### 相关资源

- [Google Play 16KB 页面大小要求](https://developer.android.com/guide/practices/page-sizes)
- [CMake 链接器选项文档](https://cmake.org/cmake/help/latest/command/target_link_options.html)

---
