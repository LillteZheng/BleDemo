package com.cvte.blesdk.server

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.os.Message
import com.cvte.blesdk.BleError
import com.cvte.blesdk.DataPackageManager
import com.cvte.blesdk.BleSdk
import com.cvte.blesdk.DATA_TYPE
import com.cvte.blesdk.GattStatus
import com.cvte.blesdk.MTU_TYPE
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
        private const val MSG_RESTART_AD = 0x02
        private const val TIME_OUT = 1000L
    }

    private var option: BleServerOption.Builder? = null
    private var iBleListener: IServerBle.IBleEventListener? = null
    private var gattServer: ServerGattChar? = null
    private var bleAdvServer: BleAdvServer? = null

    private var clientName = "null"

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
        closeServer()
        //开启广播
        if (bleAdvServer == null) {
            bleAdvServer = BleAdvServer(bluetoothAdapter!!)
        }
        bluetoothAdapter?.name = option?.name
        bleAdvServer?.startBroadcast(advertiseCallback)
        pushLog("start advertise")

    }


    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when(msg.what){
            MSG_WAIT_NAME->{
                val name = msg.obj?.let { mag->
                    mag as String
                }?:"null"
                iBleListener?.onEvent(ServerStatus.CLIENT_CONNECTED, name)
            }
            MSG_RESTART_AD->{
                if (bleAdvServer == null) {
                    bleAdvServer = BleAdvServer(bluetoothAdapter!!)
                }
                bleAdvServer?.startBroadcast(advertiseCallback)
            }
        }

    }

    private fun pushLog(msg: String) {
        option?.logListener?.onLog(msg)
    }

    override fun release() {
        DataPackageManager.resetBuffer()
        bleAdvServer?.stopBroadcast()
        bleAdvServer = null
        gattServer?.release()
        gattServer = null
        releaseHandle()

    }



    private fun isGattConnected(): Boolean {
        return gattServer?.isConnected() ?: false
    }


    override fun closeServer() {
        DataPackageManager.resetBuffer()
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

                override fun onEvent(status: GattStatus, obj: String?) {
                    pushLog("server status change:$status")
                    when (status) {
                        GattStatus.CLIENT_READ -> {
                          //  pushLog("receiver data:${String(obj as ByteArray)}")
                            iBleListener?.onEvent(ServerStatus.CLIENT_WRITE, obj)
                        }

                        GattStatus.SERVER_DISCONNECTED -> {
                            pushLog("client ($clientName) disconnect,reset advertise and gatt service")
                            iBleListener?.onEvent(ServerStatus.CLIENT_DISCONNECT,clientName)
                            closeServer()
                            handler?.sendEmptyMessageDelayed(MSG_RESTART_AD,500)
                        }

                        GattStatus.SERVER_CONNECTED->{
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
                            clientName = obj!!
                            handler?.removeMessages(MSG_WAIT_NAME)
                            Message.obtain().apply {
                                what = MSG_WAIT_NAME
                                this.obj = obj
                                handler?.sendMessage(this)
                            }
                        }
                        GattStatus.MTU->{
                            sendData(MTU_TYPE,byteArrayOf(0x00))
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
        sendData(DATA_TYPE,data)
    }

    private fun sendData(type:Byte,data: ByteArray){
        DataPackageManager.splitPacket(data, type, object : DataPackageManager.ISplitListener {
            override fun onResult(data: ByteArray) {
                gattServer?.send(data)
            }

        })
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