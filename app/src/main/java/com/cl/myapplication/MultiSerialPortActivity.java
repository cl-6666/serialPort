package com.cl.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.cl.myapplication.adapter.SpAdapter;
import com.cl.myapplication.databinding.ActivityMultiSerialBinding;
import com.kongqw.serialportlibrary.MultiSerialPortManager;
import com.kongqw.serialportlibrary.SerialPortFinder;
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
    
    private ActivityMultiSerialBinding binding;
    private TextView statusText;
    
    // 串口设备列表
    private String[] mDevices;
    private String[] mBaudrates = {"9600", "19200", "38400", "57600", "115200", "230400", "460800", "921600"};
    
    // 全局串口参数
    private String[] mDatabits = {"8", "7", "6", "5"};
    private String[] mParitys = {"NONE", "ODD", "EVEN", "SPACE", "MARK"};
    private String[] mStopbits = {"1", "2"};
    
    private int globalDatabits = 8;
    private int globalParity = 0;
    private int globalStopbits = 1;
    
    // 各串口的配置
    private SerialPortConfig gpsConfig = new SerialPortConfig();
    private SerialPortConfig sensorConfig = new SerialPortConfig();
    private SerialPortConfig modbusConfig = new SerialPortConfig();
    private SerialPortConfig customConfig = new SerialPortConfig();
    
    // 串口状态
    private boolean gpsOpened = false;
    private boolean sensorOpened = false;
    private boolean modbusOpened = false;
    private boolean customOpened = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_multi_serial);
        
        initViews();
        initDevices();
        setupGlobalParamSpinners();
        setupSpinners();
        setupMultiSerial();
    }
    
    private void initViews() {
        statusText = binding.tvStatus;
        
        // 设置按钮点击事件
        binding.btnRefreshPorts.setOnClickListener(v -> refreshSerialPorts());
        binding.btnOpenAll.setOnClickListener(v -> openAllSerialPorts());
        binding.btnCloseAll.setOnClickListener(v -> closeAllSerialPorts());
        binding.btnSendTest.setOnClickListener(v -> sendTestData());
        binding.btnClearLog.setOnClickListener(v -> clearLog());
        
        // 各串口的开关按钮
        binding.btnGpsToggle.setOnClickListener(v -> toggleGpsPort());
        binding.btnSensorToggle.setOnClickListener(v -> toggleSensorPort());
        binding.btnModbusToggle.setOnClickListener(v -> toggleModbusPort());
        binding.btnCustomToggle.setOnClickListener(v -> toggleCustomPort());
    }
    
    private void setupMultiSerial() {
        updateStatus("多串口管理器初始化完成");
    }
    
    
    /**
     * 发送测试数据到所有串口
     */
    private void sendTestData() {
        MultiSerialPortManager manager = SimpleSerialPortManager.multi();
        int sentCount = 0;
        
        // 向GPS发送AT命令
        if (gpsOpened) {
            manager.sendData("GPS", "AT+GPS?\r\n");
            sentCount++;
        }
        
        // 向传感器发送读取命令
        if (sensorOpened) {
            manager.sendData("SENSOR", "READ_TEMP\n");
            sentCount++;
        }
        
        // 向Modbus设备发送读取寄存器命令
        if (modbusOpened) {
            byte[] modbusCmd = {0x01, 0x03, 0x00, 0x00, 0x00, 0x01, (byte)0x84, 0x0A};
            manager.sendData("MODBUS", modbusCmd);
            sentCount++;
        }
        
        // 向自定义协议设备发送命令
        if (customOpened) {
            manager.sendData("CUSTOM", "$$START$$GET_STATUS$$END$$");
            sentCount++;
        }
        
        updateStatus("测试数据已发送到 " + sentCount + " 个已打开的串口");
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
        closeAllSerialPorts();
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
    
    /**
     * 初始化设备列表
     */
    private void initDevices() {
        SerialPortFinder serialPortFinder = new SerialPortFinder();
        mDevices = serialPortFinder.getAllDevicesPath();
        if (mDevices.length == 0) {
            mDevices = new String[]{"没有找到串口设备"};
        }
        
        // 初始化默认配置
        gpsConfig.device = mDevices.length > 0 ? mDevices[0] : "";
        gpsConfig.baudrate = "9600";
        
        sensorConfig.device = mDevices.length > 1 ? mDevices[1] : (mDevices.length > 0 ? mDevices[0] : "");
        sensorConfig.baudrate = "115200";
        
        modbusConfig.device = mDevices.length > 2 ? mDevices[2] : (mDevices.length > 0 ? mDevices[0] : "");
        modbusConfig.baudrate = "9600";
        
        customConfig.device = mDevices.length > 3 ? mDevices[3] : (mDevices.length > 0 ? mDevices[0] : "");
        customConfig.baudrate = "115200";
    }
    
    /**
     * 设置全局参数下拉框
     */
    private void setupGlobalParamSpinners() {
        // 数据位配置
        setupSpinner(binding.spinnerDatabits, mDatabits, 0, (position) -> {
            globalDatabits = Integer.parseInt(mDatabits[position]);
            updateStatus("全局数据位已设置为: " + globalDatabits);
            closeAllOpenedPorts();
        });
        
        // 校验位配置
        setupSpinner(binding.spinnerParity, mParitys, 0, (position) -> {
            globalParity = position;
            updateStatus("全局校验位已设置为: " + mParitys[position]);
            closeAllOpenedPorts();
        });
        
        // 停止位配置
        setupSpinner(binding.spinnerStopbits, mStopbits, 0, (position) -> {
            globalStopbits = Integer.parseInt(mStopbits[position]);
            updateStatus("全局停止位已设置为: " + globalStopbits);
            closeAllOpenedPorts();
        });
    }
    
    /**
     * 关闭所有已打开的串口（参数变更时使用）
     */
    private void closeAllOpenedPorts() {
        MultiSerialPortManager manager = SimpleSerialPortManager.multi();
        boolean hasOpenPorts = false;
        
        if (gpsOpened) {
            manager.closeSerialPort("GPS");
            gpsOpened = false;
            hasOpenPorts = true;
        }
        if (sensorOpened) {
            manager.closeSerialPort("SENSOR");
            sensorOpened = false;
            hasOpenPorts = true;
        }
        if (modbusOpened) {
            manager.closeSerialPort("MODBUS");
            modbusOpened = false;
            hasOpenPorts = true;
        }
        if (customOpened) {
            manager.closeSerialPort("CUSTOM");
            customOpened = false;
            hasOpenPorts = true;
        }
        
        if (hasOpenPorts) {
            updateButtonStates();
            updateStatus("参数变更，已关闭所有串口，请重新打开");
        }
    }
    
    /**
     * 设置下拉框
     */
    private void setupSpinners() {
        // GPS串口配置
        setupSpinner(binding.spinnerGpsDevice, mDevices, 0, (position) -> {
            gpsConfig.device = mDevices[position];
            if (gpsOpened) {
                updateStatus("GPS串口设备已更改，请重新打开串口");
                closeGpsPort();
            }
        });
        
        setupSpinner(binding.spinnerGpsBaudrate, mBaudrates, 0, (position) -> {
            gpsConfig.baudrate = mBaudrates[position];
            if (gpsOpened) {
                updateStatus("GPS串口波特率已更改，请重新打开串口");
                closeGpsPort();
            }
        });
        
        // 传感器串口配置
        setupSpinner(binding.spinnerSensorDevice, mDevices, mDevices.length > 1 ? 1 : 0, (position) -> {
            sensorConfig.device = mDevices[position];
            if (sensorOpened) {
                updateStatus("传感器串口设备已更改，请重新打开串口");
                closeSensorPort();
            }
        });
        
        setupSpinner(binding.spinnerSensorBaudrate, mBaudrates, 4, (position) -> {
            sensorConfig.baudrate = mBaudrates[position];
            if (sensorOpened) {
                updateStatus("传感器串口波特率已更改，请重新打开串口");
                closeSensorPort();
            }
        });
        
        // Modbus串口配置
        setupSpinner(binding.spinnerModbusDevice, mDevices, mDevices.length > 2 ? 2 : 0, (position) -> {
            modbusConfig.device = mDevices[position];
            if (modbusOpened) {
                updateStatus("Modbus串口设备已更改，请重新打开串口");
                closeModbusPort();
            }
        });
        
        setupSpinner(binding.spinnerModbusBaudrate, mBaudrates, 0, (position) -> {
            modbusConfig.baudrate = mBaudrates[position];
            if (modbusOpened) {
                updateStatus("Modbus串口波特率已更改，请重新打开串口");
                closeModbusPort();
            }
        });
        
        // 自定义串口配置
        setupSpinner(binding.spinnerCustomDevice, mDevices, mDevices.length > 3 ? 3 : 0, (position) -> {
            customConfig.device = mDevices[position];
            if (customOpened) {
                updateStatus("自定义串口设备已更改，请重新打开串口");
                closeCustomPort();
            }
        });
        
        setupSpinner(binding.spinnerCustomBaudrate, mBaudrates, 4, (position) -> {
            customConfig.baudrate = mBaudrates[position];
            if (customOpened) {
                updateStatus("自定义串口波特率已更改，请重新打开串口");
                closeCustomPort();
            }
        });
    }
    
    /**
     * 设置单个下拉框
     */
    private void setupSpinner(Spinner spinner, String[] data, int defaultSelection, OnSpinnerItemSelectedListener listener) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_default_item, data);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(defaultSelection);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                listener.onItemSelected(position);
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
    
    /**
     * 刷新串口列表
     */
    private void refreshSerialPorts() {
        initDevices();
        setupSpinners();
        updateStatus("串口列表已刷新，共找到 " + mDevices.length + " 个设备");
    }
    
    /**
     * 打开所有串口
     */
    private void openAllSerialPorts() {
        updateStatus("开始打开所有串口...");
        if (!gpsOpened) toggleGpsPort();
        if (!sensorOpened) toggleSensorPort();
        if (!modbusOpened) toggleModbusPort();
        if (!customOpened) toggleCustomPort();
    }
    
    /**
     * 关闭所有串口
     */
    private void closeAllSerialPorts() {
        MultiSerialPortManager manager = SimpleSerialPortManager.multi();
        manager.closeAllSerialPorts();
        
        gpsOpened = false;
        sensorOpened = false;
        modbusOpened = false;
        customOpened = false;
        
        updateButtonStates();
        updateStatus("所有串口已关闭");
    }
    
    /**
     * GPS串口开关
     */
    private void toggleGpsPort() {
        if (gpsOpened) {
            closeGpsPort();
        } else {
            openGpsPort();
        }
    }
    
    private void openGpsPort() {
        if (gpsConfig.device.equals("没有找到串口设备")) {
            updateStatus("GPS串口：没有可用设备");
            return;
        }
        
        MultiSerialPortManager manager = SimpleSerialPortManager.multi();
        int baudrate = Integer.parseInt(gpsConfig.baudrate);
        
        manager.openSerialPort("GPS", gpsConfig.device, baudrate,
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setDatabits(globalDatabits)
                .setParity(globalParity)
                .setStopbits(globalStopbits)
                .setStickyPacketHelpers(new BaseStickPackageHelper())
                .build(),
            (serialId, success, status) -> {
                gpsOpened = success;
                runOnUiThread(() -> {
                    updateButtonStates();
                    updateStatus(String.format("GPS串口[%s]: %s", 
                        gpsConfig.device, success ? "打开成功" : "打开失败 - " + status));
                });
            },
            new MultiSerialPortManager.OnSerialPortDataCallback() {
                @Override
                public void onDataReceived(String serialId, byte[] data) {
                    String gpsData = new String(data);
                    Log.i(TAG, "GPS数据: " + gpsData);
                    runOnUiThread(() -> updateStatus("GPS收到: " + gpsData));
                }
            });
    }
    
    private void closeGpsPort() {
        MultiSerialPortManager manager = SimpleSerialPortManager.multi();
        manager.closeSerialPort("GPS");
        gpsOpened = false;
        updateButtonStates();
        updateStatus("GPS串口已关闭");
    }
    
    /**
     * 传感器串口开关
     */
    private void toggleSensorPort() {
        if (sensorOpened) {
            closeSensorPort();
        } else {
            openSensorPort();
        }
    }
    
    private void openSensorPort() {
        if (sensorConfig.device.equals("没有找到串口设备")) {
            updateStatus("传感器串口：没有可用设备");
            return;
        }
        
        MultiSerialPortManager manager = SimpleSerialPortManager.multi();
        int baudrate = Integer.parseInt(sensorConfig.baudrate);
        
        manager.openSerialPort("SENSOR", sensorConfig.device, baudrate,
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setDatabits(globalDatabits)
                .setParity(globalParity)
                .setStopbits(globalStopbits)
                .setStickyPacketHelpers(new SpecifiedStickPackageHelper("\n"))
                .build(),
            (serialId, success, status) -> {
                sensorOpened = success;
                runOnUiThread(() -> {
                    updateButtonStates();
                    updateStatus(String.format("传感器串口[%s]: %s", 
                        sensorConfig.device, success ? "打开成功" : "打开失败 - " + status));
                });
            },
            new MultiSerialPortManager.OnSerialPortDataCallback() {
                @Override
                public void onDataReceived(String serialId, byte[] data) {
                    String sensorData = new String(data).trim();
                    Log.i(TAG, "传感器数据: " + sensorData);
                    runOnUiThread(() -> updateStatus("传感器收到: " + sensorData));
                }
            });
    }
    
    private void closeSensorPort() {
        MultiSerialPortManager manager = SimpleSerialPortManager.multi();
        manager.closeSerialPort("SENSOR");
        sensorOpened = false;
        updateButtonStates();
        updateStatus("传感器串口已关闭");
    }
    
    /**
     * Modbus串口开关
     */
    private void toggleModbusPort() {
        if (modbusOpened) {
            closeModbusPort();
        } else {
            openModbusPort();
        }
    }
    
    private void openModbusPort() {
        if (modbusConfig.device.equals("没有找到串口设备")) {
            updateStatus("Modbus串口：没有可用设备");
            return;
        }
        
        MultiSerialPortManager manager = SimpleSerialPortManager.multi();
        int baudrate = Integer.parseInt(modbusConfig.baudrate);
        
        manager.openSerialPort("MODBUS", modbusConfig.device, baudrate,
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setDatabits(globalDatabits)
                .setParity(globalParity)
                .setStopbits(globalStopbits)
                .setStickyPacketHelpers(new StaticLenStickPackageHelper(8))
                .build(),
            (serialId, success, status) -> {
                modbusOpened = success;
                runOnUiThread(() -> {
                    updateButtonStates();
                    updateStatus(String.format("Modbus串口[%s]: %s", 
                        modbusConfig.device, success ? "打开成功" : "打开失败 - " + status));
                });
            },
            new MultiSerialPortManager.OnSerialPortDataCallback() {
                @Override
                public void onDataReceived(String serialId, byte[] data) {
                    String modbusData = bytesToHex(data);
                    Log.i(TAG, "Modbus数据: " + modbusData);
                    runOnUiThread(() -> updateStatus("Modbus收到: " + modbusData));
                }
            });
    }
    
    private void closeModbusPort() {
        MultiSerialPortManager manager = SimpleSerialPortManager.multi();
        manager.closeSerialPort("MODBUS");
        modbusOpened = false;
        updateButtonStates();
        updateStatus("Modbus串口已关闭");
    }
    
    /**
     * 自定义串口开关
     */
    private void toggleCustomPort() {
        if (customOpened) {
            closeCustomPort();
        } else {
            openCustomPort();
        }
    }
    
    private void openCustomPort() {
        if (customConfig.device.equals("没有找到串口设备")) {
            updateStatus("自定义串口：没有可用设备");
            return;
        }
        
        MultiSerialPortManager manager = SimpleSerialPortManager.multi();
        int baudrate = Integer.parseInt(customConfig.baudrate);
        
        manager.openSerialPort("CUSTOM", customConfig.device, baudrate,
            new MultiSerialPortManager.SerialPortConfig.Builder()
                .setDatabits(globalDatabits)
                .setParity(globalParity)
                .setStopbits(globalStopbits)
                .setStickyPacketHelpers(new SpecifiedStickPackageHelper("$$START$$", "$$END$$"))
                .build(),
            (serialId, success, status) -> {
                customOpened = success;
                runOnUiThread(() -> {
                    updateButtonStates();
                    updateStatus(String.format("自定义串口[%s]: %s", 
                        customConfig.device, success ? "打开成功" : "打开失败 - " + status));
                });
            },
            new MultiSerialPortManager.OnSerialPortDataCallback() {
                @Override
                public void onDataReceived(String serialId, byte[] data) {
                    String customData = new String(data);
                    Log.i(TAG, "自定义协议数据: " + customData);
                    runOnUiThread(() -> updateStatus("自定义协议收到: " + customData));
                }
            });
    }
    
    private void closeCustomPort() {
        MultiSerialPortManager manager = SimpleSerialPortManager.multi();
        manager.closeSerialPort("CUSTOM");
        customOpened = false;
        updateButtonStates();
        updateStatus("自定义串口已关闭");
    }
    
    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        binding.btnGpsToggle.setText(gpsOpened ? "关闭" : "打开");
        binding.btnGpsToggle.setBackgroundColor(getResources().getColor(
            gpsOpened ? R.color.secondary_text : R.color.colorAccent));
        
        binding.btnSensorToggle.setText(sensorOpened ? "关闭" : "打开");
        binding.btnSensorToggle.setBackgroundColor(getResources().getColor(
            sensorOpened ? R.color.secondary_text : R.color.colorAccent));
        
        binding.btnModbusToggle.setText(modbusOpened ? "关闭" : "打开");
        binding.btnModbusToggle.setBackgroundColor(getResources().getColor(
            modbusOpened ? R.color.secondary_text : R.color.colorAccent));
        
        binding.btnCustomToggle.setText(customOpened ? "关闭" : "打开");
        binding.btnCustomToggle.setBackgroundColor(getResources().getColor(
            customOpened ? R.color.secondary_text : R.color.colorAccent));
    }
    
    /**
     * 清空日志
     */
    private void clearLog() {
        statusText.setText("日志已清空");
    }
    
    /**
     * 串口配置类
     */
    private static class SerialPortConfig {
        String device = "";
        String baudrate = "9600";
    }
    
    /**
     * 下拉框选择监听器
     */
    private interface OnSpinnerItemSelectedListener {
        void onItemSelected(int position);
    }
}
