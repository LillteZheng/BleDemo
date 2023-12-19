package com.cvte.blesdk.abs

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import androidx.core.util.forEach
import com.cvte.blesdk.BleError
import com.cvte.blesdk.sender.IClientBle
import com.cvte.blesdk.server.BleServerOption
import com.cvte.blesdk.server.IBleListener
import com.cvte.blesdk.utils.BleUtil
import java.security.Key

/**
 * @author by zhengshaorui 2023/12/13
 * describe：
 */
abstract class AbsBle(val context: Context?) {

    companion object{
        private const val MAX_DATA_SIZE = 19
    }



    protected val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    protected open fun checkPermission(listener:IBleListener):Boolean{
        if (context == null){
            listener.onFail(BleError.CONTEXT_NULL,"context is null,please use BleSdk.init(context) first")
            return false
        }

        if (bluetoothAdapter?.isEnabled == false){
            listener.onFail(BleError.BLUETOOTH_NOT_OPEN,"bluetooth not open")
            return false
        }

        if (!BleUtil.isBleSupport(context)) {
            listener.onFail(BleError.BLE_NOT_SUPPORT,"bluetooth not support")
            return false
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (!BleUtil.isHasBlePermission(context)){
                val msg =
                    "BLUETOOTH_ADVERTISE | BLUETOOTH_CONNECT"

                listener.onFail(
                    BleError.PERMISSION_DENIED,"ble permission denied，" +
                        "make sure you have add  permission($msg)")
                return false
            }


        }
        return true
    }


    /**
     * 发送数据，采用以下格式
     * 第一个表示标志位采用两个字节，表示uuid后四位
     * 第二个表示类型，采用1个字节，比如notify，wirite等
     * 第三个表示数据长度：考虑到超过255，所以采用3个字节
     * 第四个表示数据，最大19个字节，如果设置了 mtu，则最大为 mtu
     * 如果有 uuid 标志位，则表示是第一包，否则表示是后续包
     * 0               8               16              24            31
     *  0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7 0 1 2 3 4 5 6 7
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |      UUID     |  flag |          len          |               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+               +
     * |                                                               |
     * +                                                               +
     * |                            payload                            |
     * +                                                               +
     * |                                                               |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *
     *
     */
    open fun send(data: ByteArray){
        val datas = BleUtil.subpackage(data, MAX_DATA_SIZE)
        if (data.size > 1){
            datas.forEach { index, bytes ->
                if (index == 0){
                }else{

                }

            }
        }
    }
}