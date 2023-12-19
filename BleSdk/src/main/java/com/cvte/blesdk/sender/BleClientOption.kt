package com.cvte.blesdk.sender

import com.cvte.blesdk.UUID_READ_NOTIFY
import com.cvte.blesdk.UUID_SERVICE
import com.cvte.blesdk.UUID_WRITE
import java.util.UUID

/**
 * @author by zhengshaorui 2023/12/14
 * describe：服务端配置
 */
class BleClientOption private constructor(val builder: Builder)  {

    class Builder{
        internal var fliter: String? = null
        internal var logListener: ILogListener? = null
        internal var serviceUUid = UUID_SERVICE
        internal var writeUuid = UUID_WRITE
        internal var readAndNotifyUuid = UUID_READ_NOTIFY
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

        fun build(): BleClientOption {
            return BleClientOption(this)
        }


    }
    interface ILogListener{
        fun onLog(log:String)
    }
    

    
}