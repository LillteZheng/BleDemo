
# 蓝牙服务端说明

申请权限：
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
```

## 1.1 配置
```kotlin

val builder = BleOption.Builder()
    .context(this)
    .name("Vieunite_345663")
    .logListener(object: BleOption.ILogListener{
        override fun onLog(log: String) {
            Log.d(TAG, "$log")
        }
    }).build()
```

其中name为蓝牙广播名称，不要超过20个字节。
logListener为日志监听器，可以监听蓝牙的日志输出。

## 1.2 启动服务

```kotlin
BleServer.get().startServer(builder, object : com.zhengsr.server.server.IBle.IListener {
            override fun onFail(error: com.zhengsr.server.BleError, errorMsg: String) {
                appInfo("失败: error = $error, errorMsg = $errorMsg")
            }

            override fun onEvent(status: BleStatus, obj: String?) {
                when(status){
                    BleStatus.ADVERTISE_SUCCESS -> {
                        appInfo("开启广播成功，请搜索设备：$obj")
                    }
                    BleStatus.CLIENT_CONNECTED -> {
                        appInfo("设备($obj)，连接成功，可以通信了")
                    }
                    BleStatus.CLIENT_DISCONNECT -> {
                        appInfo("设备($obj)，断开连接")
                    }
                    BleStatus.DATA->{
                        appInfo("收到数据: $obj")
                    }
                    else -> {
                        appInfo("事件: serverStatus = $status, obj = $obj")
                    }
                }
            }

        })
```
其中，onEvent为服务端的回调，可以监听服务端的状态，以及客户端的连接状态，数据的接收等。


## 1.3 发送数据

```kotlin
 BleServer.get().send(msg2.toByteArray(), object : IBle.IWrite {
            override fun onStart() {
            }

            override fun onSuccess() {
            }

            override fun onFail(errorMsg: String) {
            }

        })
```
目前回调还未完成实现，可忽略，后续会更新。

## 1.4 断开连接

```kotlin
 BleServer.get().cancelConnect(bluetoothDevice)
```

## 1.5 是否连接
    
```kotlin
BleServer.get().isConnected(bluetoothDevice)
```

## 1.5 停止服务

```kotlin
 BleServer.get().release()
```