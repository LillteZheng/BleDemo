package com.zhengsr.client.client

import android.bluetooth.BluetoothDevice
import com.zhengsr.client.BleError
import com.zhengsr.client.BleStatus
import com.zhengsr.client.DataError
import com.zhengsr.client.ScanBeacon

/**
 * @author by zhengshaorui 2023/12/19
 * describe：
 */
interface IBle {
    fun startScan(builder: BleOption, listener: IListener)
    fun send(data:ByteArray,listener:IWrite)
    fun stopScan()
    fun connect(device: BluetoothDevice)
    fun disconnect()
    fun isConnected():Boolean
    fun release()


    interface IListener {
        fun onFail(error: BleError, errorMsg:String,obj: Any? = null)
        fun onEvent(status: BleStatus, obj: String?)
        fun onScanResult(beacon: ScanBeacon)
    }
    interface IWrite {
        fun onSuccess()
        fun onFail(dataError: DataError,errorMsg:String)
    }
}