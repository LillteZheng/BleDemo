package com.excshare.client.client

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Message
import com.excshare.client.BleError
import com.excshare.client.BleStatus
import com.excshare.client.DATA_TYPE
import com.excshare.client.DataError
import com.excshare.client.FORMAT_LEN
import com.excshare.client.GattStatus
import com.excshare.client.NAME_TYPE
import com.excshare.client.ScanBeacon
import com.excshare.client.gatt.AbsCharacteristic
import com.excshare.client.gatt.ClientGattChar
import com.excshare.client.isGpsOpen
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author by zhengshaorui 2023/12/12
 * describe：
 */
class ClientImpl() : AbsBle(), IBle {
    companion object{
        private const val TAG = "BleImpl"
        private const val DEFAULT_MTU = 19
        //ble 发送数据时，前三个字段是固定的，所以这里是3
        private const val DEFAULT_DATA_HEAD = 3
        private const val CONNECT_TIME_OUT = 5000L
        private const val MSG_SEND_DATA = 1;
        private const val MSG_RESPONSE_TIMEOUT = 2
        private const val MSG_CONNECT_TIMEOUT = 3
        private const val MSG_CONNECT_RETRY = 4
        private const val MSG_SEND_NAME = 5

    }
    private var listener: IBle.IListener? = null
    private val isScanning = AtomicBoolean(false)
    private var gattChar: ClientGattChar? = null
    private var option: BleOption.Builder? = null
    private var scanSuccess = false
    private var dataLen = DEFAULT_MTU - FORMAT_LEN
    private var writeListener:IBle.IWrite? = null
    private val dataQueue = LinkedList<ByteArray>()
    private var writeFailCount = 0
    private var connectFailCount =0
    private var blueDev :BluetoothDevice? = null
    override fun startScan(builder: BleOption, listener: IBle.IListener) {
        scanSuccess = false
        connectFailCount = 0
        option = builder.builder
        //先关闭之前的连接
        stopScan()
        gattChar?.release()
        this.listener = listener
        if (!checkPermission(option?.context,listener)) {
            return
        }
        if (isScanning.get()){
            return
        }
        isScanning.set(true)
        pushLog("start scan")
        if (gattChar?.isConnected() == true) {
           // listener.onEvent(BleStatus.SERVER_CONNECTED, "server is connected,please disconnect first")
            //已经连上了
            listener.onFail(BleError.SCAN_FAILED,"server is connected,please disconnect first")
            return
        }

        if (handler == null) {
            initHandle()
        }
        bluetoothAdapter?.bluetoothLeScanner?.startScan(null, configScanSession().build(), scanCallback)
        handler?.postDelayed({
            stopScan()
            if (!scanSuccess){
                listener.onEvent(BleStatus.SCAN_FAILED,"scan failed,please try again")
            }
        }, option!!.scanTime)
    }

    override fun isConnected() = gattChar?.isConnected() ?: false

    /**
     * 加重试机制
     */
    override fun connect(dev:BluetoothDevice){
        if (handler == null) {
            initHandle()
        }
        blueDev = dev
      //  dev.connectGatt(BleSdk.context!!,autoConnect,)
        if (gattChar == null) {
            gattChar = ClientGattChar(handler,gattListener)
        }
        pushLog("connect to ${dev.name}")
        gattChar?.connectGatt(option?.context!!,dev)
        handler?.removeMessages(MSG_CONNECT_TIMEOUT)
        handler?.sendEmptyMessageDelayed(MSG_CONNECT_TIMEOUT,CONNECT_TIME_OUT)
    }

