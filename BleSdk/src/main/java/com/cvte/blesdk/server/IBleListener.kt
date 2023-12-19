package com.cvte.blesdk.server

import com.cvte.blesdk.BleError
import com.cvte.blesdk.ServerStatus

/**
 * @author by zhengshaorui 2023/12/18
 * describe：
 */
interface IBleListener {
    fun onFail(error: BleError,errorMsg:String)
}