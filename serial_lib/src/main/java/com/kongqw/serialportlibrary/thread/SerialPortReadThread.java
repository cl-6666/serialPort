package com.kongqw.serialportlibrary.thread;

import com.kongqw.serialportlibrary.utils.SerialPortLogUtil;
import com.kongqw.serialportlibrary.enumerate.SerialPortEnum;
import com.kongqw.serialportlibrary.stick.AbsStickPackageHelper;
import com.kongqw.serialportlibrary.stick.BaseStickPackageHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;


/**
 * 串口消息读取线程
 */
public abstract class SerialPortReadThread extends Thread {

    public abstract void onDataReceived(byte[] bytes);

    private InputStream mInputStream;
    private SerialPortEnum mSerialPortEnum;
    private List<AbsStickPackageHelper> mStickPackageHelpers;

    public SerialPortReadThread(InputStream inputStream, SerialPortEnum mSerialPortEnum, List<AbsStickPackageHelper> stickPackageHelpers) {
        mInputStream = inputStream;
        this.mSerialPortEnum = mSerialPortEnum;
        this.mStickPackageHelpers = stickPackageHelpers;
        
        // 如果没有提供粘包处理器，使用默认的
        if (this.mStickPackageHelpers == null || this.mStickPackageHelpers.isEmpty()) {
            this.mStickPackageHelpers = new java.util.ArrayList<>();
            this.mStickPackageHelpers.add(new BaseStickPackageHelper());
        }
    }

    @Override
    public void run() {
        if (mInputStream == null) return;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (mStickPackageHelpers.size() > mSerialPortEnum.ordinal()) {
                    AbsStickPackageHelper helper = mStickPackageHelpers.get(mSerialPortEnum.ordinal());
                    byte[] buffer = helper.execute(mInputStream);
                    if (buffer != null && buffer.length > 0) {
                        SerialPortLogUtil.d("SerialPortReadThread", "接收数据，长度: " + buffer.length);
                        onDataReceived(buffer);
                    }
                } else {
                    // 使用第一个处理器作为默认
                    if (!mStickPackageHelpers.isEmpty()) {
                        AbsStickPackageHelper helper = mStickPackageHelpers.get(0);
                        byte[] buffer = helper.execute(mInputStream);
                        if (buffer != null && buffer.length > 0) {
                            SerialPortLogUtil.d("SerialPortReadThread", "接收数据(默认处理器)，长度: " + buffer.length);
                            onDataReceived(buffer);
                        }
                    } else {
                        SerialPortLogUtil.e("SerialPortReadThread", "没有可用的粘包处理器");
                        break;
                    }
                }
            } catch (Exception e) {
                SerialPortLogUtil.e("SerialPortReadThread", "读取数据异常: " + e.getMessage());
                e.printStackTrace();
                break;
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