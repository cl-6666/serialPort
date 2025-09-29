package com.kongqw.serialportlibrary.stick;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 组合式黏包处理器
 * 首先尝试使用主要处理器，如果失败则使用备用处理器
 * Author: cl
 * Date: 2023/10/26
 */
public class CompositeStickPackageHelper implements AbsStickPackageHelper {
    
    private final AbsStickPackageHelper primaryHelper;
    private final AbsStickPackageHelper fallbackHelper;
    private final List<Byte> buffer = new ArrayList<>();
    
    public CompositeStickPackageHelper(AbsStickPackageHelper primaryHelper, AbsStickPackageHelper fallbackHelper) {
        this.primaryHelper = primaryHelper;
        this.fallbackHelper = fallbackHelper;
    }
    
    @Override
    public byte[] execute(InputStream is) {
        // 先尝试读取一些数据到缓冲区
        try {
            int available = is.available();
            if (available > 0) {
                byte[] tempBuffer = new byte[available];
                int readBytes = is.read(tempBuffer);
                if (readBytes > 0) {
                    for (int i = 0; i < readBytes; i++) {
                        buffer.add(tempBuffer[i]);
                    }
                }
            }
            
            if (buffer.isEmpty()) {
                return null;
            }
            
            // 将缓冲区数据转换为字节数组
            byte[] bufferData = new byte[buffer.size()];
            for (int i = 0; i < buffer.size(); i++) {
                bufferData[i] = buffer.get(i);
            }
            
            // 尝试使用主要处理器
            ByteArrayInputStream primaryStream = new ByteArrayInputStream(bufferData);
            byte[] primaryResult = primaryHelper.execute(primaryStream);
            
            if (primaryResult != null && primaryResult.length > 0) {
                // 主要处理器成功，清除已处理的数据
                if (primaryResult.length <= buffer.size()) {
                    for (int i = 0; i < primaryResult.length; i++) {
                        buffer.remove(0);
                    }
                }
                return primaryResult;
            }
            
            // 主要处理器失败，尝试备用处理器
            ByteArrayInputStream fallbackStream = new ByteArrayInputStream(bufferData);
            byte[] fallbackResult = fallbackHelper.execute(fallbackStream);
            
            if (fallbackResult != null && fallbackResult.length > 0) {
                // 备用处理器成功，清除已处理的数据
                if (fallbackResult.length <= buffer.size()) {
                    for (int i = 0; i < fallbackResult.length; i++) {
                        buffer.remove(0);
                    }
                }
                return fallbackResult;
            }
            
            // 两个处理器都失败，保持缓冲区数据等待更多数据
            return null;
            
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
