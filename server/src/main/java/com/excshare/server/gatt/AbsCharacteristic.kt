package com.excshare.server.gatt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServerCallback
import android.os.Handler
import android.util.Log
import com.excshare.server.DATA_FLAG
import com.excshare.server.FORMAT_LEN
import com.excshare.server.GattStatus
import java.nio.ByteBuffer

/**
 * @author by zhengshaorui 2023/12/14
 * describe：
 */
abstract class AbsCharacteristic(val handler: Handler?,val listener: IGattListener, tag: String) :
    BluetoothGattServerCallback() {
    protected val TAG = "Characteristic - $tag"
    protected var isConnect = false
    private var buffer: ByteBuffer? = null

    //   private var type: Byte = 0
    private var receiverPackages: MutableList<Int>? = null
    private var timeOutRunnable: TimeOutRunnable? = null

    open interface IGattListener {
        fun onEvent(status: GattStatus, obj: String?)
        fun onDataMiss(status: GattStatus, obj: String?, missData:List<Int>?)
    }


    override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
        super.onConnectionStateChange(device, status, newState)
    }


    override fun onCharacteristicReadRequest(
        device: BluetoothDevice?,
        requestId: Int,
        offset: Int,
        characteristic: BluetoothGattCharacteristic?
    ) {
        super.onCharacteristicReadRequest(device, requestId, offset, characteristic)


    }

    override fun onCharacteristicWriteRequest(
        device: BluetoothDevice?,
        requestId: Int,
        characteristic: BluetoothGattCharacteristic?,
        preparedWrite: Boolean,
        responseNeeded: Boolean,
        offset: Int,
        value: ByteArray?
    ) {
        super.onCharacteristicWriteRequest(
            device,
            requestId,
            characteristic,
            preparedWrite,
            responseNeeded,
            offset,
            value
        )
        //  listener.onEvent(GattStatus.SERVER_WRITE,value)

    }


    override fun onExecuteWrite(device: BluetoothDevice?, requestId: Int, execute: Boolean) {
        super.onExecuteWrite(device, requestId, execute)
        Log.d(
            TAG,
            "onExecuteWrite() called with: device = $device, requestId = $requestId, execute = $execute"
        )
    }

    override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
        super.onNotificationSent(device, status)
    }

    override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
        super.onMtuChanged(device, mtu)
    }



    protected open fun pushLog(msg: String) {
        listener.onEvent(GattStatus.LOG, "$TAG: $msg")
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
    fun formData(value: ByteArray, ) {
        val formatLen = FORMAT_LEN
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
                        timeOutRunnable = TimeOutRunnable(count,type)
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
                      //  listener.onResult(type, array(),null)
                        onPackageResult(type,array(),null)
                        resetBuffer()
                    }

                }
            }
        } catch (e: Exception) {
            resetBuffer()
            Log.e(TAG, " formData: $e")
        }

    }

    private inner class TimeOutRunnable(val count:Int,val type: Byte) : Runnable {
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
               // listener.onResult( type, it.array(),missIndex)
                onPackageResult(type,it.array(),missIndex)
                resetBuffer()
            }

        }

    }

    protected open fun onPackageResult(type: Byte, data: ByteArray,missPackages:List<Int>?) {

    }

    protected open fun resetBuffer() {
        receiverPackages = null
        buffer = null
    }


}