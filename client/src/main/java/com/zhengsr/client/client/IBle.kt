package com.zhengsr.client.client

import android.bluetooth.BluetoothDevice
import com.zhengsr.client.BleError
import com.zhengsr.client.BleStatus
import com.zhengsr.client.ScanBeacon

/**
 * @author by zhengshaorui 2023/12/19
 * describe：
 */
interface IBle {
    fun startScan(builder: BleOption, listener: IListener)
    fun send(data:ByteArray)
    fun stopScan()
    fun connect(device: BluetoothDevice)
    fun disconnect()
    fun release()


    interface IListener {
        fun onFail(error: BleError, errorMsg:String)
        fun onEvent(status: BleStatus, obj: String?)
        fun onScanResult(beacon: ScanBeacon)
    }
}