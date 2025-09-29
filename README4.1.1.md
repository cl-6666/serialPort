# Android串口通信框架 SerialPort v4.1.1

[![Version](https://img.shields.io/badge/version-4.1.1-orange.svg)](https://github.com/cl-6666/serialPort)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)

> 这是串口通信框架的4.1.1稳定版本文档。如果你是新用户，建议使用最新的 [5.0.0版本](README.md)，它提供了更简单的API和更强大的功能。

⚠️ **注意**: 4.1.1版本为历史版本，仅用于维护现有项目。新项目请使用 [5.0.0版本](README.md)。

## 📖 版本说明

- **当前版本**: 4.1.1 (稳定维护版本)
- **推荐版本**: [5.0.0版本](README.md) - 功能更强大，API更简单

## 🚀 快速开始

### 依赖集成

在项目的 `build.gradle` 中添加依赖：

```gradle
dependencies {
    implementation 'com.github.cl-6666:serialPort:4.1.1'
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

### 1. Application中初始化

```java
   public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        
        // 方式1：使用XLogConfig配置日志
        XLogConfig logConfig = new XLogConfig.Builder()
                .logSwitch(true)        // 开启日志
                .tag("SerialPort")      // 设置tag
                .build();
        
        SerialConfig serialConfig = new SerialConfig.Builder()
                .setXLogConfig(logConfig)    // 配置日志参数
                .setIntervalSleep(50)        // 设置读取间隔
                .setSerialPortReconnection(false)  // 是否开启串口重连
                .setFlags(0)             // 标志位
                .setDatabits(8)          // 数据位
                .setStopbits(1)          // 停止位
                .setParity(0)            // 校验位：0无校验，1奇校验，2偶校验
                .build();
        
        SerialUtils.getInstance().init(this, serialConfig);
        
        // 方式2：简化初始化
        SerialUtils.getInstance().init(this, true, "SerialPort", 50, 8, 0, 1);
    }
}
```

### 2. 单串口使用

```java
public class MainActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 设置串口监听
        SerialUtils.getInstance().setmSerialPortDirectorListens(new SerialPortDirectorListens() {
            @Override
            public void onSerialPortOpenSuccess(File device, SerialPortEnum serialPortEnum) {
                Log.i("Serial", "串口打开成功: " + device.getPath());
            }
            
            @Override
            public void onSerialPortOpenFail(File device, SerialStatus status) {
                Log.e("Serial", "串口打开失败: " + status);
            }
            
            @Override
            public void onDataReceive(byte[] bytes, SerialPortEnum serialPortEnum) {
                String data = new String(bytes);
                Log.i("Serial", "接收数据: " + data);
                // 处理接收到的数据
            }
            
            @Override
            public void onDataSend(byte[] bytes, SerialPortEnum serialPortEnum) {
                Log.i("Serial", "发送数据: " + new String(bytes));
            }
        });
        
        // 打开串口
        List<Device> deviceList = new ArrayList<>();
        deviceList.add(new Device("/dev/ttyS4", "115200", new File("/dev/ttyS4")));
        SerialUtils.getInstance().manyOpenSerialPort(deviceList);
    }
    
    // 发送数据
    private void sendData() {
        String data = "Hello World";
        SerialUtils.getInstance().sendData(SerialPortEnum.SERIAL_ONE, data.getBytes());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 关闭串口
        SerialUtils.getInstance().serialPortClose();
    }
}
```

### 3. 多串口使用

```java
public class MultiSerialActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置粘包处理
        SerialUtils.getInstance().setStickPackageHelper(
            new BaseStickPackageHelper(),              // 串口1：不处理粘包
            new SpecifiedStickPackageHelper("\n"),     // 串口2：换行符分包
            new StaticLenStickPackageHelper(8)         // 串口3：固定8字节
        );
        
        // 设置监听
        SerialUtils.getInstance().setmSerialPortDirectorListens(new SerialPortDirectorListens() {
            @Override
            public void onSerialPortOpenSuccess(File device, SerialPortEnum serialPortEnum) {
                Log.i("Serial", "串口[" + serialPortEnum + "]打开成功: " + device.getPath());
            }
            
            @Override
            public void onSerialPortOpenFail(File device, SerialStatus status) {
                Log.e("Serial", "串口打开失败: " + status);
            }
            
            @Override
            public void onDataReceive(byte[] bytes, SerialPortEnum serialPortEnum) {
                String data = new String(bytes);
                Log.i("Serial", "串口[" + serialPortEnum + "]收到: " + data);
                
                // 根据串口类型处理数据
                switch (serialPortEnum) {
                    case SERIAL_ONE:
                        handleGpsData(data);
                        break;
                    case SERIAL_TWO:
                        handleSensorData(data);
                        break;
                    case SERIAL_THREE:
                        handleModbusData(bytes);
                        break;
                }
            }
            
            @Override
            public void onDataSend(byte[] bytes, SerialPortEnum serialPortEnum) {
                Log.i("Serial", "串口[" + serialPortEnum + "]发送: " + new String(bytes));
            }
        });
        
        // 打开多个串口
        List<Device> deviceList = new ArrayList<>();
        deviceList.add(new Device("/dev/ttyS1", "9600", new File("/dev/ttyS1")));    // GPS
        deviceList.add(new Device("/dev/ttyS2", "115200", new File("/dev/ttyS2")));  // 传感器
        deviceList.add(new Device("/dev/ttyS3", "9600", new File("/dev/ttyS3")));    // Modbus
        
        SerialUtils.getInstance().manyOpenSerialPort(deviceList);
    }
    
    // 向不同串口发送数据
    private void sendToSerial() {
        // 向串口1发送GPS命令
        SerialUtils.getInstance().sendData(SerialPortEnum.SERIAL_ONE, "AT+GPS?\r\n".getBytes());
        
        // 向串口2发送传感器命令
        SerialUtils.getInstance().sendData(SerialPortEnum.SERIAL_TWO, "READ_TEMP\n".getBytes());
        
        // 向串口3发送Modbus命令
        byte[] modbusCmd = {0x01, 0x03, 0x00, 0x00, 0x00, 0x01, (byte)0x84, 0x0A};
        SerialUtils.getInstance().sendData(SerialPortEnum.SERIAL_THREE, modbusCmd);
    }
    
    private void handleGpsData(String data) {
        // 处理GPS数据
    }
    
    private void handleSensorData(String data) {
        // 处理传感器数据
    }
    
    private void handleModbusData(byte[] data) {
        // 处理Modbus数据
    }
}
```

### 4. 粘包处理配置

```java
public class StickyPacketConfig {
    
