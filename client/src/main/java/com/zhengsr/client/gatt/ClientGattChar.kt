package com.zhengsr.client.gatt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import com.zhengsr.client.GattStatus
import com.zhengsr.common.DataPackage
import com.zhengsr.common.FORMAT_LEN
import com.zhengsr.common.NAME_TYPE
import com.zhengsr.common.UUID_READ_DESCRIBE
import com.zhengsr.common.UUID_READ_NOTIFY
import com.zhengsr.common.UUID_SERVICE
import com.zhengsr.common.UUID_WRITE
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue


/**
 * @author by zhengshaorui 2023/12/14
 * describe：
 */
class ClientGattChar(listener: IGattListener) : AbsCharacteristic(listener, "client") {

    private var dataPackage: DataPackage? = null
    private var blueGatt: BluetoothGatt? = null
    fun connectGatt(context: Context, dev: BluetoothDevice) {
        dataPackage?.resetBuffer()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            dev.connectGatt(
                context,
                false,
                this,
                BluetoothDevice.TRANSPORT_LE
            )
        } else {
            dev.connectGatt(context, false, this)
        }
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        pushLog("onClientStateChange() called with: gatt = $gatt, status = $status, newState = $newState")
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            //todo 提前设置 mtu？
            gatt?.discoverServices()
        } else {
            isConnect = false
            listener.onEvent(GattStatus.DISCONNECT_FROM_SERVER, gatt?.device?.name)
            release()

        }
    }


    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        super.onServicesDiscovered(gatt, status)
        isConnect = true
        blueGatt = gatt



        if (status == BluetoothGatt.GATT_SUCCESS) {
            //支持通知属性，当设置为true,onCharacteristicChanged 会回调
            gatt?.getService(UUID_SERVICE)?.getCharacteristic(UUID_READ_NOTIFY)?.let { char ->
                val isSuccess = gatt.setCharacteristicNotification(char, true)
                val descriptor = char.getDescriptor(UUID_READ_DESCRIBE)
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                gatt.writeDescriptor(descriptor)
                pushLog("setCharacteristicNotification: $isSuccess")
            }

            listener.onEvent(GattStatus.CONNECT_TO_SERVER, gatt?.device?.name)
        } else {
            listener.onEvent(GattStatus.DISCONNECT_FROM_SERVER, gatt?.device?.name)
            gatt?.close()
        }
        pushLog("connect to gatt Service,now you can communicate with it")
    }

    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
        super.onMtuChanged(gatt, mtu, status)
        pushLog("onMtuChanged() called with: gatt = $gatt, mtu = $mtu, status = $status")
        if (status == BluetoothGatt.GATT_SUCCESS) {
            listener.onEvent(GattStatus.MTU_CHANGE, mtu.toString())
        }
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        super.onCharacteristicChanged(gatt, characteristic, value)
        if (dataPackage == null) {
            dataPackage = DataPackage(FORMAT_LEN)
        }
        dataPackage?.formData(value, object : DataPackage.IPackageListener {
            override fun onResult(type: Byte, data: ByteArray) {
                val status = when (type) {
                    //MTU_TYPE-> GattStatus.MTU_CHANGE
                    NAME_TYPE -> GattStatus.BLUE_NAME
                    else -> GattStatus.WRITE_RESPONSE
                }
                listener.onEvent(status, String(data))
            }

        })
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        super.onCharacteristicWrite(gatt, characteristic, status)
        //todo 这里需要判断是否发送成功
        Log.d(TAG, "zsr onCharacteristicWrite: ${queue.size}")
        if (status == BluetoothGatt.GATT_SUCCESS) {
            queue.poll()?.let {
                send(it)
            }
        }
    }


    private val queue = ConcurrentLinkedQueue<ByteArray>()
    @Synchronized
    fun send(data: ByteArray): Boolean {
        //uuid 是一对的

        queue.add(data)
        val isSuccess = blueGatt?.getService(UUID_SERVICE)?.getCharacteristic(UUID_WRITE)?.let {
            it.value = data
            blueGatt?.writeCharacteristic(it)
        } ?: false
        //需要放队列里面，重新发
        if (isSuccess) {
            queue.poll()
        }
        Log.d(TAG, "zsr send: $isSuccess")
        return isSuccess
    }


    fun release() {
        blueGatt?.let {
            it.close()
            it.disconnect()
        }
        dataPackage?.resetBuffer()
        blueGatt = null
    }
}