package com.cvte.blesdk.characteristic

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import com.cvte.blesdk.BleSdk
import com.cvte.blesdk.GattStatus
import com.cvte.blesdk.UUID_READ_NOTIFY
import com.cvte.blesdk.UUID_SERVICE
import com.cvte.blesdk.UUID_WRITE
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * @author by zhengshaorui 2023/12/14
 * describe：
 */
class ClientGattChar(listener: IGattListener) : AbsCharacteristic(listener, "client") {
    private var blueGatt: BluetoothGatt? = null
    private var name: String? = null
    fun connectGatt(dev: BluetoothDevice,name:String?, autoConnect: Boolean) {
        pushLog("connectGatt: ${dev.name}")
        this.name = name
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            dev.connectGatt(
                BleSdk.context!!,
                autoConnect,
                gattClientCallback,
                BluetoothDevice.TRANSPORT_LE
            )
        } else {
            dev.connectGatt(BleSdk.context!!, autoConnect, gattClientCallback)
        }
    }

    override fun onClientStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        super.onClientStateChange(gatt, status, newState)
        pushLog("onClientStateChange() called with: gatt = $gatt, status = $status, newState = $newState")
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            gatt?.discoverServices()
        } else {
            isConnect = false
            listener.onEvent(GattStatus.CLIENT_DISCONNECTED, gatt?.device?.name)
            release()

        }
    }

    private var count = 0
    private var max = 0
    private var buffer:ByteBuffer? = null
    override fun onClientRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
        super.onClientRead(gatt, characteristic)
        characteristic?.let {
            it.value?.let { value ->
                val msg = String(value)
                if (msg == "over") {
                    gatt?.close()
                    listener.onEvent(GattStatus.CLIENT_DISCONNECTED, gatt?.device?.name)
                } else if (msg.startsWith("_len")) {

                    val size  = msg.substring(4, msg.length).toInt()
                    buffer = ByteBuffer.allocate(size)
                    listener.onEvent(GattStatus.CLIENT_READ, "收到服务端消息长度：$size")
                } else if (msg.startsWith("_count")){
                    count = 0
                    max = msg.substring(6, msg.length).toInt()
                    listener.onEvent(GattStatus.CLIENT_READ, "收到服务端消息包大小：$max")
                }else {
                    // listener.onEvent(GattStatus.CLIENT_READ, msg)
                    buffer?.let {
                        count++
                        Log.d(TAG, "zsr onClientRead: $count,${value.size}")
                        it.put(value)
                        if (count >= max){
                            listener.onEvent(GattStatus.CLIENT_READ, String(it.array()) )
                        }
                    }
                    /*Log.d(TAG, "zsr onClientRead: $bu")
                    if (count <= max){
                       // datas.add(value)
                        buffer?.put(value)
                    }else{
                        listener.onEvent(GattStatus.CLIENT_READ, buffer?.array()?.let { String(it) })
                    }*/

                }
            }
        }
    }

    override fun onClientConnectService(gatt: BluetoothGatt?, status: Int) {
        super.onClientConnectService(gatt, status)
        blueGatt = gatt
        //设置一个notify，用来监听外设信息
        pushLog("onClientConnectService: ${gatt?.device?.name},is can get service: ${gatt?.getService(UUID_SERVICE)}")
        gatt?.getService(UUID_SERVICE)?.let {
            it.getCharacteristic(UUID_READ_NOTIFY)?.let { char ->
                val isSuccess = gatt.setCharacteristicNotification(char, true)
                pushLog("setCharacteristicNotification: $isSuccess")
            }
        }
        //  blueGatt?.setCharacteristicNotification()
        if (status == BluetoothGatt.GATT_SUCCESS) {
            listener.onEvent(GattStatus.CLIENT_CONNECTED, gatt?.device?.name)
        } else {
            listener.onEvent(GattStatus.CLIENT_DISCONNECTED, gatt?.device?.name)
            gatt?.close()
        }
        pushLog("connect to gatt Service,now you can communicate with it")
        //先发送蓝牙名字
       // send(name.toByteArray())
        name?.let {
            send(it.toByteArray())
        }
    }

    override fun send(data: ByteArray) {
        var isSuccess = false
        //uuid 是一对的
        pushLog("found service?: ${blueGatt?.getService(UUID_SERVICE)}")

        isSuccess = blueGatt?.getService(UUID_SERVICE)?.getCharacteristic(UUID_WRITE)?.let {
            it.value = data
            blueGatt?.writeCharacteristic(it)
        }?:false
        pushLog("send: $isSuccess")
    }

    override fun release() {
        super.release()
        blueGatt?.let {
            it.close()
            it.disconnect()
        }
        blueGatt = null
    }
}