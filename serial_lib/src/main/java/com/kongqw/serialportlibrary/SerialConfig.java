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


    public SerialConfig(Builder builder) {
        this.xLogConfig=builder.xLogConfig;
        this.intervalSleep=builder.intervalSleep;
        this.serialPortReconnection=builder.serialPortReconnection;
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

    public static class Builder {

        //配置日志相关参数
        private XLogConfig xLogConfig;
        //串口接收间隔时间
        private int intervalSleep = 100;
        //串口重连机制
        private boolean serialPortReconnection = false;

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

        public SerialConfig build() {
            return new SerialConfig(this);
        }
    }


}
