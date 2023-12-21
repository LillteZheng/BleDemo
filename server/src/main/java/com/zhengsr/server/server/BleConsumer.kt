package com.zhengsr.server.server

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.Message
import com.zhengsr.common.DATA_TYPE
import com.zhengsr.common.DataSpilt
import com.zhengsr.server.BleAdvServer
import com.zhengsr.server.BleError
import com.zhengsr.server.BleOption
import com.zhengsr.server.BleStatus
import com.zhengsr.server.GattStatus
import com.zhengsr.server.gatt.AbsCharacteristic
import com.zhengsr.server.gatt.ServerGattChar


/**
 * @author by zhengshaorui 2023/12/12
 * describe：蓝牙服务端，主要负责发送广播，开启蓝牙服务
 */
internal class BleConsumer : AbsBle(), IBle {
    companion object {
        private const val TAG = "BleServer"
        private const val MAX_NAME_SIZE = 20
        private const val MSG_WAIT_NAME = 0x01
        private const val MSG_RESTART_AD = 0x02
        private const val TIME_OUT = 1000L
    }

    private var listener: IBle.IListener? = null
    private var gattServer: ServerGattChar? = null
    private var bleAdvServer: BleAdvServer? = null

    private var clientName = "null"
    private var mtu = 15
    private var option: BleOption.Builder? = null
    override fun startServer(builder: BleOption, listener: IBle.IListener) {
        this.option = builder.builder
        this.listener = listener
        if (!checkPermission(option?.context,listener)) {
            return
        }
        if (isGattConnected()) {
            listener.onFail(
                BleError.ADVERTISE_FAILED,
                "gatt is connected,please disconnect first"
            )
            return
        }
        if (handler == null) {
            initHandle()
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
        when (msg.what) {
            MSG_WAIT_NAME -> {
                val name = msg.obj?.let { mag ->
                    mag as String
                } ?: "null"
                listener?.onEvent(BleStatus.CLIENT_CONNECTED, name)
            }

            MSG_RESTART_AD -> {
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
        pushLog("close server if need: bleAdvServer:$bleAdvServer, gattServer:$gattServer")
        bleAdvServer?.stopBroadcast()
        gattServer?.release()
        bleAdvServer = null
        gattServer = null

    }

    override fun cancelConnect(dev: BluetoothDevice) {
        gattServer?.cancelConnection(dev)
    }


    private fun startGattService() {
        pushLog("start gatt service: $gattServer")
        if (gattServer == null) {
            gattServer = ServerGattChar(object : AbsCharacteristic.IGattListener {

                override fun onEvent(status: GattStatus, obj: String?) {
                    pushLog("server status change:$status")
                    when (status) {
                        GattStatus.WRITE_RESPONSE -> {
                            //  pushLog("receiver data:${String(obj as ByteArray)}")
                            listener?.onEvent(BleStatus.DATA, obj)
                        }

                        GattStatus.CLIENT_DISCONNECTED -> {
                            pushLog("client ($clientName) disconnect,reset advertise and gatt service")
                            listener?.onEvent(BleStatus.CLIENT_DISCONNECT, clientName)
                            closeServer()
                            handler?.sendEmptyMessageDelayed(MSG_RESTART_AD, 500)
                        }

                        GattStatus.CLIENT_CONNECTED -> {
                            handler?.removeMessages(MSG_WAIT_NAME)
                            Message.obtain().apply {
                                what = MSG_WAIT_NAME
                                this.obj = obj
                                handler?.sendMessageDelayed(this, TIME_OUT)
                            }
                        }

                        GattStatus.BLUE_NAME -> {
                            pushLog("client name:$obj")
                            //iBleListener?.onEvent(BleStatus.CLIENT_CONNECTED,obj)
                            clientName = obj!!
                            handler?.removeMessages(MSG_WAIT_NAME)
                            Message.obtain().apply {
                                what = MSG_WAIT_NAME
                                this.obj = obj
                                handler?.sendMessage(this)
                            }
                        }
                        GattStatus.MTU_CHANGE ->{
                            mtu = obj?.toInt() ?: 15
                        }


                        else -> {
                            pushLog("$obj ,  status change:$status")
                        }
                    }
                }

            })
        }
        gattServer?.startGattService(option?.context!!, option!!)
    }


    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            pushLog("advertise success,try to start gatt service")
            listener?.onEvent(BleStatus.ADVERTISE_SUCCESS, bluetoothAdapter?.name)
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


    override fun send(data: ByteArray) {
        sendData(DATA_TYPE, data)
    }

    private fun sendData(type: Byte, data: ByteArray) {

        DataSpilt.subData(mtu,data, type, object : DataSpilt.ISplitListener {
            override fun onResult(data: ByteArray) {
                gattServer?.send(data)
            }

        })
    }


    private fun fail(error: BleError, msg: String) {
        listener?.onFail(error, msg)
    }


    override fun checkPermission(context: Context?,listener: IBle.IListener): Boolean {
        var permission = super.checkPermission(context,listener)
        if (option?.context == null){
            listener.onFail(BleError.CONTEXT_NULL, "context is null")
            permission = false
        }
        if (option?.name == null || option?.name?.length!! > MAX_NAME_SIZE) {
            listener.onFail(BleError.NAME_TOO_LONG, "name is null or length > $MAX_NAME_SIZE")
            permission = false
        }
        return permission
    }


}