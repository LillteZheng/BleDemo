package com.zhengsr.server.gatt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServerCallback
import android.util.Log
import com.zhengsr.server.GattStatus

/**
 * @author by zhengshaorui 2023/12/14
 * describe：
 */
abstract class AbsCharacteristic(val listener: IGattListener, tag: String) :
    BluetoothGattServerCallback() {
    protected val TAG = "Characteristic - $tag"
    protected var isConnect = false

    open interface IGattListener {
        fun onEvent(status: GattStatus, obj: String?)
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


}