    public void configureStickyPacket() {
        // 1. 不处理粘包（默认）
        SerialUtils.getInstance().setStickPackageHelper(new BaseStickPackageHelper());
        
        // 2. 按分隔符分包
        SerialUtils.getInstance().setStickPackageHelper(
            new SpecifiedStickPackageHelper("\r\n".getBytes())  // 按\r\n分包
        );
        
        // 3. 固定长度分包
        SerialUtils.getInstance().setStickPackageHelper(
            new StaticLenStickPackageHelper(16)  // 固定16字节
        );
        
        // 4. 可变长度分包
        SerialUtils.getInstance().setStickPackageHelper(
            new VariableLenStickPackageHelper(
                java.nio.ByteOrder.BIG_ENDIAN,  // 字节序
                2,    // 长度字段大小
                2,    // 长度字段位置
                12    // 包头长度
            )
        );
        
        // 5. 多串口不同策略
        SerialUtils.getInstance().setStickPackageHelper(
            new BaseStickPackageHelper(),                    // 串口1：不处理
            new SpecifiedStickPackageHelper("\n"),           // 串口2：换行符
            new StaticLenStickPackageHelper(8),              // 串口3：固定长度
            new VariableLenStickPackageHelper(               // 串口4：可变长度
                java.nio.ByteOrder.BIG_ENDIAN, 2, 2, 12)
        );
    }
}
```  

### 5. 串口参数配置

```java
public class SerialParamConfig {
    
    public void configureParams() {
        SerialConfig serialConfig = new SerialConfig.Builder()
            .setIntervalSleep(50)           // 读取间隔50ms
            .setDatabits(8)                 // 数据位8
            .setStopbits(1)                 // 停止位1
            .setParity(0)                   // 校验位：0=无校验
            .setFlags(0)                    // 标志位
            .setSerialPortReconnection(false)  // 是否重连
            .build();
        
        SerialUtils.getInstance().init(getApplication(), serialConfig);
    }
    
