package com.kongqw.serialportlibrary.example;

import android.app.Application;
import android.util.Log;

import com.kongqw.serialportlibrary.MultiSerialPortManager;
import com.kongqw.serialportlibrary.enumerate.SerialStatus;
import com.kongqw.serialportlibrary.stick.AbsStickPackageHelper;
import com.kongqw.serialportlibrary.stick.BaseStickPackageHelper;
import com.kongqw.serialportlibrary.stick.SpecifiedStickPackageHelper;
import com.kongqw.serialportlibrary.stick.StaticLenStickPackageHelper;
import com.kongqw.serialportlibrary.stick.StickyPacketHelperFactory;

/**
 * 多串口使用示例
 * 展示如何同时管理多个串口，每个串口使用不同的粘包处理策略
 */
public class MultiSerialPortExample {
    
    private static final String TAG = "MultiSerialPortExample";
    
    /**
     * 示例1：基础多串口使用
     */
    public void basicMultiSerialExample(Application application) {
        MultiSerialPortManager manager = MultiSerialPortManager.getInstance();
        
        // 串口1：不需要粘包处理，用于简单的数据传输
        manager.openSerialPort("GPS", "/dev/ttyS1", 9600, 
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setDatabits(8)
                .setParity(0)
                .setStopbits(1)
                .setStickyPacketHelpers(new BaseStickPackageHelper()) // 不处理粘包
                .build(),
            // 状态回调
            (serialId, success, status) -> {
                Log.i(TAG, String.format("串口[%s] 状态: %s", serialId, success ? "打开成功" : "打开失败"));
            },
            // 数据回调
            new MultiSerialPortManager.OnSerialPortDataCallback() {
                @Override
                public void onDataReceived(String serialId, byte[] data) {
                    String gpsData = new String(data);
                    Log.i(TAG, "GPS数据: " + gpsData);
                    // 处理GPS数据
                }
            });
        
        // 串口2：需要按换行符分包，用于文本协议
        manager.openSerialPort("SENSOR", "/dev/ttyS2", 115200,
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setDatabits(8)
                .setParity(0)
                .setStopbits(1)
                .setStickyPacketHelpers(new SpecifiedStickPackageHelper("\n")) // 按换行符分包
                .build(),
            null, // 不需要状态回调
            new MultiSerialPortManager.OnSerialPortDataCallback() {
                @Override
                public void onDataReceived(String serialId, byte[] data) {
                    String sensorData = new String(data).trim();
                    Log.i(TAG, "传感器数据: " + sensorData);
                    // 处理传感器数据
                }
            });
        
        // 发送数据到不同串口
        manager.sendData("GPS", "AT+GPS\r\n");
        manager.sendData("SENSOR", "READ_TEMP\n");
    }
    
    /**
     * 示例2：复杂的多串口场景
     */
    public void advancedMultiSerialExample(Application application) {
        MultiSerialPortManager manager = MultiSerialPortManager.getInstance();
        
        // 串口1：Modbus RTU协议，固定长度包
        manager.openSerialPort("MODBUS", "/dev/ttyS3", 9600,
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setDatabits(8)
                .setParity(2) // 偶校验
                .setStopbits(1)
                .setStickyPacketHelpers(new StaticLenStickPackageHelper(8)) // 固定8字节
                .build(),
            (serialId, success, status) -> {
                if (success) {
                    Log.i(TAG, "Modbus串口打开成功");
                    // 发送读取寄存器命令
                    byte[] readCmd = {0x01, 0x03, 0x00, 0x00, 0x00, 0x01, (byte)0x84, 0x0A};
                    manager.sendData("MODBUS", readCmd);
                }
            },
            new MultiSerialPortManager.OnSerialPortDataCallback() {
                @Override
                public void onDataReceived(String serialId, byte[] data) {
                    Log.i(TAG, "Modbus响应: " + bytesToHex(data));
                    // 解析Modbus响应
                    parseModbusResponse(data);
                }
            });
        
        // 串口2：自定义协议，可变长度包
        manager.openSerialPort("CUSTOM", "/dev/ttyS4", 115200,
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setDatabits(8)
                .setParity(0)
                .setStopbits(1)
                .setStickyPacketHelpers(
                    StickyPacketHelperFactory.createVariableLength(
                        java.nio.ByteOrder.BIG_ENDIAN, 2, 2, 12)) // 可变长度包
                .build(),
            null,
            new MultiSerialPortManager.OnSerialPortDataCallback() {
                @Override
                public void onDataReceived(String serialId, byte[] data) {
                    Log.i(TAG, "自定义协议数据: " + bytesToHex(data));
                    // 处理自定义协议数据
                    parseCustomProtocol(data);
                }
            });
        
        // 串口3：AT命令，多种分隔符
        manager.openSerialPort("MODEM", "/dev/ttyS5", 115200,
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setDatabits(8)
                .setParity(0)
                .setStopbits(1)
                .setStickyPacketHelpers(
                    StickyPacketHelperFactory.Common.createATCommand()) // AT命令分包
                .build(),
            null,
            new MultiSerialPortManager.OnSerialPortDataCallback() {
                @Override
                public void onDataReceived(String serialId, byte[] data) {
                    String response = new String(data).trim();
                    Log.i(TAG, "AT响应: " + response);
                    // 处理AT命令响应
                    parseATResponse(response);
                }
            });
    }
    
