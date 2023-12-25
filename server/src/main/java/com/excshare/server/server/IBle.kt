package com.excshare.server.server

import android.bluetooth.BluetoothDevice
import com.excshare.server.BleError
import com.excshare.server.BleStatus

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
        fun onFail(error: BleError, errorMsg:String, obj:Any?=null)
        fun onEvent(status: BleStatus, obj: String?)
    }
    interface IWrite {
        fun onSuccess()
        fun onFail(errorMsg:String)
    }
}