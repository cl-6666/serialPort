package com.kongqw.serialportlibrary.listener;

import com.kongqw.serialportlibrary.enumerate.SerialPortEnum;
import com.kongqw.serialportlibrary.enumerate.SerialStatus;

import java.io.File;

/**
 * 打开串口监听
 */
public interface OnOpenSerialPortListener {

    void openState(SerialPortEnum serialPortEnum, File device, SerialStatus status);

}
