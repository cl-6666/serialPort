package com.cl.serialportlibrary.listener;

import com.cl.serialportlibrary.enumerate.SerialPortEnum;

/**
 * 串口消息监听
 */
public interface OnSerialPortDataListener {

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

}
