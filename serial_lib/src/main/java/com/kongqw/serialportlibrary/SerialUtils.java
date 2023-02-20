package com.kongqw.serialportlibrary;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import com.cl.log.XConsolePrinter;
import com.cl.log.XFilePrinter;
import com.cl.log.XLog;
import com.cl.log.XLogConfig;
import com.cl.log.XLogManager;
import com.kongqw.serialportlibrary.enumerate.SerialPortEnum;
import com.kongqw.serialportlibrary.enumerate.SerialStatus;
import com.kongqw.serialportlibrary.listener.OnOpenSerialPortListener;
import com.kongqw.serialportlibrary.listener.OnSerialPortDataListener;
import com.kongqw.serialportlibrary.listener.SerialPortDirectorListens;

import java.io.File;
import java.util.List;

/**
 * name：cl
 * date：2023/2/17
 * desc：框架初始化
 */
public class SerialUtils implements OnOpenSerialPortListener, OnSerialPortDataListener {

    /**
     * Application 对象
     */
    private Application sApplication;

    /**
     * 多串口读和写，目前内部定死只支持3种串口，已满足大部分人需求
     */
    private SerialPortManager serialPortManager1;
    //串口2
    private SerialPortManager serialPortManager2;
    //串口3
    private SerialPortManager serialPortManager3;
    //串口4
    private SerialPortManager serialPortManager4;
    //串口5
    private SerialPortManager serialPortManager5;
    //串口6
    private SerialPortManager serialPortManager6;

    //串口监听
    private SerialPortDirectorListens mSerialPortDirectorListens;


    private static final Handler HANDLER = new Handler(Looper.getMainLooper());


    private SerialUtils() {
    }


    private static class SerialUtilsInstance {
        private static final SerialUtils INSTANCE = new SerialUtils();
    }

    public static SerialUtils getInstance() {
        return SerialUtilsInstance.INSTANCE;
    }


    /**
     * 初始化 串口框架，需要在 Application.create 中初始化
     *
     * @param application 应用的上下文
     */
    public void init(Application application, boolean logSwitch, String logLabel) {
        sApplication = application;
        initLog(logSwitch, logLabel);
    }


    public void initLog(final boolean logSwitch, final String logLabel) {
        XLogManager.init(new XLogConfig() {
            @Override
            public String getGlobalTag() {
                return logLabel;
            }

            @Override
            public boolean enable() {
                return logSwitch;
            }

            @Override
            public JsonParser injectJsonParser() {
                //TODO 根据需求自行添加
                return super.injectJsonParser();
            }

            @Override
            public boolean includeThread() {
                return false;
            }

            @Override
            public int stackTraceDepth() {
                return 5;
            }
        }, new XConsolePrinter(), XFilePrinter.getInstance(sApplication.getCacheDir().getAbsolutePath(), 0));
    }


