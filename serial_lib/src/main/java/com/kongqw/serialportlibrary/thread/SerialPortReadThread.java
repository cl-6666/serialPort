package com.kongqw.serialportlibrary.thread;

import com.cl.log.XLog;
import com.kongqw.serialportlibrary.SerialUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * 串口消息读取线程
 */
public abstract class SerialPortReadThread extends Thread {

    public abstract void onDataReceived(byte[] bytes);
    private static final String TAG = SerialPortReadThread.class.getSimpleName();
    private InputStream mInputStream;
    ByteArrayOutputStream byteArrayOutputStream;
    private byte[] mReadBuffer;
    byte[] readBytes = null;

    public SerialPortReadThread(InputStream inputStream) {
        mInputStream = inputStream;
        byteArrayOutputStream = new ByteArrayOutputStream();
        mReadBuffer = new byte[1024];
    }

    @Override
    public void run() {
        super.run();
        while (!isInterrupted()) {
            try {
                Thread.sleep(SerialUtils.getInstance().getmSerialConfig().getIntervalSleep());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int value;
            try {
                if (mInputStream == null) return;
                //inputStream.available()方法返回实际可读字节数，也就是总大小，同时时inputStream.read()方法不阻塞
                if (mInputStream.available() == 0) continue;
                byte[] buffers = new byte[50000];
                while (mInputStream.available() > 0 && (value = mInputStream.read(buffers)) != 0x0d) {
                    byteArrayOutputStream.write(buffers, 0, value);
                }
                byte[] res = byteArrayOutputStream.toByteArray();
                onDataReceived(res);
                byteArrayOutputStream.reset();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }


    @Override
    public synchronized void start() {
        super.start();
    }

    /**
     * 关闭线程 释放资源
     */
    public void release() {
        interrupt();

        if (null != mInputStream) {
            try {
                mInputStream.close();
                mInputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            byteArrayOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public byte[] subByte(byte[] b, int off, int length) {

        byte[] b1 = new byte[length];

        System.arraycopy(b, off, b1, 0, length);

        return b1;

    }


    public static String bytesToHex(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if(hex.length() < 2){
                sb.append(0);
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}


