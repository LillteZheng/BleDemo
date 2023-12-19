package com.cvte.blesdk.sender

import android.bluetooth.BluetoothDevice
import com.cvte.blesdk.BleError
import com.cvte.blesdk.ClientStatus
import com.cvte.blesdk.server.IBleListener

/**
 * @author by zhengshaorui 2023/12/13
 * describe：
 */
interface IClientBle {
    fun startScan(builder: BleClientOption, listener: IBleClientListener)
    fun stopScan()
    fun connect(device:BluetoothDevice)
    fun disconnect()
    fun release()
    fun send(data:ByteArray)


    interface IBleClientListener : IBleListener{
        fun onEvent(status: ClientStatus, obj: Any?)
    }
}