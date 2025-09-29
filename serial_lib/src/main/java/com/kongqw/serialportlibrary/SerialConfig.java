package com.kongqw.serialportlibrary;

import com.kongqw.serialportlibrary.stick.AbsStickPackageHelper;
import com.kongqw.serialportlibrary.stick.BaseStickPackageHelper;

/**
 * name：cl
 * date：2023/10/26
 * desc：串口配置类
 */
public class SerialConfig {

    //配置日志相关参数
    private boolean enableLogging;
    //串口接收间隔时间
    private int intervalSleep;
    //串口重连机制
    private boolean serialPortReconnection;
    int flags;
    int databits;
    int stopbits;
    int parity;
    
    // 黏包处理相关配置
    private boolean enableStickyPacketProcessing;
    private int maxPacketSize;
    private int packetTimeout;
    private AbsStickPackageHelper[] stickyPacketHelpers;
    private boolean autoReconnect;
    private int reconnectInterval;
    private int maxReconnectAttempts;


    public SerialConfig(Builder builder) {
        this.enableLogging=builder.enableLogging;
        this.intervalSleep=builder.intervalSleep;
        this.serialPortReconnection=builder.serialPortReconnection;
        this.flags=builder.flags;
        this.databits=builder.databits;
        this.stopbits=builder.stopbits;
        this.parity=builder.parity;
        this.enableStickyPacketProcessing=builder.enableStickyPacketProcessing;
        this.maxPacketSize=builder.maxPacketSize;
        this.packetTimeout=builder.packetTimeout;
        this.stickyPacketHelpers=builder.stickyPacketHelpers;
        this.autoReconnect=builder.autoReconnect;
        this.reconnectInterval=builder.reconnectInterval;
        this.maxReconnectAttempts=builder.maxReconnectAttempts;
    }


    public boolean isEnableLogging() {
        return enableLogging;
    }
    
    public void setEnableLogging(boolean enableLogging) {
        this.enableLogging = enableLogging;
    }

    public int getIntervalSleep() {
        return intervalSleep;
    }

    public boolean isSerialPortReconnection() {
        return serialPortReconnection;
    }

    public void setIntervalSleep(int intervalSleep) {
        this.intervalSleep = intervalSleep;
    }

    public int getFlags() {
        return flags;
    }

    public int getDatabits() {
        return databits;
    }

    public int getStopbits() {
        return stopbits;
    }

    public int getParity() {
        return parity;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public void setDatabits(int databits) {
        this.databits = databits;
    }

    public void setStopbits(int stopbits) {
        this.stopbits = stopbits;
    }

    public void setParity(int parity) {
        this.parity = parity;
    }

    public boolean isEnableStickyPacketProcessing() {
        return enableStickyPacketProcessing;
    }

    public void setEnableStickyPacketProcessing(boolean enableStickyPacketProcessing) {
        this.enableStickyPacketProcessing = enableStickyPacketProcessing;
    }

    public int getMaxPacketSize() {
        return maxPacketSize;
    }

    public void setMaxPacketSize(int maxPacketSize) {
        this.maxPacketSize = maxPacketSize;
    }

    public int getPacketTimeout() {
        return packetTimeout;
    }

    public void setPacketTimeout(int packetTimeout) {
        this.packetTimeout = packetTimeout;
    }

    public AbsStickPackageHelper[] getStickyPacketHelpers() {
        return stickyPacketHelpers;
    }

    public void setStickyPacketHelpers(AbsStickPackageHelper[] stickyPacketHelpers) {
        this.stickyPacketHelpers = stickyPacketHelpers;
    }

    public boolean isAutoReconnect() {
        return autoReconnect;
    }

    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }

    public int getReconnectInterval() {
        return reconnectInterval;
    }

    public void setReconnectInterval(int reconnectInterval) {
        this.reconnectInterval = reconnectInterval;
    }

    public int getMaxReconnectAttempts() {
        return maxReconnectAttempts;
    }

    public void setMaxReconnectAttempts(int maxReconnectAttempts) {
        this.maxReconnectAttempts = maxReconnectAttempts;
    }

    public static class Builder {

        //配置日志相关参数
        private boolean enableLogging = true;
        //串口接收间隔时间
        private int intervalSleep = 50;
        //串口重连机制
        private boolean serialPortReconnection = false;
        // 标志位
        int flags = 0;
        // 数据位
        int databits = 8;
        // 停止位
        int stopbits = 1;
        // 校验位：0 表示无校验位，1 表示奇校验，2 表示偶校验
        int parity = 0;
        
        // 黏包处理相关配置
        private boolean enableStickyPacketProcessing = true;
        private int maxPacketSize = 1024;
        private int packetTimeout = 1000;
        private AbsStickPackageHelper[] stickyPacketHelpers = {new BaseStickPackageHelper()};
        private boolean autoReconnect = false;
        private int reconnectInterval = 5000;
        private int maxReconnectAttempts = 3;

        public Builder setEnableLogging(boolean enableLogging) {
            this.enableLogging = enableLogging;
            return this;
        }

        public Builder setIntervalSleep(int sleep) {
            intervalSleep = sleep;
            return this;
        }

        public Builder setSerialPortReconnection(boolean serialReconnection) {
            serialPortReconnection = serialReconnection;
            return this;
        }

        public Builder setFlags(int flags) {
            this.flags = flags;
            return this;
        }

        public Builder setDatabits(int databits) {
            this.databits = databits;
            return this;
        }

        public Builder setStopbits(int stopbits) {
            this.stopbits = stopbits;
            return this;
        }

        public Builder setParity(int parity) {
            this.parity = parity;
            return this;
        }

        public Builder setEnableStickyPacketProcessing(boolean enableStickyPacketProcessing) {
            this.enableStickyPacketProcessing = enableStickyPacketProcessing;
            return this;
        }

        public Builder setMaxPacketSize(int maxPacketSize) {
            this.maxPacketSize = maxPacketSize;
            return this;
        }

        public Builder setPacketTimeout(int packetTimeout) {
            this.packetTimeout = packetTimeout;
            return this;
        }

        public Builder setStickyPacketHelpers(AbsStickPackageHelper... stickyPacketHelpers) {
            this.stickyPacketHelpers = stickyPacketHelpers;
            return this;
        }

        public Builder setAutoReconnect(boolean autoReconnect) {
            this.autoReconnect = autoReconnect;
            return this;
        }

        public Builder setReconnectInterval(int reconnectInterval) {
            this.reconnectInterval = reconnectInterval;
            return this;
        }

        public Builder setMaxReconnectAttempts(int maxReconnectAttempts) {
            this.maxReconnectAttempts = maxReconnectAttempts;
            return this;
        }

        public SerialConfig build() {
            return new SerialConfig(this);
        }
    }
}
