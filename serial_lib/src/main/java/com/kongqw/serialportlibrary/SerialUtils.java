package com.kongqw.serialportlibrary;

import android.app.Application;

import com.cl.log.XConsolePrinter;
import com.cl.log.XLog;
import com.cl.log.XLogConfig;
import com.cl.log.XLogManager;
import com.kongqw.serialportlibrary.enumerate.SerialPortEnum;
import com.kongqw.serialportlibrary.enumerate.SerialStatus;
import com.kongqw.serialportlibrary.listener.OnOpenSerialPortListener;
import com.kongqw.serialportlibrary.listener.OnSerialPortDataListener;
import com.kongqw.serialportlibrary.listener.SerialPortDirectorListens;
import com.kongqw.serialportlibrary.stick.AbsStickPackageHelper;
import com.kongqw.serialportlibrary.stick.BaseStickPackageHelper;

import java.io.File;
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


    private AbsStickPackageHelper mStickPackageHelper = new BaseStickPackageHelper();

    public AbsStickPackageHelper getStickPackageHelper() {
        return mStickPackageHelper;
    }

    public void setStickPackageHelper(AbsStickPackageHelper mStickPackageHelper) {
        this.mStickPackageHelper = mStickPackageHelper;
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

    public void init(Application application, SerialConfig serialConfig) {
        sApplication = application;
        mSerialConfig = serialConfig;
        if (serialConfig.getxLogConfig() == null) {
            initLog(true, "SerialUtils");
        } else {
            XLogManager.getInstance().init(serialConfig.getxLogConfig(), new XConsolePrinter());
        }
    }

    public void init(Application application, boolean logSwitch, String logLabel, int sleep) {
        sApplication = application;
        mSerialConfig = new SerialConfig(new SerialConfig.Builder().setIntervalSleep(sleep));
        initLog(logSwitch, logLabel);
    }

    public void init(Application application, boolean logSwitch, String logLabel, int sleep,
                     int databits, int parity, int stopbits) {
        sApplication = application;
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

    public void manyOpenSerialPort(final List<Driver> list) {
        if (list.size() > 0 && list.size() <= serialPortManagers.length) {
            for (int i = 0; i < list.size(); i++) {
                serialPortManagers[i] = new SerialPortManager(SerialPortEnum.values()[i]);
                serialPortManagers[i].setOnOpenSerialPortListener(this);
                serialPortManagers[i].setOnSerialPortDataListener(this);
                serialPortManagers[i].openSerialPort(list.get(i).getName(), Integer.parseInt(list.get(i).getmDeviceRoot()));
            }
        } else {
            XLog.i("串口数量不符合要求！");
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
        SerialPortManager manager = serialPortManagers[serialPortEnum.ordinal()];
        if (manager != null) {
            return manager.sendBytes(sendBytes);
        } else {
            XLog.i("未初始化的串口：" + serialPortEnum.name());
            return false;
        }
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
}