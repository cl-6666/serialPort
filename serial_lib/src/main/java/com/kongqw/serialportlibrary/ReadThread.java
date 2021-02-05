package com.kongqw.serialportlibrary;

import java.io.IOException;
import java.io.InputStream;

/**
 * 项目：translation
 * 作者：Arry
 * 创建日期：2020/6/29
 * 描述：
 * 修订历史：
 */
public abstract class ReadThread extends Thread {


    private SendRunnable runnable = new SendRunnable();
    //        private long last;
    //第一次运行线程时设置成true
    private boolean beginning = false;
    //缓冲区()
    byte[] buffer = new byte[64];

    private InputStream mInputStream;



    @Override
    public void run() {
        super.run();

        while (!isInterrupted()) {
            int size;
            try {

                if (mInputStream == null) return;

                //读取数据,同时获取数据长度(数据长度不是数组长度,而是实际接收到的数据长度),数据被读取到了缓冲区 buffer中
                size = mInputStream.read(buffer);
                if (size > 0) {
                    System.err.println("接收数据长度:" + size);

                    //临时数组,将缓冲区buffer中的有效数据读取出来,临时数据长度就是接收到的数据长度。
                    byte[] temp = new byte[size];
                    System.arraycopy(buffer, 0, temp, 0, size);
                    //具体注释见init方法
                    runnable.init(temp, size);
                    //如果程序第一次运行
//                    if (!beginning) {
//                        //运行runnable,只在第一次执行,如果重复执行虽不会抛出异常,但是也无法正常执行功能
//                        mHandler.post(runnable);
//                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
