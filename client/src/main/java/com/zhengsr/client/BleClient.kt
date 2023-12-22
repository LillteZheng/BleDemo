package com.zhengsr.client

import com.zhengsr.client.client.ClientImpl
import com.zhengsr.client.client.IBle

/**
 * @author by zhengshaorui 2023/12/21
 * describe：
 */
object BleClient {
    private var client : IBle? = null

    @Synchronized
    @JvmStatic
    fun get(): IBle {
        if (client == null) {
            client = ClientImpl()
        }
        return client!!
    }
    @Synchronized
    @JvmStatic
    fun release() {
        client?.release()
        client = null
    }
}