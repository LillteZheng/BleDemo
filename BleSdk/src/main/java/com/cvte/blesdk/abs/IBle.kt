package com.cvte.blesdk.abs

import com.cvte.blesdk.BleError

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