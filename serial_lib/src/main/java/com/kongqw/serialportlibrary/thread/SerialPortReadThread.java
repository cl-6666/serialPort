package com.kongqw.serialportlibrary.thread;

import com.cl.log.XLog;
import com.kongqw.serialportlibrary.SerialUtils;
import com.kongqw.serialportlibrary.enumerate.SerialErrorCode;
import com.kongqw.serialportlibrary.enumerate.SerialPortEnum;

import java.io.IOException;
import java.io.InputStream;


/**
 * 串口消息读取线程
 */
public abstract class SerialPortReadThread extends Thread {

    public abstract void onDataReceived(byte[] bytes);

    private InputStream mInputStream;
    private SerialPortEnum mSerialPortEnum;

    public SerialPortReadThread(InputStream inputStream, SerialPortEnum mSerialPortEnum) {
        mInputStream = inputStream;
        this.mSerialPortEnum = mSerialPortEnum;
    }

    @Override
    public void run() {
        if (mInputStream == null) return;
        while (!Thread.currentThread().isInterrupted()) {
            if (SerialUtils.getInstance().getStickPackageHelper().size() >= mSerialPortEnum.ordinal()+1){
                byte[] buffer = SerialUtils.getInstance().getStickPackageHelper()
                        .get(mSerialPortEnum.ordinal()).execute(mInputStream);
                if (buffer != null && buffer.length > 0) {
                    onDataReceived(buffer);
                }
            }else {
                SerialUtils.getInstance().handleError(SerialErrorCode.STICK_PACKAGE_CONFIG_ERROR);
            }
        }
    }

    /**
     * 关闭线程，释放资源
     */
    public void release() {
        interrupt();
        if (mInputStream != null) {
            try {
                mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mInputStream = null;
        }
    }
}