    /**
     * 示例3：动态管理串口
     */
    public void dynamicSerialManagement() {
        MultiSerialPortManager manager = MultiSerialPortManager.getInstance();
        
        // 创建一个通用的数据回调
        MultiSerialPortManager.OnSerialPortDataCallback commonCallback = 
            new MultiSerialPortManager.OnSerialPortDataCallback() {
                @Override
                public void onDataReceived(String serialId, byte[] data) {
                    Log.i(TAG, String.format("串口[%s] 收到数据: %s", serialId, new String(data)));
                }
                
                @Override
                public void onDataSent(String serialId, byte[] data) {
                    Log.d(TAG, String.format("串口[%s] 发送数据: %s", serialId, new String(data)));
                }
            };
        
        // 批量打开串口
        String[] devices = {"/dev/ttyS1", "/dev/ttyS2", "/dev/ttyS3"};
        int[] baudRates = {9600, 115200, 57600};
        
        for (int i = 0; i < devices.length; i++) {
            String serialId = "SERIAL_" + (i + 1);
            
            // 根据不同需求配置不同的粘包处理
            MultiSerialPortManager.SerialPortConfig config;
            switch (i) {
                case 0: // 第一个串口不需要粘包处理
                    config = new MultiSerialPortManager.SerialPortConfig.Builder()
                        .setStickyPacketHelpers(new BaseStickPackageHelper())
                        .build();
                    break;
                case 1: // 第二个串口需要换行符分包
                    config = new MultiSerialPortManager.SerialPortConfig.Builder()
                        .setStickyPacketHelpers(new SpecifiedStickPackageHelper("\n"))
                        .build();
                    break;
                case 2: // 第三个串口需要固定长度分包
                    config = new MultiSerialPortManager.SerialPortConfig.Builder()
                        .setStickyPacketHelpers(new StaticLenStickPackageHelper(16))
                        .build();
                    break;
                default:
                    config = new MultiSerialPortManager.SerialPortConfig.Builder().build();
                    break;
            }
            
            manager.openSerialPort(serialId, devices[i], baudRates[i], config, null, commonCallback);
        }
        
        // 打印所有串口状态
        manager.printAllSerialStatus();
        
        // 向所有串口发送测试数据
        for (String serialId : manager.getOpenedSerialPorts()) {
            manager.sendData(serialId, "Hello from " + serialId + "\n");
        }
        
        // 动态更新某个串口的粘包处理器
        manager.updateStickyPacketHelpers("SERIAL_1", 
            new AbsStickPackageHelper[]{new SpecifiedStickPackageHelper("END")});
    }
    
    /**
     * 示例4：串口数据路由
     */
    public void serialDataRouting() {
        MultiSerialPortManager manager = MultiSerialPortManager.getInstance();
        
        // 主控制串口：接收外部命令
        manager.openSerialPort("MAIN_CTRL", "/dev/ttyS1", 115200,
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setStickyPacketHelpers(new SpecifiedStickPackageHelper("\r\n"))
                .build(),
            null,
            new MultiSerialPortManager.OnSerialPortDataCallback() {
                @Override
                public void onDataReceived(String serialId, byte[] data) {
                    String command = new String(data).trim();
                    Log.i(TAG, "主控制命令: " + command);
                    
                    // 根据命令路由到不同的串口
                    if (command.startsWith("GPS:")) {
                        String gpsCmd = command.substring(4);
                        manager.sendData("GPS_MODULE", gpsCmd + "\r\n");
                    } else if (command.startsWith("SENSOR:")) {
                        String sensorCmd = command.substring(7);
                        manager.sendData("SENSOR_MODULE", sensorCmd + "\n");
                    }
                }
            });
        
        // GPS模块串口
        manager.openSerialPort("GPS_MODULE", "/dev/ttyS2", 9600,
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setStickyPacketHelpers(new SpecifiedStickPackageHelper("\r\n"))
                .build(),
            null,
            new MultiSerialPortManager.OnSerialPortDataCallback() {
                @Override
                public void onDataReceived(String serialId, byte[] data) {
                    String gpsResponse = new String(data).trim();
                    Log.i(TAG, "GPS响应: " + gpsResponse);
                    // 将GPS响应转发到主控制串口
                    manager.sendData("MAIN_CTRL", "GPS_RESP:" + gpsResponse + "\r\n");
                }
            });
        
        // 传感器模块串口
        manager.openSerialPort("SENSOR_MODULE", "/dev/ttyS3", 115200,
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setStickyPacketHelpers(new SpecifiedStickPackageHelper("\n"))
                .build(),
            null,
            new MultiSerialPortManager.OnSerialPortDataCallback() {
                @Override
                public void onDataReceived(String serialId, byte[] data) {
                    String sensorResponse = new String(data).trim();
                    Log.i(TAG, "传感器响应: " + sensorResponse);
                    // 将传感器响应转发到主控制串口
                    manager.sendData("MAIN_CTRL", "SENSOR_RESP:" + sensorResponse + "\r\n");
                }
            });
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        // 关闭所有串口
        MultiSerialPortManager.getInstance().closeAllSerialPorts();
        Log.i(TAG, "所有串口已关闭");
    }
    
    // 辅助方法
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
    
    private void parseModbusResponse(byte[] data) {
        // Modbus响应解析逻辑
        if (data.length >= 3) {
            int slaveId = data[0] & 0xFF;
            int functionCode = data[1] & 0xFF;
            Log.i(TAG, String.format("Modbus - 从机ID: %d, 功能码: %d", slaveId, functionCode));
        }
    }
    
    private void parseCustomProtocol(byte[] data) {
        // 自定义协议解析逻辑
        Log.i(TAG, "解析自定义协议数据，长度: " + data.length);
    }
    
    private void parseATResponse(String response) {
        // AT命令响应解析逻辑
        if (response.equals("OK")) {
            Log.i(TAG, "AT命令执行成功");
        } else if (response.equals("ERROR")) {
            Log.e(TAG, "AT命令执行失败");
        } else if (response.startsWith("+")) {
            Log.i(TAG, "AT命令数据响应: " + response);
        }
    }
}
