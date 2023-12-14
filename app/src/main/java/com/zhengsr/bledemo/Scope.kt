package com.zhengsr.bledemo

import android.content.Context
import android.os.Trace
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import kotlinx.coroutines.*

/**
 * @author by zhengshaorui 2020/11/26 09:17
 * describe：协程封装类
 */

private val job = Job()
val scope = CoroutineScope(job)

fun scopeIo(block: suspend CoroutineScope.() -> Unit) =
    scope.launch(Dispatchers.IO) { block(this) }
fun scopeDe(block: suspend CoroutineScope.() -> Unit) =
    scope.launch(Dispatchers.Default) { block(this) }
fun scopeMain(block: suspend CoroutineScope.() -> Unit) =
    scope.launch(Dispatchers.Main) { block(this) }
suspend fun <T> withIo(block: suspend CoroutineScope.() -> T) =
    withContext(Dispatchers.IO) { block(this) }
suspend fun <T> withDe(block: suspend CoroutineScope.() -> T) =
    withContext(Dispatchers.Default) { block(this) }

suspend fun <T> withMain(block: suspend CoroutineScope.() -> T) =
    withContext(Dispatchers.Main) { block(this) }
//释放
fun releaseScope(){
    job.cancel()
}

