package com.excshare.server

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanRecord
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.util.SparseArray
import androidx.annotation.RequiresApi
import java.util.UUID

/**
 * @author by zhengshaorui 2023/12/21
 * describe：
 */

val UUID_SERVICE = UUID.fromString("10000000-0000-0000-0000-000000000000")
val UUID_READ_NOTIFY = UUID.fromString("11000000-0000-0000-0000-000000000000")
val UUID_WRITE = UUID.fromString("12000000-0000-0000-0000-000000000000")
val UUID_READ_DESCRIBE = UUID.fromString("12100000-0000-0000-0000-000000000000")
val UUID_WRITE_DESCRIBE = UUID.fromString("12200000-0000-0000-0000-000000000000")
val DATA_FLAG = 0X78.toByte()
val NAME_TYPE = 0X00.toByte()
val DATA_TYPE = 0X01.toByte()
val MTU_TYPE = 0X02.toByte()
val FORMAT_LEN = 9
val VERSION = 1

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

/**
 * 系统GPS是否打开
 * @return true = 打开
 */
fun isGpsOpen(context: Context?): Boolean {
    val locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
    return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
            || locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
}

/**
 * 判断是否拥有[permission]权限
 * @return true = 拥有该权限
 */
@RequiresApi(Build.VERSION_CODES.M)
fun isPermission(context: Context?, permission: String): Boolean {
    return context?.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}

/**
 * 设备是否支持蓝牙
 *  @return true = 支持
 */
fun isBleSupport(context: Context?): Boolean {
    return context?.packageManager?.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)?: false
}

/**
 * 判断是否拥有蓝牙权限
 * @return true = 拥有该权限
 */
@RequiresApi(Build.VERSION_CODES.M)
fun isPermission(context: Context?): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        isPermission(context?.applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION) &&
        isPermission(context?.applicationContext,
            Manifest.permission.ACCESS_COARSE_LOCATION) &&
        isPermission(context?.applicationContext,
            Manifest.permission.BLUETOOTH_SCAN) &&
        isPermission(context?.applicationContext,
            Manifest.permission.BLUETOOTH_ADVERTISE) &&
        isPermission(context?.applicationContext,
            Manifest.permission.BLUETOOTH_CONNECT)) {
        return true
    } else if (isPermission(context?.applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION) &&
        isPermission(context?.applicationContext,
            Manifest.permission.ACCESS_COARSE_LOCATION)) {
        return true
    }
    return false
}
@RequiresApi(Build.VERSION_CODES.M)
fun isHasBlePermission(context: Context?):Boolean{
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
        return isPermission(context?.applicationContext,
            Manifest.permission.BLUETOOTH_ADVERTISE) &&
                isPermission(context?.applicationContext,
                    Manifest.permission.BLUETOOTH_CONNECT)
    }

    return true
}


@RequiresApi(Build.VERSION_CODES.M)
fun isHasLocationPermission(context: Context?):Boolean{
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        isPermission(
            context?.applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }else{
        isPermission(
            context?.applicationContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
}

fun isSupportBle(context: Context?):Boolean{
    return context?.packageManager?.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)?: false
}



/**
 * 分包
 * @param data 需要分别的数据
 * @param packageLength 每个数据包最大长度
 */
fun subpackage(data: ByteArray, packageLength: Int): SparseArray<ByteArray> {
    val subpackages = SparseArray<ByteArray>()
    var offset = 0
    var packageId = 0

    while (offset < data.size) {
        val length = if (offset + packageLength <= data.size) {
            packageLength
        } else {
            data.size - offset
        }

        val subpackage = ByteArray(length)
        System.arraycopy(data, offset, subpackage, 0, length)
        subpackages.put(packageId, subpackage)
        offset += packageLength
        packageId++
    }

    return subpackages
}

