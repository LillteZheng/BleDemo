## 蓝牙发送端说明

## 1. 申请权限
```kotlin
val bluetooth = BluetoothAdapter.getDefaultAdapter()
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ), 1)
        if (bluetooth == null) {
            Toast.makeText(this, "您的设备未找到蓝牙驱动！!", Toast.LENGTH_SHORT).show()
            finish()
        }else {
            if (!bluetooth.isEnabled) {
                startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),1)
            }
        }

//在 Android 10 还需要开启 gps
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    val lm: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
        Toast.makeText(this, "请您先开启gps,否则蓝牙不可用", Toast.LENGTH_SHORT).show()
    }
}
```

## 2. 配置
```kotlin
        val option = BleOption.Builder()
            .context(this)
            .scanTime(6000L)
            .logListener(object : BleOption.ILogListener {
                override fun onLog(log: String) {
                    Log.d(TAG, log)
                }

            }).build()
```
其中，scanTime为扫描时间，单位毫秒，Android 要求不能一直扫描，默认为5s，logListener为日志监听器，可以监听蓝牙的日志输出。

## 3. 开始扫描
```kotlin
BleClient.get().startScan(option,object : IBle.IListener {
    override fun onEvent(status: BleStatus, obj: String?) {
        when(status){

            BleStatus.SERVER_CONNECTED->{
                appInfo("连接上服务端：$obj")
            }
            BleStatus.SERVER_DISCONNECTED->{
                appInfo("服务端断开连接：$obj")
            }
            BleStatus.SERVER_WRITE->{
                appInfo("服务端写入数据：$obj")
            }
            BleStatus.SCAN_FAILED->{
                appInfo("扫描失败：请重新扫描 ")
            }

            else -> {}
        }
    }

    override fun onScanResult(beacon: ScanBeacon) {
        if (mData.size == 0 || mData.none { beacon.name == it.name }) {
            mData.add(beacon)
            mBleAdapter?.notifyItemInserted(mData.size - 1)
        }
    }

    override fun onFail(errorCode: BleError, msg: String, obj:Any?) {
        appInfo("errorCode = $errorCode, msg = $msg,obj= $obj")
    }

})
```
其中，onEvent为客户端的回调，可以监听客户端的状态，以及服务端的连接状态，数据的接收等。
onScanResult为扫描到的设备，可以通过beacon.name来判断是否是自己需要的设备。

## 4. 连接服务端
```kotlin
  BleClient.get().connect(bluetooDevice)
```
其中，bluetooDevice为扫描到的设备，可以通过beacon.device来获取,回调会在 onEvent 中回调。


## 5. 发送数据
```kotlin
BleClient.get().send(msg.toByteArray(), object : IBle.IWrite {
    override fun onSuccess() {
        appInfo("发送成功")
    }

    override fun onFail(dataError: DataError, errorMsg: String) {
        appInfo("发送失败：$errorMsg")
    }

})
```
其中，msg为需要发送的数据，回调会在 onEvent 中回调。

## 6. 是否连接
```kotlin
BleClient.get().isConnected()
```

## 6. 断开服务器
```kotlin
   BleClient.get().disconnect()
```

## 7. 停止扫描
```kotlin
   BleClient.get().stopScan()
```

## 8. 退出
```kotlin
   BleClient.get().release()
```


1. 有时发送完数据，就会断开连接，特别是在分包的时候