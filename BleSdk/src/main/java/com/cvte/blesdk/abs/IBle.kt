package com.cvte.blesdk.abs

import com.cvte.blesdk.BleError
import com.cvte.blesdk.ClientStatus
import com.cvte.blesdk.server.IBleListener

/**
 * @author by zhengshaorui 2023/12/19
 * describe：
 */
interface IBle {
    fun send(data:ByteArray)
    fun release()
    interface IListener {
        fun onFail(error: BleError, errorMsg:String)
    }
}