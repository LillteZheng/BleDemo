package com.excshare.server.server

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.Message
import com.excshare.server.BleError
import com.excshare.server.BleStatus
import com.excshare.server.DATA_TYPE
import com.excshare.server.FORMAT_LEN
import com.excshare.server.GattStatus
import com.excshare.server.gatt.AbsCharacteristic
import com.excshare.server.gatt.ServerGattChar
import java.util.LinkedList


/**
 * @author by zhengshaorui 2023/12/12
 * describe：蓝牙服务端，主要负责发送广播，开启蓝牙服务
 */
internal class Serverlmpl : AbsBle(), IBle {
    companion object {
        private const val TAG = "BleServer"
        private const val DEFAULT_MTU = 19
        private const val DEFAULT_DATA_HEAD = 3
        private const val MAX_NAME_SIZE = 20
        private const val MSG_WAIT_NAME = 0x01
        private const val MSG_RESTART_AD = 0x02
        private const val MSG_SEND_DATA = 0x03
        private const val MSG_RESPONSE_TIMEOUT = 0x04
        private const val MSG_BLUE_CONFIG_FAIL = 0x05
        private const val TIME_OUT = 2000L
    }

    private var listener: IBle.IListener? = null
    private var gattServer: ServerGattChar? = null
    private var bleAdvServer: BleAdvServer? = null

    private var clientName = "null"
    private var mtu = DEFAULT_MTU - FORMAT_LEN
    private var option: BleOption.Builder? = null
    private var writeListener:IBle.IWrite? = null
    private val dataQueue = LinkedList<ByteArray>()
    private var writeFailCount = 0
    override fun startServer(builder: BleOption, listener: IBle.IListener) {
        this.option = builder.builder
        this.listener = listener
        if (!checkPermission(option?.context,listener)) {
            return
        }
        if (isConnected()) {
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
        bluetoothAdapter?.name = option?.name
        //由于第一次修改蓝牙名称，系统没有那么快响应，监听广播也不是特别靠谱，所以这里通过校准的方式
        if (getBlueName() == option?.name){
            //开启广播
            if (bleAdvServer == null) {
                bleAdvServer = BleAdvServer(bluetoothAdapter!!)
            }
            bleAdvServer?.startBroadcast(advertiseCallback)
        }else{
            handler?.removeMessages(MSG_BLUE_CONFIG_FAIL)
            handler?.sendEmptyMessageDelayed(MSG_BLUE_CONFIG_FAIL,200)
        }
        pushLog("start advertise: ${getBlueName()}")

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
            MSG_BLUE_CONFIG_FAIL->{
                if (getBlueName() == option?.name){
                    //开启广播
                    if (bleAdvServer == null) {
                        bleAdvServer = BleAdvServer(bluetoothAdapter!!)
                    }
                    bleAdvServer?.startBroadcast(advertiseCallback)
                }else{
                    handler?.sendEmptyMessageDelayed(MSG_BLUE_CONFIG_FAIL,200)
                }
            }

            MSG_RESTART_AD -> {
                if (bleAdvServer == null) {
                    bleAdvServer = BleAdvServer(bluetoothAdapter!!)
                }
                bleAdvServer?.startBroadcast(advertiseCallback)
            }
            MSG_SEND_DATA->{
                dataQueue.poll()?.let {
                    val ret = gattServer?.send(it)
                    pushLog("send success: $ret")
                    if (ret == false) {
                        //todo 如果失败了，重发3次，如果还是失败，就不发了
                        //1. 把值重新填回去 dataQueue
                        //2. 重新发送MSG_SEND_DATA消息，
                        //3. 计算count，如果大于3，则表示失败了

                        writeFailCount++
                        if (writeFailCount > 3){
                            writeListener?.onFail( "send failed")
                            dataQueue.clear()

                        }else{
                            dataQueue.addFirst(it)
                            handler?.sendEmptyMessageDelayed(MSG_SEND_DATA,200)
                        }

                        //  writeListener?.onFail(DataError.SEND_FAILED, "send failed")
                    } else {
                        pushLog("cache data size: ${dataQueue.size}")
                        if (dataQueue.isEmpty()){
                            writeListener?.onSuccess()
                            return
                        }
                        handler?.sendEmptyMessageDelayed(MSG_SEND_DATA, waitResponseTime)
                    }
                }
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




    override fun isConnected() =  gattServer?.isConnected() ?: false

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
            gattServer = ServerGattChar(handler,object : AbsCharacteristic.IGattListener {

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
                            mtu = obj?.toInt()  ?: DEFAULT_MTU
                            mtu -= (DEFAULT_DATA_HEAD+ FORMAT_LEN)
                        }


                        else -> {
                            pushLog("$obj ,  status change:$status")
                        }
                    }
                }

                override fun onDataMiss(status: GattStatus, obj: String?, missData: List<Int>?) {
                    val msg = obj ?: "null"
                    listener?.onFail(BleError.PACKAGE_MISS, msg,missData)
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



    override fun send(data: ByteArray,iWrite: IBle.IWrite) {
        writeListener = iWrite
        /**
         * 1. 判断是否连接
         * 2. 分包发送
         * 3. 等待回复 - 完成
         */
        if (!isConnected()){
            pushLog("send data fail,client not connected")
            return
        }
        if (dataQueue.isNotEmpty()){
            pushLog("data sending,please wait..")
            return
        }


        dataQueue.clear()
        subData(data, DATA_TYPE,mtu,dataQueue)
        //todo 临时处理
      //  dataQueue.removeAt(dataQueue.size-1)
        pushLog("send data size: ${dataQueue.size}")
        handler?.removeMessages(MSG_SEND_DATA)
        handler?.sendEmptyMessage(MSG_SEND_DATA)

        //sendData(DATA_TYPE, data)
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