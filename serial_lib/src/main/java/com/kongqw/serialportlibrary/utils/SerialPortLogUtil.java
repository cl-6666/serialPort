package com.kongqw.serialportlibrary.utils;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 串口日志工具类
 * 替代XLog的增强实现，包含更详细的调试信息
 * 提供时间戳、调用位置、数据格式化等功能
 */
public class SerialPortLogUtil {
    
    private static final String DEFAULT_TAG = "SerialPort";
    private static boolean isDebugEnabled = true;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
    
    /**
     * 设置是否启用调试日志
     * @param enabled 是否启用
     */
    public static void setDebugEnabled(boolean enabled) {
        isDebugEnabled = enabled;
        if (enabled) {
            Log.i(DEFAULT_TAG, "================== 串口日志系统已启用 ==================");
        }
    }
    
    /**
     * 获取是否启用调试日志
     */
    public static boolean isDebugEnabled() {
        return isDebugEnabled;
    }
    
    /**
     * 获取当前时间戳
     */
    private static String getTimeStamp() {
        return DATE_FORMAT.format(new Date());
    }
    
    /**
     * 获取调用者信息
     */
    private static String getCallerInfo() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // 跳过前面的调用栈，找到真正的调用者
        for (int i = 4; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            String className = element.getClassName();
            if (!className.equals(SerialPortLogUtil.class.getName())) {
                String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
                return String.format("[%s.%s:%d]", simpleClassName, element.getMethodName(), element.getLineNumber());
            }
        }
        return "[Unknown]";
    }
    
    /**
     * 格式化消息
     */
    private static String formatMessage(String message) {
        return String.format("%s %s %s", getTimeStamp(), getCallerInfo(), message);
    }
    
    /**
     * 输出调试日志
     * @param tag 标签
     * @param message 消息
     */
    public static void d(String tag, String message) {
        if (isDebugEnabled) {
            Log.d(tag != null ? tag : DEFAULT_TAG, formatMessage(message));
        }
    }
    
    /**
     * 输出调试日志（使用默认标签）
     * @param message 消息
     */
    public static void d(String message) {
        d(DEFAULT_TAG, message);
    }
    
    /**
     * 输出信息日志
     * @param tag 标签
     * @param message 消息
     */
    public static void i(String tag, String message) {
        if (isDebugEnabled) {
            Log.i(tag != null ? tag : DEFAULT_TAG, formatMessage(message));
        }
    }
    
    /**
     * 输出信息日志（使用默认标签）
     * @param message 消息
     */
    public static void i(String message) {
        i(DEFAULT_TAG, message);
    }
    
    /**
     * 输出警告日志
     * @param tag 标签
     * @param message 消息
     */
    public static void w(String tag, String message) {
        if (isDebugEnabled) {
            Log.w(tag != null ? tag : DEFAULT_TAG, formatMessage(message));
        }
    }
    
    /**
     * 输出警告日志（使用默认标签）
     * @param message 消息
     */
    public static void w(String message) {
        w(DEFAULT_TAG, message);
    }
    
    /**
     * 输出错误日志
     * @param tag 标签
     * @param message 消息
     */
    public static void e(String tag, String message) {
        // 错误日志始终输出，不受isDebugEnabled控制
        Log.e(tag != null ? tag : DEFAULT_TAG, formatMessage(message));
    }
    
    /**
     * 输出错误日志（使用默认标签）
     * @param message 消息
     */
    public static void e(String message) {
        e(DEFAULT_TAG, message);
    }
    
    /**
     * 输出错误日志（带异常）
     * @param tag 标签
     * @param message 消息
     * @param throwable 异常
     */
    public static void e(String tag, String message, Throwable throwable) {
        // 错误日志始终输出，不受isDebugEnabled控制
        Log.e(tag != null ? tag : DEFAULT_TAG, formatMessage(message), throwable);
    }
    
    /**
     * 输出错误日志（带异常，使用默认标签）
     * @param message 消息
     * @param throwable 异常
     */
    public static void e(String message, Throwable throwable) {
        e(DEFAULT_TAG, message, throwable);
    }
    
    /**
     * 打印数据（专门用于串口数据调试）
     * @param tag 标签
     * @param prefix 前缀
     * @param data 数据
     */
    public static void printData(String tag, String prefix, byte[] data) {
        if (!isDebugEnabled || data == null) return;
        
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append(" [").append(data.length).append(" bytes]: ");
        
        // 十六进制格式
        sb.append("HEX[");
        for (int i = 0; i < Math.min(data.length, 32); i++) { // 最多显示32字节
            if (i > 0) sb.append(" ");
            sb.append(String.format("%02X", data[i] & 0xFF));
        }
        if (data.length > 32) {
            sb.append("...");
        }
        sb.append("] ");
        
        // ASCII格式（可打印字符）
        sb.append("ASCII[");
        for (int i = 0; i < Math.min(data.length, 32); i++) {
            byte b = data[i];
            if (b >= 32 && b < 127) {
                sb.append((char) b);
            } else {
                sb.append('.');
            }
        }
        if (data.length > 32) {
            sb.append("...");
        }
        sb.append("]");
        
        d(tag, sb.toString());
    }
    
    /**
     * 打印数据（使用默认标签）
     * @param prefix 前缀
     * @param data 数据
     */
    public static void printData(String prefix, byte[] data) {
        printData(DEFAULT_TAG, prefix, data);
    }
    
    /**
     * 打印串口状态信息
     * @param tag 标签
     * @param devicePath 设备路径
     * @param baudRate 波特率
     * @param isOpen 是否打开
     */
    public static void printSerialStatus(String tag, String devicePath, int baudRate, boolean isOpen) {
        i(tag, String.format("串口状态 - 设备: %s, 波特率: %d, 状态: %s", 
            devicePath, baudRate, isOpen ? "已打开" : "已关闭"));
    }
    
    /**
     * 打印串口配置信息
     * @param tag 标签
     * @param databits 数据位
     * @param parity 校验位
     * @param stopbits 停止位
     * @param flags 标志位
     */
    public static void printSerialConfig(String tag, int databits, int parity, int stopbits, int flags) {
        String parityStr;
        switch (parity) {
            case 0: parityStr = "无校验"; break;
            case 1: parityStr = "奇校验"; break;
            case 2: parityStr = "偶校验"; break;
            default: parityStr = "未知(" + parity + ")"; break;
        }
        
        i(tag, String.format("串口配置 - 数据位: %d, 校验位: %s, 停止位: %d, 标志位: 0x%X", 
            databits, parityStr, stopbits, flags));
    }
    
    /**
     * 打印性能信息
     * @param tag 标签
     * @param operation 操作名称
     * @param startTime 开始时间
     */
    public static void printPerformance(String tag, String operation, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        d(tag, String.format("性能统计 - %s 耗时: %dms", operation, duration));
    }
    
    /**
     * 打印分隔线
     * @param tag 标签
     * @param title 标题
     */
    public static void printSeparator(String tag, String title) {
        if (isDebugEnabled) {
            String separator = "==================== " + title + " ====================";
            i(tag, separator);
        }
    }
    
    /**
     * 打印分隔线（使用默认标签）
     * @param title 标题
     */
    public static void printSeparator(String title) {
        printSeparator(DEFAULT_TAG, title);
    }
}