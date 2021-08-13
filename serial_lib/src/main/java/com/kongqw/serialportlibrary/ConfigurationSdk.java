package com.kongqw.serialportlibrary;

import androidx.annotation.IntRange;

import java.io.File;

/**
 * 项目：inspection
 * 作者：Arry
 * 创建日期：2021/8/9
 * 描述：   sdk参数配置
 * 修订历史：
 */
public class ConfigurationSdk {

    private final File device;
    private final int baudRate;

    private final byte[] msgHead;
    private final byte[] msgTail;
    //默认配置
    private final boolean sDebug;
    private final boolean sIncludeThread;
    private final String sLogType;


    public ConfigurationSdk(ConfigurationBuilder configurationBuilder) {
        this.device = configurationBuilder.device;
        this.baudRate = configurationBuilder.baudRate;
        this.msgHead = configurationBuilder.msgHead;
        this.msgTail = configurationBuilder.msgTail;
        this.sDebug = configurationBuilder.sDebug;
        this.sIncludeThread = configurationBuilder.sIncludeThread;
        this.sLogType = configurationBuilder.sLogType;
    }

    public File getDevice() {
        return device;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public boolean issDebug() {
        return sDebug;
    }

    public boolean issIncludeThread() {
        return sIncludeThread;
    }

    public String getsLogType() {
        return sLogType;
    }

    public byte[] getMsgHead() {
        return msgHead;
    }

    public byte[] getMsgTail() {
        return msgTail;
    }

    public static class ConfigurationBuilder {
        private final File device;
        private final int baudRate;
        private byte[] msgHead;
        private byte[] msgTail;
        private boolean sDebug = false;
        private boolean sIncludeThread = false;
        private String sLogType = "inspection";

        public ConfigurationBuilder(File device, int baudRate) {
            this.device = device;
            this.baudRate = baudRate;
        }

        public ConfigurationBuilder log(String sLogType, boolean sDebug, boolean sIncludeThread) {
            this.sLogType = sLogType;
            this.sDebug = sDebug;
            this.sIncludeThread = sIncludeThread;
            return this;
        }
        public ConfigurationBuilder msgHead(byte[] msgHead) {
            this.msgHead = msgHead;
            return this;
        }

        public ConfigurationBuilder msgTail(byte[] msgTail) {
            this.msgTail = msgTail;
            return this;
        }

        public ConfigurationSdk build() {
            return new ConfigurationSdk(this);
        }
    }

}
