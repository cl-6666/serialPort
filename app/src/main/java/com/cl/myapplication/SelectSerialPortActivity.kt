package com.cl.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity
import com.cl.myapplication.adapter.DeviceAdapter
import com.kongqw.serialportlibrary.SerialPortFinder
import kotlinx.android.synthetic.main.activity_select_serial_port.*

class SelectSerialPortActivity : AppCompatActivity(), OnItemClickListener {


    private var mDeviceAdapter: DeviceAdapter? = null
    val DEVICE = "device"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_serial_port)

        val serialPortFinder = SerialPortFinder()
        val devices = serialPortFinder.devices
        if (lv_devices != null) {
            lv_devices.emptyView = tv_empty
            mDeviceAdapter = DeviceAdapter(applicationContext, devices)
            lv_devices.adapter = mDeviceAdapter
            lv_devices.onItemClickListener = this
        }

    }

    override fun onItemClick(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
        val device = mDeviceAdapter!!.getItem(position)
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(DEVICE, device)
        startActivity(intent)
    }
}