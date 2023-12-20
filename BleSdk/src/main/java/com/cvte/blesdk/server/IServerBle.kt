package com.cvte.blesdk.server

import com.cvte.blesdk.ClientStatus
import com.cvte.blesdk.ServerStatus
import com.cvte.blesdk.abs.IBle

/**
 * @author by zhengshaorui 2023/12/19
 * describe：
 */
interface IServerBle :IBle{
    fun startServer(bleOption: BleServerOption, iBleListener: IBleEventListener)
    fun closeServer()

    interface IBleEventListener:IBle.IListener {
        fun onEvent(status: ServerStatus, obj: String?)
    }
}