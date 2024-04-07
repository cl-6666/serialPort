package com.kongqw.serialportlibrary;

import androidx.annotation.NonNull;

import com.cl.log.XLogConfig;

/**
 * name：cl
 * date：2023/10/26
 * desc：串口配置类
 */
public class SerialConfig {

    //配置日志相关参数
    private XLogConfig xLogConfig;
    //串口接收间隔时间
    private int intervalSleep;
    //串口重连机制
    private boolean serialPortReconnection;
    int flags;
    int databits;
    int stopbits;
    int parity;


    public SerialConfig(Builder builder) {
        this.xLogConfig=builder.xLogConfig;
        this.intervalSleep=builder.intervalSleep;
        this.serialPortReconnection=builder.serialPortReconnection;
        this.flags=builder.flags;
        this.databits=builder.databits;
        this.stopbits=builder.stopbits;
        this.parity=builder.parity;
    }


    public XLogConfig getxLogConfig() {
        return xLogConfig;
    }

    public int getIntervalSleep() {
        return intervalSleep;
    }

    public boolean isSerialPortReconnection() {
        return serialPortReconnection;
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

    public static class Builder {

        //配置日志相关参数
        private XLogConfig xLogConfig;
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

        public Builder setXLogConfig(@NonNull XLogConfig config) {
            xLogConfig = config;
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
            flags = flags;
            return this;
        }

        public Builder setDatabits(int databits) {
            databits = databits;
            return this;
        }

        public Builder setStopbits(int stopbits) {
            stopbits = stopbits;
            return this;
        }

        public Builder setParity(int parity) {
            parity = parity;
            return this;
        }

        public SerialConfig build() {
            return new SerialConfig(this);
        }
    }
}
