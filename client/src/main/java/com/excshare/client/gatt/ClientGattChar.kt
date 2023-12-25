package com.excshare.client.gatt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.os.Handler
import android.util.Log
import com.excshare.client.GattStatus
import com.excshare.client.LastState
import com.excshare.common.DataPackage
import com.excshare.common.FORMAT_LEN
import com.excshare.common.NAME_TYPE
import com.excshare.common.UUID_READ_DESCRIBE
import com.excshare.common.UUID_READ_NOTIFY
import com.excshare.common.UUID_SERVICE
import com.excshare.common.UUID_WRITE


/**
 * @author by zhengshaorui 2023/12/14
 * describe：
 */
class ClientGattChar(listener: IGattListener,val handler: Handler?) : AbsCharacteristic(listener, "client") {

    private var dataPackage: DataPackage? = null
    private var blueGatt: BluetoothGatt? = null
    private var lastState = LastState.IDEL
    fun connectGatt(context: Context, dev: BluetoothDevice) {
        dataPackage?.resetBuffer()
        lastState = LastState.CONNECTING
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
        //todo 133 问题，需要重新扫描再配对
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            gatt?.requestMtu(500)
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED){
            isConnect = false
            if (lastState == LastState.CONNECTING){
                //连接中失败，重试几次
                listener.onEvent(GattStatus.CONNECT_FAIL, gatt?.device?.name)
            }else {
                listener.onEvent(GattStatus.DISCONNECT_FROM_SERVER, gatt?.device?.name)
                release()
            }
        }else{
            lastState = LastState.IDEL
            isConnect = false
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
                descriptor?.let {
                    it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(it)
                }
                pushLog("setCharacteristicNotification: $isSuccess")
            }

            listener.onEvent(GattStatus.CONNECT_TO_SERVER, gatt?.device?.name)
            listener.onEvent(GattStatus.SEND_BLUE_NAME,"")
            lastState = LastState.CONNECTED
        } else {
            lastState = LastState.IDEL
            listener.onEvent(GattStatus.DISCONNECT_FROM_SERVER, gatt?.device?.name)
            gatt?.close()
        }
        pushLog("connect to gatt Service,now you can communicate with it")
    }

    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
        super.onMtuChanged(gatt, mtu, status)
        pushLog("onMtuChanged() called with: gatt = $gatt, mtu = $mtu, status = $status")
        gatt?.discoverServices()
        if (status == BluetoothGatt.GATT_SUCCESS) {
            listener.onEvent(GattStatus.MTU_CHANGE, mtu.toString())
        }
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?
    ) {
        super.onCharacteristicChanged(gatt, characteristic)
        characteristic?.let {
            val value = it.value
            if (dataPackage == null) {
                dataPackage = DataPackage(FORMAT_LEN,handler)
            }
            dataPackage?.formData(value, object : DataPackage.IPackageListener {


                override fun onResult(type: Byte, data: ByteArray, missPackages: List<Int>?) {
                    val status = when (type) {
                        //MTU_TYPE-> GattStatus.MTU_CHANGE
                        NAME_TYPE -> GattStatus.SEND_BLUE_NAME
                        else -> GattStatus.NORMAL_DATA
                    }
                    if (!missPackages.isNullOrEmpty()){
                        listener.onDataMiss(status, String(data),missPackages)
                    }else{
                        listener.onEvent(status, String(data))
                    }
                }

            })
        }
    }



    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        super.onCharacteristicWrite(gatt, characteristic, status)
        listener.onEvent(GattStatus.WRITE_RESPONSE, status.toString())
    }
    @Synchronized
    fun refreshDeviceCache() {
        try {
            if (isConnect) {
                blueGatt?.disconnect()
                blueGatt?.close()
            }
            val refresh = BluetoothGatt::class.java.getMethod("refresh")
            if (refresh != null && blueGatt != null) {
                val success = refresh.invoke(blueGatt) as Boolean
                Log.d(TAG,"refreshDeviceCache, is success:  $success")
            }
        } catch (e: Exception) {
            Log.e(TAG,"exception occur while refreshing device: " + e.message)
            e.printStackTrace()
        }
    }

    @Synchronized
    fun send(data: ByteArray): Boolean {
        //uuid 是一对的
        val isSuccess = blueGatt?.getService(UUID_SERVICE)?.getCharacteristic(UUID_WRITE)?.let {
            it.value = data
            blueGatt?.writeCharacteristic(it)
        } ?: false
        //需要放队列里面，重新发
        return isSuccess
    }




    fun release() {
        lastState = LastState.IDEL
        blueGatt?.let {
            it.close()
            it.disconnect()
        }
        dataPackage?.resetBuffer()
        blueGatt = null
    }
}