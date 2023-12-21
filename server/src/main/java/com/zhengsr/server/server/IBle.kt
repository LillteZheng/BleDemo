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
    fun send(data:ByteArray,listener:IWrite)
    fun closeServer()
    fun isConnected():Boolean
    fun cancelConnect(dev:BluetoothDevice)
    fun release()

    interface IListener {
        fun onFail(error: BleError, errorMsg:String)
        fun onEvent(status: BleStatus, obj: String?)
    }
    interface IWrite {
        fun onStart()
        fun onSuccess()
        fun onFail(errorMsg:String)
    }
}