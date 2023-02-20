package com.kongqw.serialportlibrary.listener;

import com.kongqw.serialportlibrary.enumerate.SerialPortEnum;
import com.kongqw.serialportlibrary.enumerate.SerialStatus;

import java.io.File;

/**
 * name：cl
 * date：2023/2/20
 * desc：对外串口监听
 */
public interface SerialPortDirectorListens {

    /**
     * 数据接收
     *
     * @param bytes 接收到的数据
     */
    void onDataReceived(byte[] bytes, SerialPortEnum serialPortEnum);

    /**
     * 数据发送
     *
     * @param bytes 发送的数据
     */
    void onDataSent(byte[] bytes,SerialPortEnum serialPortEnum);


    /**
     * 打开串口监听
     */
    void openState(SerialPortEnum serialPortEnum, File device, SerialStatus status);
}
