package com.cvte.blesdk.abs

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import androidx.core.util.forEach
import com.cvte.blesdk.BleError
import com.cvte.blesdk.DATA_FLAG
import com.cvte.blesdk.utils.BleUtil

/**
 * @author by zhengshaorui 2023/12/13
 * describe：
 */
abstract class AbsBle(val context: Context?) {

    companion object{
          var MAX_DATA_SIZE = 15
    }

    private var handlerThread: HandlerThread? = null
    protected var handler: Handler? = null
    init {
        if (handler == null) {
            handlerThread = object : HandlerThread("ble_server") {
                override fun onLooperPrepared() {
                    super.onLooperPrepared()
                    handler = android.os.Handler(Looper.myLooper()!!, callback)
                }
            }
            handlerThread?.start()
        }
    }

    private val callback = android.os.Handler.Callback {
        handleMessage(it)
        true
    }

    protected open fun handleMessage(msg:Message){}



    protected val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    protected open fun checkPermission(listener:IBle.IListener):Boolean{
        if (context == null){
            listener.onFail(BleError.CONTEXT_NULL,"context is null,please use BleSdk.init(context) first")
            return false
        }

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