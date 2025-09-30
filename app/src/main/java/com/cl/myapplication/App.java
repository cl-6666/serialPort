package com.cl.myapplication;

import android.app.Application;

import com.hjq.toast.ToastUtils;
import com.cl.serialportlibrary.SimpleSerialPortManager;

/**
 * 项目：serialPort
 * 作者：Arry
 * 创建日期：2021/10/20
 * 描述：
 * 修订历史：
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化 Toast 框架
        ToastUtils.init(this);
        
        // 使用新的SimpleSerialPortManager进行全局初始化
        new SimpleSerialPortManager.QuickConfig()
                .setIntervalSleep(50)                    // 读取间隔50ms
                .setEnableLog(true)                      // 启用日志
                .setLogTag("SerialPortApp")              // 设置日志标签
                .setDatabits(8)                          // 数据位8
                .setParity(0)                            // 无校验
                .setStopbits(1)                          // 停止位1
                .setStickyPacketStrategy(SimpleSerialPortManager.StickyPacketStrategy.NO_PROCESSING) // 不处理黏包
                .setMaxPacketSize(1024)                  // 最大包大小1KB
                .apply(this);
    }
}
