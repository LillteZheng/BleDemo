package com.cvte.blesdk.abs

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import com.cvte.blesdk.BleError
import com.cvte.blesdk.server.BleServer
import com.cvte.blesdk.utils.BleUtil

/**
 * @author by zhengshaorui 2023/12/13
 * describe：
 */
abstract class AbsBle(val context: Context?) {

    protected val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    protected open fun checkPermission(listener:IBle):Boolean{
        if (context == null) {
            listener.onFail(BleError.CONTEXT_NULL,"context is null,please use BleSdk.inject(context) to init")
            return false
        }

        if (bluetoothAdapter?.isEnabled == false){
            listener.onFail(BleError.BLE_NOT_OPEN,"bluetooth not open")
            return false
        }

        if (!BleUtil.isBleSupport(context)) {
            listener.onFail(BleError.BLE_NOT_SUPPORT,"bluetooth not support")
            return false
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (!BleUtil.isHasBlePermission(context)){
                val msg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                    "BLUETOOTH_ADMIN | BLUETOOTH_ADVERTISE | BLUETOOTH_CONNECT"
                }else{
                    "ACCESS_COARSE_LOCATION"
                }
                listener.onFail(
                    BleError.PERMISSION_DENIED,"ble permission denied，" +
                        "make sure you have add  permission($msg)")
                return false
            }
            //ble 不需要位置权限
            /*if (!BleUtil.isHasLocationPermission(context)){
                val msg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    "ACCESS_FINE_LOCATION"
                }else{
                    "ACCESS_COARSE_LOCATION"
                }
                listener.onFail(
                    BleError.PERMISSION_DENIED,"location permission denied," +
                        "make sure you have add permission($msg)")
                return false
            }*/

        }
        return true
    }
}