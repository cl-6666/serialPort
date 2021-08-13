# 说明  
串口通信是软件和硬件通信经常用到的东西，我在这里开源一个我自己平常也在用的串口库，大家在使用遇到问题了欢迎指出，我会第一时间修复，强烈建议以依赖的方式导入，这样避免代码同步。

# 拆包及粘包解决方案  
<img src="https://github.com/cl-6666/serialPort/blob/master/WechatIMG415.png" width="200" height="200" alt="介绍"/>

# 效果图  
<img src="https://github.com/cl-6666/serialPort/blob/master/device-img.png" width="200" height="200" alt="演示"/>  

版本更新历史：  
[![](https://jitpack.io/v/cl-6666/serialPort.svg)](https://jitpack.io/#cl-6666/serialPort) 

V2.0.0：   
1、解决数据包接收不完整bug  
2、增加粘包配置，也支持协议自己配置

V1.0.0：    
第一代版本sdk提交

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
  implementation 'com.github.cl-6666:serialPort:v2.0.0'
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

### 初始化

``` Java
   //构建初始化参数
        ConfigurationSdk sdk = new ConfigurationSdk.ConfigurationBuilder(device.getFile(), 115200)
                .log("TAG", true, false)
     //                .msgHead(b1)   打开说明需要效验数据粘包，不打开就自己封装粘包协议
                .build();
        SerialPortManager.getInstance().init(sdk,this);

        // 打开串口
        SerialPortManager.getInstance().setOnOpenSerialPortListener(this)
                .setOnSerialPortDataListener(new OnSerialPortDataListener() {
                    @Override
                    public void onDataReceived(byte[] bytes) {
                        Log.i(TAG, "onDataReceived [ byte[] ]: " + Arrays.toString(bytes));
                        Log.i(TAG, "onDataReceived [ String ]: " + new String(bytes));
                        final byte[] finalBytes = bytes;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showToast(String.format("接收\n%s", new String(finalBytes)));
                            }
                        });
                    }

                    @Override
                    public void onDataSent(byte[] bytes) {
                        Log.i(TAG, "onDataSent [ byte[] ]: " + Arrays.toString(bytes));
                        Log.i(TAG, "onDataSent [ String ]: " + new String(bytes));
                        final byte[] finalBytes = bytes;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showToast(String.format("发送\n%s", new String(finalBytes)));
                            }
                        });
                    }
                });

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

博客地址：https://blog.csdn.net/a214024475/article/details/113735085
