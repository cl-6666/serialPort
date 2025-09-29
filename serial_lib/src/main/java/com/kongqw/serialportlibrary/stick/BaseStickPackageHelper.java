package com.kongqw.serialportlibrary.stick;

import android.os.SystemClock;

import com.kongqw.serialportlibrary.utils.SerialPortLogUtil;

import java.io.IOException;
import java.io.InputStream;

/**
 * The simplest thing to do is not to deal with sticky packets,
 * read directly and return as much as InputStream.available() reads
 */
public class BaseStickPackageHelper implements AbsStickPackageHelper {
    public BaseStickPackageHelper() {
    }

    @Override
    public byte[] execute(InputStream is) {
        try {
            int available = is.available();
            if (available > 0) {
                byte[] buffer = new byte[available];
                int size = is.read(buffer);
                if (size > 0) {
                    return buffer;
                }
                SerialPortLogUtil.d("BaseStickPackageHelper", "原始数据长度: " + buffer.length);
            } else {
                SystemClock.sleep(50); // 默认50ms间隔
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