    /**
     * 打开多个串口
     *
     * @param list 串口号、端口号
     */
    public void manyOpenSerialPort(final List<Driver> list) {
        if (list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                switch (i) {
                    case 0:
                        serialPortManager1 = new SerialPortManager(SerialPortEnum.SERIAL_ONE);
                        serialPortManager1.setOnOpenSerialPortListener(this);
                        serialPortManager1.setOnSerialPortDataListener(this);
                        serialPortManager1.openSerialPort(list.get(0).getName(), Integer.parseInt(list.get(0).getmDeviceRoot()));
                        break;
                    case 1:
                        serialPortManager2 = new SerialPortManager(SerialPortEnum.SERIAL_TWO);
                        serialPortManager2.setOnOpenSerialPortListener(this);
                        serialPortManager2.setOnSerialPortDataListener(this);
                        serialPortManager2.openSerialPort(list.get(1).getName(), Integer.parseInt(list.get(1).getmDeviceRoot()));
                        break;
                    case 2:
                        serialPortManager3 = new SerialPortManager(SerialPortEnum.SERIAL_THREE);
                        serialPortManager3.setOnOpenSerialPortListener(this);
                        serialPortManager3.setOnSerialPortDataListener(this);
                        serialPortManager3.openSerialPort(list.get(2).getName(), Integer.parseInt(list.get(2).getmDeviceRoot()));
                        break;
                    case 3:
                        serialPortManager4 = new SerialPortManager(SerialPortEnum.SERIAL_FOUR);
                        serialPortManager4.setOnOpenSerialPortListener(this);
                        serialPortManager4.setOnSerialPortDataListener(this);
                        serialPortManager4.openSerialPort(list.get(3).getName(), Integer.parseInt(list.get(3).getmDeviceRoot()));
                        break;
                    case 4:
                        serialPortManager5 = new SerialPortManager(SerialPortEnum.SERIAL_FIVE);
                        serialPortManager5.setOnOpenSerialPortListener(this);
                        serialPortManager5.setOnSerialPortDataListener(this);
                        serialPortManager5.openSerialPort(list.get(4).getName(), Integer.parseInt(list.get(4).getmDeviceRoot()));
                        break;
                    case 5:
                        serialPortManager6 = new SerialPortManager(SerialPortEnum.SERIAL_SIX);
                        serialPortManager6.setOnOpenSerialPortListener(this);
                        serialPortManager6.setOnSerialPortDataListener(this);
                        serialPortManager6.openSerialPort(list.get(5).getName(), Integer.parseInt(list.get(5).getmDeviceRoot()));
                        break;
                    default:
                        XLog.i("目前sdk最多只支持4个串口使用案例");
                        break;
                }
            }
        } else {
            XLog.i("请检测List<Driver>是否为空！");
        }
    }

    /**
     * 串口监听
     */
    public void setmSerialPortDirectorListens(SerialPortDirectorListens mSerialPortDirectorListens) {
        this.mSerialPortDirectorListens = mSerialPortDirectorListens;
    }


    /**
     * 串口销毁
     */
    public void serialPortClose() {
        if (serialPortManager1 != null) {
            serialPortManager1.closeSerialPort();
        }
        if (serialPortManager2 != null) {
            serialPortManager2.closeSerialPort();
        }
        if (serialPortManager3 != null) {
            serialPortManager3.closeSerialPort();
        }
        if (serialPortManager4 != null) {
            serialPortManager4.closeSerialPort();
        }
        if (serialPortManager5 != null) {
            serialPortManager5.closeSerialPort();
        }
        if (serialPortManager6 != null) {
            serialPortManager6.closeSerialPort();
        }
    }


    /**
     * 发送数据
     *
     * @param serialPortEnum 哪一路串口
     * @param sendBytes      发送数据
     * @return
     */
    public boolean sendData(SerialPortEnum serialPortEnum, byte[] sendBytes) {
        switch (serialPortEnum) {
            case SERIAL_ONE:
                if (serialPortManager1 != null) {
                    return serialPortManager1.sendBytes(sendBytes);
                } else {
                    XLog.i("请检测当前类型是否初始化--"+serialPortEnum.name());
                    return false;
                }
            case SERIAL_TWO:
                if (serialPortManager2 != null) {
                    return serialPortManager2.sendBytes(sendBytes);
                } else {
                    XLog.i("请检测当前类型是否初始化--"+serialPortEnum.name());
                    return false;
                }
            case SERIAL_THREE:
                if (serialPortManager3 != null) {
                    return serialPortManager3.sendBytes(sendBytes);
                } else {
                    XLog.i("请检测当前类型是否初始化--"+serialPortEnum.name());
                    return false;
                }
            case SERIAL_FOUR:
                if (serialPortManager4 != null) {
                    return serialPortManager4.sendBytes(sendBytes);
                } else {
                    XLog.i("请检测当前类型是否初始化--"+serialPortEnum.name());
                    return false;
                }
            case SERIAL_FIVE:
                if (serialPortManager5 != null) {
                    return serialPortManager5.sendBytes(sendBytes);
                } else {
                    XLog.i("请检测当前类型是否初始化--"+serialPortEnum.name());
                    return false;
                }
            case SERIAL_SIX:
                if (serialPortManager6 != null) {
                    return serialPortManager6.sendBytes(sendBytes);
                } else {
                    XLog.i("请检测当前类型是否初始化--"+serialPortEnum.name());
                    return false;
                }
            default:
                XLog.i("没有找到对应的串口！");
                return false;
        }
    }


    @Override
    public void openState(SerialPortEnum serialPortEnum, File device, SerialStatus status) {
        mSerialPortDirectorListens.openState(serialPortEnum, device, status);
    }

    @Override
    public void onDataReceived(byte[] bytes, SerialPortEnum serialPortEnum) {
        mSerialPortDirectorListens.onDataReceived(bytes, serialPortEnum);

    }

    @Override
    public void onDataSent(byte[] bytes, SerialPortEnum serialPortEnum) {
        mSerialPortDirectorListens.onDataSent(bytes, serialPortEnum);
    }
}
