package com.excshare.server.server

import android.content.Context
import com.excshare.server.UUID_READ_NOTIFY
import com.excshare.server.UUID_SERVICE
import com.excshare.server.UUID_WRITE
import java.util.UUID

/**
 * @author by zhengshaorui 2023/12/14
 * describe：服务端配置
 */
class BleOption private constructor(val builder: Builder)  {
    companion object{
        private const val MAX_NAME_SIZE = 20
    }
    
    class Builder{
        internal var name: String? = null
        internal var logListener: ILogListener? = null
        internal var serviceUUid = UUID_SERVICE
        internal var writeUuid = UUID_WRITE
        internal var readAndNotifyUuid = UUID_READ_NOTIFY
        internal var context: Context? = null
        fun context(context: Context): Builder {
            this.context = context
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
        fun name(name: String): Builder {
            this.name = name
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