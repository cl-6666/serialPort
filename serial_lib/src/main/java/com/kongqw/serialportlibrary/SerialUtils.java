package com.kongqw.serialportlibrary;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import com.cl.log.XConsolePrinter;
import com.cl.log.XLog;
import com.cl.log.XLogConfig;
import com.cl.log.XLogManager;
import com.kongqw.serialportlibrary.enumerate.SerialErrorCode;
import com.kongqw.serialportlibrary.enumerate.SerialPortEnum;
import com.kongqw.serialportlibrary.enumerate.SerialStatus;
import com.kongqw.serialportlibrary.listener.OnOpenSerialPortListener;
import com.kongqw.serialportlibrary.listener.OnSerialPortDataListener;
import com.kongqw.serialportlibrary.listener.SerialPortDirectorListens;
import com.kongqw.serialportlibrary.stick.AbsStickPackageHelper;
import com.kongqw.serialportlibrary.stick.BaseStickPackageHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * name：cl
 * date：2023/2/17
 * desc：框架初始化
 */
public class SerialUtils implements OnOpenSerialPortListener, OnSerialPortDataListener {

    private Application sApplication;
    private SerialPortManager[] serialPortManagers = new SerialPortManager[6];
    private SerialPortDirectorListens mSerialPortDirectorListens;
    private SerialConfig mSerialConfig;
    private Handler handler= new Handler(Looper.getMainLooper());


    //默认串口处理类
    private AbsStickPackageHelper mStickPackageHelper = new BaseStickPackageHelper();

    private List<AbsStickPackageHelper> stickPackageHelpers;


    public List<AbsStickPackageHelper> getStickPackageHelper() {
        return stickPackageHelpers;
    }


    /**
     * 黏包处理类
     */
    public void setStickPackageHelper(AbsStickPackageHelper... stickPackageHelpers) {
        // 创建一个可修改的ArrayList
        if (this.stickPackageHelpers == null) {
            this.stickPackageHelpers = new ArrayList<>();
        } else {
            this.stickPackageHelpers.clear(); // 先清空现有的
        }
        this.stickPackageHelpers.addAll(Arrays.asList(stickPackageHelpers)); // 添加新的黏包处理类
    }


    private SerialUtils() {
    }

    private static class SerialUtilsInstance {
        private static final SerialUtils INSTANCE = new SerialUtils();
    }

    public static SerialUtils getInstance() {
        return SerialUtilsInstance.INSTANCE;
    }

    public SerialConfig getmSerialConfig() {
        return mSerialConfig;
    }

    /**
     * 初始化串口框架 复杂配置
     * //配置串口相关参数
     * SerialConfig serialConfig = new SerialConfig.Builder()
     * //配置日志参数
     * .setXLogConfig(logConfig)
     * //配置发送间隔速度
     * .setIntervalSleep(200)
     * //是否开启串口重连   目前还没有实现
     * .setSerialPortReconnection(false)
     * //标志位
     * .setFlags(0)
     * //数据位
     * .setDatabits(8)
     * //停止位
     * .setStopbits(1)
     * //校验位：0 表示无校验位，1 表示奇校验，2 表示偶校验
     * .setParity(0)
     * .build();
     * @param application  application
     * @param serialConfig 如上相关配置参数
     */
    public void init(Application application, SerialConfig serialConfig) {
        sApplication = application;
        setStickPackageHelper(mStickPackageHelper);
        mSerialConfig = serialConfig;
        if (serialConfig.getxLogConfig() == null) {
            initLog(true, "SerialUtils");
        } else {
            XLogManager.getInstance().init(serialConfig.getxLogConfig(), new XConsolePrinter());
        }
    }

    /**
     * 初始化串口框架  简单配置
     *
     * @param application application
     * @param logSwitch   是否打开日
     * @param logLabel    日志标识
     * @param sleep       串口接发间隔速度
     */
    public void init(Application application, boolean logSwitch, String logLabel, int sleep) {
        sApplication = application;
        setStickPackageHelper(mStickPackageHelper);
        mSerialConfig = new SerialConfig(new SerialConfig.Builder().setIntervalSleep(sleep));
        initLog(logSwitch, logLabel);
    }

