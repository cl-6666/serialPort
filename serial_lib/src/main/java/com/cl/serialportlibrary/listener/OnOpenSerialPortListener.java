package com.cl.serialportlibrary.listener;

import com.cl.serialportlibrary.enumerate.SerialPortEnum;
import com.cl.serialportlibrary.enumerate.SerialStatus;

import java.io.File;

/**
 * 打开串口监听
 */
public interface OnOpenSerialPortListener {

    void openState(SerialPortEnum serialPortEnum, File device, SerialStatus status);

}
