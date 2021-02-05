package com.kongqw.serialportlibrary.thread;

import android.os.SystemClock;
import android.util.Log;

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
    private byte[] mReadBuffer;

    public SerialPortReadThread(InputStream inputStream) {
        mInputStream = inputStream;
        mReadBuffer = new byte[1024];
    }

    @Override
    public void run() {
        super.run();

//        while (!isInterrupted()) {
//            try {
//                if (null == mInputStream) {
//                    return;
//                }
//
//                Log.i(TAG, "run: ");
//                int size = mInputStream.read(mReadBuffer);
//
//                if (-1 == size || 0 >= size) {
//                    return;
//                }
//
//                byte[] readBytes = new byte[size];
//
//                System.arraycopy(mReadBuffer, 0, readBytes, 0, size);
//
//                Log.i(TAG, "run: readBytes = " + new String(readBytes));
//                Log.i(TAG, "readBytes = " + readBytes);
//                onDataReceived(readBytes);
////                checkRules(readBytes);
//
//            } catch (IOException e) {
//                e.printStackTrace();
//                return;
//            }
//        }


        while (!isInterrupted()) {
            try {
                if (null == mInputStream) {
                    return;
                }

                int available = mInputStream.available();

                if (available > 0) {
                    Log.i(TAG, "run: ");
                    int size = mInputStream.read(mReadBuffer);

                    if (-1 == size || 0 >= size) {
                        return;
                    }

                    if (size > 0) {
                        byte[] readBytes = new byte[size];
                        System.arraycopy(mReadBuffer, 0, readBytes, 0, size);
                        Log.i(TAG, "run: readBytes = " + new String(readBytes));

                        Log.i(TAG, "readBytes = " + readBytes);
                        onDataReceived(readBytes);
                    }
                } else {
                    SystemClock.sleep(200);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


//        while (!isInterrupted()) {
//            if (null == mInputStream) {
//                return;
//            }
//
//            Log.i(TAG, "run: ");
//            int size = 0;
//
//            try {
//                /** 获取流中数据的量*/
//                int i = mInputStream.available();
//                if (i == 0) {
//                    size = 0;
//                } else {
//                    /** 流中有数据，则添加到临时数组中*/
//                    size = mInputStream.read(mReadBuffer);
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//            if (size > 0) {
//                /** 发现有信息后就追加到临时变量*/
//                Log.i("SerialPortReadThread", size + "");
//                readBytes = DataUtil.arrayAppend(readBytes, mReadBuffer, size);
//                Log.i("SerialPortReadThread", DataUtil.bytesToHexString(readBytes, readBytes.length));
//            } else {
//                /** 没有需要追加的数据了，回调*/
//                if (readBytes != null) {
//                    onDataReceived(readBytes);
//                }
//
//                /** 清空，等待下个信息单元*/
//                readBytes = null;
//            }
//
//            SystemClock.sleep(50);
//
//        }
    }


    /**
     * 检查接收数据是否合法，否则丢弃
     *
     * @param readBytes
     */
    private void checkRules(byte[] readBytes) {
//        String bytes = Arrays.toString(readBytes);
        Log.e("TAG", "原始readBytes----" + Arrays.toString(readBytes));

        Log.e("TAG", "readBytes[0]原始readBytes" + readBytes[0]);
        Log.e("TAG", "readBytes[1]原始readBytes" + readBytes[1]);
        Log.e("TAG", "转换readBytes----" + Integer.toHexString(readBytes[0] & 0XFF));
        Log.e("TAG", "转换readBytes----" + Integer.toHexString(readBytes[1] & 0XFF));
        Log.e("TAG", "转换readBytes----" + Integer.toHexString(readBytes[2] & 0XFF));
        Log.e("TAG", "转换readBytes----" + Integer.toHexString(readBytes[3] & 0XFF));

        //解析帧头
        if (readBytes[0] == -33 ||
                readBytes[1] == -3) {
            Log.e("TAG", "成立");


        } else {
            Log.e("TAG", "数据不合法丢弃");
        }


//        if (Integer.toHexString(readBytes[0] & 0XFF).equals("df") ||
//                Integer.toHexString(readBytes[1] & 0XFF).equals("fd")) {
//            Log.e("TAG", "成立");
//
//
//        } else {
//            Log.e("TAG", "数据不合法丢弃");
//        }

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
}
