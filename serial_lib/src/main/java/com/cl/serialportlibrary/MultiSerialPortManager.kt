package com.cl.serialportlibrary;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import com.cl.serialportlibrary.enumerate.SerialPortEnum;
import com.cl.serialportlibrary.enumerate.SerialStatus;
import com.cl.serialportlibrary.listener.OnOpenSerialPortListener;
import com.cl.serialportlibrary.listener.OnSerialPortDataListener;
import com.cl.serialportlibrary.stick.AbsStickPackageHelper;
import com.cl.serialportlibrary.stick.BaseStickPackageHelper;
import com.cl.serialportlibrary.utils.SerialPortLogUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多串口管理器
 * 支持同时管理多个串口，每个串口可以有独立的配置
 * Author: cl
 * Date: 2023/10/26
 */
public class MultiSerialPortManager {
    
    private static final String TAG = "MultiSerialPortManager";
    private static MultiSerialPortManager instance;
    private Handler handler = new Handler(Looper.getMainLooper());
    
    // 串口管理器映射 <串口ID, SerialPortManager>
    private final Map<String, SerialPortManager> serialPortManagers = new ConcurrentHashMap<>();
    
    // 串口配置映射 <串口ID, SerialConfig>
    private final Map<String, SerialConfig> serialConfigs = new ConcurrentHashMap<>();
    
    // 回调映射 <串口ID, 回调接口>
    private final Map<String, OnSerialPortStatusCallback> statusCallbacks = new ConcurrentHashMap<>();
    private final Map<String, OnSerialPortDataCallback> dataCallbacks = new ConcurrentHashMap<>();
    
    // 串口枚举映射 <串口ID, SerialPortEnum>
    private final Map<String, SerialPortEnum> serialPortEnums = new ConcurrentHashMap<>();
    
    private MultiSerialPortManager() {}
    
