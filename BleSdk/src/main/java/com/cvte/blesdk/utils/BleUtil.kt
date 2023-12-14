/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.cvte.blesdk.utils

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.util.SparseArray
import androidx.annotation.RequiresApi
import kotlin.math.roundToInt


/**
 * 工具类
 */
object BleUtil {

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
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            isPermission(context?.applicationContext,
                Manifest.permission.BLUETOOTH_ADVERTISE) &&
            isPermission(context?.applicationContext,
                Manifest.permission.BLUETOOTH_CONNECT)
                ){
             true
        }  else
            isPermission(context?.applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION)
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
     * ScanResult转BleDevice
     */
    @SuppressLint("MissingPermission")
   /* fun scanResultToBleDevice(scanResult: ScanResult): BleDevice {
        return BleDevice(
            deviceInfo = scanResult.device,
            deviceName = scanResult.device?.name,
            deviceAddress = scanResult.device?.address,
            rssi = scanResult.rssi,
            timestampNanos = scanResult.timestampNanos,
            scanRecord = scanResult.scanRecord?.bytes,
            tag = null
        )
    }*/

    /**
     * 字节数组转16进制字符串
     *
     * @param bytes 需要转换的byte数组
     * @param addSpace 是否添加空格
     * @return 转换后的Hex字符串
     */
    fun bytesToHex(bytes: ByteArray?, addSpace: Boolean = true): String {
        if (bytes == null) {
            return ""
        }
        val sb = StringBuilder()
        for (aByte in bytes) {
            val hex = Integer.toHexString(aByte.toInt() and 0xFF)
            if (hex.length < 2) {
                sb.append(0)
            }
            sb.append(hex)
            if (addSpace) {
                sb.append(" ")
            }
        }
        return sb.toString()
    }

    /**
     * 分包
     * @param data 需要分别的数据
     * @param packageLength 每个数据包最大长度
     */
    fun subpackage(data: ByteArray, packageLength: Int): SparseArray<ByteArray> {
        val listData: SparseArray<ByteArray>
        if (data.size > packageLength) {
            val pkgCount = if (data.size % packageLength == 0) {
                data.size / packageLength
            } else {
                (data.size / packageLength + 1).toFloat().roundToInt()
            }
            listData = SparseArray<ByteArray>(pkgCount)
            for (i in 0 until pkgCount) {
                var dataPkg: ByteArray
                var length: Int
                if (pkgCount == 1 || i == pkgCount - 1) {
                    length = if (data.size % packageLength == 0) {
                        packageLength
                    } else {
                        data.size % packageLength
                    }
                    System.arraycopy(
                        data,
                        i * packageLength,
                        ByteArray(length).also { dataPkg = it },
                        0,
                        length
                    )
                } else {
                    System.arraycopy(
                        data,
                        i * packageLength,
                        ByteArray(packageLength).also { dataPkg = it },
                        0,
                        packageLength
                    )
                }
                listData.put(i, dataPkg)
            }
        } else {
            listData = SparseArray<ByteArray>(1)
            listData.put(0, data)
        }
        return listData
    }
}