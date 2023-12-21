package com.zhengsr.common

import android.util.Log
import androidx.core.util.forEach
import androidx.core.util.size
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author by zhengshaorui 2023/12/21
 * describe：数据分包
 */
object DataSpilt {
    private const val TAG = "DataSpilt"
    private var isSplit = AtomicBoolean(true)
    @Synchronized
    fun subData(mtu:Int, data: ByteArray, type: Byte, listener: ISplitListener) {
        if (isSplit.get()) {
            //每一个包之间写入数据需要设置间隔，比如100ms。
            isSplit.set(false)
            val datas = BleUtil.subpackage(data, mtu)
            Log.d(TAG, "包大小: ${datas.size} ${data.size}")
            datas.forEach { index, bytes ->
                //格式+数据
                if (index == 0) {
                    //第一个包，包含所有的标志位
                    //两个字节，表示数据长度
                    val highByte = (data.size shr 8).toByte()
                    val lowByte = (data.size and 0xFF).toByte()
                    val byte = byteArrayOf(DATA_FLAG, type, highByte, lowByte).plus(bytes)
                    listener.onResult(byte)
                } else {
                    //数据包
                    // sendData(bytes)
                    listener.onResult(bytes)
                }
            }
            isSplit.set(true)
        }
    }

    interface ISplitListener {
        fun onResult(data: ByteArray)
    }
}