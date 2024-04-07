package com.cl.myapplication

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kongqw.serialportlibrary.Device
import com.kongqw.serialportlibrary.SerialPortManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(){

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
    }


    fun onSend(view: View) {
        val editTextSendContent = et_send_content.text.toString()
        if (TextUtils.isEmpty(editTextSendContent)) {
            Log.i(TAG, "onSend: 发送内容为 null")
            return
        }
        val sendContentBytes = editTextSendContent.toByteArray()
        val sendBytes = mSerialPortManager?.sendBytes(sendContentBytes)
        Log.i(
            TAG,
            "onSend: sendBytes = $sendBytes"
        )
        showToast(if (sendBytes == true) "发送成功" else "发送失败")
    }


    fun onDestroy(view: View){

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