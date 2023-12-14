package com.cvte.blesdk.server

import android.Manifest
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.Build
import com.cvte.blesdk.BleError
import com.cvte.blesdk.GattStatus
import com.cvte.blesdk.abs.AbsBle
import com.cvte.blesdk.abs.IBle
import com.cvte.blesdk.characteristic.AbsCharacteristic
import com.cvte.blesdk.characteristic.ServerGattChar
import com.cvte.blesdk.utils.BleUtil

/**
 * @author by zhengshaorui 2023/12/12
 * describe：蓝牙服务端，主要负责发送广播，开启蓝牙服务
 */
class BleServer(context: Context?) :AbsBle(context){
    companion object{
        private const val TAG = "BleServer"
        private const val MAX_NAME_SIZE = 20
    }
    private var gattServer: ServerGattChar? = null
    private var listener:IBleServerCallback? = null

    private var bleAdvServer: BleAdvServer? = null
    fun initBle(name:String, callback:IBleServerCallback){
        listener = callback

        if (!checkPermission(callback)){
            return
        }
        if (name.length > MAX_NAME_SIZE){
            callback.onFail(BleError.NAME_TOO_LONG,"name length must less than $MAX_NAME_SIZE")
            return
        }

        bluetoothAdapter?.name = name
        bleAdvServer = BleAdvServer(bluetoothAdapter!!)
        bleAdvServer?.startBroadcast(advertiseCallback)
    }

    override fun checkPermission(listener: IBle): Boolean {
        var permission =  super.checkPermission(listener)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!BleUtil.isPermission(
                    context?.applicationContext,
                    Manifest.permission.BLUETOOTH_ADMIN
                )){
                listener.onFail(BleError.PERMISSION_DENIED, "BLUETOOTH_CONNECT permission denied")
                permission = false
            }
        }

        return permission
    }

    fun release(){
        bleAdvServer?.stopBroadcast()
        bleAdvServer = null
        gattServer?.release()
        gattServer = null
    }


    interface IBleServerCallback:IBle{
        fun onSuccess(name: String?)

    }

    fun closeServer() {
        bleAdvServer?.stopBroadcast()
        gattServer?.release()
    }

    private fun startGattService(){
        if (gattServer == null) {
            gattServer = ServerGattChar(object : AbsCharacteristic.IGattListener {

                override fun onEvent(status: GattStatus, obj: Any?) {
                    when(status){
                        GattStatus.SERVER_WRITE->{
                            listener?.onLog("client: write data:${String(obj as ByteArray)}")
                        }
                        GattStatus.SERVER_DISCONNECTED->{
                            listener?.onLog("$obj ,  status change:$status,重新启动广播")
                            closeServer()
                            bleAdvServer?.startBroadcast(advertiseCallback)
                        }
                        else ->{
                            listener?.onLog("$obj ,  status change:$status")
                        }
                    }
                }

            })
        }
        gattServer?.startGattService()
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            listener?.onSuccess(bluetoothAdapter?.name)
            startGattService()
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            when (errorCode) {
                ADVERTISE_FAILED_DATA_TOO_LARGE -> {
                    listener?.onFail(BleError.ADVERTISE_FAILED,"advertise data too large,over 31 bytes")
                }
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> {
                    listener?.onFail(BleError.ADVERTISE_FAILED,"too many advertisers")
                }
                ADVERTISE_FAILED_ALREADY_STARTED -> {
                    listener?.onFail(BleError.ADVERTISE_FAILED,"advertise already started")
                }
                ADVERTISE_FAILED_INTERNAL_ERROR -> {
                    listener?.onFail(BleError.ADVERTISE_FAILED,"advertise internal error")
                }
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> {
                    listener?.onFail(BleError.ADVERTISE_FAILED,"advertise feature unsupported")
                }
                else -> {
                    listener?.onFail(BleError.ADVERTISE_FAILED,"advertise failed")
                }
            }
        }
    }
}