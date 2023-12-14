package com.cvte.blesdk.sender

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import androidx.core.content.PermissionChecker.PermissionResult
import com.cvte.blesdk.BleError
import com.cvte.blesdk.BleSdk
import com.cvte.blesdk.GattStatus
import com.cvte.blesdk.ScanBeacon
import com.cvte.blesdk.abs.AbsBle
import com.cvte.blesdk.abs.IBle
import com.cvte.blesdk.characteristic.AbsCharacteristic
import com.cvte.blesdk.characteristic.ClientGattChar
import com.cvte.blesdk.utils.BleUtil
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author by zhengshaorui 2023/12/12
 * describe：
 */
class BleClient(context: Context?) : AbsBle(context) {
    private var listener: IBleClientListener? = null
    private val isScanning = AtomicBoolean(false)
    private var gattChar:ClientGattChar? = null
    fun startScan(listener: IBleClientListener) {
        this.listener = listener
        if (!checkPermission(listener)) {
            return
        }
        if (isScanning.get()){
            return
        }
        isScanning.set(true)
        bluetoothAdapter?.bluetoothLeScanner?.startScan(null, configScanSession().build(), scanCallback)
        Handler().postDelayed({
            stopScan()
        }, 3000)
    }

    fun connect(dev:BluetoothDevice,autoConnect:Boolean){
      //  dev.connectGatt(BleSdk.context!!,autoConnect,)
        if (gattChar == null) {
            gattChar = ClientGattChar(object : AbsCharacteristic.IGattListener {
                override fun onEvent(status: GattStatus, obj: Any?) {
                    listener?.onLog("onEvent status = $status,obj = $obj")
                    /*when(status){
                        GattStatus.CLIENT_CONNECTED -> {
                            listener?.onSuccess()
                        }
                    }*/
                }

            })
        }
        gattChar?.connectGatt(dev,autoConnect)
    }

    fun stopScan() {
        if (isScanning.get()) {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            isScanning.set(false)
        }
    }


    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            //不断回调，所以不建议做复杂的动作
            result ?: return
            result.device.name ?: return

            listener?.onScanResult(ScanBeacon(result.device.name,result.device,result.scanRecord))
        }


    }

    override fun checkPermission(listener: IBle): Boolean {
        var permission = super.checkPermission(listener)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!BleUtil.isGpsOpen(context)) {
                listener.onFail(BleError.GPS_NOT_OPEN, "gps not open")
                permission = false
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!BleUtil.isPermission(context, Manifest.permission.BLUETOOTH_CONNECT)) {
                listener.onFail(BleError.PERMISSION_DENIED, "BLUETOOTH_CONNECT permission denied")
                permission = false
            }
            if (!BleUtil.isPermission(context, Manifest.permission.BLUETOOTH_SCAN)) {
                listener.onFail(BleError.PERMISSION_DENIED, "BLUETOOTH_SCAN permission denied")
                permission = false
            }


        }
        return permission
    }

    interface IBleClientListener:IBle{
        fun onScanResult(beacon: ScanBeacon)
    }


    private fun configScanSession():ScanSettings.Builder{
        //扫描设置

        val builder = ScanSettings.Builder()
            /**
             * 三种模式
             * - SCAN_MODE_LOW_POWER : 低功耗模式，默认此模式，如果应用不在前台，则强制此模式
             * - SCAN_MODE_BALANCED ： 平衡模式，一定频率下返回结果
             * - SCAN_MODE_LOW_LATENCY 高功耗模式，建议应用在前台才使用此模式
             */
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)//高功耗，应用在前台

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            /**
             * 三种回调模式
             * - CALLBACK_TYPE_ALL_MATCHED : 寻找符合过滤条件的广播，如果没有，则返回全部广播
             * - CALLBACK_TYPE_FIRST_MATCH : 仅筛选匹配第一个广播包出发结果回调的
             * - CALLBACK_TYPE_MATCH_LOST : 这个看英文文档吧，不满足第一个条件的时候，不好解释
             */
            builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        }

        //判断手机蓝牙芯片是否支持皮批处理扫描
        if (bluetoothAdapter?.isOffloadedFilteringSupported == true) {
            builder.setReportDelay(0L)
        }

        return builder
    }

}