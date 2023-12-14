package com.cvte.blesdk.characteristic

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.cvte.blesdk.BleSdk
import com.cvte.blesdk.GattStatus
import com.cvte.blesdk.UUID_DESCRIBE
import com.cvte.blesdk.UUID_READ_NOTIFY
import com.cvte.blesdk.UUID_SERVICE
import com.cvte.blesdk.UUID_WRITE

/**
 * @author by zhengshaorui 2023/12/13
 * describe：Gatt 服务
 */
class ServerGattChar(listener: IGattListener) : AbsCharacteristic(listener,"server"){
    private var bluetoothGattServer: BluetoothGattServer? = null
    private var gattService:BluetoothGattService? = null
    private var connectDevice:BluetoothDevice? = null
    fun startGattService() {
        val readNotifyChar = BluetoothGattCharacteristic(
            UUID_READ_NOTIFY,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        val writeChar = BluetoothGattCharacteristic(
            UUID_WRITE,
            BluetoothGattCharacteristic.PROPERTY_WRITE ,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        ).apply {
            //添加描述符
            addDescriptor(BluetoothGattDescriptor(
                UUID_DESCRIBE,
                BluetoothGattDescriptor.PERMISSION_WRITE
            ))
        }



        /**
         * 添加 Gatt service 用来通信
         */

        //开启广播service，这样才能通信，包含一个或多个 characteristic ，每个service 都有一个 uuid
         gattService =
            BluetoothGattService(
                UUID_SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            ).apply {
                addCharacteristic(readNotifyChar)
                addCharacteristic(writeChar)
            }


        val bluetoothManager = BleSdk.context!!.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        //打开 GATT 服务，方便客户端连接
        bluetoothGattServer = bluetoothManager.openGattServer(BleSdk.context!!, gattServerCallback)
        Log.d(TAG, "start: ${bluetoothGattServer?.services}")
        bluetoothGattServer?.addService(gattService)


    }

    override fun onServerStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
        super.onServerStateChange(device, status, newState)
        Log.d(
            TAG,
            "onServerStateChange() called with: device = $device, status = $status, newState = $newState"
        )
        if (status == BluetoothGatt.GATT_SUCCESS && newState == 2) {
            isConnect = true
            listener.onEvent(GattStatus.SERVER_CONNECTED,device?.name)
            connectDevice = device
          //  bluetoothGattServer?.sendResponse(device,0,BluetoothGatt.GATT_SUCCESS,0,"hello world".toByteArray())
        } else {
            isConnect = false
            listener.onEvent(GattStatus.SERVER_DISCONNECTED,device?.name)
        }
    }

    override fun onServerCharWriteRequest(
        device: BluetoothDevice?,
        requestId: Int,
        characteristic: BluetoothGattCharacteristic?,
        preparedWrite: Boolean,
        responseNeeded: Boolean,
        offset: Int,
        value: ByteArray?
    ) {
        super.onServerCharWriteRequest(
            device,
            requestId,
            characteristic,
            preparedWrite,
            responseNeeded,
            offset,
            value
        )
        //回复客户端
        bluetoothGattServer?.sendResponse(device,requestId,
            BluetoothGatt.GATT_SUCCESS,offset,"服务端: 已收到你的消息".toByteArray())
    }



    override fun release(){

        Log.d(TAG, "release: $connectDevice")
        try {
            connectDevice?.let {
                bluetoothGattServer?.getService(UUID_SERVICE)?.getCharacteristic(UUID_READ_NOTIFY)?.apply {
                    value = "over".toByteArray()
                    bluetoothGattServer?.notifyCharacteristicChanged(connectDevice!!,this,false)
                }
            }
            bluetoothGattServer?.close()
            bluetoothGattServer = null
        } catch (e: Exception) {
            Log.e(TAG, "release: $e")
        }
    }

    override fun send(data: ByteArray) {
        if (isConnected()) {
            connectDevice?.let {
                bluetoothGattServer?.getService(UUID_SERVICE)?.getCharacteristic(UUID_READ_NOTIFY)
                    ?.apply {
                        value = data
                        bluetoothGattServer?.notifyCharacteristicChanged(connectDevice!!, this, false)
                    }
            }
        }
    }
}