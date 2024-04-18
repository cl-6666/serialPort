package com.cl.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentManager;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.cl.log.XLog;
import com.cl.myapplication.adapter.SpAdapter;
import com.cl.myapplication.constant.PreferenceKeys;
import com.cl.myapplication.databinding.ActivityMainJavaBinding;
import com.cl.myapplication.fragment.LogFragment;
import com.cl.myapplication.message.ConversionNoticeEvent;
import com.cl.myapplication.message.IMessage;
import com.cl.myapplication.message.LogManager;
import com.cl.myapplication.message.RecvMessage;
import com.cl.myapplication.message.SendMessage;
import com.cl.myapplication.util.PrefHelper;
import com.hjq.toast.ToastUtils;
import com.kongqw.serialportlibrary.Device;
import com.kongqw.serialportlibrary.Driver;
import com.kongqw.serialportlibrary.enumerate.SerialPortEnum;
import com.kongqw.serialportlibrary.SerialPortFinder;
import com.kongqw.serialportlibrary.SerialUtils;
import com.kongqw.serialportlibrary.enumerate.SerialStatus;
import com.kongqw.serialportlibrary.listener.SerialPortDirectorListens;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainJavaActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private ActivityMainJavaBinding binding;
    private Device mDevice;
    private byte[] b1 = {(byte) 33, (byte) -3};

    private byte[] t = {(byte) 67, (byte) 72,(byte) 73,(byte) 78};

    private String[] mDevices;
    private String[] mBaudrates;
    private int mDeviceIndex;
    private int mBaudrateIndex;
    private boolean mOpened = false;
    private boolean mConversionNotice = true;
    private LogFragment mLogFragment;

    final String[] databits = new String[]{"8", "7", "6", "5"};
    final String[] paritys = new String[]{"NONE", "ODD", "EVEN", "SPACE", "MARK"};
    final String[] stopbits = new String[]{"1", "2"};

    //先定义
    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_java);
        verifyStoragePermissions(this);
        initFragment();
        initDevice();
        initSpinners();
        //串口数据监听
        SerialUtils.getInstance().setmSerialPortDirectorListens(new SerialPortDirectorListens() {
            /**
             *  接收回调
             * @param bytes 接收到的数据
             * @param serialPortEnum  串口类型
             */
            @Override
            public void onDataReceived(byte[] bytes, SerialPortEnum serialPortEnum) {
                XLog.i( "当前接收串口类型：" + serialPortEnum.name());
                XLog.i( "onDataReceived [ byte[] ]: " + Arrays.toString(bytes));
                XLog.i("onDataReceived [ String ]: " + new String(bytes));
                if (mConversionNotice) {
                    LogManager.instance().post(new RecvMessage(bytesToHex(bytes)));
                } else {
                    LogManager.instance().post(new RecvMessage(Arrays.toString(bytes)));
                }
            }

            /**
             *  发送回调
             * @param bytes 发送的数据
             * @param serialPortEnum  串口类型
             */
            @Override
            public void onDataSent(byte[] bytes, SerialPortEnum serialPortEnum) {
                XLog.i( "当前发送串口类型：" + serialPortEnum.name());
                XLog.i( "onDataSent [ byte[] ]: " + Arrays.toString(bytes));
                XLog.i( "onDataSent [ String ]: " + new String(bytes));
                if (mConversionNotice) {
                    LogManager.instance().post(new SendMessage(bytesToHex(bytes)));
                } else {
                    LogManager.instance().post(new SendMessage(Arrays.toString(bytes)));
                }
            }

            /**
             * 串口打开回调
             * @param serialPortEnum  串口类型
             * @param device  串口号
             * @param status 打开状态
             */
            @Override
            public void openState(SerialPortEnum serialPortEnum, File device, SerialStatus status) {
                XLog.i("TAG","串口打开状态："+device.getName()+"---打开状态："+status.name());
                switch (serialPortEnum) {
                    case SERIAL_ONE:
                        switch (status) {
                            case SUCCESS_OPENED:
                                ToastUtils.show("串口打开成功");
                                mOpened = true;
                                updateViewState(true);
                                break;
                            case NO_READ_WRITE_PERMISSION:
                                ToastUtils.show("没有读写权限");
                                updateViewState(false);
                                break;
                            case OPEN_FAIL:
                                ToastUtils.show("串口打开失败");
                                updateViewState(false);
                                break;
                        }
                        break;
                    case SERIAL_TWO:
                        XLog.i("根据实际多串口场景演示");
                        break;
                }
            }
        });

        //设置数据位
        SpAdapter spAdapter1 = new SpAdapter(this);
        spAdapter1.setDatas(databits);
        binding.spDatabits.setAdapter(spAdapter1);
        binding.spDatabits.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                closeSerialPort();
                SerialUtils.getInstance().getmSerialConfig().setDatabits(Integer.parseInt(databits[position]));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //设置数据位
        SpAdapter spAdapter2 = new SpAdapter(this);
        spAdapter2.setDatas(paritys);
        binding.spParity.setAdapter(spAdapter2);
        binding.spParity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                closeSerialPort();
                if (position == 0) {
                    SerialUtils.getInstance().getmSerialConfig().setParity(0);
                } else if (position == 1) {
                    SerialUtils.getInstance().getmSerialConfig().setParity(1);
                } else if (position == 2) {
                    SerialUtils.getInstance().getmSerialConfig().setParity(2);
                } else if (position == 3) {
                    SerialUtils.getInstance().getmSerialConfig().setParity(3);
                } else if (position == 4) {
                    SerialUtils.getInstance().getmSerialConfig().setParity(4);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //设置停止位
        SpAdapter spAdapter3 = new SpAdapter(this);
        spAdapter3.setDatas(stopbits);
        binding.spStopbits.setAdapter(spAdapter3);
        binding.spStopbits.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                closeSerialPort();
                SerialUtils.getInstance().getmSerialConfig().setStopbits(Integer.parseInt(stopbits[position]));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //多串口演示
//        List<Driver> list2=new ArrayList<>();
//        //串口ttyS4
//        list2.add(new Driver("/dev/ttyS4", "115200"));
//        list2.add(new Driver("/dev/ttyS2", "115200"));
////        list2.add(new Driver("/dev/ttyS4", "115200"));
//        SerialUtils.getInstance().manyOpenSerialPort(list2);



        binding.btnOpenDevice.setOnClickListener(v -> {
            if (mOpened) {
                closeSerialPort();
            } else {
                //多串口演示
                List<Driver> list = new ArrayList<>();
                list.clear();
                list.add(new Driver(mDevice.getName(), mDevice.getRoot()));
                // 打开串口
                XLog.i( "打开的串口为：" + mDevice.getName() + "----" + Integer.parseInt(mDevice.getRoot()));
                SerialUtils.getInstance().manyOpenSerialPort(list);
            }
            updateViewState(mOpened);
        });

        binding.btnSendData.setOnClickListener((view) -> {
            onSend();
        });
    }
    private void closeSerialPort() {
        SerialUtils.getInstance().serialPortClose();
        mOpened = false;
        updateViewState(mOpened);
    }

    public static void verifyStoragePermissions(Activity activity) {
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 更新视图状态
     *
     * @param isSerialPortOpened
     */
    private void updateViewState(boolean isSerialPortOpened) {
        int stringRes = isSerialPortOpened ? R.string.close_serial_port : R.string.open_serial_port;
        binding.btnOpenDevice.setText(stringRes);
        binding.spinnerDevices.setEnabled(!isSerialPortOpened);
        binding.spinnerBaudrate.setEnabled(!isSerialPortOpened);
        binding.btnSendData.setEnabled(isSerialPortOpened);
        binding.btnLoadList.setEnabled(isSerialPortOpened);
    }

    /**
     * 初始化设备列表
     */
    private void initDevice() {
        PrefHelper.initDefault(this);
        SerialPortFinder serialPortFinder = new SerialPortFinder();
        // 设备
        mDevices = serialPortFinder.getAllDevicesPath();
        if (mDevices.length == 0) {
            mDevices = new String[]{
                    getString(R.string.no_serial_device)
            };
        }
        // 波特率
        mBaudrates = getResources().getStringArray(R.array.baudrates);

        mDeviceIndex = PrefHelper.getDefault().getInt(PreferenceKeys.SERIAL_PORT_DEVICES, 0);
        mDeviceIndex = mDeviceIndex >= mDevices.length ? mDevices.length - 1 : mDeviceIndex;
        mBaudrateIndex = PrefHelper.getDefault().getInt(PreferenceKeys.BAUD_RATE, 0);

        mDevice = new Device(mDevices[mDeviceIndex], mBaudrates[mBaudrateIndex], null);
    }


    /**
     * 初始化下拉选项
     */
    private void initSpinners() {
        ArrayAdapter<String> deviceAdapter =
                new ArrayAdapter<String>(this, R.layout.spinner_default_item, mDevices);
        deviceAdapter.setDropDownViewResource(R.layout.spinner_item);
        binding.spinnerDevices.setAdapter(deviceAdapter);
        binding.spinnerDevices.setOnItemSelectedListener(this);

        ArrayAdapter<String> baudrateAdapter =
                new ArrayAdapter<String>(this, R.layout.spinner_default_item, mBaudrates);
        baudrateAdapter.setDropDownViewResource(R.layout.spinner_item);
        binding.spinnerBaudrate.setAdapter(baudrateAdapter);
        binding.spinnerBaudrate.setOnItemSelectedListener(this);

        binding.spinnerDevices.setSelection(mDeviceIndex);
        binding.spinnerBaudrate.setSelection(mBaudrateIndex);
    }


    /**
     * 发送数据
     */
    public void onSend() {
        String sendContent = binding.etData.getText().toString().trim();
        if (TextUtils.isEmpty(sendContent)) {
            XLog.i( "onSend: 发送内容为 null");
            return;
        }
        byte[] sendContentBytes = sendContent.getBytes();
        //todo 这里默认发送一路串口，根据用户自定义
        boolean sendBytes = SerialUtils.getInstance().sendData(SerialPortEnum.SERIAL_ONE, sendContentBytes);
        XLog.i( "onSend: sendBytes = " + sendBytes);

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // Spinner 选择监听
        switch (parent.getId()) {
            case R.id.spinner_devices:
                mDeviceIndex = position;
                mDevice.setName(mDevices[mDeviceIndex]);
                break;
            case R.id.spinner_baudrate:
                mBaudrateIndex = position;
                mDevice.setRoot(mBaudrates[mBaudrateIndex]);
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    protected void onDestroy() {
        SerialUtils.getInstance().serialPortClose();
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshLogList();
    }

    /**
     * 初始化日志Fragment
     */
    protected void initFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        mLogFragment = (LogFragment) fragmentManager.findFragmentById(R.id.log_fragment);
    }


    /**
     * 刷新日志列表
     */
    protected void refreshLogList() {
        mLogFragment.updateAutoEndButton();
        mLogFragment.updateList();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(IMessage message) {
        // 收到时间，刷新界面
        mLogFragment.add(message);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConversionNotice(ConversionNoticeEvent messageEvent) {
        if (messageEvent.getMessage().equals("1")) {
            mConversionNotice = false;
        } else {
            mConversionNotice = true;
        }

    }


    /**
     * 字节数组转16进制
     *
     * @param bytes 需要转换的byte数组
     * @return 转换后的Hex字符串
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if (hex.length() < 2) {
                sb.append(0);
            }
            sb.append(hex);
        }
        return sb.toString();
    }


}