package com.kongqw.serialportlibrary.thread;

import android.os.SystemClock;
import android.util.Log;

import com.cl.log.XLog;
import com.kongqw.serialportlibrary.ConfigurationSdk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;


/**
 * 串口消息读取线程
 */
public abstract class SerialPortReadThread extends Thread {

    public abstract void onDataReceived(byte[] bytes);

    private static final String TAG = SerialPortReadThread.class.getSimpleName();
    private InputStream mInputStream;
    ByteArrayOutputStream byteArrayOutputStream;
    private ConfigurationSdk sdk;

    public SerialPortReadThread(InputStream inputStream, ConfigurationSdk mSdk) {
        mInputStream = inputStream;
        sdk = mSdk;
        byteArrayOutputStream = new ByteArrayOutputStream();
    }

    @Override
    public void run() {
        super.run();

        while (!isInterrupted()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int value;
            try {
                if (mInputStream == null) return;
                //inputStream.available()方法返回实际可读字节数，也就是总大小，同时时inputStream.read()方法不阻塞
                if (mInputStream.available() == 0) continue;
                byte[] buffers = new byte[mInputStream.available()];
                while (mInputStream.available() > 0 && (value = mInputStream.read(buffers)) != 0x0d) {
                    byteArrayOutputStream.write(buffers, 0, value);
                }
                byte[] res = byteArrayOutputStream.toByteArray();
                checkRules(res);
//                onDataReceived(res);
                byteArrayOutputStream.reset();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }


    /**
     * 检查接收数据是否合法，否则丢弃
     *
     * @param readBytes
     */
    private void checkRules(byte[] readBytes) {
        if (sdk.getMsgHead() != null) {
            //解析帧头
            if (readBytes[0] == sdk.getMsgHead()[0] || readBytes[1] == sdk.getMsgHead()[1]) {
                XLog.e("TAG", "成立");
                for (int i = 4; i < readBytes.length; i++) {
                    if (readBytes[i] == sdk.getMsgHead()[0] ||
                            readBytes[i] == sdk.getMsgHead()[1]) {
                        XLog.i("转码的数据" +bytesToHex(subByte(readBytes, i, readBytes.length / 2)));
                        onDataReceived(subByte(readBytes, i, readBytes.length));
                    }
                }
            } else {
                XLog.e("TAG", "数据不合法丢弃");
            }
        } else {
            onDataReceived(readBytes);
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


