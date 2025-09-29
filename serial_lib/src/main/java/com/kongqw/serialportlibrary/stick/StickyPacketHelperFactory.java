package com.kongqw.serialportlibrary.stick;

import java.nio.charset.StandardCharsets;

/**
 * 黏包处理器工厂类
 * 提供常用的黏包处理器配置
 * Author: cl
 * Date: 2023/10/26
 */
public class StickyPacketHelperFactory {
    
    /**
     * 创建无黏包处理的处理器
     */
    public static AbsStickPackageHelper createNoProcessing() {
        return new BaseStickPackageHelper();
    }
    
    /**
     * 创建固定长度的黏包处理器
     * @param length 数据包固定长度
     */
    public static AbsStickPackageHelper createFixedLength(int length) {
        return new StaticLenStickPackageHelper(length);
    }
    
    /**
     * 创建基于分隔符的黏包处理器
     * @param delimiter 分隔符字符串
     */
    public static AbsStickPackageHelper createDelimiterBased(String delimiter) {
        byte[] delimiterBytes = delimiter.getBytes(StandardCharsets.UTF_8);
        return new SpecifiedStickPackageHelper(new byte[0], delimiterBytes);
    }
    
    /**
     * 创建基于开始和结束标识的黏包处理器
     * @param startMarker 开始标识
     * @param endMarker 结束标识
     */
    public static AbsStickPackageHelper createMarkerBased(String startMarker, String endMarker) {
        byte[] startBytes = startMarker.getBytes(StandardCharsets.UTF_8);
        byte[] endBytes = endMarker.getBytes(StandardCharsets.UTF_8);
        return new SpecifiedStickPackageHelper(startBytes, endBytes);
    }
    
    /**
     * 创建基于开始和结束标识的黏包处理器 (字节版本)
     * @param startMarker 开始标识字节数组
     * @param endMarker 结束标识字节数组
     */
    public static AbsStickPackageHelper createMarkerBased(byte[] startMarker, byte[] endMarker) {
        return new SpecifiedStickPackageHelper(startMarker, endMarker);
    }
    
    /**
     * 创建变长黏包处理器
     * @param byteOrder 字节序
     * @param lenSize 长度字段大小
     * @param lenIndex 长度字段位置
     * @param offset 偏移量
     */
    public static AbsStickPackageHelper createVariableLength(java.nio.ByteOrder byteOrder, int lenSize, int lenIndex, int offset) {
        return new VariableLenStickPackageHelper(byteOrder, lenSize, lenIndex, offset);
    }
    
    /**
     * 创建默认变长黏包处理器
     * 默认配置：大端字节序，2字节长度字段，位置在索引2，偏移12字节
     */
    public static AbsStickPackageHelper createVariableLength() {
        return new VariableLenStickPackageHelper(java.nio.ByteOrder.BIG_ENDIAN, 2, 2, 12);
    }
    
    /**
     * 创建自定义超时的基础处理器
     * @param timeout 超时时间（毫秒）
     */
    public static AbsStickPackageHelper createTimeoutBased(int timeout) {
        return new TimeoutStickPackageHelper(timeout);
    }
    
    /**
     * 创建组合式黏包处理器
     * @param primaryHelper 主要处理器
     * @param fallbackHelper 备用处理器
     */
    public static AbsStickPackageHelper createComposite(AbsStickPackageHelper primaryHelper, 
                                                       AbsStickPackageHelper fallbackHelper) {
        return new CompositeStickPackageHelper(primaryHelper, fallbackHelper);
    }
    
    /**
     * 常用协议的快速创建方法
     */
    public static class Common {
        
        /**
         * AT指令协议（以\r\n结尾）
         */
        public static AbsStickPackageHelper createATCommand() {
            return createDelimiterBased("\r\n");
        }
        
        /**
         * JSON协议（以换行符结尾）
         */
        public static AbsStickPackageHelper createJsonLine() {
            return createDelimiterBased("\n");
        }
        
        /**
         * Modbus RTU协议（固定长度，通常8字节）
         */
        public static AbsStickPackageHelper createModbusRTU() {
            return createFixedLength(8);
        }
        
        /**
         * 自定义协议（STX/ETX包围）
         */
        public static AbsStickPackageHelper createSTXETX() {
            return createMarkerBased(new byte[]{0x02}, new byte[]{0x03}); // STX, ETX
        }
        
        /**
         * 自定义协议（SOH/EOT包围）
         */
        public static AbsStickPackageHelper createSOHEOT() {
            return createMarkerBased(new byte[]{0x01}, new byte[]{0x04}); // SOH, EOT
        }
    }
}
