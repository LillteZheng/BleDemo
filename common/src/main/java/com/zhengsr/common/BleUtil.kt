/*
 * Copyright (c) 2022-2032 buhuiming
 * 不能修改和删除上面的版权声明
 * 此代码属于buhuiming编写，在未经允许的情况下不得传播复制
 */
package com.zhengsr.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.util.Log
import android.util.SparseArray
import androidx.annotation.RequiresApi
import java.util.LinkedList
import java.util.Queue


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



}