package com.cl.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.kongqw.serialportlibrary.MultiSerialPortManager;
import com.kongqw.serialportlibrary.SimpleSerialPortManager;
import com.kongqw.serialportlibrary.enumerate.SerialStatus;
import com.kongqw.serialportlibrary.stick.AbsStickPackageHelper;
import com.kongqw.serialportlibrary.stick.BaseStickPackageHelper;
import com.kongqw.serialportlibrary.stick.SpecifiedStickPackageHelper;
import com.kongqw.serialportlibrary.stick.StaticLenStickPackageHelper;

/**
 * 多串口演示Activity
 * 展示如何同时管理多个串口，每个串口使用不同的粘包策略
 */
public class MultiSerialPortActivity extends AppCompatActivity {
    
    private static final String TAG = "MultiSerialPortActivity";
    
    private TextView statusText;
    private Button openBtn, closeBtn, sendBtn;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_serial);
        
        initViews();
        setupMultiSerial();
    }
    
    private void initViews() {
        statusText = findViewById(R.id.tv_status);
        openBtn = findViewById(R.id.btn_open);
        closeBtn = findViewById(R.id.btn_close);
        sendBtn = findViewById(R.id.btn_send);
        
        openBtn.setOnClickListener(v -> openMultiSerialPorts());
        closeBtn.setOnClickListener(v -> closeMultiSerialPorts());
        sendBtn.setOnClickListener(v -> sendTestData());
    }
    
    private void setupMultiSerial() {
        updateStatus("多串口管理器初始化完成");
    }
    
    /**
     * 打开多个串口，每个串口配置不同的粘包处理
     */
    private void openMultiSerialPorts() {
        MultiSerialPortManager manager = SimpleSerialPortManager.multi();
        
        updateStatus("开始打开多个串口...");
        
        // 串口1：GPS模块，不需要粘包处理
        manager.openSerialPort("GPS", "/dev/ttyS1", 9600,
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setDatabits(8)
                .setParity(0)
                .setStopbits(1)
                .setStickyPacketHelpers(new BaseStickPackageHelper()) // 不处理粘包
                .build(),
            // 状态回调
            this::onSerialPortStatusChanged,
            // 数据回调
            new MultiSerialPortManager.OnSerialPortDataCallback() {
                @Override
                public void onDataReceived(String serialId, byte[] data) {
                    String gpsData = new String(data);
                    Log.i(TAG, "GPS数据: " + gpsData);
                    runOnUiThread(() -> updateStatus("GPS收到: " + gpsData));
                }
            });
        
        // 串口2：传感器模块，按换行符分包
        manager.openSerialPort("SENSOR", "/dev/ttyS2", 115200,
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setDatabits(8)
                .setParity(0)
                .setStopbits(1)
                .setStickyPacketHelpers(new SpecifiedStickPackageHelper("\n")) // 按换行符分包
                .build(),
            this::onSerialPortStatusChanged,
            new MultiSerialPortManager.OnSerialPortDataCallback() {
                @Override
                public void onDataReceived(String serialId, byte[] data) {
                    String sensorData = new String(data).trim();
                    Log.i(TAG, "传感器数据: " + sensorData);
                    runOnUiThread(() -> updateStatus("传感器收到: " + sensorData));
                }
            });
        
        // 串口3：Modbus设备，固定长度分包
        manager.openSerialPort("MODBUS", "/dev/ttyS3", 9600,
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setDatabits(8)
                .setParity(2) // 偶校验
                .setStopbits(1)
                .setStickyPacketHelpers(new StaticLenStickPackageHelper(8)) // 固定8字节
                .build(),
            this::onSerialPortStatusChanged,
            new MultiSerialPortManager.OnSerialPortDataCallback() {
                @Override
                public void onDataReceived(String serialId, byte[] data) {
                    String modbusData = bytesToHex(data);
                    Log.i(TAG, "Modbus数据: " + modbusData);
                    runOnUiThread(() -> updateStatus("Modbus收到: " + modbusData));
                }
            });
        
        // 串口4：自定义协议，使用自定义分隔符
        manager.openSerialPort("CUSTOM", "/dev/ttyS4", 115200,
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setDatabits(8)
                .setParity(0)
                .setStopbits(1)
                .setStickyPacketHelpers(
                    new SpecifiedStickPackageHelper("$$START$$", "$$END$$")) // 自定义开始和结束标记
                .build(),
            this::onSerialPortStatusChanged,
            new MultiSerialPortManager.OnSerialPortDataCallback() {
                @Override
                public void onDataReceived(String serialId, byte[] data) {
                    String customData = new String(data);
                    Log.i(TAG, "自定义协议数据: " + customData);
                    runOnUiThread(() -> updateStatus("自定义协议收到: " + customData));
                }
            });
        
        // 延迟打印所有串口状态
        statusText.postDelayed(() -> {
            manager.printAllSerialStatus();
            updateStatus("所有串口打开完成，共" + manager.getOpenedSerialPorts().size() + "个");
        }, 1000);
    }
    
    /**
     * 关闭所有串口
     */
    private void closeMultiSerialPorts() {
        MultiSerialPortManager manager = SimpleSerialPortManager.multi();
        manager.closeAllSerialPorts();
        updateStatus("所有串口已关闭");
    }
    
    /**
     * 发送测试数据到所有串口
     */
    private void sendTestData() {
        MultiSerialPortManager manager = SimpleSerialPortManager.multi();
        
        // 向GPS发送AT命令
        manager.sendData("GPS", "AT+GPS?\r\n");
        
        // 向传感器发送读取命令
        manager.sendData("SENSOR", "READ_TEMP\n");
        
        // 向Modbus设备发送读取寄存器命令
        byte[] modbusCmd = {0x01, 0x03, 0x00, 0x00, 0x00, 0x01, (byte)0x84, 0x0A};
        manager.sendData("MODBUS", modbusCmd);
        
        // 向自定义协议设备发送命令
        manager.sendData("CUSTOM", "$$START$$GET_STATUS$$END$$");
        
        updateStatus("测试数据已发送到所有串口");
    }
    
    /**
     * 串口状态变化回调
     */
    private void onSerialPortStatusChanged(String serialId, boolean success, SerialStatus status) {
        String message = String.format("串口[%s]: %s", serialId, success ? "打开成功" : "打开失败 - " + status);
        Log.i(TAG, message);
        runOnUiThread(() -> updateStatus(message));
    }
    
    /**
     * 更新状态显示
     */
    private void updateStatus(String message) {
        if (statusText != null) {
            String currentText = statusText.getText().toString();
            String newText = message + "\n" + currentText;
            // 保持最多20行
            String[] lines = newText.split("\n");
            if (lines.length > 20) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 20; i++) {
                    sb.append(lines[i]).append("\n");
                }
                newText = sb.toString();
            }
            statusText.setText(newText);
        }
        Log.i(TAG, message);
    }
    
    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理资源
        closeMultiSerialPorts();
    }
    
    /**
     * 演示动态更新粘包处理器
     */
    public void updateStickyPacketExample(View view) {
        MultiSerialPortManager manager = SimpleSerialPortManager.multi();
        
        // 动态更新GPS串口的粘包处理器，改为按回车换行分包
        boolean success = manager.updateStickyPacketHelpers("GPS", 
            new AbsStickPackageHelper[]{new SpecifiedStickPackageHelper("\r\n")});
        
        if (success) {
            updateStatus("GPS串口粘包处理器已更新为\\r\\n分包");
        } else {
            updateStatus("GPS串口粘包处理器更新失败");
        }
    }
    
    /**
     * 展示串口数据路由功能
     */
    public void serialRoutingExample() {
        MultiSerialPortManager manager = SimpleSerialPortManager.multi();
        
        // 主控制串口：接收外部命令并路由到其他串口
        manager.openSerialPort("MAIN_CTRL", "/dev/ttyS0", 115200,
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setStickyPacketHelpers(new SpecifiedStickPackageHelper("\r\n"))
                .build(),
            null,
            new MultiSerialPortManager.OnSerialPortDataCallback() {
                @Override
                public void onDataReceived(String serialId, byte[] data) {
                    String command = new String(data).trim();
                    Log.i(TAG, "主控制命令: " + command);
                    
                    // 根据命令前缀路由到不同串口
                    if (command.startsWith("GPS:")) {
                        String gpsCmd = command.substring(4);
                        manager.sendData("GPS", gpsCmd + "\r\n");
                        updateStatus("路由命令到GPS: " + gpsCmd);
                    } else if (command.startsWith("SENSOR:")) {
                        String sensorCmd = command.substring(7);
                        manager.sendData("SENSOR", sensorCmd + "\n");
                        updateStatus("路由命令到传感器: " + sensorCmd);
                    } else if (command.startsWith("MODBUS:")) {
                        // 这里可以解析十六进制字符串并发送到Modbus
                        updateStatus("路由命令到Modbus: " + command);
                    }
                }
            });
        
        updateStatus("串口路由功能已启用，可通过主控制串口发送命令");
    }
}
