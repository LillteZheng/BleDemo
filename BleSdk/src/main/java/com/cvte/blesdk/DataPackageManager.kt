package com.cvte.blesdk

import android.util.Log
import androidx.core.util.forEach
import com.cvte.blesdk.abs.AbsBle
import com.cvte.blesdk.utils.BleUtil
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author by zhengshaorui 2023/12/20
 * describe：主要是用于数据的封装
 */
object DataPackageManager {
    private const val TAG = "DataPackageManager"
    private var isSplit = AtomicBoolean(true)
    private var buffer: ByteBuffer? = null
    private var type = DATA_TYPE
    private var mtu = 15
    fun setMtu(mtu:Int){
       this.mtu = mtu
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
    fun splitPacket(data: ByteArray, type: Byte, listener: ISplitListener) {
        if (isSplit.get()) {
            isSplit.set(false)
            val datas = BleUtil.subpackage(data, AbsBle.MAX_DATA_SIZE)
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

    private var count = 0
    fun packageData(value: ByteArray, listener: IPackageListener) {
        if (value[0] == 0x78.toByte() && value.size >= FORMAT_LEN && buffer == null) {
            Log.d(TAG, "zsr 第一个包")
            type = value[1]
            val len = ((value[2].toInt() shl 8) or (value[3].toInt() and 0xFF))
            Log.d(TAG, "zsr packageData: len = $len")
            buffer = ByteBuffer.allocate(len)
            buffer?.apply{
                put(value, FORMAT_LEN, value.size - FORMAT_LEN)
                if (position() >= limit()) {
                    val type = if (type == NAME_TYPE) {
                        GattStatus.BLUE_NAME
                    } else if (type == MTU_TYPE){
                        GattStatus.MTU
                    }else {
                        GattStatus.CLIENT_READ
                    }
                    listener.onResult(type, array())
                    buffer = null
                }
            }
            count++

        } else {
            count++
            Log.d(TAG, "zsr packageData: 后续包 :$count")
            buffer?.apply {
                put(value)
                if (position() >= limit()) {
                    val type = if (type == NAME_TYPE) {
                        GattStatus.BLUE_NAME
                    } else {
                        GattStatus.CLIENT_READ
                    }
                    listener.onResult(type, array())
                    buffer = null
                }
            }
        }
    }
    fun resetBuffer() {
        buffer = null
    }


    interface ISplitListener {
        fun onResult(data: ByteArray)
    }

    interface IPackageListener {
        fun onResult(type: GattStatus, data: ByteArray)
    }
}