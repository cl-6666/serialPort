# 说明  
>串口通信是软件和硬件通信经常用到的东西，我在这里开源一个我自己平常也在用的串口库，大家在使用遇到问题了欢迎指出，我会第一时间修复，强烈建议以依赖的方式导入，这样避免代码同步。

# 拆包及粘包解决方案  
<img src="https://github.com/cl-6666/serialPort/blob/master/WechatIMG415.png" width="200" height="200" alt="介绍"/>

# 效果图  
<img src="https://github.com/cl-6666/serialPort/blob/master/sample_picture.png" width="650" height="360" alt="演示"/>  

版本更新历史：  
[![](https://jitpack.io/v/cl-6666/serialPort.svg)](https://jitpack.io/#cl-6666/serialPort) 

V3.0.0：   
1、基于现有的串口框架增加调试助手，方便测试  
2、框架新增打开方法，支持直接修改串口  
3、代码优化  

V2.0.0：   
1、解决数据包接收不完整bug  
2、增加参数构建  
3、代码优化  

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
    implementation 'com.github.cl-6666:serialPort:v3.0.0'
}
```  
### kotlin使用初始化  
``` kotlin  
   //构建初始化参数
        val sdk = ConfigurationBuilder(device.file, 115200)   //串口号，波特率
            .log("TAG", true, false)   //日志标识，是否开启sdk日志，是否开启日志堆栈信息显示
//            .msgHead(b1)  明确协议头可以开启
            .build()
        mSerialPortManager = SerialPortManager.getInstance()
        mSerialPortManager?.init(sdk, this)

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


```

### 查看串口

``` Java
SerialPortFinder serialPortFinder = new SerialPortFinder();
ArrayList<Device> devices = serialPortFinder.getDevices();
```

### java使用初始化

``` Java
      //构建初始化参数
        ConfigurationSdk sdk = new ConfigurationSdk.ConfigurationBuilder(device.getFile(), 115200)
                .log("TAG", true, false)
//                .msgHead(b1)   打开说明需要效验
                .build();
        mSerialPortManager = SerialPortManager.getInstance();
        mSerialPortManager.init(sdk, this);

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
    mSerialPortManager.init(sdk, this);
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



## 通用疑问解答  
1.假如正常打开串口，但是收不到消息的话，请检查你的波特率  
2.假如提示没有权限的话，请通过运行SelectSerialPortActivity起来看一下是否有读写权限  

