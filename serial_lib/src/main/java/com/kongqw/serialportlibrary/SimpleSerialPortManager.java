package com.kongqw.serialportlibrary;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import com.kongqw.serialportlibrary.enumerate.SerialPortEnum;
import com.kongqw.serialportlibrary.enumerate.SerialStatus;
import com.kongqw.serialportlibrary.listener.OnOpenSerialPortListener;
import com.kongqw.serialportlibrary.listener.OnSerialPortDataListener;
import com.kongqw.serialportlibrary.stick.AbsStickPackageHelper;
import com.kongqw.serialportlibrary.stick.BaseStickPackageHelper;
import com.kongqw.serialportlibrary.stick.SpecifiedStickPackageHelper;
import com.kongqw.serialportlibrary.stick.StaticLenStickPackageHelper;
import com.kongqw.serialportlibrary.stick.VariableLenStickPackageHelper;
import com.kongqw.serialportlibrary.utils.SerialPortLogUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 简化的串口管理器，提供更简单的API供外部使用
 * 完全独立，不依赖SerialUtils
 * Author: cl
 * Date: 2023/10/26
 */
public class SimpleSerialPortManager {
    
    private static final String TAG = "SimpleSerialPortManager";
    private static SimpleSerialPortManager instance;
    private OnOpenSerialPortCallback openCallback;
    private OnDataReceivedCallback dataCallback;
    
    // 串口管理器
    private SerialPortManager serialPortManager;
    private SerialConfig serialConfig;
    private Handler handler = new Handler(Looper.getMainLooper());
    private List<AbsStickPackageHelper> stickPackageHelpers = new ArrayList<>();
    private boolean isInitialized = false;
    
    // 串口参数配置
    private int databits = 8;      // 数据位，默认8
    private int parity = 0;        // 校验位，默认0（无校验）
    private int stopbits = 1;      // 停止位，默认1
    private int flags = 0;         // 标志位，默认0
    
    private SimpleSerialPortManager() {
        // 默认添加基础粘包处理器
        stickPackageHelpers.add(new BaseStickPackageHelper());
    }
    
