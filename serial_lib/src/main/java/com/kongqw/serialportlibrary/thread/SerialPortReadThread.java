package com.kongqw.serialportlibrary.thread;

import com.kongqw.serialportlibrary.SerialUtils;
import java.io.IOException;
import java.io.InputStream;


/**
 * 串口消息读取线程
 */
public abstract class SerialPortReadThread extends Thread {

    public abstract void onDataReceived(byte[] bytes);

    private InputStream mInputStream;

    public SerialPortReadThread(InputStream inputStream) {
        mInputStream = inputStream;
    }

    @Override
    public void run() {
        super.run();
        while (!isInterrupted()) {
            byte[] buffer = SerialUtils.getInstance().getStickPackageHelper().execute(mInputStream);
            if (buffer != null && buffer.length > 0) {
                // 调用 onDataReceived 方法处理数据
                onDataReceived(buffer);
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