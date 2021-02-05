# 说明  
串口通信是软件和硬件通信经常用到的东西，大家在使用遇到问题了欢迎指出，我会第一时间修复

# 项目依赖
``` Gradle
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

Step 2. Add the dependency

``` Gradle
dependencies {
       implementation 'com.github.cl-6666:serialPort:v1.0.0'
}
```  
## kotlin使用介绍  
``` kotlin  
 // 打开串口
        val openSerialPort = mSerialPortManager!!.setOnOpenSerialPortListener(this)
            .setOnSerialPortDataListener(object : OnSerialPortDataListener {
                override fun onDataReceived(bytes: ByteArray) {
                    Log.i(
                        TAG,
                        "onDataReceived [ byte[] ]: " + Arrays.toString(bytes)
                    )
                    Log.i(
                        TAG,
                        "onDataReceived [ String ]: " + String(bytes)
                    )
                    runOnUiThread { showToast(String.format("接收\n%s", String(bytes))) }
                }

                override fun onDataSent(bytes: ByteArray) {
                    Log.i(
                        TAG,
                        "onDataSent [ byte[] ]: " + Arrays.toString(bytes)
                    )
                    Log.i(
                        TAG,
                        "onDataSent [ String ]: " + String(bytes)
                    )
                    runOnUiThread { showToast(String.format("发送\n%s", String(bytes))) }
                }
            })
            .openSerialPort(device.file, 115200)

        Log.i(
            TAG,
            "onCreate: openSerialPort = $openSerialPort"
        )

```

## 查看串口

``` Java
SerialPortFinder serialPortFinder = new SerialPortFinder();
ArrayList<Device> devices = serialPortFinder.getDevices();
```

## 打开串口

### 初始化

``` Java
mSerialPortManager = new SerialPortManager();
```

### 添加打开串口监听

``` Java
mSerialPortManager.setOnOpenSerialPortListener(new OnOpenSerialPortListener() {
    @Override
    public void onSuccess(File device) {
        
    }

    @Override
    public void onFail(File device, Status status) {

    }
});
```

### 添加数据通信监听

``` Java
mSerialPortManager.setOnSerialPortDataListener(new OnSerialPortDataListener() {
    @Override
    public void onDataReceived(byte[] bytes) {
        
    }

    @Override
    public void onDataSent(byte[] bytes) {

    }
});
```

### 打开串口

- 参数1：串口
- 参数2：波特率
- 返回：串口打开是否成功

``` Java
boolean openSerialPort = mSerialPortManager.openSerialPort(device.getFile(), 115200);
```

### 发送数据

- 参数：发送数据 byte[]
- 返回：发送是否成功

``` Java
boolean sendBytes = mSerialPortManager.sendBytes(sendContentBytes);
```

## 关闭串口

``` Java
mSerialPortManager.closeSerialPort();
```

> PS：传输协议需自行封装
