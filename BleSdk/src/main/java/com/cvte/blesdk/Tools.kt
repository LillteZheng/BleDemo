package com.cvte.blesdk

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanRecord
import java.util.UUID


enum class BleError {
    CONTEXT_NULL,
    BLE_NOT_SUPPORT,
    BLE_NOT_OPEN,
    PERMISSION_DENIED,
    GPS_NOT_OPEN,
    NAME_TOO_LONG,
    ADVERTISE_FAILED
}
enum class GattStatus{
    //服务端
    SERVER_CONNECTED,
    SERVER_DISCONNECTED,
    SERVER_WRITE,

    //客户端
    CLIENT_CONNECTED,
    CLIENT_DISCONNECTED,
    CLIENT_READ
}

val UUID_SERVICE = UUID.fromString("10000000-0000-0000-0000-000000000000")
val UUID_READ_NOTIFY = UUID.fromString("11000000-0000-0000-0000-000000000000")
val UUID_WRITE = UUID.fromString("12000000-0000-0000-0000-000000000000")
val UUID_DESCRIBE = UUID.fromString("12000000-0000-0000-0000-000000000000")

data class ScanBeacon(val name:String?,val rssi:Int,val device:BluetoothDevice?,val record:ScanRecord?)