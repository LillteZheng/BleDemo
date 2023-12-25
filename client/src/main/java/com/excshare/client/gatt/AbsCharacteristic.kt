package com.excshare.client.gatt

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.util.Log
import com.excshare.client.GattStatus

/**
 * @author by zhengshaorui 2023/12/14
 * describe：
 */
abstract class AbsCharacteristic(val listener: IGattListener, tag: String) :
    BluetoothGattCallback() {
    protected val TAG = "Characteristic - $tag"
    protected var isConnect = false

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


}