package com.kongqw.serialportlibrary;

/**
 * 项目：translation
 * 作者：Arry
 * 创建日期：2020/6/29
 * 描述：
 * 修订历史：
 */
public class SendRunnable implements Runnable {

    private byte[] lastBuffer;
    int time = 0;
    boolean work = true;
    private int lastBufferLength;


    //断包处理逻辑包含其中
    public void init(byte[] buffer, int size) {

        if (lastBuffer == null) {
            lastBuffer = buffer;
        } else {
            lastBufferLength = lastBuffer.length;
            byte[] temp = new byte[lastBufferLength + size];
            //先拷贝之前的数据
            System.arraycopy(lastBuffer, 0, temp, 0, lastBufferLength);
            //再拷贝刚接收到的数据
            System.arraycopy(buffer, 0, temp, lastBufferLength, size);
            lastBuffer = null;
            lastBuffer = temp;
            temp = null;
        }
        work = true;
        time = 0;
    }

    public void reStart() {
        work = true;
        time = 0;
    }

    public void stop() {
        work = false;
        time = 0;
    }

    //接收完成后重置完整消息缓冲区
    public void reset() {
        work = false;
        time = 0;
        lastBuffer = null;
    }

    @Override
    public void run() {
        while (work) {
            try {
                Thread.sleep(20);
                time += 20;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (time >= 100) {
                byte[] finalBuffer = lastBuffer;
                reset();
                //业务处理方法
//                onDataReceived(finalBuffer, finalBuffer.length);
            }
        }

    }
}
