package com.cl.serialportlibrary;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.cl.serialportlibrary.utils.SerialPortLogUtil;
import com.cl.serialportlibrary.enumerate.SerialPortEnum;
import com.cl.serialportlibrary.enumerate.SerialStatus;
import com.cl.serialportlibrary.listener.OnOpenSerialPortListener;
import com.cl.serialportlibrary.listener.OnSerialPortDataListener;
import com.cl.serialportlibrary.thread.SerialPortReadThread;
import com.cl.serialportlibrary.stick.AbsStickPackageHelper;
import com.cl.serialportlibrary.stick.BaseStickPackageHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.io.Closeable;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


public class SerialPortManager extends SerialPort {

    private static final String TAG = SerialPortManager.class.getSimpleName();
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;
    private FileDescriptor mFd;
    private OnOpenSerialPortListener mOnOpenSerialPortListener;
    private OnSerialPortDataListener mOnSerialPortDataListener;
    private HandlerThread mSendingHandlerThread;
    private Handler mSendingHandler;
    private SerialPortReadThread mSerialPortReadThread;
    //串口类型
    private final SerialPortEnum mSerialPortEnum;
    //串口配置
    private SerialConfig mSerialConfig;
    //粘包处理器
    private List<AbsStickPackageHelper> mStickPackageHelpers;

    public SerialPortManager() {
        this(SerialPortEnum.SERIAL_ONE);
    }

    public SerialPortManager(SerialPortEnum mSerialPortEnum) {
        this.mSerialPortEnum = mSerialPortEnum;
        this.mStickPackageHelpers = new ArrayList<>();
        this.mStickPackageHelpers.add(new BaseStickPackageHelper());
    }
    
    /**
     * 设置串口配置
     */
    public void setSerialConfig(SerialConfig config) {
        this.mSerialConfig = config;
        if (config.getStickyPacketHelpers() != null && config.getStickyPacketHelpers().length > 0) {
            this.mStickPackageHelpers = Arrays.asList(config.getStickyPacketHelpers());
        }
    }
    
    /**
     * 设置粘包处理器
     */
    public void setStickPackageHelpers(List<AbsStickPackageHelper> helpers) {
        if (helpers != null) {
            this.mStickPackageHelpers = helpers;
        }
    }

    /**
     * 打开串口
     *
     * @param devicePath 串口号
     * @param baudRate   波特率
     */
    public boolean openSerialPort(String devicePath, int baudRate) {
        closeSerialPort();
        SerialPortLogUtil.i(TAG, "openSerialPort: " + String.format("打开串口 %s  波特率 %s", devicePath, baudRate));
        // 校验串口权限
        if (!checkSerialPortPermission(devicePath)) {
            return false;
        }
        try {
            // 使用配置参数或默认值
            int flags = mSerialConfig != null ? mSerialConfig.getFlags() : 0;
            int databits = mSerialConfig != null ? mSerialConfig.getDatabits() : 8;
            int stopbits = mSerialConfig != null ? mSerialConfig.getStopbits() : 1;
            int parity = mSerialConfig != null ? mSerialConfig.getParity() : 0;
            
            SerialPortLogUtil.d(TAG, "串口参数 - flags: " + flags + ", databits: " + databits + ", stopbits: " + stopbits + ", parity: " + parity);
            mFd = open(devicePath, baudRate, flags, databits, stopbits, parity);
            mFileInputStream = new FileInputStream(mFd);
            mFileOutputStream = new FileOutputStream(mFd);
            SerialPortLogUtil.i(TAG, "openSerialPort: 串口已经打开 " + mFd);
            notifySerialPortOpened(new File(devicePath), SerialStatus.SUCCESS_OPENED);
            // 开启发送消息的线程
            startSendThread();
            // 开启接收消息的线程
            startReadThread();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            notifySerialPortOpened(new File(devicePath), SerialStatus.OPEN_FAIL);
            return false;
        }
    }

