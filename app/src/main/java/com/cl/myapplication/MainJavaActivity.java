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
import android.widget.EditText;
import android.widget.Toast;

import com.cl.log.XLog;
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
import com.kongqw.serialportlibrary.ConfigurationSdk;
import com.kongqw.serialportlibrary.Device;
import com.kongqw.serialportlibrary.SerialPortFinder;
import com.kongqw.serialportlibrary.SerialPortManager;
import com.kongqw.serialportlibrary.listener.OnOpenSerialPortListener;
import com.kongqw.serialportlibrary.listener.OnSerialPortDataListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.Arrays;

public class MainJavaActivity extends AppCompatActivity implements OnOpenSerialPortListener, AdapterView.OnItemSelectedListener {

    private static final String TAG = MainJavaActivity.class.getSimpleName();
    private SerialPortManager mSerialPortManager;
    private ActivityMainJavaBinding binding;
    private Device mDevice;
    private byte[] b1 = {(byte) 33, (byte) -3};
    private String[] mDevices;
    private String[] mBaudrates;
    private int mDeviceIndex;
    private int mBaudrateIndex;
    private boolean mOpened = false;
    private boolean mConversionNotice = true;
    private LogFragment mLogFragment;

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
        SerialPortManager.getInstance().setOnOpenSerialPortListener(this);
//        //构建初始化参数
        ConfigurationSdk sdk = new ConfigurationSdk.ConfigurationBuilder(new File("/dev/ttyS4"), 115200)
                .log("TAG", true, false)
//                .msgHead(b1)   打开说明需要效验
                .build();
        mSerialPortManager = SerialPortManager.getInstance();
        mSerialPortManager.init(sdk, this);


        SerialPortManager.getInstance().setOnSerialPortDataListener(new OnSerialPortDataListener() {
            @Override
            public void onDataReceived(byte[] bytes) {
                Log.i(TAG, "onDataReceived [ byte[] ]: " + Arrays.toString(bytes));
                Log.i(TAG, "onDataReceived [ String ]: " + new String(bytes));
                if (mConversionNotice) {
                    LogManager.instance().post(new RecvMessage(bytesToHex(bytes)));
                } else {
                    LogManager.instance().post(new RecvMessage(Arrays.toString(bytes)));
                }

            }

            @Override
            public void onDataSent(byte[] bytes) {
                Log.i(TAG, "onDataSent [ byte[] ]: " + Arrays.toString(bytes));
                Log.i(TAG, "onDataSent [ String ]: " + new String(bytes));
                if (mConversionNotice) {
                    LogManager.instance().post(new SendMessage(bytesToHex(bytes)));
                } else {
                    LogManager.instance().post(new SendMessage(Arrays.toString(bytes)));
                }
            }
        });
//

        binding.btnOpenDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOpened) {
                    mSerialPortManager.closeSerialPort();
                    mOpened = false;
                } else {
                    // 打开串口
                    XLog.i(TAG, "打开的串口为：" + mDevice.getName() + "----" + Integer.parseInt(mDevice.getRoot()));
                    mSerialPortManager.openSerialPort(mDevice.getName(), Integer.parseInt(mDevice.getRoot()));
                }
                updateViewState(mOpened);
            }
        });


        binding.btnSendData.setOnClickListener((view) -> {
            onSend();
        });


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


    @Override
    public void onSuccess(File device) {
        ToastUtils.show("串口打开成功");
        mOpened = true;
        updateViewState(true);
    }

    @Override
    public void onFail(File device, Status status) {
        switch (status) {
            case NO_READ_WRITE_PERMISSION:
                ToastUtils.show("没有读写权限");
                updateViewState(false);
                break;
            case OPEN_FAIL:
            default:
                ToastUtils.show("串口打开失败");
                updateViewState(false);
                break;
        }
    }


    /**
     * 发送数据
     *
     */
    public void onSend() {
        String sendContent =  binding.etData.getText().toString().trim();
        if (TextUtils.isEmpty(sendContent)) {
            Log.i(TAG, "onSend: 发送内容为 null");
            return;
        }
        byte[] sendContentBytes = sendContent.getBytes();
        boolean sendBytes = mSerialPortManager.sendBytes(sendContentBytes);
        Log.i(TAG, "onSend: sendBytes = " + sendBytes);

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
        if (null != mSerialPortManager) {
            mSerialPortManager.closeSerialPort();
            mSerialPortManager = null;
        }
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