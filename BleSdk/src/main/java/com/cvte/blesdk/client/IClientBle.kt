package com.cvte.blesdk.client

import android.bluetooth.BluetoothDevice
import com.cvte.blesdk.ClientStatus
import com.cvte.blesdk.ScanBeacon
import com.cvte.blesdk.abs.IBle

/**
 * @author by zhengshaorui 2023/12/13
 * describe：
 */
interface IClientBle:IBle {
    fun startScan(builder: BleClientOption, listener: IBleEventListener)
    fun stopScan()
    fun connect(device:BluetoothDevice)
    fun disconnect()

    interface IBleEventListener:IBle.IListener {
        fun onEvent(status: ClientStatus, obj: String?)
        fun onScanResult(beacon: ScanBeacon)
    }



}