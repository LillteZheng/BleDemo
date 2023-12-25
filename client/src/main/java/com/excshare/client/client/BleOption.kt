package com.excshare.client.client

import android.content.Context
import com.excshare.common.UUID_READ_NOTIFY
import com.excshare.common.UUID_SERVICE
import com.excshare.common.UUID_WRITE
import java.util.UUID

/**
 * @author by zhengshaorui 2023/12/14
 * describe：服务端配置
 */
class BleOption private constructor(val builder: Builder)  {

    class Builder{
        internal var fliter: String? = null
        internal var logListener: ILogListener? = null
        internal var serviceUUid = UUID_SERVICE
        internal var writeUuid = UUID_WRITE
        internal var readAndNotifyUuid = UUID_READ_NOTIFY
        internal var context: Context? = null
        internal var scanTime: Long = 5000L
        internal var connectRetry = 3
        fun context(context: Context): Builder {
            this.context = context
            return this
        }
        fun connectRetry(connectRetry: Int): Builder{
            this.connectRetry = connectRetry
            return this
        }
        fun scanTime(scanTime: Long): Builder {
            this.scanTime = scanTime
            return this
        }
        fun serviceUuid(serviceUUid: UUID): Builder {
            this.serviceUUid = serviceUUid
            return this
        }
        fun writeUuid(writeUuid: UUID): Builder {
            this.writeUuid = writeUuid
            return this
        }
        fun readAndNotifyUuid(readAndNotifyUuid: UUID): Builder {
            this.readAndNotifyUuid = readAndNotifyUuid
            return this
        }
        fun fliterName(name: String): Builder {
            this.fliter = name
            return this
        }
        fun logListener(logListener: ILogListener): Builder {
            this.logListener = logListener
            return this
        }

        fun build(): BleOption {
            return BleOption(this)
        }


    }
    interface ILogListener{
        fun onLog(log:String)
    }
    

    
}