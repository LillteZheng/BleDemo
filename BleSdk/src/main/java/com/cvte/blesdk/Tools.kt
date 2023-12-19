package com.cvte.blesdk

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanRecord
import java.util.UUID


enum class BleError {
    CONTEXT_NULL,
    BLE_NOT_SUPPORT,
    BLUETOOTH_NOT_OPEN,
    PERMISSION_DENIED,
    GPS_NOT_OPEN,
    NAME_TOO_LONG,
    ADVERTISE_FAILED,
    NAME_NULL
}
enum class GattStatus{
    //服务端
    SERVER_CONNECTED,
    SERVER_DISCONNECTED,
    SERVER_WRITE,

    //客户端
    CLIENT_CONNECTED,
    CLIENT_DISCONNECTED,
    CLIENT_READ,

    LOG
}


enum class ClientStatus{
    SCAN_RESULT,
    SERVER_CONNECTED,
    SERVER_DISCONNECTED,
    SCAN_FAILED,
    SERVER_WRITE,
}
enum class ServerStatus{
    ADVERTISE_SUCCESS,
    CLIENT_CONNECTED,
    CLIENT_DISCONNECT,
    CLIENT_WRITE
}

val UUID_WRITE_SUB = "1212"
val UUID_READ_NOTIFY_SUB = "1213"
val UUID_SERVICE = UUID.fromString("10000000-0000-0000-0000-000000001211")
val UUID_READ_NOTIFY = UUID.fromString("11000000-0000-0000-0000-00000000$UUID_READ_NOTIFY_SUB")
val UUID_WRITE = UUID.fromString("12000000-0000-0000-0000-00000000$UUID_WRITE_SUB")


data class ScanBeacon(val name:String?,val rssi:Int,val device:BluetoothDevice?,val record:ScanRecord?)