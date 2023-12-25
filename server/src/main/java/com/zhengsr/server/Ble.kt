package com.zhengsr.server

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanRecord

/**
 * @author by zhengshaorui 2023/12/21
 * describe：
 */
enum class BleError {
    CONTEXT_NULL,
    BLE_NOT_SUPPORT,
    BLUETOOTH_NOT_OPEN,
    PERMISSION_DENIED,
    NAME_TOO_LONG,
    ADVERTISE_FAILED,
    PACKAGE_MISS
}

enum class BleStatus{
    ADVERTISE_SUCCESS,
    CLIENT_CONNECTED,
    CLIENT_DISCONNECT,
    DATA
}

enum class GattStatus{

    CLIENT_CONNECTED,
    CLIENT_DISCONNECTED,
    MTU_CHANGE,
    WRITE_RESPONSE,
    INCOMPLETE_DATA,
    BLUE_NAME,
    LOG
}

