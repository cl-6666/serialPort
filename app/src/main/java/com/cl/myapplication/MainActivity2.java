package com.cl.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.kongqw.serialportlibrary.ConfigurationSdk;
import com.kongqw.serialportlibrary.Device;
import com.kongqw.serialportlibrary.SerialPortManager;
import com.kongqw.serialportlibrary.listener.OnOpenSerialPortListener;
import com.kongqw.serialportlibrary.listener.OnSerialPortDataListener;

import java.io.File;
import java.util.Arrays;

public class MainActivity2 extends AppCompatActivity implements OnOpenSerialPortListener {


    private static final String TAG = MainActivity2.class.getSimpleName();
    public static final String DEVICE = "device";
    private SerialPortManager mSerialPortManager;
    private Toast mToast;

    private byte[] b1 = {(byte) 33, (byte) -3};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        Device device = (Device) getIntent().getSerializableExtra(DEVICE);
        Log.i(TAG, "onCreate: device = " + device);
        if (null == device) {
            finish();
            return;
        }

        //构建初始化参数
        ConfigurationSdk sdk = new ConfigurationSdk.ConfigurationBuilder(device.getFile(), 115200)
                .log("TAG", true, false)
                .msgHead(b1)
                .build();
        SerialPortManager.getInstance().init(sdk,this);

        // 打开串口
        SerialPortManager.getInstance().setOnOpenSerialPortListener(this)
                .setOnSerialPortDataListener(new OnSerialPortDataListener() {
                    @Override
                    public void onDataReceived(byte[] bytes) {
                        Log.i(TAG, "onDataReceived [ byte[] ]: " + Arrays.toString(bytes));
                        Log.i(TAG, "onDataReceived [ String ]: " + new String(bytes));
                        final byte[] finalBytes = bytes;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showToast(String.format("接收\n%s", new String(finalBytes)));
                            }
                        });
                    }

                    @Override
                    public void onDataSent(byte[] bytes) {
                        Log.i(TAG, "onDataSent [ byte[] ]: " + Arrays.toString(bytes));
                        Log.i(TAG, "onDataSent [ String ]: " + new String(bytes));
                        final byte[] finalBytes = bytes;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showToast(String.format("发送\n%s", new String(finalBytes)));
                            }
                        });
                    }
                });

    }


    @Override
    public void onSuccess(File device) {
        showToast("串口打开成功");
    }

    @Override
    public void onFail(File device, Status status) {
        switch (status) {
            case NO_READ_WRITE_PERMISSION:
                showToast("没有读写权限");
                break;
            case OPEN_FAIL:
            default:
                showToast("串口打开失败");
                break;
        }
    }


    /**
     * 发送数据
     *
     * @param view view
     */
    public void onSend(View view) {
        EditText editTextSendContent = (EditText) findViewById(R.id.et_send_content);
        if (null == editTextSendContent) {
            return;
        }
        String sendContent = editTextSendContent.getText().toString().trim();
        if (TextUtils.isEmpty(sendContent)) {
            Log.i(TAG, "onSend: 发送内容为 null");
            return;
        }

        byte[] sendContentBytes = sendContent.getBytes();

        boolean sendBytes = mSerialPortManager.sendBytes(sendContentBytes);
        Log.i(TAG, "onSend: sendBytes = " + sendBytes);
        showToast(sendBytes ? "发送成功" : "发送失败");
    }


    /**
     * Toast
     *
     * @param content content
     */
    private void showToast(String content) {
        if (null == mToast) {
            mToast = Toast.makeText(getApplicationContext(), null, Toast.LENGTH_SHORT);
        }
        mToast.setText(content);
        mToast.show();
    }

    @Override
    protected void onDestroy() {
        if (null != mSerialPortManager) {
            mSerialPortManager.closeSerialPort();
            mSerialPortManager = null;
        }
        super.onDestroy();
    }
}