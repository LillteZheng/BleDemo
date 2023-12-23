package com.zhengsr.common

import android.util.Log
import java.nio.ByteBuffer

/**
 * @author by zhengshaorui 2023/12/21
 * describe：
 */
class DataPackage(val formatLen: Int) {
    companion object {
        private const val TAG = "DataPackage"
    }

    private var buffer: ByteBuffer? = null
 //   private var type: Byte = 0
    private var count = 0

    interface IPackageListener {
        fun onResult(type: Byte, data: ByteArray)
    }

    /**
     * 0               8               16              24
     *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |      flag     |  packet_type  |         packet_length         |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |             count             |             index             |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |    version    |                                               |
     * +-+-+-+-+-+-+-+-+                                               +
     * |                              data                             |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * flag: 0x78
     * type: byte ，data，name
     * len: 2 byte
     * count : 2 byte ,ios mtu 最小是100，2byte 比较保险
     * index : 2 byte
     * version : 1 byte
     */
    @Synchronized
    fun formData(value: ByteArray, listener: IPackageListener) {
        try {
            if (value.size < formatLen) {
                return
            }
            val flag = value[0]
            val type = value[1]
            val len = ((value[2].toInt() shl 8) or (value[3].toInt() and 0xFF))
            count = ((value[4].toInt() shl 8) or (value[5].toInt() and 0xFF))
            val index = ((value[6].toInt() shl 8) or (value[7].toInt() and 0xFF))
            val version = value[8]
            Log.d(TAG, "zsr formData: flag = $flag ,type = $type ,len = $len ,count = $count ,index = $index ,version = $version")
            if (flag == DATA_FLAG){
                if (buffer == null) {
                    buffer = ByteBuffer.allocate(len)
                }
                buffer?.apply {
                    put(value, formatLen, value.size - formatLen)
                    if (position() >= limit()) {
                        listener.onResult(type, array())
                        resetBuffer()
                    }
                }
            }
        } catch (e: Exception) {
            resetBuffer()
            Log.e(TAG, " formData: $e")
        }

    }

    fun resetBuffer() {
        count = 0
        buffer = null
    }

}