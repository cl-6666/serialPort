package com.cl.serialportlibrary.stick;

import android.os.SystemClock;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于超时的黏包处理器
 * 在指定时间内没有新数据到达时，认为是一个完整的数据包
 * Author: cl
 * Date: 2023/10/26
 */
public class TimeoutStickPackageHelper implements AbsStickPackageHelper {
    
    private final int timeout; // 超时时间（毫秒）
    private final List<Byte> buffer = new ArrayList<>();
    
    public TimeoutStickPackageHelper(int timeout) {
        this.timeout = timeout;
    }
    
    @Override
    public byte[] execute(InputStream is) {
        buffer.clear();
        long lastDataTime = System.currentTimeMillis();
        
        try {
            while (true) {
                int available = is.available();
                if (available > 0) {
                    // 有数据可读
                    byte[] tempBuffer = new byte[available];
                    int readBytes = is.read(tempBuffer);
                    if (readBytes > 0) {
                        for (int i = 0; i < readBytes; i++) {
                            buffer.add(tempBuffer[i]);
                        }
                        lastDataTime = System.currentTimeMillis();
                    }
                } else {
                    // 没有数据，检查超时
                    if (!buffer.isEmpty() && (System.currentTimeMillis() - lastDataTime) >= timeout) {
                        // 超时且缓冲区有数据，返回数据包
                        byte[] result = new byte[buffer.size()];
                        for (int i = 0; i < buffer.size(); i++) {
                            result[i] = buffer.get(i);
                        }
                        return result;
                    }
                    
                    // 短暂休眠，避免CPU过度占用
                    SystemClock.sleep(10);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
