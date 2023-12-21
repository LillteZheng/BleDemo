package com.zhengsr.client

import com.zhengsr.client.client.BleImpl
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
            client = BleImpl()
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