package com.cvte.blesdk.characteristic

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.util.Log
import com.cvte.blesdk.BleSdk
import com.cvte.blesdk.GattStatus
import com.cvte.blesdk.UUID_SERVICE
import com.cvte.blesdk.UUID_WRITE

/**
 * @author by zhengshaorui 2023/12/14
 * describe：
 */
class ClientGattChar(listener: IGattListener) : AbsCharacteristic(listener,"client") {
    private var blueGatt: BluetoothGatt? = null
    fun connectGatt(dev: BluetoothDevice, autoConnect: Boolean) {
        dev.connectGatt(BleSdk.context!!, autoConnect, gattClientCallback,BluetoothDevice.TRANSPORT_LE)
    }

    override fun onClientStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        super.onClientStateChange(gatt, status, newState)
        Log.d(
            TAG,
            "onClientStateChange() called with: gatt = $gatt, status = $status, newState = $newState"
        )
        if (newState == BluetoothProfile.STATE_CONNECTED) {
           // isConnected = true
          //  listener.onEvent(GattStatus.CLIENT_CONNECTED,gatt?.device?.name)
            gatt?.discoverServices()
        } else {
            isConnect = false
            listener.onEvent(GattStatus.CLIENT_DISCONNECTED,gatt?.device?.name)
        }
    }

    override fun onClientConnectService(gatt: BluetoothGatt?, status: Int) {
        super.onClientConnectService(gatt, status)
        blueGatt = gatt
        if (status == BluetoothGatt.GATT_SUCCESS) {
            listener.onEvent(GattStatus.CLIENT_CONNECTED,gatt?.device?.name)
        } else {
            listener.onEvent(GattStatus.CLIENT_DISCONNECTED,gatt?.device?.name)
        }
        Log.d(TAG, "onClientConnectService() called with: gatt = ${gatt?.device}, status = $status")
      /*  val service = gatt?.getService(BleBlueImpl.UUID_SERVICE)
        mBluetoothGatt = gatt
        logInfo("已连接上 GATT 服务，可以通信! ")*/
    }

    fun send(data:ByteArray){
        //uuid 是一对的
        blueGatt?.getService(UUID_SERVICE)?.let {
            it.getCharacteristic(UUID_WRITE)?.let { char->
                char.value = data
                val isSuccess = blueGatt?.writeCharacteristic(char)
                Log.d(TAG, "zsr send: $isSuccess")
            }
        }

    }

    override fun release() {
        super.release()
        blueGatt?.let {
            it.disconnect()
            it.close()
        }
    }
}