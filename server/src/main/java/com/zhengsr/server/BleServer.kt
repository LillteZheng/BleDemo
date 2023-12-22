package com.zhengsr.server

import com.zhengsr.server.server.IBle
import com.zhengsr.server.server.Serverlmpl

/**
 * @author by zhengshaorui 2023/12/21
 * describe：
 */
object BleServer {
    private const val TAG = "BleServer"
    private var server: IBle? = null

    @Synchronized
    @JvmStatic
    fun get(): IBle{
        if (server == null) {
            server = Serverlmpl()
        }
        return server!!
    }
    @Synchronized
    @JvmStatic
    fun release(){
        server?.release()
        server = null
    }
}