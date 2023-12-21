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
    private var type: Byte = 0
    private var count = 0

    interface IPackageListener {
        fun onResult(type: Byte, data: ByteArray)
    }

    /**
     * 发送数据，采用以下格式
     * 第一个表示标志位采用两个字节，表示uuid后四位
     * 第二个表示类型，采用1个字节，比如notify，wirite等
     * 第三个表示数据长度：考虑到超过255，所以采用2个字节
     * 第四个表示数据，最大14个字节，如果设置了 mtu，则最大为 mtu - 5
     * 如果有 uuid 标志位，则表示是第一包，否则表示是后续包
     *  0               8               16              24            31
     *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |              UUID             |      type     |      len      |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |               |                                               |
     * +-+-+-+-+-+-+-+-+                                               +
     * |                                                               |
     * +                                                               +
     * |                            payload                            |
     * +                                               +-+-+-+-+-+-+-+-+
     * |                                               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    @Synchronized
    fun formData(value: ByteArray, listener: IPackageListener) {
        try {
            if (value[0] == 0x78.toByte() && value.size >= formatLen && buffer == null) {
                Log.d(TAG, "zsr 第一个包")
                type = value[1]
                val len = ((value[2].toInt() shl 8) or (value[3].toInt() and 0xFF))
                Log.d(TAG, "zsr packageData: len = $len")
                buffer = ByteBuffer.allocate(len)
                buffer?.apply {
                    put(value, formatLen, value.size - formatLen)
                    Log.d(
                        TAG,
                        "zsr packageData: 第一个 $count,:${value.size} ,${position()} ,${limit()}"
                    )
                    if (position() >= limit()) {
                        listener.onResult(type, array())
                        resetBuffer()
                    }
                }
                count++

            } else {
                count++
                buffer?.apply {
                    Log.d(TAG, "zsr formData: ${value.size} ${remaining()}")
                    if (value.size > remaining()) {
                        Log.d(TAG, "zsr formData: 接收端异常")
                        val temp = array()
                        val byte = ByteArray(temp.size + value.size)
                        byte.plus(temp).plus(value)
                        listener.onResult(type, array())
                        return
                    }
                    put(value)
                    Log.d(
                        TAG,
                        "zsr packageData: 后续包 $count,:${value.size} ,${position()} ,${limit()}"
                    )
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