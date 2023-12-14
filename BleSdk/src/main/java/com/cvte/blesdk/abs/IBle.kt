package com.cvte.blesdk.abs

import com.cvte.blesdk.BleError

/**
 * @author by zhengshaorui 2023/12/13
 * describe：
 */
interface IBle {
    fun onLog(msg:String)
    fun onFail(errorCode: BleError, msg:String)
}