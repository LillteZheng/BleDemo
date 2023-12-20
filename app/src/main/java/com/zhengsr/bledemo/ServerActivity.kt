package com.zhengsr.bledemo

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.cvte.blesdk.BleError
import com.cvte.blesdk.BleSdk
import com.cvte.blesdk.ServerStatus
import com.cvte.blesdk.server.BleServerOption
import com.cvte.blesdk.server.IServerBle
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
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
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
        val option = BleServerOption.Builder()
            .name("Vieunite_345663")
            .logListener(object : BleServerOption.ILogListener {
                override fun onLog(log: String) {
                    Log.d(TAG, "$log")
                }
            }).build()

        BleSdk.getServer().startServer(option, object : IServerBle.IBleEventListener {
            override fun onEvent(serverStatus: ServerStatus, obj: String?) {
                when(serverStatus){
                    ServerStatus.ADVERTISE_SUCCESS -> {
                        appInfo("开启广播成功，请搜索设备：$obj")
                    }
                    ServerStatus.CLIENT_CONNECTED -> {
                        appInfo("设备($obj)，连接成功，可以通信了")
                    }
                    ServerStatus.CLIENT_DISCONNECT -> {
                        appInfo("设备($obj)，断开连接")
                    }
                    ServerStatus.CLIENT_WRITE->{
                        appInfo("收到数据: $obj")
                    }
                    else -> {
                        appInfo("事件: serverStatus = $serverStatus, obj = $obj")
                    }
                }
            }


            override fun onFail(error: BleError, errorMsg: String) {
                appInfo("失败: error = $error, errorMsg = $errorMsg")
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
      //  BleSdk.getServer().release()
        BleSdk.getServer().release()
    }

    fun send(view: View) {
        Log.d(TAG, "zsr send: ${msg.length} ${msg.toByteArray().size}")
        val msg = binding.editMsg.text.trim().toString()
        BleSdk.getServer().send(msg.toByteArray())
    }

    private val msg = """
        123344
        So I’ve faced some issues with a BLE read, and here is the best summary I have:

        “Receive String” works on a READ characteristic
        “Receive Byte Array” returns on error on the exact same characteristic.
        Here is my case, I can connect to a BLE device, and then read on a button click. 
        Using a counter, I can alternative between the two reads. The read string version returns 
        the data, but the Receive Byte array always flags an error. Ultimately, 
        I need the Read Byte Array for my application, but I haven’t been able to debug 
        the error difference between the calls.
    """.trimIndent()
}



