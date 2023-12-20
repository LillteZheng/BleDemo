package com.cvte.blesdk.server

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.media.MediaSession2Service
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import com.cvte.blesdk.BleError
import com.cvte.blesdk.BleSdk
import com.cvte.blesdk.DATA_TYPE
import com.cvte.blesdk.GattStatus
import com.cvte.blesdk.ServerStatus
import com.cvte.blesdk.abs.AbsBle
import com.cvte.blesdk.abs.IBle
import com.cvte.blesdk.characteristic.AbsCharacteristic
import com.cvte.blesdk.characteristic.ServerGattChar

/**
 * @author by zhengshaorui 2023/12/12
 * describe：蓝牙服务端，主要负责发送广播，开启蓝牙服务
 */
class BleServer : AbsBle(BleSdk.context),IServerBle {
    companion object {
        private const val TAG = "BleServer"
        private const val MAX_NAME_SIZE = 20
        private const val MSG_WAIT_NAME = 0x01
        private const val TIME_OUT = 1000L
    }

    private var option: BleServerOption.Builder? = null
    private var iBleListener: IServerBle.IBleEventListener? = null
    private var gattServer: ServerGattChar? = null
    private var bleAdvServer: BleAdvServer? = null

    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    override fun startServer(bleOption: BleServerOption, iBleListener: IServerBle.IBleEventListener) {
        this.option = bleOption.builder
        this.iBleListener = iBleListener
        if (!checkPermission(iBleListener)) {
            return
        }
        if (isGattConnected()){
            iBleListener.onFail(BleError.ADVERTISE_FAILED,
                "gatt is connected,please disconnect first")
            return
        }

        if (handler == null) {
            handlerThread = object : HandlerThread("ble_server") {
                override fun onLooperPrepared() {
                    super.onLooperPrepared()
                    pushLog("wait name")
                    handler = Handler(Looper.myLooper()!!, callback)
                }
            }
            handlerThread?.start()
        }
        //先关闭之前的广播和服务
        closeServer()
        //开启广播
        if (bleAdvServer == null) {
            bleAdvServer = BleAdvServer(bluetoothAdapter!!)
        }
        bluetoothAdapter?.name = option?.name
        bleAdvServer?.startBroadcast(advertiseCallback)
        pushLog("start advertise")

    }

    private val callback = Handler.Callback {
        pushLog("wait name timeout: ${it.obj} ${it.what}")
        if (it.what == MSG_WAIT_NAME) {
            iBleListener?.onEvent(ServerStatus.CLIENT_CONNECTED, it.obj as String)
        }
        true
    }

    private fun pushLog(msg: String) {
        option?.logListener?.onLog(msg)
    }

    override fun release() {
        bleAdvServer?.stopBroadcast()
        bleAdvServer = null
        gattServer?.release()
        gattServer = null
        handlerThread?.quit()
        handlerThread = null
        handler = null

    }



    private fun isGattConnected(): Boolean {
        return gattServer?.isConnected() ?: false
    }


    override fun closeServer() {
        pushLog("close server if need: bleAdvServer:$bleAdvServer, gattServer:$gattServer")
        bleAdvServer?.stopBroadcast()
        gattServer?.release()
        bleAdvServer = null
        gattServer = null
    }


    private fun startGattService() {
        pushLog("start gatt service: $gattServer")
        if (gattServer == null) {
            gattServer = ServerGattChar(object : AbsCharacteristic.IGattListener {

                override fun onEvent(status: GattStatus, obj: Any?) {
                    pushLog("server status change:$status")
                    when (status) {
                        GattStatus.CLIENT_READ -> {
                          //  pushLog("receiver data:${String(obj as ByteArray)}")
                            iBleListener?.onEvent(ServerStatus.CLIENT_WRITE, obj)
                        }

                        GattStatus.SERVER_DISCONNECTED -> {
                            pushLog("client ($obj) disconnect,reset advertise and gatt service")
                            closeServer()
                            bleAdvServer?.startBroadcast(advertiseCallback)
                            iBleListener?.onEvent(ServerStatus.CLIENT_DISCONNECT,obj)
                        }

                        GattStatus.SERVER_CONNECTED->{
                            /*obj?.let {
                                val mac = obj as String
                                bluetoothAdapter?.getRemoteDevice(mac)?.let {
                                    iBleListener?.onEvent(ServerStatus.CLIENT_CONNECTED,it)
                                }
                            }
                            pushLog("client ($obj) connected")
                            iBleListener?.onEvent(ServerStatus.CLIENT_CONNECTED,obj)*/
                            //为了拿到日志，需要等发送端发过来，等待1秒
                            handler?.removeMessages(MSG_WAIT_NAME)
                            Message.obtain().apply {
                                what = MSG_WAIT_NAME
                                this.obj = obj
                                handler?.sendMessageDelayed(this, TIME_OUT)
                            }
                        }
                        GattStatus.BLUE_NAME->{
                            pushLog("client name:$obj")
                            //iBleListener?.onEvent(ServerStatus.CLIENT_CONNECTED,obj)
                            handler?.removeMessages(MSG_WAIT_NAME)
                            Message.obtain().apply {
                                what = MSG_WAIT_NAME
                                this.obj = obj
                                handler?.sendMessage(this)
                            }
                        }
                        else -> {
                            pushLog("$obj ,  status change:$status")
                        }
                    }
                }

            })
        }
        gattServer?.startGattService(option!!)
    }


    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            pushLog("advertise success,try to start gatt service")
            iBleListener?.onEvent(ServerStatus.ADVERTISE_SUCCESS,bluetoothAdapter?.name)
            startGattService()
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            closeServer()
            when (errorCode) {
                ADVERTISE_FAILED_DATA_TOO_LARGE -> {
                    fail(BleError.ADVERTISE_FAILED, "advertise data too large,over 31 bytes")
                }

                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> {
                    fail(BleError.ADVERTISE_FAILED, "too many advertisers")
                }

                ADVERTISE_FAILED_ALREADY_STARTED -> {
                    fail(BleError.ADVERTISE_FAILED, "advertise already started")
                }

                ADVERTISE_FAILED_INTERNAL_ERROR -> {
                    fail(BleError.ADVERTISE_FAILED, "advertise internal error")
                }

                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> {
                    fail(BleError.ADVERTISE_FAILED, "advertise feature unsupported")
                }

                else -> {
                    fail(BleError.ADVERTISE_FAILED, "advertise failed: $errorCode")
                }
            }
        }
    }

    override fun send(data: ByteArray){
        subSend(data, DATA_TYPE)
    }

    override fun sendData(data: ByteArray) {
        gattServer?.send(data)
    }


    private fun fail(error: BleError, msg: String) {
        iBleListener?.onFail(error, msg)
    }


    override fun checkPermission(listener: IBle.IListener): Boolean {
        var permission =  super.checkPermission(listener)
        if(option?.name == null || option?.name?.length!! > MAX_NAME_SIZE){
            listener.onFail(BleError.PERMISSION_DENIED,"name is null or length > $MAX_NAME_SIZE")
            permission = false
        }
        return permission
    }


}