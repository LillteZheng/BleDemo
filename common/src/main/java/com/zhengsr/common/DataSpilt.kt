package com.zhengsr.common

import androidx.core.util.forEach
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author by zhengshaorui 2023/12/21
 * describe：数据分包
 */
object DataSpilt {
    private var isSplit = AtomicBoolean(true)
    @Synchronized
    fun subData(mtu:Int, data: ByteArray, type: Byte, listener: ISplitListener) {
        if (isSplit.get()) {
            isSplit.set(false)
            val datas = BleUtil.subpackage(data, mtu)
            datas.forEach { index, bytes ->
                //格式+数据
                if (index == 0) {
                    //第一个包，包含所有的标志位
                    //两个字节，表示数据长度
                    val highByte = (data.size shr 8).toByte()
                    val lowByte = (data.size and 0xFF).toByte()
                    //   sendData(byteArrayOf(DATA_FLAG,type,highByte,lowByte).plus(bytes))
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