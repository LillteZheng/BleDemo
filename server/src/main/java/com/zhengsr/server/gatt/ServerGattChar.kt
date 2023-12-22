package com.zhengsr.server.gatt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.zhengsr.common.DataPackage
import com.zhengsr.common.FORMAT_LEN
import com.zhengsr.common.NAME_TYPE
import com.zhengsr.common.UUID_READ_DESCRIBE
import com.zhengsr.common.UUID_READ_NOTIFY
import com.zhengsr.common.UUID_SERVICE
import com.zhengsr.common.UUID_WRITE_DESCRIBE
import com.zhengsr.server.server.BleOption
import com.zhengsr.server.GattStatus
import java.util.UUID


/**
 * @author by zhengshaorui 2023/12/13
 * describe：Gatt 服务
 */
class ServerGattChar(listener: IGattListener) : AbsCharacteristic(listener, "server") {
    private var bluetoothGattServer: BluetoothGattServer? = null
    private var gattService: BluetoothGattService? = null
    private var connectDevice: BluetoothDevice? = null
    private var dataPackage: DataPackage? = null
    fun startGattService(context: Context, builder: BleOption.Builder) {
        val readNotifyChar = BluetoothGattCharacteristic(
            builder.readAndNotifyUuid,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        val readDescriptor = BluetoothGattDescriptor(
             UUID_READ_DESCRIBE,
             BluetoothGattDescriptor.PERMISSION_READ
        )

        readNotifyChar.addDescriptor(readDescriptor)


        val writeChar = BluetoothGattCharacteristic(
            builder.writeUuid,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        val writeDescriptor = BluetoothGattDescriptor(
            UUID_WRITE_DESCRIBE,
            BluetoothGattDescriptor.PERMISSION_WRITE
        )
        writeChar.addDescriptor(writeDescriptor)
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


        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        //打开 GATT 服务，方便客户端连接
        bluetoothGattServer = bluetoothManager.openGattServer(context, this)
        pushLog("start gatt service")
        bluetoothGattServer?.addService(gattService)
    }

    /**
     * 状态回调
     */
    override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
        super.onConnectionStateChange(device, status, newState)
        pushLog(
            "onConnectionStateChange() called with: device = $device, status = $status, newState = $newState"

        )
        if (status == BluetoothGatt.GATT_SUCCESS && newState == 2) {
            isConnect = true
            listener.onEvent(GattStatus.CLIENT_CONNECTED, device?.name)
            connectDevice = device
            //需要先调用 connect; 后续 cancel 断开才会起作用
            bluetoothGattServer?.connect(device, false)
        } else {
            isConnect = false
            listener.onEvent(GattStatus.CLIENT_DISCONNECTED, device?.name)
            connectDevice = null
        }
    }

    /**
     * 写入回调
     */
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

        Log.d(TAG, "zsr onCharacteristicWriteRequest: ${value?.size}")
        //当客户端写入数据时，会回调这里，需要调用 sendResponse 告诉客户端是否写入成功
        bluetoothGattServer?.sendResponse(
            device, requestId,
            BluetoothGatt.GATT_SUCCESS, offset, "2".toByteArray()
        )
        value?.let {
            //packetData(value)
            if (dataPackage == null) {
                dataPackage = DataPackage(FORMAT_LEN)
            }
            dataPackage?.formData(it, object : DataPackage.IPackageListener {
                override fun onResult(type: Byte, data: ByteArray) {
                    val status = when(type){
                        //MTU_TYPE-> GattStatus.MTU_CHANGE
                        NAME_TYPE -> GattStatus.BLUE_NAME
                        else -> GattStatus.WRITE_RESPONSE
                    }
                    listener.onEvent(status, String(data))

                }

            })
        }
    }

    override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
        super.onMtuChanged(device, mtu)
        pushLog("onMtuChanged() called with: device = $device, mtu = $mtu")
        listener.onEvent(GattStatus.MTU_CHANGE, mtu.toString())
    }

    /**
     * 取消连接
     */
    fun cancelConnection(dev: BluetoothDevice?) {
        bluetoothGattServer?.cancelConnection(dev)

    }

    fun release() {
        Log.d(TAG, "release: $connectDevice")
        try {
            dataPackage?.resetBuffer()
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

    fun isConnected() = isConnect

    fun send(data: ByteArray): Boolean {
        var isSuccess = false
        if (isConnected()) {
            connectDevice?.let {
                val char = bluetoothGattServer?.getService(UUID_SERVICE)?.getCharacteristic(UUID_READ_NOTIFY)?.apply {
                    value = data
                }
                isSuccess = bluetoothGattServer?.notifyCharacteristicChanged(
                    it,
                    char,
                    false
                ) == true


            }
        }
        return isSuccess
    }
}