package com.zhengsr.client.client

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.core.util.forEach
import androidx.core.util.size
import com.zhengsr.client.BleError

import com.zhengsr.common.BleUtil
import com.zhengsr.common.DATA_FLAG
import com.zhengsr.common.DataSpilt
import java.util.LinkedList
import java.util.Queue

/**
 * @author by zhengshaorui 2023/12/13
 * describe：
 */
abstract class AbsBle {
    companion object {
        private const val WAIT_TIME = 110L
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

        if (!BleUtil.isBleSupport(context)) {
            listener.onFail(BleError.BLE_NOT_SUPPORT, "bluetooth not support")
            return false
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (!BleUtil.isHasBlePermission(context)) {
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


    fun subData(data: ByteArray, type: Byte, mtu: Int,queue:LinkedList<ByteArray>) {
        val datas = BleUtil.subpackage(data, mtu)
        datas.forEach { index, bytes ->
            //格式+数据
            if (index == 0) {
                //第一个包，包含所有的标志位
                //两个字节，表示数据长度
                val highByte = (data.size shr 8).toByte()
                val lowByte = (data.size and 0xFF).toByte()
                val byte = byteArrayOf(DATA_FLAG, type, highByte, lowByte).plus(bytes)
                // listener.onResult(byte)
                queue.add(byte)
            } else {
                //数据包
                // sendData(bytes)
                // listener.onResult(bytes)
                queue.add(bytes)
            }
        }
        //根据分包的个数，去设置等待时间
         waitResponseTime = (datas.size * WAIT_TIME)
    }


}