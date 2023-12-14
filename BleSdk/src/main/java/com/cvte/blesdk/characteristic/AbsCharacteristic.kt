package com.cvte.blesdk.characteristic

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServerCallback
import android.util.Log
import com.cvte.blesdk.GattStatus

/**
 * @author by zhengshaorui 2023/12/14
 * describe：
 */
abstract class AbsCharacteristic(val listener: IGattListener,tag:String) {
    protected  val TAG = "AbsCharacteristic - $tag"
    protected var isConnect = false
    open interface IGattListener{
        fun onEvent(status:GattStatus,obj:Any?)
    }

    open fun isConnected() = isConnect

    protected open fun onServerCharWriteRequest(device: BluetoothDevice?,
                                                requestId: Int,
                                                characteristic: BluetoothGattCharacteristic?,
                                                preparedWrite: Boolean,
                                                responseNeeded: Boolean,
                                                offset: Int,
                                                value: ByteArray?){}
    protected open fun onCharReadRequest(device: BluetoothDevice?,
                                         requestId: Int,
                                         offset: Int,
                                         characteristic: BluetoothGattCharacteristic?){}

    protected val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            if (status == BluetoothGatt.GATT_SUCCESS && newState == 2) {
                isConnect = true
                listener.onEvent(GattStatus.SERVER_CONNECTED,device?.name)
            } else {
                isConnect = false
                listener.onEvent(GattStatus.SERVER_DISCONNECTED,device?.name)
            }
        }


        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)

            /**
             * 中心设备read时，回调
             */
            val data = "this is a test from ble server"
            /* mBluetoothGattServer?.sendResponse(
                 device, requestId, BluetoothGatt.GATT_SUCCESS,
                 offset, data.toByteArray()
             )
             logInfo("客户端读取 [characteristic ${characteristic?.uuid}] $data")*/
            Log.d(
                TAG,
                "onCharacteristicReadRequest() called with: device = $device, requestId = $requestId, offset = $offset, characteristic = $characteristic"
            )
            onCharReadRequest(device,requestId,offset,characteristic)
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
            listener.onEvent(GattStatus.SERVER_WRITE,value)
            onServerCharWriteRequest(device,requestId,characteristic,preparedWrite,responseNeeded,offset,value)
        }

        override fun onDescriptorReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            descriptor: BluetoothGattDescriptor?
        ) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor)
            Log.d(
                TAG,
                "onDescriptorReadRequest() called with: device = $device, requestId = $requestId, offset = $offset, descriptor = $descriptor"
            )
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            descriptor: BluetoothGattDescriptor?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onDescriptorWriteRequest(
                device,
                requestId,
                descriptor,
                preparedWrite,
                responseNeeded,
                offset,
                value
            )

            Log.d(
                TAG,
                "onDescriptorWriteRequest() called with: device = $device, requestId = $requestId, descriptor = $descriptor, preparedWrite = $preparedWrite, responseNeeded = $responseNeeded, offset = $offset, value = $value"
            )


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
            Log.d(TAG, "onNotificationSent() called with: device = $device, status = $status")
        }

        override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
            super.onMtuChanged(device, mtu)
            Log.d(TAG, "onMtuChanged() called with: device = $device, mtu = $mtu")
        }
    }

    protected open fun onClientStateChange(gatt: BluetoothGatt?, status: Int, newState: Int){}
    protected open fun onClientConnectService(gatt: BluetoothGatt?, status: Int){}

    protected val gattClientCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            onClientStateChange(gatt,status,newState)

        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            onClientConnectService(gatt,status)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)

        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)

        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)

        }

        override fun onDescriptorRead(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorRead(gatt, descriptor, status)

        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)

        }
    }

    open fun release(){
    }
}