    override fun send(data:ByteArray,listener: IBle.IWrite){
       sendData(data, DATA_TYPE,listener)
    }

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when(msg.what){
            MSG_SEND_NAME->{
                val name = bluetoothAdapter?.name ?: "null"
                sendData(name.toByteArray(), NAME_TYPE,null)
            }
            MSG_SEND_DATA->{
                dataQueue.poll()?.let {
                    val ret = gattChar?.send(it)
                    pushLog("send success: $ret")
                    if (ret == false) {
                    //todo 如果失败了，重发3次，如果还是失败，就不发了
                        //1. 把值重新填回去 dataQueue
                        //2. 重新发送MSG_SEND_DATA消息，
                        //3. 计算count，如果大于3，则表示失败了

                        writeFailCount++
                        if (writeFailCount > 3){
                            writeListener?.onFail(DataError.WRITE_FAIL, "send failed")
                            dataQueue.clear()

                        }else{
                            pushLog("send failed,try again")
                            dataQueue.addFirst(it)
                            handler?.sendEmptyMessageDelayed(MSG_SEND_DATA,200)
                        }

                      //  writeListener?.onFail(DataError.SEND_FAILED, "send failed")
                    } else {
                        handler?.removeMessages(MSG_RESPONSE_TIMEOUT)
                        handler?.sendEmptyMessageDelayed(MSG_RESPONSE_TIMEOUT, waitResponseTime)
                    }
                }
            }
            MSG_RESPONSE_TIMEOUT->{
                //没有回复，失败了
                writeListener?.onFail(DataError.NO_RESPONSE,"server not response")
            }
            MSG_CONNECT_RETRY->{
                gattChar?.refreshDeviceCache()
                blueDev?.let {
                    handler?.postDelayed({
                        connect(it)
                    },200)

                }



            }
            MSG_CONNECT_TIMEOUT->{
                //连接超时
                listener?.onFail(BleError.CONNECT_TIMEOUT,"connect timeout,please scan and try again")
            }
        }
    }

     fun sendData(data: ByteArray,type:Byte,listener: IBle.IWrite?) {
         writeListener = listener
         writeFailCount = 0
         if (!isConnected()){
             pushLog("please connect to server first")
             return
         }
         if (dataQueue.isNotEmpty()){
             pushLog("data sending,please wait..")
             return
         }


         dataQueue.clear()
         subData(data, type,dataLen,dataQueue)
         pushLog("send data size: ${dataQueue.size}")
         handler?.removeMessages(MSG_SEND_DATA)
         handler?.sendEmptyMessage(MSG_SEND_DATA)
    }

    private val gattListener = object : AbsCharacteristic.IGattListener{
        override fun onEvent(status: GattStatus, obj: String?) {

            when(status){
                GattStatus.CONNECT_TO_SERVER -> {
                    connectFailCount = 0
                    handler?.removeMessages(MSG_CONNECT_TIMEOUT)
                    listener?.onEvent(BleStatus.SERVER_CONNECTED,obj)
                }
                GattStatus.DISCONNECT_FROM_SERVER -> {
                    handler?.removeMessages(MSG_CONNECT_TIMEOUT)
                    listener?.onEvent(BleStatus.SERVER_DISCONNECTED,obj)
                }
                GattStatus.CONNECT_FAIL->{
                    connectFailCount++
                    pushLog("connect fail,try again：$connectFailCount")
                    if (connectFailCount < option?.connectRetry!!){
                        handler?.sendEmptyMessage(MSG_CONNECT_RETRY)
                    }else{
                        connectFailCount = 0
                        handler?.sendEmptyMessage(MSG_CONNECT_TIMEOUT)
                        release()
                    }
                }
                GattStatus.NORMAL_DATA ->{
                    listener?.onEvent(BleStatus.SERVER_WRITE,obj)
                }
                GattStatus.SEND_BLUE_NAME ->{
                    Message.obtain().apply {
                        what = MSG_SEND_NAME
                        handler?.sendMessageDelayed(this,300)
                    }
                }
                GattStatus.MTU_CHANGE ->{
                    dataLen = obj?.toInt()?:DEFAULT_MTU
                    dataLen -= (DEFAULT_DATA_HEAD+ FORMAT_LEN)
                }
                GattStatus.WRITE_RESPONSE->{
                    pushLog("write response,cache data size: ${dataQueue.size}")
                    handler?.removeMessages(MSG_RESPONSE_TIMEOUT)
                    if (dataQueue.isNotEmpty()){
                        //还有数据,继续发
                        handler?.sendEmptyMessage(MSG_SEND_DATA)
                    }else{
                        //没有数据了，回调成功
                        writeListener?.onSuccess()
                    }
                }
                else -> {
                    pushLog("status: $status,obj:$obj")
                }
            }
        }

        override fun onDataMiss(status: GattStatus, obj: String?, missData: List<Int>?) {
            val msg = obj ?: "null"
            listener?.onFail(BleError.PACKAGE_MISS, msg,missData)
        }

    }



    override fun stopScan() {
        if (isScanning.get()) {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            isScanning.set(false)
        }
    }



    override fun disconnect() {
        stopScan()
        gattChar?.release()
        gattChar = null
    }

    override fun release(){
        disconnect()
        releaseHandle()
    }


    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            //不断回调，所以不建议做复杂的动作
            result ?: return
            result.device.name ?: return

            option?.fliter?.let {
                if (!result.device.name.contains(it)){
                    return
                }
            }
            scanSuccess = true
            listener?.onScanResult(ScanBeacon(result.device.name, result.rssi,result.device,result.scanRecord))
        }


    }

    override fun checkPermission(context: Context?,listener: IBle.IListener): Boolean {
        val permission = super.checkPermission(context,listener)
        if (!permission){
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!isGpsOpen(context)) {
                listener.onFail(BleError.GPS_NOT_OPEN, "gps not open")
                return   false
            }
        }

        return true
    }



    private fun configScanSession():ScanSettings.Builder{
        //扫描设置

        val builder = ScanSettings.Builder()
            /**
             * 三种模式
             * - SCAN_MODE_LOW_POWER : 低功耗模式，默认此模式，如果应用不在前台，则强制此模式
             * - SCAN_MODE_BALANCED ： 平衡模式，一定频率下返回结果
             * - SCAN_MODE_LOW_LATENCY 高功耗模式，建议应用在前台才使用此模式
             */
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)//高功耗，应用在前台

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            /**
             * 三种回调模式
             * - CALLBACK_TYPE_ALL_MATCHED : 寻找符合过滤条件的广播，如果没有，则返回全部广播
             * - CALLBACK_TYPE_FIRST_MATCH : 仅筛选匹配第一个广播包出发结果回调的
             * - CALLBACK_TYPE_MATCH_LOST : 这个看英文文档吧，不满足第一个条件的时候，不好解释
             */
            builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        }

        //判断手机蓝牙芯片是否支持皮批处理扫描
        if (bluetoothAdapter?.isOffloadedFilteringSupported == true) {
            builder.setReportDelay(0L)
        }

        return builder
    }
    private fun pushLog(msg:String){
        option?.logListener?.onLog(msg)
    }

}