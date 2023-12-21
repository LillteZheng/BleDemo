package com.zhengsr.client.client

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import com.zhengsr.client.BleError

import com.zhengsr.common.BleUtil

/**
 * @author by zhengshaorui 2023/12/13
 * describe：
 */
abstract class AbsBle{



    private var handlerThread: HandlerThread? = null
    protected var handler: Handler? = null
    private val callback = android.os.Handler.Callback {
        handleMessage(it)
        true
    }
    protected open fun initHandle(){
        if (handler == null) {
            handlerThread = object : HandlerThread("ble_server") {
                override fun onLooperPrepared() {
                    super.onLooperPrepared()
                    handler = Handler(Looper.myLooper()!!, callback)
                }
            }
            handlerThread?.start()
        }
    }

    protected open fun handleMessage(msg:Message){}



    protected var bluetoothAdapter: BluetoothAdapter? = null

    protected open fun checkPermission(context: Context?,listener: IBle.IListener):Boolean{
        if (context == null) {
            listener.onFail(BleError.CONTEXT_NULL,"context is null,please use BleOption.context(context) first")
            return false
        }
        val bluetoothManager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter?.isEnabled == false){
            listener.onFail(BleError.BLUETOOTH_NOT_OPEN,"bluetooth not open")
            return false
        }

        if (!BleUtil.isBleSupport(context)) {
            listener.onFail(BleError.BLE_NOT_SUPPORT,"bluetooth not support")
            return false
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (!BleUtil.isHasBlePermission(context)){
                val msg =
                    "BLUETOOTH_ADVERTISE | BLUETOOTH_CONNECT"

                listener.onFail(
                    BleError.PERMISSION_DENIED,"ble permission denied，" +
                        "make sure you have add  permission($msg)")
                return false
            }


        }
        return true
    }




    fun releaseHandle(){
        handlerThread?.quitSafely()
        handlerThread = null
        handler = null
    }







}