package com.cl.myapplication;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentManager;

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
import com.cl.serialportlibrary.Device;
import com.cl.serialportlibrary.SerialPortFinder;
import com.cl.serialportlibrary.SimpleSerialPortManager;
import com.cl.serialportlibrary.utils.SerialPortLogUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;

/**
 * 单串口演示
 */
public class SingleSerialPortActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private ActivityMainJavaBinding binding;
    private Device mDevice;

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

        //设置数据位
        SpAdapter spAdapter1 = new SpAdapter(this);
        spAdapter1.setDatas(databits);
        binding.spDatabits.setAdapter(spAdapter1);
        binding.spDatabits.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                closeSerialPort();
                int dataBitValue = Integer.parseInt(databits[position]);
                SimpleSerialPortManager.getInstance().setDatabits(dataBitValue);
                SerialPortLogUtil.i("MainJavaActivity", "设置数据位: " + dataBitValue);
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
                SimpleSerialPortManager.getInstance().setParity(position);
                SerialPortLogUtil.i("MainJavaActivity", "设置校验位: " + paritys[position] + " (值: " + position + ")");
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
                int stopBitValue = Integer.parseInt(stopbits[position]);
                SimpleSerialPortManager.getInstance().setStopbits(stopBitValue);
                SerialPortLogUtil.i("MainJavaActivity", "设置停止位: " + stopBitValue);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        binding.btnOpenDevice.setOnClickListener(v -> {
            if (mOpened) {
                closeSerialPort();
            } else {
                openSerialPort();
            }
        });

        binding.btnSendData.setOnClickListener((view) -> {
            onSend();
        });
    }
    
    /**
     * 打开串口
     */
    private void openSerialPort() {
        String devicePath = mDevice.getName();
        int baudRate = Integer.parseInt(mDevice.getRoot());
        
        SerialPortLogUtil.i("MainJavaActivity", "打开的串口为：" + devicePath + "----" + baudRate);
        
        // 使用SimpleSerialPortManager打开串口
        boolean success = SimpleSerialPortManager.getInstance()
                .openSerialPort(devicePath, baudRate,
                        // 打开状态回调
                        (isSuccess, status) -> {
                            runOnUiThread(() -> {
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
                            });
                        },
                        // 数据接收回调
                        new SimpleSerialPortManager.OnDataReceivedCallback() {
                            @Override
                            public void onDataReceived(byte[] data) {
                                SerialPortLogUtil.i("MainJavaActivity", "onDataReceived [ byte[] ]: " + Arrays.toString(data));
                                SerialPortLogUtil.i("MainJavaActivity", "onDataReceived [ String ]: " + new String(data));
                                
                                runOnUiThread(() -> {
                                    if (mConversionNotice) {
                                        LogManager.instance().post(new RecvMessage(bytesToHex(data)));
                                    } else {
                                        LogManager.instance().post(new RecvMessage(Arrays.toString(data)));
                                    }
                                });
                            }
                            
                            @Override
                            public void onDataSent(byte[] data) {
                                SerialPortLogUtil.i("MainJavaActivity", "onDataSent [ byte[] ]: " + Arrays.toString(data));
                                SerialPortLogUtil.i("MainJavaActivity", "onDataSent [ String ]: " + new String(data));
                                
                                runOnUiThread(() -> {
                                    if (mConversionNotice) {
                                        LogManager.instance().post(new SendMessage(bytesToHex(data)));
                                    } else {
                                        LogManager.instance().post(new SendMessage(Arrays.toString(data)));
                                    }
                                });
                            }
                        });
    }
    
    private void closeSerialPort() {
        SimpleSerialPortManager.getInstance().closeSerialPort();
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
            SerialPortLogUtil.i("MainJavaActivity", "onSend: 发送内容为 null");
            return;
        }
        byte[] sendContentBytes = sendContent.getBytes();
        // 使用SimpleSerialPortManager发送数据
        boolean sendBytes = SimpleSerialPortManager.getInstance().sendData(sendContentBytes);
        SerialPortLogUtil.i("MainJavaActivity", "onSend: sendBytes = " + sendBytes);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // Spinner 选择监听
        int parentId = parent.getId();
        if (parentId == R.id.spinner_devices) {
            mDeviceIndex = position;
            mDevice.setName(mDevices[mDeviceIndex]);
        } else if (parentId == R.id.spinner_baudrate) {
            mBaudrateIndex = position;
            mDevice.setRoot(mBaudrates[mBaudrateIndex]);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    protected void onDestroy() {
        SimpleSerialPortManager.getInstance().closeSerialPort();
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