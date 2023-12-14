package com.zhengsr.bledemo

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.listener.OnItemClickListener
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.cvte.blesdk.BleError
import com.cvte.blesdk.BleSdk
import com.cvte.blesdk.ScanBeacon
import com.cvte.blesdk.sender.BleClient
import com.zhengsr.bledemo.databinding.ActivityClientBinding

class ClientActivity : AppCompatActivity(), OnItemClickListener {
    companion object{
        private const val TAG = "ClientActivity"
    }
    private lateinit var binding: ActivityClientBinding
    private val mData: MutableList<ScanBeacon> = mutableListOf()
    private var mBleAdapter: BlueAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
        ), 1)
        initRecyclerView()
        //在 Android 10 还需要开启 gps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val lm: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                Toast.makeText(this, "请您先开启gps,否则蓝牙不可用", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.recycler)
        val manager = LinearLayoutManager(this)
        recyclerView.layoutManager = manager
        mBleAdapter = BlueAdapter(R.layout.recy_ble_item_layout, mData)
        recyclerView.adapter = mBleAdapter

        mBleAdapter?.setOnItemClickListener(this)
    }

    class BlueAdapter(layoutId: Int, datas: MutableList<ScanBeacon>) :
        BaseQuickAdapter<ScanBeacon, BaseViewHolder>(layoutId, datas) {
        override fun convert(holder: BaseViewHolder, item: ScanBeacon) {
            //没有名字不显示
            holder.setText(R.id.item_ble_name_tv, "名称: " + item.name ?: "Null")
                .setText(R.id.item_ble_rssi_tv, "信号: " + item.rssi)
                .setText(R.id.item_ble_mac_tv, "地址: " + item.device?.address)
                .setText(R.id.item_ble_device_tv, item.record?.toString() ?: "Null")
        }

    }

    override fun onItemClick(adapter: BaseQuickAdapter<*, *>, view: View, position: Int) {
        //连接之前先关闭连接
      //  closeConnect()
        BleSdk.getClient().release()
        val bleData = mData[position]
        bleData.device?.let {
            appInfo("开始连接 ${it.name} ...")
            BleSdk.getClient().connect(it,false)

        }
      //  blueGatt = bleData.dev.connectGatt(this, false, blueGattListener)
       // logInfo("开始与 ${bleData.dev.name} 连接.... $blueGatt")
    }

    /**
     * 扫描
     */
    fun scan(view: View) {
        appInfo("开始扫描...")
        mData.clear()
        mBleAdapter?.notifyDataSetChanged()
        BleSdk.getClient().startScan(object : BleClient.IBleClientListener {

            override fun onScanResult(beacon: ScanBeacon) {
                if (mData.size == 0 || mData.none { beacon.name == it.name }) {
                    mData.add(beacon)
                    mBleAdapter?.notifyItemInserted(mData.size - 1)
                }
            }

            override fun onLog(msg: String) {
               // Log.d(TAG, "onLog() called with: msg = $msg")
                appInfo(msg)
            }

            override fun onFail(errorCode: BleError, msg: String) {
                appInfo("errorCode = $errorCode, msg = $msg")
            }

        })
    }

    fun writeData(view: View) {
        val msg = binding.edit.text.toString().trim()
        BleSdk.getClient().send(msg.toByteArray())
    }

    fun appInfo(msg:String){
        runOnUiThread {
            binding.infoTv.append(msg+"\n")
        }
    }

    fun clearInfo(view: View) {
        binding.infoTv.text = ""
    }

    override fun onDestroy() {
        super.onDestroy()
        BleSdk.getClient().release()
    }
}