package com.cl.myapplication

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kongqw.serialportlibrary.Device
import com.kongqw.serialportlibrary.SerialPortManager
import com.kongqw.serialportlibrary.listener.OnOpenSerialPortListener
import com.kongqw.serialportlibrary.listener.OnSerialPortDataListener
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity(), OnOpenSerialPortListener {

    private val TAG = MainActivity::class.java.simpleName
    val DEVICE = "device"
    private var mSerialPortManager: SerialPortManager? = null
    private var mToast: Toast? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val device = intent.getSerializableExtra(DEVICE) as Device?
        Log.i(TAG, "onCreate: device = $device")
        if (null == device) {
            finish()
            return
        }
        mSerialPortManager = SerialPortManager()

        // 打开串口

        // 打开串口
        val openSerialPort = mSerialPortManager!!.setOnOpenSerialPortListener(this)
            .setOnSerialPortDataListener(object : OnSerialPortDataListener {
                override fun onDataReceived(bytes: ByteArray) {
                    Log.i(
                        TAG,
                        "onDataReceived [ byte[] ]: " + Arrays.toString(bytes)
                    )
                    Log.i(
                        TAG,
                        "onDataReceived [ String ]: " + String(bytes)
                    )
                    runOnUiThread { showToast(String.format("接收\n%s", String(bytes))) }
                }

                override fun onDataSent(bytes: ByteArray) {
                    Log.i(
                        TAG,
                        "onDataSent [ byte[] ]: " + Arrays.toString(bytes)
                    )
                    Log.i(
                        TAG,
                        "onDataSent [ String ]: " + String(bytes)
                    )
                    runOnUiThread { showToast(String.format("发送\n%s", String(bytes))) }
                }
            })
            .openSerialPort(device.file, 115200)

        Log.i(
            TAG,
            "onCreate: openSerialPort = $openSerialPort"
        )
    }


    override fun onSuccess(device: File?) {
        Toast.makeText(
            this,
            String.format("串口 [%s] 打开成功", device!!.path),
            Toast.LENGTH_SHORT
        ).show()

    }

    override fun onFail(device: File?, status: OnOpenSerialPortListener.Status?) {

    }

    fun onSend(view: View) {
        val editTextSendContent = et_send_content.text.toString()
        if (TextUtils.isEmpty(editTextSendContent)) {
            Log.i(TAG, "onSend: 发送内容为 null")
            return
        }
        val sendContentBytes = editTextSendContent.toByteArray()
        val sendBytes = mSerialPortManager!!.sendBytes(sendContentBytes)
        Log.i(
            TAG,
            "onSend: sendBytes = $sendBytes"
        )
        showToast(if (sendBytes) "发送成功" else "发送失败")
    }


    /**
     * Toast
     *
     * @param content content
     */
    private fun showToast(content: String) {
        if (null == mToast) {
            mToast = Toast.makeText(applicationContext, null, Toast.LENGTH_SHORT)
        }
        mToast?.setText(content)
        mToast?.show()
    }

}