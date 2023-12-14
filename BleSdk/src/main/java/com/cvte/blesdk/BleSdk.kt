package com.cvte.blesdk

import android.annotation.SuppressLint
import android.content.Context
import com.cvte.blesdk.sender.BleClient
import com.cvte.blesdk.server.BleServer

/**
 * @author by zhengshaorui 2023/12/12
 * describe：Ble 门面
 */
@SuppressLint("StaticFieldLeak")
object BleSdk {
    private var server: BleServer? = null
    private var client: BleClient? = null
    internal var context:Context? = null
    internal fun inject(context: Context?){
        this.context = context
    }
    @Synchronized
    @JvmStatic
    fun getServer() : BleServer {
        if (server == null) {
            server = BleServer(context)
        }
        return server!!
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
        server?.release()
        server = null
    }
}