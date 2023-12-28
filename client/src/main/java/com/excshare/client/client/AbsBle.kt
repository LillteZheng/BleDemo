package com.excshare.client.client

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import androidx.core.util.forEach
import androidx.core.util.size
import com.excshare.client.BleError
import com.excshare.client.DATA_FLAG
import com.excshare.client.VERSION
import com.excshare.client.isBleSupport
import com.excshare.client.isHasBlePermission
import com.excshare.client.subpackage
import java.util.LinkedList

/**
 * @author by zhengshaorui 2023/12/13
 * describe：
 */
abstract class AbsBle {
    companion object {
        private const val WAIT_TIME = 1000L
    }
    //默认100
    protected var waitResponseTime = WAIT_TIME
    private var handlerThread: HandlerThread? = null
    protected var handler: Handler? = null
    private val callback = android.os.Handler.Callback {
        handleMessage(it)
        true
    }

    protected open fun initHandle() {
        if (handler == null) {
            handlerThread = object : HandlerThread("ble_server") {
                override fun onLooperPrepared() {
                    super.onLooperPrepared()
                    handler = Handler(Looper.myLooper()!!, callback)
                }
            }
            handlerThread?.start()
        }
    }

    protected open fun handleMessage(msg: Message) {}


    protected var bluetoothAdapter: BluetoothAdapter? = null

    protected open fun checkPermission(context: Context?, listener: IBle.IListener): Boolean {
        if (context == null) {
            listener.onFail(
                BleError.CONTEXT_NULL,
                "context is null,please use BleOption.context(context) first"
            )
            return false
        }
        val bluetoothManager =
            context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter?.isEnabled == false) {
            listener.onFail(BleError.BLUETOOTH_NOT_OPEN, "bluetooth not open")
            return false
        }

        if (!isBleSupport(context)) {
            listener.onFail(BleError.BLE_NOT_SUPPORT, "bluetooth not support")
            return false
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (!isHasBlePermission(context)) {
                val msg =
                    "BLUETOOTH_ADVERTISE | BLUETOOTH_CONNECT"

                listener.onFail(
                    BleError.PERMISSION_DENIED, "ble permission denied，" +
                            "make sure you have add  permission($msg)"
                )
                return false
            }


        }
        return true
    }


    fun releaseHandle() {
        handlerThread?.quitSafely()
        handlerThread = null
        handler = null
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
    fun subData(data: ByteArray, type: Byte, mtu: Int,queue: LinkedList<ByteArray>) {
        val spiltData = subpackage(data, mtu)
        spiltData.forEach { index, bytes ->
            //格式+数据
            //第一个包，包含所有的标志位
            //两个字节，表示数据长度
            val sizeHigh = (data.size shr 8).toByte()
            val sizeLow = (data.size and 0xFF).toByte()

            val countHigh = (spiltData.size shr 8).toByte()
            val countLow = (spiltData.size and 0xFF).toByte()

            val indexHigh = (index shr 8).toByte()
            val indexLow = (index and 0xFF).toByte()

            val versionByte = VERSION.toByte() //版本号


            val byte = byteArrayOf(versionByte,DATA_FLAG, type, sizeHigh, sizeLow,
                countHigh,countLow,indexHigh,indexLow).plus(bytes)
            // listener.onResult(byte)
            queue.add(byte)

        }
    }


}