    /**
     * 获取单例实例
     */
    public static MultiSerialPortManager getInstance() {
        if (instance == null) {
            synchronized (MultiSerialPortManager.class) {
                if (instance == null) {
                    instance = new MultiSerialPortManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 配置并打开串口
     * @param serialId 串口ID（自定义标识）
     * @param devicePath 设备路径
     * @param baudRate 波特率
     * @param config 串口配置
     * @param statusCallback 状态回调
     * @param dataCallback 数据回调
     * @return 是否打开成功
     */
    public boolean openSerialPort(String serialId, String devicePath, int baudRate, 
                                 SerialPortConfig config, OnSerialPortStatusCallback statusCallback, 
                                 OnSerialPortDataCallback dataCallback) {
        
        SerialPortLogUtil.printSeparator(TAG, "打开串口 " + serialId);
        SerialPortLogUtil.i(TAG, String.format("串口[%s] - 设备: %s, 波特率: %d", serialId, devicePath, baudRate));
        
        // 检查串口是否已经打开
        if (serialPortManagers.containsKey(serialId)) {
            SerialPortLogUtil.w(TAG, "串口[" + serialId + "]已经打开，先关闭旧连接");
            closeSerialPort(serialId);
        }
        
        // 创建串口配置
        SerialConfig serialConfig = new SerialConfig.Builder()
                .setEnableLogging(config.enableLogging)
                .setIntervalSleep(config.intervalSleep)
                .setDatabits(config.databits)
                .setParity(config.parity)
                .setStopbits(config.stopbits)
                .setFlags(config.flags)
                .setEnableStickyPacketProcessing(config.stickyPacketHelpers != null && config.stickyPacketHelpers.length > 0)
                .setStickyPacketHelpers(config.stickyPacketHelpers != null ? config.stickyPacketHelpers : new AbsStickPackageHelper[]{new BaseStickPackageHelper()})
                .build();
        
        // 保存配置和回调
        serialConfigs.put(serialId, serialConfig);
        if (statusCallback != null) statusCallbacks.put(serialId, statusCallback);
        if (dataCallback != null) dataCallbacks.put(serialId, dataCallback);
        
        // 分配串口枚举
        SerialPortEnum serialPortEnum = getAvailableSerialPortEnum();
        serialPortEnums.put(serialId, serialPortEnum);
        
        SerialPortLogUtil.printSerialConfig(TAG + "_" + serialId, config.databits, config.parity, config.stopbits, config.flags);
        if (config.stickyPacketHelpers != null) {
            SerialPortLogUtil.i(TAG, String.format("串口[%s] 配置了 %d 个粘包处理器", serialId, config.stickyPacketHelpers.length));
        }
        
        // 创建SerialPortManager
        SerialPortManager serialPortManager = new SerialPortManager(serialPortEnum);
        serialPortManager.setSerialConfig(serialConfig);
        
        // 设置监听器
        serialPortManager.setOnOpenSerialPortListener(new OnOpenSerialPortListener() {
            @Override
            public void openState(SerialPortEnum serialPortEnum, File device, SerialStatus status) {
                String logMessage = String.format("串口[%s] 状态变化: %s - %s", serialId, device.getPath(), status);
                if (status == SerialStatus.SUCCESS_OPENED) {
                    SerialPortLogUtil.i(TAG, logMessage);
                } else {
                    SerialPortLogUtil.e(TAG, logMessage);
                }
                
                handler.post(() -> {
                    OnSerialPortStatusCallback callback = statusCallbacks.get(serialId);
                    if (callback != null) {
                        callback.onStatusChanged(serialId, status == SerialStatus.SUCCESS_OPENED, status);
                    }
                });
            }
        });
        
        serialPortManager.setOnSerialPortDataListener(new OnSerialPortDataListener() {
            @Override
            public void onDataReceived(byte[] data, SerialPortEnum serialPortEnum) {
                SerialPortLogUtil.printData(TAG + "_" + serialId, "接收数据", data);
                handler.post(() -> {
                    OnSerialPortDataCallback callback = dataCallbacks.get(serialId);
                    if (callback != null) {
                        callback.onDataReceived(serialId, data);
                    }
                });
            }
            
            @Override
            public void onDataSent(byte[] data, SerialPortEnum serialPortEnum) {
                SerialPortLogUtil.printData(TAG + "_" + serialId, "发送数据", data);
                handler.post(() -> {
                    OnSerialPortDataCallback callback = dataCallbacks.get(serialId);
                    if (callback != null) {
                        callback.onDataSent(serialId, data);
                    }
                });
            }
        });
        
        // 打开串口
        boolean success = serialPortManager.openSerialPort(devicePath, baudRate);
        if (success) {
            serialPortManagers.put(serialId, serialPortManager);
            SerialPortLogUtil.i(TAG, "串口[" + serialId + "] 打开成功");
        } else {
            // 清理资源
            serialConfigs.remove(serialId);
            statusCallbacks.remove(serialId);
            dataCallbacks.remove(serialId);
            serialPortEnums.remove(serialId);
            SerialPortLogUtil.e(TAG, "串口[" + serialId + "] 打开失败");
        }
        
        return success;
    }
    
    /**
     * 简化的打开串口方法
     */
    public boolean openSerialPort(String serialId, String devicePath, int baudRate, 
                                 OnSerialPortDataCallback dataCallback) {
        SerialPortConfig config = new SerialPortConfig.Builder().build();
        return openSerialPort(serialId, devicePath, baudRate, config, null, dataCallback);
    }
    
    /**
     * 发送数据到指定串口
     * @param serialId 串口ID
     * @param data 数据
     * @return 是否发送成功
     */
    public boolean sendData(String serialId, byte[] data) {
        SerialPortManager manager = serialPortManagers.get(serialId);
        if (manager == null) {
            SerialPortLogUtil.e(TAG, "串口[" + serialId + "] 未打开，无法发送数据");
            return false;
        }
        
        if (data == null || data.length == 0) {
            SerialPortLogUtil.w(TAG, "串口[" + serialId + "] 尝试发送空数据");
            return false;
        }
        
        long startTime = System.currentTimeMillis();
        SerialPortLogUtil.printData(TAG + "_" + serialId, "准备发送", data);
        
        boolean result = manager.sendBytes(data);
        SerialPortLogUtil.printPerformance(TAG + "_" + serialId, "发送数据", startTime);
        
        if (!result) {
            SerialPortLogUtil.e(TAG, "串口[" + serialId + "] 数据发送失败");
        }
        
        return result;
    }
    
    /**
     * 发送字符串数据到指定串口
     */
    public boolean sendData(String serialId, String data) {
        return sendData(serialId, data.getBytes());
    }
    
    /**
     * 关闭指定串口
     * @param serialId 串口ID
     */
    public void closeSerialPort(String serialId) {
        SerialPortManager manager = serialPortManagers.remove(serialId);
        if (manager != null) {
            SerialPortLogUtil.i(TAG, "关闭串口[" + serialId + "]");
            manager.closeSerialPort();
        }
        
        // 清理相关资源
        serialConfigs.remove(serialId);
        statusCallbacks.remove(serialId);
        dataCallbacks.remove(serialId);
        serialPortEnums.remove(serialId);
    }
    
    /**
     * 关闭所有串口
     */
    public void closeAllSerialPorts() {
        SerialPortLogUtil.printSeparator(TAG, "关闭所有串口");
        List<String> serialIds = new ArrayList<>(serialPortManagers.keySet());
        for (String serialId : serialIds) {
            closeSerialPort(serialId);
        }
    }
    
    /**
     * 检查指定串口是否已打开
     */
    public boolean isSerialPortOpened(String serialId) {
        SerialPortManager manager = serialPortManagers.get(serialId);
        return manager != null && manager.isOpen();
    }
    
    /**
     * 获取所有已打开的串口ID
     */
    public List<String> getOpenedSerialPorts() {
        List<String> openedPorts = new ArrayList<>();
        for (Map.Entry<String, SerialPortManager> entry : serialPortManagers.entrySet()) {
            if (entry.getValue().isOpen()) {
                openedPorts.add(entry.getKey());
            }
        }
        return openedPorts;
    }
    
    /**
     * 获取指定串口的配置
     */
    public SerialConfig getSerialConfig(String serialId) {
        return serialConfigs.get(serialId);
    }
    
    /**
     * 更新指定串口的粘包处理器
     */
    public boolean updateStickyPacketHelpers(String serialId, AbsStickPackageHelper[] helpers) {
        SerialPortManager manager = serialPortManagers.get(serialId);
        SerialConfig config = serialConfigs.get(serialId);
        
        if (manager == null || config == null) {
            SerialPortLogUtil.e(TAG, "串口[" + serialId + "] 未打开，无法更新粘包处理器");
            return false;
        }
        
        config.setStickyPacketHelpers(helpers);
        List<AbsStickPackageHelper> helperList = new ArrayList<>();
        for (AbsStickPackageHelper helper : helpers) {
            helperList.add(helper);
        }
        manager.setStickPackageHelpers(helperList);
        
        SerialPortLogUtil.i(TAG, String.format("串口[%s] 更新粘包处理器，数量: %d", serialId, helpers.length));
        return true;
    }
    
    /**
     * 打印所有串口状态
     */
    public void printAllSerialStatus() {
        SerialPortLogUtil.printSeparator(TAG, "所有串口状态");
        if (serialPortManagers.isEmpty()) {
            SerialPortLogUtil.i(TAG, "当前没有打开的串口");
            return;
        }
        
        for (Map.Entry<String, SerialPortManager> entry : serialPortManagers.entrySet()) {
            String serialId = entry.getKey();
            SerialPortManager manager = entry.getValue();
            SerialConfig config = serialConfigs.get(serialId);
            
            SerialPortLogUtil.i(TAG, String.format("串口[%s] - 状态: %s", 
                serialId, manager.isOpen() ? "已打开" : "已关闭"));
            
            if (config != null) {
                SerialPortLogUtil.printSerialConfig(TAG + "_" + serialId, 
                    config.getDatabits(), config.getParity(), config.getStopbits(), config.getFlags());
            }
        }
    }
    
    /**
     * 获取可用的串口枚举
     */
    private SerialPortEnum getAvailableSerialPortEnum() {
        // 简单的策略：按顺序分配，最多支持6个串口
        SerialPortEnum[] enums = {
            SerialPortEnum.SERIAL_ONE,
            SerialPortEnum.SERIAL_TWO,
            SerialPortEnum.SERIAL_THREE,
            SerialPortEnum.SERIAL_FOUR,
            SerialPortEnum.SERIAL_FIVE,
            SerialPortEnum.SERIAL_SIX
        };
        
        for (SerialPortEnum serialPortEnum : enums) {
            boolean isUsed = false;
            for (SerialPortEnum usedEnum : serialPortEnums.values()) {
                if (usedEnum == serialPortEnum) {
                    isUsed = true;
                    break;
                }
            }
            if (!isUsed) {
                return serialPortEnum;
            }
        }
        
        // 如果所有枚举都被使用，返回第一个（可能会有冲突，但至少不会崩溃）
        SerialPortLogUtil.w(TAG, "所有串口枚举都已被使用，可能会有冲突");
        return SerialPortEnum.SERIAL_ONE;
    }
    
    /**
     * 串口状态回调接口
     */
    public interface OnSerialPortStatusCallback {
        /**
         * 串口状态变化
         * @param serialId 串口ID
         * @param success 是否成功
         * @param status 状态
         */
        void onStatusChanged(String serialId, boolean success, SerialStatus status);
    }
    
    /**
     * 串口数据回调接口
     */
    public interface OnSerialPortDataCallback {
        /**
         * 接收到数据
         * @param serialId 串口ID
         * @param data 数据
         */
        void onDataReceived(String serialId, byte[] data);
        
        /**
         * 数据发送完成
         * @param serialId 串口ID
         * @param data 数据
         */
        default void onDataSent(String serialId, byte[] data) {
            // 默认空实现
        }
    }
    
    /**
     * 串口配置类
     */
    public static class SerialPortConfig {
        private boolean enableLogging = true;
        private int intervalSleep = 50;
        private int databits = 8;
        private int parity = 0;
        private int stopbits = 1;
        private int flags = 0;
        private AbsStickPackageHelper[] stickyPacketHelpers;
        
        private SerialPortConfig(Builder builder) {
            this.enableLogging = builder.enableLogging;
            this.intervalSleep = builder.intervalSleep;
            this.databits = builder.databits;
            this.parity = builder.parity;
            this.stopbits = builder.stopbits;
            this.flags = builder.flags;
            this.stickyPacketHelpers = builder.stickyPacketHelpers;
        }
        
        public static class Builder {
            private boolean enableLogging = true;
            private int intervalSleep = 50;
            private int databits = 8;
            private int parity = 0;
            private int stopbits = 1;
            private int flags = 0;
            private AbsStickPackageHelper[] stickyPacketHelpers;
            
            public Builder setEnableLogging(boolean enableLogging) {
                this.enableLogging = enableLogging;
                return this;
            }
            
            public Builder setIntervalSleep(int intervalSleep) {
                this.intervalSleep = intervalSleep;
                return this;
            }
            
            public Builder setDatabits(int databits) {
                this.databits = databits;
                return this;
            }
            
            public Builder setParity(int parity) {
                this.parity = parity;
                return this;
            }
            
            public Builder setStopbits(int stopbits) {
                this.stopbits = stopbits;
                return this;
            }
            
            public Builder setFlags(int flags) {
                this.flags = flags;
                return this;
            }
            
            public Builder setStickyPacketHelpers(AbsStickPackageHelper... helpers) {
                this.stickyPacketHelpers = helpers;
                return this;
            }
            
            public SerialPortConfig build() {
                return new SerialPortConfig(this);
            }
        }
    }
}