    /**
     * 获取单例实例
     */
    public static SimpleSerialPortManager getInstance() {
        if (instance == null) {
            synchronized (SimpleSerialPortManager.class) {
                if (instance == null) {
                    instance = new SimpleSerialPortManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 简单初始化 - 最基本的配置
     * @param application 应用程序上下文
     */
    public SimpleSerialPortManager init(Application application) {
        return init(application, true, "SerialPort", 50);
    }
    
    /**
     * 初始化串口管理器 - 基础配置
     * @param application 应用程序上下文
     * @param enableLog 是否启用日志
     * @param logTag 日志标签
     * @param intervalSleep 读取间隔时间(ms)
     */
    public SimpleSerialPortManager init(Application application, boolean enableLog, String logTag, int intervalSleep) {
        SerialPortLogUtil.setDebugEnabled(enableLog);
        SerialPortLogUtil.i(TAG, "初始化SimpleSerialPortManager - enableLog: " + enableLog + ", tag: " + logTag + ", interval: " + intervalSleep);
        
        // 创建默认配置
        serialConfig = new SerialConfig.Builder()
                .setEnableLogging(enableLog)
                .setIntervalSleep(intervalSleep)
                .setDatabits(databits)
                .setParity(parity)
                .setStopbits(stopbits)
                .setFlags(flags)
                .setEnableStickyPacketProcessing(true)
                .setStickyPacketHelpers(stickPackageHelpers.toArray(new AbsStickPackageHelper[0]))
                .build();
                
        isInitialized = true;
        SerialPortLogUtil.i(TAG, "SimpleSerialPortManager初始化完成");
        return this;
    }
    
    /**
     * 初始化串口管理器 - 使用SerialConfig配置
     * @param application 应用程序上下文
     * @param config 串口配置
     */
    public SimpleSerialPortManager init(Application application, SerialConfig config) {
        this.serialConfig = config;
        SerialPortLogUtil.setDebugEnabled(config.isEnableLogging());
        SerialPortLogUtil.i(TAG, "使用SerialConfig初始化SimpleSerialPortManager");
        
        // 同步串口参数
        this.databits = config.getDatabits();
        this.parity = config.getParity();
        this.stopbits = config.getStopbits();
        this.flags = config.getFlags();
        
        isInitialized = true;
        SerialPortLogUtil.i(TAG, "SimpleSerialPortManager初始化完成");
        return this;
    }
    
    /**
     * 配置粘包处理策略
     */
    public SimpleSerialPortManager configureStickyPacket(StickyPacketStrategy strategy) {
        stickPackageHelpers.clear();
        
        AbsStickPackageHelper helper;
        switch (strategy) {
            case DELIMITER_BASED:
                helper = new SpecifiedStickPackageHelper("\n");
                SerialPortLogUtil.i(TAG, "配置粘包处理策略: 分隔符模式");
                break;
            case FIXED_LENGTH:
                helper = new StaticLenStickPackageHelper();
                SerialPortLogUtil.i(TAG, "配置粘包处理策略: 固定长度模式");
                break;
            case VARIABLE_LENGTH:
                helper = new VariableLenStickPackageHelper(java.nio.ByteOrder.BIG_ENDIAN, 2, 2, 12);
                SerialPortLogUtil.i(TAG, "配置粘包处理策略: 可变长度模式");
                break;
            case NO_PROCESSING:
            default:
                helper = new BaseStickPackageHelper();
                SerialPortLogUtil.i(TAG, "配置粘包处理策略: 无处理模式");
                break;
        }
        
        stickPackageHelpers.add(helper);
        
        // 如果已经初始化，更新配置
        if (serialConfig != null) {
            serialConfig.setStickyPacketHelpers(stickPackageHelpers.toArray(new AbsStickPackageHelper[0]));
        }
        
        return this;
    }
    
    /**
     * 设置自定义粘包处理器
     */
    public SimpleSerialPortManager setStickyPacketHelpers(AbsStickPackageHelper... helpers) {
        stickPackageHelpers.clear();
        for (AbsStickPackageHelper helper : helpers) {
            stickPackageHelpers.add(helper);
        }
        
        // 如果已经初始化，更新配置
        if (serialConfig != null) {
            serialConfig.setStickyPacketHelpers(helpers);
        }
        
        SerialPortLogUtil.i(TAG, "设置自定义粘包处理器，数量: " + helpers.length);
        return this;
    }
    
    /**
     * 打开串口
     * @param devicePath 设备路径
     * @param baudRate 波特率
     * @param callback 数据接收回调
     * @return 是否打开成功
     */
    public boolean openSerialPort(String devicePath, int baudRate, OnDataReceivedCallback callback) {
        return openSerialPort(devicePath, baudRate, null, callback);
    }
    
    /**
     * 打开串口
     * @param devicePath 设备路径
     * @param baudRate 波特率
     * @param openCallback 打开状态回调
     * @param dataCallback 数据接收回调
     * @return 是否打开成功
     */
    public boolean openSerialPort(String devicePath, int baudRate, OnOpenSerialPortCallback openCallback, OnDataReceivedCallback dataCallback) {
        if (!isInitialized) {
            SerialPortLogUtil.e(TAG, "SimpleSerialPortManager未初始化，请先调用init()方法");
            return false;
        }
        
        this.openCallback = openCallback;
        this.dataCallback = dataCallback;
        
        SerialPortLogUtil.i(TAG, "尝试打开串口: " + devicePath + ", 波特率: " + baudRate);
        SerialPortLogUtil.printSerialConfig(TAG, databits, parity, stopbits, flags);
        
        // 更新配置中的串口参数
        updateSerialConfig();
        
        // 创建SerialPortManager并设置配置
        serialPortManager = new SerialPortManager();
        serialPortManager.setSerialConfig(serialConfig);
        serialPortManager.setStickPackageHelpers(stickPackageHelpers);
        
        // 设置监听器
        serialPortManager.setOnOpenSerialPortListener(new OnOpenSerialPortListener() {
            @Override
            public void openState(SerialPortEnum serialPortEnum, File device, SerialStatus status) {
                if (status == SerialStatus.SUCCESS_OPENED) {
                    SerialPortLogUtil.i(TAG, "串口打开成功: " + device.getPath());
                    handler.post(() -> {
                        if (openCallback != null) {
                            openCallback.onStatusChanged(true, status);
                        }
                    });
                } else {
                    SerialPortLogUtil.e(TAG, "串口打开失败: " + device.getPath() + ", 状态: " + status);
                    handler.post(() -> {
                        if (openCallback != null) {
                            openCallback.onStatusChanged(false, status);
                        }
                    });
                }
            }
        });
        
        serialPortManager.setOnSerialPortDataListener(new OnSerialPortDataListener() {
            @Override
            public void onDataReceived(byte[] data, SerialPortEnum serialPortEnum) {
                SerialPortLogUtil.printData(TAG, "接收数据", data);
                handler.post(() -> {
                    if (dataCallback != null) {
                        dataCallback.onDataReceived(data);
                    }
                });
            }
            
            @Override
            public void onDataSent(byte[] data, SerialPortEnum serialPortEnum) {
                SerialPortLogUtil.printData(TAG, "发送数据", data);
                handler.post(() -> {
                    if (dataCallback != null) {
                        dataCallback.onDataSent(data);
                    }
                });
            }
        });
        
        // 打开串口
        return serialPortManager.openSerialPort(devicePath, baudRate);
    }
    
    /**
     * 更新串口配置参数
     */
    private void updateSerialConfig() {
        if (serialConfig != null) {
            serialConfig.setDatabits(this.databits);
            serialConfig.setParity(this.parity);
            serialConfig.setStopbits(this.stopbits);
            serialConfig.setFlags(this.flags);
            SerialPortLogUtil.d(TAG, "更新串口配置 - 数据位: " + databits + ", 校验位: " + parity + ", 停止位: " + stopbits);
        }
    }
    
    /**
     * 发送数据
     * @param data 要发送的数据
     * @return 是否发送成功
     */
    public boolean sendData(byte[] data) {
        if (serialPortManager == null) {
            SerialPortLogUtil.e(TAG, "串口未打开，无法发送数据");
            return false;
        }
        
        if (data == null || data.length == 0) {
            SerialPortLogUtil.w(TAG, "尝试发送空数据");
            return false;
        }
        
        long startTime = System.currentTimeMillis();
        SerialPortLogUtil.printData(TAG, "准备发送", data);
        
        boolean result = serialPortManager.sendBytes(data);
        SerialPortLogUtil.printPerformance(TAG, "发送数据", startTime);
        
        if (!result) {
            SerialPortLogUtil.e(TAG, "数据发送失败");
        }
        
        return result;
    }
    
    /**
     * 发送字符串数据
     * @param data 要发送的字符串
     * @return 是否发送成功
     */
    public boolean sendData(String data) {
        return sendData(data.getBytes());
    }
    
    /**
     * 关闭串口
     */
    public void closeSerialPort() {
        if (serialPortManager != null) {
            SerialPortLogUtil.i(TAG, "关闭串口");
            serialPortManager.closeSerialPort();
            serialPortManager = null;
        }
        openCallback = null;
        dataCallback = null;
    }
    
    /**
     * 检查串口是否已打开
     */
    public boolean isSerialPortOpened() {
        return serialPortManager != null && serialPortManager.isOpen();
    }
    
    // Getter和Setter方法
    public SimpleSerialPortManager setDatabits(int databits) {
        this.databits = databits;
        SerialPortLogUtil.d(TAG, "设置数据位: " + databits);
        return this;
    }
    
    public SimpleSerialPortManager setParity(int parity) {
        this.parity = parity;
        SerialPortLogUtil.d(TAG, "设置校验位: " + parity);
        return this;
    }
    
    public SimpleSerialPortManager setStopbits(int stopbits) {
        this.stopbits = stopbits;
        SerialPortLogUtil.d(TAG, "设置停止位: " + stopbits);
        return this;
    }
    
    public SimpleSerialPortManager setFlags(int flags) {
        this.flags = flags;
        SerialPortLogUtil.d(TAG, "设置标志位: " + flags);
        return this;
    }
    
    public int getDatabits() {
        return databits;
    }
    
    public int getParity() {
        return parity;
    }
    
    public int getStopbits() {
        return stopbits;
    }
    
    public int getFlags() {
        return flags;
    }
    
    public SerialConfig getSerialConfig() {
        return serialConfig;
    }
    
    /**
     * 粘包处理策略枚举
     */
    public enum StickyPacketStrategy {
        NO_PROCESSING,      // 不处理
        DELIMITER_BASED,    // 基于分隔符
        FIXED_LENGTH,       // 固定长度
        VARIABLE_LENGTH     // 可变长度
    }
    
    /**
     * 串口打开状态回调接口
     */
    public interface OnOpenSerialPortCallback {
        /**
         * 串口状态变化回调
         * @param success 是否成功
         * @param status 状态
         */
        void onStatusChanged(boolean success, SerialStatus status);
    }
    
    /**
     * 数据接收回调接口
     */
    public interface OnDataReceivedCallback {
        /**
         * 接收到数据
         * @param data 接收到的数据
         */
        void onDataReceived(byte[] data);
        
        /**
         * 数据发送完成
         * @param data 发送的数据
         */
        default void onDataSent(byte[] data) {
            // 默认空实现
        }
    }
    
    /**
     * 多串口管理 - 获取多串口管理器实例
     * @return MultiSerialPortManager实例
     */
    public static MultiSerialPortManager multi() {
        return MultiSerialPortManager.getInstance();
    }
    
    /**
     * 快速配置构建器
     */
    public static class QuickConfig {
        private int intervalSleep = 50;
        private boolean enableLog = true;
        private String logTag = "SerialPort";
        private StickyPacketStrategy stickyPacketStrategy = StickyPacketStrategy.NO_PROCESSING;
        private int maxPacketSize = 1024;
        private boolean autoReconnect = false;
        private int databits = 8;
        private int parity = 0;
        private int stopbits = 1;
        private int flags = 0;
        
        public QuickConfig setIntervalSleep(int intervalSleep) {
            this.intervalSleep = intervalSleep;
            return this;
        }
        
        public QuickConfig setEnableLog(boolean enableLog) {
            this.enableLog = enableLog;
            return this;
        }
        
        public QuickConfig setLogTag(String logTag) {
            this.logTag = logTag;
            return this;
        }
        
        public QuickConfig setStickyPacketStrategy(StickyPacketStrategy strategy) {
            this.stickyPacketStrategy = strategy;
            return this;
        }
        
        public QuickConfig setMaxPacketSize(int maxPacketSize) {
            this.maxPacketSize = maxPacketSize;
            return this;
        }
        
        public QuickConfig setAutoReconnect(boolean autoReconnect) {
            this.autoReconnect = autoReconnect;
            return this;
        }
        
        public QuickConfig setDatabits(int databits) {
            this.databits = databits;
            return this;
        }
        
        public QuickConfig setParity(int parity) {
            this.parity = parity;
            return this;
        }
        
        public QuickConfig setStopbits(int stopbits) {
            this.stopbits = stopbits;
            return this;
        }
        
        public QuickConfig setFlags(int flags) {
            this.flags = flags;
            return this;
        }
        
        /**
         * 应用配置并返回SimpleSerialPortManager实例
         */
        public SimpleSerialPortManager apply(Application application) {
            SerialConfig config = new SerialConfig.Builder()
                    .setIntervalSleep(intervalSleep)
                    .setEnableLogging(enableLog)
                    .setEnableStickyPacketProcessing(stickyPacketStrategy != StickyPacketStrategy.NO_PROCESSING)
                    .setMaxPacketSize(maxPacketSize)
                    .setAutoReconnect(autoReconnect)
                    .setDatabits(databits)
                    .setParity(parity)
                    .setStopbits(stopbits)
                    .setFlags(flags)
                    .build();

            SimpleSerialPortManager manager = SimpleSerialPortManager.getInstance()
                    .init(application, config)
                    .configureStickyPacket(stickyPacketStrategy);

            // 设置串口参数
            manager.setDatabits(databits)
                   .setParity(parity)
                   .setStopbits(stopbits)
                   .setFlags(flags);

            SerialPortLogUtil.i("QuickConfig", "快速配置完成 - 间隔: " + intervalSleep + "ms, 日志: " + enableLog + ", 策略: " + stickyPacketStrategy);
            return manager;
        }
    }
}