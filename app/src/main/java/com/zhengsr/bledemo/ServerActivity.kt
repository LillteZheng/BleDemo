package com.zhengsr.bledemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.cvte.blesdk.BleError
import com.cvte.blesdk.BleSdk
import com.cvte.blesdk.server.BleServer
import com.zhengsr.bledemo.databinding.ActivityClientBinding
import com.zhengsr.bledemo.databinding.ActivityServerBinding

class ServerActivity : AppCompatActivity() {
    companion object{
        private const val TAG = "MainActivity"
    }
    private lateinit var binding: ActivityServerBinding
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       // setContentView(R.layout.main_layout)
         binding = ActivityServerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            ), 1)


        //在 Android 10 还需要开启 gps,搜索才需要
     /*   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val lm: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                Toast.makeText(this@MainActivity, "请您先开启gps,否则蓝牙不可用", Toast.LENGTH_SHORT).show()
            }
        }*/
    }

    @SuppressLint("MissingPermission")
    fun openServer(view: View) {
        BleSdk.getServer().initBle("Vieunite_12345688", object : BleServer.IBleServerCallback {
            override fun onLog(msg: String) {
                Log.d(TAG, "onLog: $msg")
               appInfo(msg)
            }

            override fun onFail(errorCode: BleError, msg: String) {
                appInfo( "fail: errorCode = $errorCode, msg = $msg")

            }

            override fun onSuccess(name: String?) {
                appInfo("广播启动成功，请搜索: name = $name")
            }

        })



    }

    private fun appInfo(msg:String){
        runOnUiThread {
            binding.textInfo.append(msg+"\n")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        BleSdk.getServer().release()
    }

    fun closeServer(view: View) {
       // BleSdk.getServer().closeServer()

    }
}



