package com.cl.myapplication

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.cl.myapplication.databinding.ActivityMainBinding
import com.cl.serialportlibrary.Device
import com.cl.serialportlibrary.SimpleSerialPortManager

class MainActivity : AppCompatActivity(){

    private val TAG = MainActivity::class.java.simpleName
    val DEVICE = "device"
    private var isSerialPortOpened = false
    private var mToast: Toast? = null
    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        val device = intent.getSerializableExtra(DEVICE) as Device?
        Log.i(TAG, "onCreate: device = $device")
        if (null == device) {
            finish()
            return
        }
        
        // 使用SimpleSerialPortManager打开串口
        val devicePath = device.name
        val baudRate = device.root.toInt()
        SimpleSerialPortManager.getInstance().openSerialPort(devicePath, baudRate) { data ->
            runOnUiThread {
                val receivedData = String(data)
                Log.i(TAG, "接收到数据: $receivedData")
//                binding.tvReceiveContent.text = receivedData
            }
        }
        isSerialPortOpened = true
    }


    fun onSend(view: View) {
        val editTextSendContent = binding.etSendContent.text.toString()
        if (TextUtils.isEmpty(editTextSendContent)) {
            Log.i(TAG, "onSend: 发送内容为 null")
            return
        }
        val sendContentBytes = editTextSendContent.toByteArray()
        val sendBytes = SimpleSerialPortManager.getInstance().sendData(sendContentBytes)
        Log.i(TAG, "onSend: sendBytes = $sendBytes")
        showToast(if (sendBytes) "发送成功" else "发送失败")
    }


    fun onDestroy(view: View) {
        if (isSerialPortOpened) {
            SimpleSerialPortManager.getInstance().closeSerialPort()
            isSerialPortOpened = false
        }
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isSerialPortOpened) {
            SimpleSerialPortManager.getInstance().closeSerialPort()
        }
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