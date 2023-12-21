package com.zhengsr.server.server

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings

/**
 * @author by zhengshaorui 2023/12/12
 * describe：广播服务端
 */
internal class BleAdvServer(val bluetoothAdapter: BluetoothAdapter) {

    private var advertiseCallback: AdvertiseCallback? = null
    fun startBroadcast(advertiseCallback: AdvertiseCallback) {
        this.advertiseCallback = advertiseCallback
        /**
         * GAP广播数据最长只能31个字节，包含两中： 广播数据和扫描回复
         * - 广播数据是必须的，外设需要不断发送广播，让中心设备知道
         * - 扫描回复是可选的，当中心设备扫描到才会扫描回复
         * 广播间隔越长，越省电
         */

        //广播设置
        val advSetting = AdvertiseSettings.Builder()

            //使用 高功耗模式，建议应用在前台才使用此模式
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            // 高的发送功率
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            // 可连接
            .setConnectable(true)
            //广播时限。最多180000毫秒。值为0将禁用时间限制。（不设置则为无限广播时长）
            .setTimeout(0)
            .build()
        //设置广播包，这个是必须要设置的
        val advData = AdvertiseData.Builder()
            .setIncludeDeviceName(true) //显示名字
            .setIncludeTxPowerLevel(true)//设置功率
            //设置 UUID 服务的 uuid，其实数据可以隐藏在这个 UUID 里面，不设置，可支持27个字节，待验证：https://blog.csdn.net/baidu_35757025/article/details/114392518
            // .addServiceUuid(ParcelUuid(UUID_SERVICE))
            .build()

        /**
         *  扫描广播数据（可不写，客户端扫描才发送，实际上，厂商数据两个，长度和类型各一个，实际可用数据为27
         *  当广播被扫描到时，会发送扫描回复数据，这个数据可以自定义，但是长度不能超过31个字节
         */

        val scanResponse = AdvertiseData.Builder()
            //设置厂商数据
            .addManufacturerData(0xff, byteArrayOf(1))
            .build()
        var bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
        //开启广播,这个外设就开始发送广播了
        bluetoothLeAdvertiser.startAdvertising(
            advSetting,
            advData,
            scanResponse,
            advertiseCallback
        )
    }

    fun stopBroadcast() {
        advertiseCallback?.let {
            bluetoothAdapter.bluetoothLeAdvertiser?.stopAdvertising(it)
        }
    }
}