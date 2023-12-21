package com.zhengsr.client.client

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Message
import android.util.Log
import com.zhengsr.client.BleError
import com.zhengsr.client.BleStatus
import com.zhengsr.client.DataError
import com.zhengsr.client.GattStatus
import com.zhengsr.client.ScanBeacon
import com.zhengsr.client.gatt.AbsCharacteristic
import com.zhengsr.client.gatt.ClientGattChar
import com.zhengsr.common.BleUtil
import com.zhengsr.common.DATA_TYPE
import com.zhengsr.common.DataSpilt
import com.zhengsr.common.FORMAT_LEN
import com.zhengsr.common.NAME_TYPE
import java.util.LinkedList
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author by zhengshaorui 2023/12/12
 * describe：
 */
class BleImpl() : AbsBle(), IBle {
    companion object{
        private const val TAG = "BleImpl"
        private const val DEFAULT_MTU = 19
        //ble 发送数据时，前三个字段是固定的，所以这里是3
        private const val DEFAULT_DATA_HEAD = 3
        private const val MSG_SEND_DATA = 1;
        private const val MSG_RESPONSE_TIMEOUT = 2;
    }
    private var listener: IBle.IListener? = null
    private val isScanning = AtomicBoolean(false)
    private var gattChar: ClientGattChar? = null
    private var option: BleOption.Builder? = null
    private var scanSuccess = false
    private var mtu = DEFAULT_MTU - FORMAT_LEN

    override fun startScan(builder: BleOption, listener: IBle.IListener) {
        scanSuccess = false
        option = builder.builder
        //先关闭之前的连接
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
            listener.onEvent(BleStatus.SERVER_CONNECTED, "server is connected,please disconnect first")
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
      //  dev.connectGatt(BleSdk.context!!,autoConnect,)
        if (gattChar == null) {
            gattChar = ClientGattChar(object : AbsCharacteristic.IGattListener {
                override fun onEvent(status: GattStatus, obj: String?) {
                    pushLog("status: $status,obj:$obj")
                    if (status == GattStatus.DISCONNECT_FROM_SERVER){
                        release()
                    }
                    when(status){
                        GattStatus.CONNECT_TO_SERVER -> {
                            listener?.onEvent(BleStatus.SERVER_CONNECTED,obj)
                        }
                        GattStatus.DISCONNECT_FROM_SERVER -> {
                            listener?.onEvent(BleStatus.SERVER_DISCONNECTED,obj)
                            release()
                        }
                        GattStatus.NORMAL_DATA ->{
                            listener?.onEvent(BleStatus.SERVER_WRITE,obj)
                        }
                        GattStatus.BLUE_NAME ->{
                            val name = bluetoothAdapter?.name?:"null"
                            sendData(name.toByteArray(), NAME_TYPE)
                        }
                        GattStatus.MTU_CHANGE ->{
                            mtu = obj?.toInt()?:DEFAULT_MTU
                            mtu -= (DEFAULT_DATA_HEAD+ FORMAT_LEN)
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

            })
        }
        pushLog("connect to ${dev.name}")
        gattChar?.connectGatt(option?.context!!,dev)
    }
    private var writeListener:IBle.IWrite? = null
    private val dataQueue = LinkedList<ByteArray>()
    private var writeFailCount = 0
    override fun send(data:ByteArray,listener: IBle.IWrite){
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
        subData(data, DATA_TYPE,mtu,dataQueue)
        pushLog("send data size: ${dataQueue.size}")
        handler?.removeMessages(MSG_SEND_DATA)
        handler?.sendEmptyMessage(MSG_SEND_DATA)
    }

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when(msg.what){
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
        }
    }

     fun sendData(data: ByteArray,type:Byte) {
         handler?.post {
             DataSpilt.subData(mtu,data,type,object:DataSpilt.ISplitListener{
                 override fun onResult(data: ByteArray) {
                     val isSuccess = gattChar?.send(data)
                     pushLog("send success: $isSuccess")
                 }

             })
         }
    }



    override fun stopScan() {
        if (isScanning.get()) {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            isScanning.set(false)
        }
    }



    override fun disconnect() {
        release()
    }

    override fun release(){
        stopScan()
        gattChar?.release()
        gattChar = null
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
            if (!BleUtil.isGpsOpen(context)) {
                listener.onFail(BleError.GPS_NOT_OPEN, "gps not open")
                return   false
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //貌似不需要检测这两个？
            /*if (!BleUtil.isPermission(context, Manifest.permission.BLUETOOTH_CONNECT)) {
                listener.onFail(BleError.PERMISSION_DENIED, "BLUETOOTH_CONNECT permission denied")
               return  false
            }*/
            /*if (!BleUtil.isPermission(context, Manifest.permission.BLUETOOTH_SCAN)) {
                listener.onFail(BleError.PERMISSION_DENIED, "BLUETOOTH_SCAN permission denied")
                return  false
            }*/


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