    // 常用配置
    public void commonConfigs() {
        // 标准配置 8N1
        SerialConfig config8N1 = new SerialConfig.Builder()
            .setDatabits(8).setParity(0).setStopbits(1)
            .build();
        
        // Modbus RTU 8E1
        SerialConfig configModbus = new SerialConfig.Builder()
            .setDatabits(8).setParity(2).setStopbits(1)  // 偶校验
            .build();
        
        // 老式设备 7E2
        SerialConfig configOld = new SerialConfig.Builder()
            .setDatabits(7).setParity(2).setStopbits(2)
            .build();
    }
}
```

## 📖 API参考

### SerialUtils 主要方法
| 方法 | 说明 |
|------|------|
| `init(Application, SerialConfig)` | 初始化串口框架 |
| `init(Application, boolean, String, int, int, int, int)` | 简化初始化 |
| `setmSerialPortDirectorListens(SerialPortDirectorListens)` | 设置串口监听 |
| `setStickPackageHelper(AbsStickPackageHelper...)` | 设置粘包处理 |
| `manyOpenSerialPort(List<Device>)` | 打开多个串口 |
| `sendData(SerialPortEnum, byte[])` | 发送数据 |
| `serialPortClose()` | 关闭串口 |

### SerialConfig 配置项
| 参数 | 说明 | 默认值 |
|------|------|--------|
| `intervalSleep` | 读取间隔(ms) | 50 |
| `databits` | 数据位 | 8 |
| `stopbits` | 停止位 | 1 |
| `parity` | 校验位 | 0 |
| `flags` | 标志位 | 0 |
| `serialPortReconnection` | 是否重连 | false |

### 粘包处理器
| 类型 | 说明 | 适用场景 |
|------|------|----------|
| `BaseStickPackageHelper` | 不处理粘包 | 简单数据流 |
| `SpecifiedStickPackageHelper` | 分隔符分包 | 文本协议 |
| `StaticLenStickPackageHelper` | 固定长度分包 | 固定格式协议 |
| `VariableLenStickPackageHelper` | 可变长度分包 | 复杂二进制协议 |

## 🛠️ 故障排查

### 常见问题

1. **串口打开失败**
   ```java
   // 检查设备路径
   String[] devices = new SerialPortFinder().getAllDevicesPath();
   
   // 检查权限
   File deviceFile = new File("/dev/ttyS4");
   if (!deviceFile.canRead() || !deviceFile.canWrite()) {
       Log.e("Serial", "设备权限不足");
   }
   ```

2. **数据接收不完整**
   ```java
   // 尝试不处理粘包
   SerialUtils.getInstance().setStickPackageHelper(new BaseStickPackageHelper());
   
   // 或者调整读取间隔
   SerialConfig config = new SerialConfig.Builder()
       .setIntervalSleep(20)  // 减少到20ms
       .build();
   ```

3. **日志输出问题**
   ```java
   // 确保启用日志
   XLogConfig logConfig = new XLogConfig.Builder()
       .logSwitch(true)
       .tag("SerialPort")
       .build();
   ```

## 🎯 升级到5.0.0

如果你想升级到最新的5.0.0版本，以下是主要的变化：

### 4.1.1版本
```java
// 初始化
SerialUtils.getInstance().init(this, true, "TAG", 50, 8, 0, 1);

// 使用
SerialUtils.getInstance().setmSerialPortDirectorListens(...);
SerialUtils.getInstance().manyOpenSerialPort(deviceList);
SerialUtils.getInstance().sendData(SerialPortEnum.SERIAL_ONE, data);
```

### 5.0.0版本 (推荐)
```java
// 简化初始化
new SimpleSerialPortManager.QuickConfig()
    .setDatabits(8).setParity(0).setStopbits(1)
    .apply(this);

// 简化使用
SimpleSerialPortManager.getInstance()
    .openSerialPort("/dev/ttyS4", 115200, data -> {
        // 处理数据
    });

// 多串口
MultiSerialPortManager manager = SimpleSerialPortManager.multi();
manager.openSerialPort("GPS", "/dev/ttyS1", 9600, config, statusCallback, dataCallback);
```

**升级优势**：
- API更简单易用
- 支持真正的多串口管理
- 更好的错误处理
- 增强的日志系统
- 更高的性能

详细的5.0.0版本使用请查看 [最新文档](README.md)。

## 📞 联系我们

- **QQ群**: 458173716
- **博客**: https://blog.csdn.net/a214024475/article/details/113735085
- **GitHub**: https://github.com/cl-6666/serialPort

## 📄 许可证

```
Licensed under the Apache License, Version 2.0
```

---

⚠️ **再次提醒**: 4.1.1版本为历史版本，新项目建议使用 [5.0.0版本](README.md)，功能更强大，使用更简单！