    /**
     * 检查串口权限
     *
     * @param devicePath 串口号
     * @return 是否有权限
     */
    private boolean checkSerialPortPermission(String devicePath) {
        File deviceFile = new File(devicePath);
        if (!deviceFile.canRead() || !deviceFile.canWrite()) {
            boolean chmod777 = chmod777(deviceFile);
            if (!chmod777) {
                SerialPortLogUtil.e(TAG, "串口权限不足: " + devicePath);
                notifySerialPortOpened(deviceFile, SerialStatus.NO_READ_WRITE_PERMISSION);
                return false;
            }
        }
        return true;
    }

    /**
     * 通知串口打开状态
     *
     * @param deviceFile 串口文件
     * @param status     打开状态
     */
    private void notifySerialPortOpened(File deviceFile, SerialStatus status) {
        if (null != mOnOpenSerialPortListener) {
            mOnOpenSerialPortListener.openState(mSerialPortEnum, deviceFile, status);
        }
    }

    /**
     * 检查串口是否已打开
     */
    public boolean isOpen() {
        return mFd != null && mFileInputStream != null && mFileOutputStream != null;
    }
    
    /**
     * 关闭串口
     */
    public void closeSerialPort() {
        if (null != mFd) {
            close();
            mFd = null;
        }
        // 停止发送消息的线程
        stopSendThread();
        // 停止接收消息的线程
        stopReadThread();
        closeStream(mFileInputStream);
        closeStream(mFileOutputStream);
    }

    /**
     * 关闭流
     *
     * @param stream 流对象
     */
    private void closeStream(Closeable stream) {
        if (null != stream) {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 添加打开串口监听
     *
     * @param listener listener
     * @return SerialPortManager
     */
    public SerialPortManager setOnOpenSerialPortListener(OnOpenSerialPortListener listener) {
        mOnOpenSerialPortListener = listener;
        return this;
    }

    /**
     * 添加数据通信监听
     *
     * @param listener listener
     * @return SerialPortManager
     */
    public SerialPortManager setOnSerialPortDataListener(OnSerialPortDataListener listener) {
        mOnSerialPortDataListener = listener;
        return this;
    }

    /**
     * 开启发送消息的线程
     */
    private void startSendThread() {
        // 开启发送消息的线程
        mSendingHandlerThread = new HandlerThread("mSendingHandlerThread" + mSerialPortEnum.name());
        mSendingHandlerThread.start();
        // Handler
        mSendingHandler = new Handler(mSendingHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                byte[] sendBytes = (byte[]) msg.obj;

                if (null != mFileOutputStream && null != sendBytes && 0 < sendBytes.length) {
                    try {
                        mFileOutputStream.write(sendBytes);
                        if (null != mOnSerialPortDataListener) {
                            mOnSerialPortDataListener.onDataSent(sendBytes, mSerialPortEnum);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    /**
     * 停止发送消息线程
     */
    private void stopSendThread() {
        mSendingHandler = null;
        if (null != mSendingHandlerThread) {
            mSendingHandlerThread.interrupt();
            mSendingHandlerThread.quit();
            mSendingHandlerThread = null;
        }
    }

    /**
     * 开启接收消息的线程
     */
    private void startReadThread() {
        mSerialPortReadThread = new SerialPortReadThread(mFileInputStream, mSerialPortEnum, mStickPackageHelpers) {
            @Override
            public void onDataReceived(byte[] bytes) {
                if (null != mOnSerialPortDataListener) {
                    mOnSerialPortDataListener.onDataReceived(bytes, mSerialPortEnum);
                }
            }
        };
        mSerialPortReadThread.start();
        SerialPortLogUtil.d(TAG, "启动数据接收线程");
    }

    /**
     * 停止接收消息的线程
     */
    private void stopReadThread() {
        if (null != mSerialPortReadThread) {
            mSerialPortReadThread.release();
        }
    }

    /**
     * 发送数据
     *
     * @param sendBytes 发送数据
     * @return 发送是否成功
     */
    public boolean sendBytes(byte[] sendBytes) {
        if (null != mFd && null != mFileInputStream && null != mFileOutputStream) {
            if (null != mSendingHandler) {
                Message message = Message.obtain();
                message.obj = sendBytes;
                return mSendingHandler.sendMessage(message);
            }
        }
        return false;
    }

}