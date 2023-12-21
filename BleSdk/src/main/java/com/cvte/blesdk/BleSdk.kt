package com.cvte.blesdk

import android.annotation.SuppressLint
import android.content.Context
import com.cvte.blesdk.client.BleClient
import com.zhengsr.server.BleOption
import com.zhengsr.server.server.IBle

/**
 * @author by zhengshaorui 2023/12/12
 * describe：Ble 门面
 */
@SuppressLint("StaticFieldLeak")
object BleSdk {
    private var client: BleClient? = null
    internal var context:Context? = null
    internal fun inject(context: Context?){
        this.context = context
    }


    @Synchronized
    @JvmStatic
    fun getClient() : BleClient {
        if (client == null) {
            client = BleClient(context)
        }
        return client!!
    }


    @Synchronized
    @JvmStatic
    fun release(){

    }
}