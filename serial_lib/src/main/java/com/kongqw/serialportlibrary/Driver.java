package com.kongqw.serialportlibrary;

import java.io.File;
import java.util.ArrayList;

/**
 * 串口驱动信息类
 * 简化版本，仅用于SerialPortFinder
 */
public class Driver {
    
    private String mDriverName;
    private String mDeviceRoot;
    
    public Driver(String name, String root) {
        mDriverName = name;
        mDeviceRoot = root;
    }
    
    /**
     * 获取驱动名称
     */
    public String getName() {
        return mDriverName;
    }
    
    /**
     * 获取设备根路径
     */
    public String getRoot() {
        return mDeviceRoot;
    }
    
    /**
     * 获取该驱动下的所有设备
     */
    public ArrayList<File> getDevices() {
        ArrayList<File> devices = new ArrayList<>();
        File dev = new File("/dev");
        File[] files = dev.listFiles();
        
        if (files != null) {
            for (File file : files) {
                if (file.getAbsolutePath().startsWith(mDeviceRoot)) {
                    devices.add(file);
                }
            }
        }
        
        return devices;
    }
    
    @Override
    public String toString() {
        return "Driver{" +
                "name='" + mDriverName + '\'' +
                ", root='" + mDeviceRoot + '\'' +
                '}';
    }
}
