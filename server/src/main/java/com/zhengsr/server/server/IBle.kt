package com.zhengsr.server.server

import android.bluetooth.BluetoothDevice
import com.zhengsr.server.BleError
import com.zhengsr.server.BleStatus

/**
 * @author by zhengshaorui 2023/12/21
 * describe：
 */
interface IBle {
    fun startServer(builder: BleOption, listener: IListener)
    fun send(data:ByteArray)
    fun closeServer()
    fun cancelConnect(dev:BluetoothDevice)
    fun release()

    interface IListener {
        fun onFail(error: BleError, errorMsg:String)
        fun onEvent(status: BleStatus, obj: String?)
    }
}