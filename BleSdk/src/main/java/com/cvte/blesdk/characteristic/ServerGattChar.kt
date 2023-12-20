package com.cvte.blesdk.characteristic

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.cvte.blesdk.DataPackageManager
import com.cvte.blesdk.BleSdk
import com.cvte.blesdk.GattStatus
import com.cvte.blesdk.UUID_READ_NOTIFY
import com.cvte.blesdk.UUID_SERVICE
import com.cvte.blesdk.server.BleServerOption

/**
 * @author by zhengshaorui 2023/12/13
 * describe：Gatt 服务
 */
class ServerGattChar(listener: IGattListener) : AbsCharacteristic(listener,"server"){
    private var bluetoothGattServer: BluetoothGattServer? = null
    private var gattService:BluetoothGattService? = null
    private var connectDevice:BluetoothDevice? = null
    fun startGattService(builder:BleServerOption.Builder) {
        val readNotifyChar = BluetoothGattCharacteristic(
            builder.readAndNotifyUuid,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        val writeChar = BluetoothGattCharacteristic(
            builder.writeUuid,
            BluetoothGattCharacteristic.PROPERTY_WRITE ,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        pushLog("config characteristic ,write ,read nad notify")



        /**
         * 添加 Gatt service 用来通信
         */

        //开启广播service，这样才能通信，包含一个或多个 characteristic ，每个service 都有一个 uuid
         gattService =
            BluetoothGattService(
                builder.serviceUUid,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            ).apply {
                addCharacteristic(readNotifyChar)
                addCharacteristic(writeChar)
            }


        val bluetoothManager = BleSdk.context!!.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        //打开 GATT 服务，方便客户端连接
        bluetoothGattServer = bluetoothManager.openGattServer(BleSdk.context!!, gattServerCallback)
        pushLog("start gatt service")
        bluetoothGattServer?.addService(gattService)


    }

    override fun onServerStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
        super.onServerStateChange(device, status, newState)
        pushLog(
            "onServerStateChange() called with: device = $device, status = $status, newState = $newState"

        )
        /**
         * todo ： bluetooth 的name，是从缓存里面拿的，需要执行一遍扫描才能拿到最新的name
         * 后续优化
         */
        if (status == BluetoothGatt.GATT_SUCCESS && newState == 2) {
            isConnect = true
            listener.onEvent(GattStatus.SERVER_CONNECTED,device?.name)
            connectDevice = device
            //需要先调用 connect，cancel 才会起作用
            bluetoothGattServer?.connect(device,false)
        } else {
            isConnect = false
            listener.onEvent(GattStatus.SERVER_DISCONNECTED,device?.name)
            connectDevice = null
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
       // listener.onEvent(GattStatus.SERVER_WRITE,value)
        //需要setResponse，不然客户端会一直等待
        bluetoothGattServer?.sendResponse(device,requestId,
            BluetoothGatt.GATT_SUCCESS,offset,"2".toByteArray())
        value?.let {
            //packetData(value)
            DataPackageManager.packageData(value,object :DataPackageManager.IPackageListener{
                override fun onResult(type: GattStatus, data: ByteArray) {
                    listener.onEvent(type, String(data))
                }

            })
        }
    }

    override fun onServerMtuChanged(device: BluetoothDevice?, mtu: Int) {
        super.onServerMtuChanged(device, mtu)
        DataPackageManager.setMtu(mtu)
        pushLog("onServerMtuChanged() called with: device = $device, mtu = $mtu")
        listener.onEvent(GattStatus.MTU,null)
    }



    override fun release(){

        Log.d(TAG, "release: $connectDevice")
        try {
            connectDevice?.let {
                bluetoothGattServer?.cancelConnection(it)
            }
            bluetoothGattServer?.clearServices()
            bluetoothGattServer?.close()
            bluetoothGattServer = null
        } catch (e: Exception) {
            Log.e(TAG, "release: $e")
        }
    }

    override fun send(data: ByteArray) :Boolean{
        var isSuccess = false
        if (isConnected()) {
            connectDevice?.let {
                bluetoothGattServer?.getService(UUID_SERVICE)?.getCharacteristic(UUID_READ_NOTIFY)
                    ?.apply {
                        value = data
                        isSuccess = bluetoothGattServer?.notifyCharacteristicChanged(connectDevice!!, this, false) == true
                    }
            }
        }
        return isSuccess
    }
}