package com.kongqw.serialportlibrary;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.cl.log.XConsolePrinter;
import com.cl.log.XFilePrinter;
import com.cl.log.XLog;
import com.cl.log.XLogConfig;
import com.cl.log.XLogManager;
import com.kongqw.serialportlibrary.listener.OnOpenSerialPortListener;
import com.kongqw.serialportlibrary.listener.OnSerialPortDataListener;
import com.kongqw.serialportlibrary.thread.SerialPortReadThread;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


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
    private ConfigurationSdk mSdk;
    private Context mContext;
    //标记是否初始化
    private boolean isInit = false;


    private SerialPortManager() {
    }

    private static class SerialPortInstance {
        private static final SerialPortManager SERIALPORT = new SerialPortManager();
    }

    public static SerialPortManager getInstance() {
        return SerialPortInstance.SERIALPORT;
    }


    public void init(ConfigurationSdk sdk, Context context) {
        if (isInit) return;
        this.mSdk = sdk;
        this.mContext = context;
        initLog();
        openSerialPort();
        isInit = true;
    }


    private void initLog() {
        XLogManager.init(new XLogConfig() {
            @Override
            public String getGlobalTag() {
                return mSdk.getsLogType();
            }

            @Override
            public boolean enable() {
                return mSdk.issDebug();
            }

            @Override
            public JsonParser injectJsonParser() {
                //TODO 根据需求自行添加
                return super.injectJsonParser();
            }

            @Override
            public boolean includeThread() {
                return mSdk.issIncludeThread();
            }

            @Override
            public int stackTraceDepth() {
                return 5;
            }
        }, new XConsolePrinter(), XFilePrinter.getInstance(mContext.getCacheDir().getAbsolutePath(), 0));

    }

    /**
     * 打开串口
     *
     * @return 打开是否成功
     */
    public boolean openSerialPort() {
        Log.i(TAG, "openSerialPort: " + String.format("打开串口 %s  波特率 %s", mSdk.getDevice().getPath(), mSdk.getBaudRate()));

        // 校验串口权限
        if (!mSdk.getDevice().canRead() || !mSdk.getDevice().canWrite()) {
            boolean chmod777 = chmod777(mSdk.getDevice());
            if (!chmod777) {
                Log.i(TAG, "openSerialPort: 没有读写权限");
                if (null != mOnOpenSerialPortListener) {
                    mOnOpenSerialPortListener.onFail(mSdk.getDevice(), OnOpenSerialPortListener.Status.NO_READ_WRITE_PERMISSION);
                }
                return false;
            }
        }

        if (!mSdk.getDevice().canRead() || !mSdk.getDevice().canWrite()) {
            try {
                /* Missing read/write permission, trying to chmod the file */
                /*Process su;
                su = Runtime.getRuntime().exec("su");
                String cmd = "chmod 666 " + device.getAbsolutePath() + "\n" + "exit\n";
                su.getOutputStream().write(cmd.getBytes());
                if ((su.waitFor() != 0) || !device.canRead() || !device.canWrite()) {
                    throw new SecurityException();
                }*/
                List<String> commnandList1 = new ArrayList<>();
                commnandList1.add("chmod 777 " + mSdk.getDevice().getAbsolutePath());
                ShellUtils.execCommand(commnandList1, true);
            } catch (Exception e) {
                e.printStackTrace();
                throw new SecurityException();
            }
        }

        try {
            mFd = open(mSdk.getDevice().getAbsolutePath(), mSdk.getBaudRate(), 0);
            mFileInputStream = new FileInputStream(mFd);
            mFileOutputStream = new FileOutputStream(mFd);
            Log.i(TAG, "openSerialPort: 串口已经打开 " + mFd);
            if (null != mOnOpenSerialPortListener) {
                mOnOpenSerialPortListener.onSuccess(mSdk.getDevice());
            }
            // 开启发送消息的线程
            startSendThread();
            // 开启接收消息的线程
            startReadThread();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (null != mOnOpenSerialPortListener) {
                mOnOpenSerialPortListener.onFail(mSdk.getDevice(), OnOpenSerialPortListener.Status.OPEN_FAIL);
            }
        }
        return false;
    }

    /**
     * 关闭串口
     */
    public void closeSerialPort() {
        isInit = false;
        if (null != mFd) {
            close();
            mFd = null;
        }
        // 停止发送消息的线程
        stopSendThread();
        // 停止接收消息的线程
        stopReadThread();

        if (null != mFileInputStream) {
            try {
                mFileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mFileInputStream = null;
        }

        if (null != mFileOutputStream) {
            try {
                mFileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mFileOutputStream = null;
        }

        mOnOpenSerialPortListener = null;

        mOnSerialPortDataListener = null;
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
        mSendingHandlerThread = new HandlerThread("mSendingHandlerThread");
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
                            mOnSerialPortDataListener.onDataSent(sendBytes);
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
        mSerialPortReadThread = new SerialPortReadThread(mFileInputStream, mSdk) {
            @Override
            public void onDataReceived(byte[] bytes) {
                if (null != mOnSerialPortDataListener) {
                    mOnSerialPortDataListener.onDataReceived(bytes);
                }
            }
        };
        mSerialPortReadThread.start();
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
