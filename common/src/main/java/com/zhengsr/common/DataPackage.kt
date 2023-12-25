package com.zhengsr.common

import android.os.Handler
import android.util.Log
import java.nio.ByteBuffer
import kotlin.math.log

/**
 * @author by zhengshaorui 2023/12/21
 * describe：
 */
class DataPackage(val formatLen: Int, val handler: Handler? = null) {
    companion object {
        private const val TAG = "DataPackage"
    }

    private var buffer: ByteBuffer? = null

    //   private var type: Byte = 0
    private var receiverPackages: MutableList<Int>? = null
    private var timeOutRunnable: TimeOutRunnable? = null

    interface IPackageListener {
        fun onResult(type: Byte, data: ByteArray,missPackages:List<Int>? = null)
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
            val count = ((value[4].toInt() shl 8) or (value[5].toInt() and 0xFF))
            val index = ((value[6].toInt() shl 8) or (value[7].toInt() and 0xFF))
            val version = value[8]
            Log.d(
                TAG,
                "formData: flag = $flag ,type = $type ,len = $len ,buffer Len = ${buffer?.position()},count = $count ,index = $index ,version = $version"
            )
            if (flag == DATA_FLAG) {
                if (buffer == null) {
                    receiverPackages = mutableListOf()
                    buffer = ByteBuffer.allocate(len)
                }
                buffer?.apply {
                    put(value, formatLen, value.size - formatLen)
                    receiverPackages?.add(index)
                    if (timeOutRunnable == null) {
                        timeOutRunnable = TimeOutRunnable(count,type,listener)
                    }
                    timeOutRunnable?.let {
                        handler?.removeCallbacks(it)
                        handler?.postDelayed(it, 5000)
                    }
                    //设置超时机制
                    if (position() >= limit()) {
                        timeOutRunnable?.let {
                            handler?.removeCallbacks(it)
                        }
                        listener.onResult(type, array(),null)
                        resetBuffer()
                    }

                }
            }
        } catch (e: Exception) {
            resetBuffer()
            Log.e(TAG, " formData: $e")
        }

    }

    private inner class TimeOutRunnable(val count:Int,val type: Byte,val listener: IPackageListener) : Runnable {
        override fun run() {
            Log.d(TAG, "receiver time out,force output")
            buffer?.let {
                handler?.removeCallbacks(this)
                val missIndex = mutableListOf<Int>()
                for (i in 0 until count) {
                    if (!receiverPackages!!.contains(i)) {
                        missIndex.add(i)
                    }
                }
                listener.onResult( type, it.array(),missIndex)
                resetBuffer()
            }

        }

    }

    fun resetBuffer() {
        receiverPackages = null
        buffer = null
    }

}