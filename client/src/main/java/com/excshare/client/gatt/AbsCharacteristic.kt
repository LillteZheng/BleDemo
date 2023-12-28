package com.excshare.client.gatt

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.os.Handler
import android.util.Log
import com.excshare.client.DATA_FLAG
import com.excshare.client.FORMAT_LEN
import com.excshare.client.GattStatus
import java.nio.ByteBuffer

/**
 * @author by zhengshaorui 2023/12/14
 * describe：
 */
abstract class AbsCharacteristic(val handler: Handler?, val listener: IGattListener, tag: String) :
    BluetoothGattCallback() {
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

    open fun isConnected() = isConnect




    override fun onCharacteristicRead(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        super.onCharacteristicRead(gatt, characteristic, status)
        Log.d(
            TAG,
            "onCharacteristicRead() called with: gatt = $gatt, characteristic = $characteristic, status = $status"
        )

    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        super.onCharacteristicWrite(gatt, characteristic, status)

    }



    override fun onDescriptorRead(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        super.onDescriptorRead(gatt, descriptor, status)
        Log.d(
            TAG,
            "onDescriptorRead() called with: gatt = $gatt, descriptor = $descriptor, status = $status"
        )

    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        super.onDescriptorWrite(gatt, descriptor, status)
        Log.d(
            TAG,
            "onDescriptorWrite() called with: gatt = $gatt, descriptor = $descriptor, status = $status"
        )

    }


    protected open fun pushLog(msg: String) {
        listener.onEvent(GattStatus.LOG, "$TAG: $msg")
    }

    /**
     *  0               8               16              24            31
     *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |    version    |      flag     |          total_length         |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |             count             |             index             |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
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
            val version = value[0]
            val flag = value[1]
            val type = value[2]
            val len = ((value[3].toInt() shl 8) or (value[4].toInt() and 0xFF))
            val count = ((value[5].toInt() shl 8) or (value[6].toInt() and 0xFF))
            val index = ((value[7].toInt() shl 8) or (value[8].toInt() and 0xFF))
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