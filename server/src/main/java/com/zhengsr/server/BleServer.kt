package com.zhengsr.server

import com.zhengsr.server.server.IBle
import com.zhengsr.server.server.BleConsumer

/**
 * @author by zhengshaorui 2023/12/21
 * describe：
 */
object BleServer {
    private const val TAG = "BleServer"
    private var server: IBle? = null


    fun get(): IBle{
        if (server == null) {
            server = BleConsumer()
        }
        return server!!
    }

    fun release(){
        server?.release()
        server = null
    }
}