    /**
     * 初始化串口框架 停止位相关配置
     *
     * @param application application
     * @param logSwitch   是否打开日
     * @param logLabel    日志标识
     * @param sleep       串口接发间隔速度
     * @param databits    停止位
     * @param parity      数据位
     * @param stopbits    校验位
     */
    public void init(Application application, boolean logSwitch, String logLabel, int sleep,
                     int databits, int parity, int stopbits) {
        sApplication = application;
        setStickPackageHelper(mStickPackageHelper);
        mSerialConfig = new SerialConfig(new SerialConfig.Builder()
                .setIntervalSleep(sleep)
                .setDatabits(databits)
                .setParity(parity)
                .setStopbits(stopbits));
        initLog(logSwitch, logLabel);
    }

    public void initLog(final boolean logSwitch, final String logLabel) {
        XLogConfig logConfig = new XLogConfig.Builder()
                .setGlobalTag(logLabel)
                .setWhetherToPrint(logSwitch)
                .setStackDeep(0)
                .build();
        XLogManager.getInstance().init(logConfig, new XConsolePrinter());
    }

    /**
     * 打开串口
     * @param list  串口列表
     */
    public void manyOpenSerialPort(final List<Driver> list) {
        if (!list.isEmpty() && list.size() <= serialPortManagers.length) {
            for (int i = 0; i < list.size(); i++) {
                final int index = i;
                handler.postDelayed(() -> {
                    serialPortManagers[index] = new SerialPortManager(SerialPortEnum.values()[index]);
                    serialPortManagers[index].setOnOpenSerialPortListener(this);
                    serialPortManagers[index].setOnSerialPortDataListener(this);
                    serialPortManagers[index].openSerialPort(list.get(index).getName(), Integer.parseInt(list.get(index).getmDeviceRoot()));
                }, 100);
            }
        } else {
            handleError(SerialErrorCode.SERIAL_PORT_NUMBER_ERROR);
        }
    }

    public void setmSerialPortDirectorListens(SerialPortDirectorListens mSerialPortDirectorListens) {
        this.mSerialPortDirectorListens = mSerialPortDirectorListens;
    }

    public void serialPortClose() {
        for (SerialPortManager manager : serialPortManagers) {
            if (manager != null) {
                manager.closeSerialPort();
            }
        }
    }

    public boolean sendData(SerialPortEnum serialPortEnum, byte[] sendBytes) {
        if (serialPortEnum.ordinal() < serialPortManagers.length) {
            SerialPortManager manager = serialPortManagers[serialPortEnum.ordinal()];
            if (manager != null) {
                return manager.sendBytes(sendBytes);
            } else {
                handleError(SerialErrorCode.UNINITIALIZED_SERIAL_PORT);
            }
        } else {
            handleError(SerialErrorCode.SERIAL_PORT_TYPE_UNKNOWN);
        }
        return false;
    }

    @Override
    public void openState(SerialPortEnum serialPortEnum, File device, SerialStatus status) {
        if (mSerialPortDirectorListens != null) {
            mSerialPortDirectorListens.openState(serialPortEnum, device, status);
        }
    }

    @Override
    public void onDataReceived(byte[] bytes, SerialPortEnum serialPortEnum) {
        if (mSerialPortDirectorListens != null) {
            mSerialPortDirectorListens.onDataReceived(bytes, serialPortEnum);
        }
    }

    @Override
    public void onDataSent(byte[] bytes, SerialPortEnum serialPortEnum) {
        if (mSerialPortDirectorListens != null) {
            mSerialPortDirectorListens.onDataSent(bytes, serialPortEnum);
        }
    }


    /**
     * 打印相关错误码，方便开发者排查
     * @param errorCode 错误码类型
     */
    public void handleError(SerialErrorCode errorCode) {
        XLog.e(errorCode.toString());
    }
}