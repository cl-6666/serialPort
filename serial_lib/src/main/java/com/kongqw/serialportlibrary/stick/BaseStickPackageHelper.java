package com.kongqw.serialportlibrary.stick;

import android.os.SystemClock;
import android.util.Log;

import com.kongqw.serialportlibrary.SerialUtils;

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
                Log.i("tag","原始数据："+ buffer);
            } else {
                SystemClock.sleep( SerialUtils.getInstance().getmSerialConfig().getIntervalSleep());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
