package com.zhengsr.bledemo

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.cvte.blesdk.server.BleServerOption

/**
 * @author by zhengshaorui 2023/12/14
 * describe：
 */
class MainActivity :AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val bluetooth = BluetoothAdapter.getDefaultAdapter()
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ), 1)
        if (bluetooth == null) {
            Toast.makeText(this, "您的设备未找到蓝牙驱动！!", Toast.LENGTH_SHORT).show()
            finish()
        }else {
            if (!bluetooth.isEnabled) {
                startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),1)
            }
        }


    }

    fun openServer(view: View) {
        startActivity(Intent(this,ServerActivity::class.java))
    }
    fun openClient(view: View) {
        startActivity(Intent(this,ClientActivity::class.java))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1){
            if (resultCode == Activity.RESULT_CANCELED){
                Toast.makeText(this, "请您不要拒绝开启蓝牙，否则应用无法运行", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

}