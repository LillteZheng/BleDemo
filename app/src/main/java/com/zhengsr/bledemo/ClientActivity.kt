package com.zhengsr.bledemo

import android.Manifest
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
import com.zhengsr.bledemo.databinding.ActivityClientBinding
import com.zhengsr.client.BleClient
import com.zhengsr.client.BleError
import com.zhengsr.client.BleStatus
import com.zhengsr.client.DataError
import com.zhengsr.client.ScanBeacon
import com.zhengsr.client.client.BleOption
import com.zhengsr.client.client.IBle
import java.io.PushbackInputStream

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
      
        val bleData = mData[position]
        bleData.device?.let {
            appInfo("开始连接 ${it.name} ...")
            BleClient.get().connect(it)

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
        val option = BleOption.Builder()
            .context(this)
            .scanTime(6000L)
            .logListener(object : BleOption.ILogListener {
                override fun onLog(log: String) {
                    Log.d(TAG, log)

                    //appInfo(log)
                }

            }).build()
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

            override fun onFail(errorCode: BleError, msg: String) {
                appInfo("errorCode = $errorCode, msg = $msg")
            }

        })
    }

    fun writeData(view: View) {
        val msg = binding.edit.text.toString().trim()
        BleClient.get().send(msg.toByteArray(), object : IBle.IWrite {
            override fun onSuccess() {
                appInfo("发送成功")
            }

            override fun onFail(dataError: DataError, errorMsg: String) {
                appInfo("发送失败：$errorMsg")
            }

        })
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
        BleClient.release()
    }

    fun disconnect(view: View) {
        appInfo("断开连接")
        BleClient.get().disconnect()
    }

    private val msg2 = """
        123344
        So I’ve faced some issues with a BLE read, and here is the best summary I have:

        “Receive String” works on a READ characteristic
        “Receive Byte Array” returns on error on the exact same characteristic.
        Here is my case, I can connect to a BLE device, and then read on a button click. 
        Using a counter, I can alternative between the two reads. The read string version returns 
        the data, but the Receive Byte array always flags an error. Ultimately, 
        I need the Read Byte Array for my application, but I haven’t been able to debug 
        the error difference between the calls.
    """.trimIndent()

    private val msg = """
       
        歌词是诗歌的一种，入乐的叫歌，不入乐的叫诗（或词）。入乐的歌在感情抒发、形象塑造上和诗没有任何区别，但在结构上、节奏上要受音乐的制约，在韵律上要照顾演唱的方便，在遣词炼字上要考虑听觉艺术的特点，因为它要入乐歌唱。歌词与诗的分别，主要是诗不一定要入乐（合乐），歌词是要合乐的。合乐成为歌曲。歌词一般是配合曲子旋律一同出现的，歌词是歌曲的本意所在。现代一般是配合音乐，便于哼唱的语句。
        基本解释
        播报
        编辑
        [words of a song] 声乐作品中的词句
        多样的歌词配上与之吻合风格的音乐 [1]
        引证解释
        播报
        编辑
        指歌曲的唱词。
        唐宋之问《奉和幸长安故城未央宫应制》：“乐思回斜日，歌词继《大风》。”
        《旧唐书·音乐志三》：“时太常相传有宫、商、角、徵、羽《宴乐》五调歌词各一卷……词多 郑 卫 ，皆近代词人杂诗。”
        清李调元《南越笔记》卷一：“男遗女一扁担，上镌歌词数首，字若蝇头。” [1]
        时太常相传有宫、商、角、徵、羽《宴乐》五调歌词各一卷……词多郑卫，皆近代词人杂诗。
        起源与发展
        播报
        编辑
        中国最早的歌词是传说中的只有四个字的《涂山氏歌》：候人兮猗。《诗经》就是入乐歌唱的，所谓南、风、雅、颂，本部都是音乐的名称，三百篇，无一不是音乐文学。以《诗经》为标志，音乐文学成熟了。 [2]宋词、元曲均是一种歌词，除独立吟诵外，亦可以配合歌曲，以当时的汉字发音唱出。可惜现时大部分曲谱已失传，而且因语音变化相当大，古人亦无录音设备，才使今人难以把这些词曲咏唱出来。
        及至今天的戏曲、歌剧、音乐剧、流行音乐等等同时涉及到文学和音乐的创作，作品的文词部分仍称作歌词。
        创作歌词，一般称为填词或作词。
        到了互联网的时代，歌词被赋予了更智能化的性能，拿QQ音乐APP举例，一名用户能看到的不仅仅是一个歌词文本，还有歌词跟随歌曲精准滚动、外文歌词搭配的翻译歌词、甚至还有长音频内容下面展示的AI歌词以及AI翻译。而在网易云以及市面上众多的音乐APP赋予了歌词“美”的含义，分享“歌词海报”到朋友圈甚至成为了当代用户分享音乐的方式。
        作用
        播报
        编辑
        由于歌词表达了歌曲的宗旨和灵魂，阐述了一首歌所要表达的感情和主旨，所以即使年代久远，有的歌词也不容易忘记。歌词的好坏有时候在很大程度决定了一首歌的好坏，因此很多著名的歌手也会请著名的填词人来创作，当前歌词创作比较优秀的人有林夕、方文山、罗大佑、李宗盛、高晓松、李焯雄、黄霑、徐佳莹、薛之谦、陈信宏、黄伟文、林若宁、黄俊郎、许嵩、毛不易等。
        随着人对歌词的重视，歌词这一文体将会被越来越多的人所借用。随着越来越多高素质文人选择歌词创作，流行歌词的艺术水准将大大提高。
        分类
        播报
        编辑
        歌词从内容上分不外乎叙事、抒情、写景、说理、对唱五种。而流行歌曲中最常见的就是”叙事+抒情”，即使是纯叙事的歌曲也需浓厚的感情做基础（如李春波的《小芳》和《一封家书》）。纯抒情的歌也不多，《一千个伤心的理由》勉强算是吧，但还是有一点点情节的影子。写景的流行歌曲好的很少，BEYOND的《长城》是其中的佼佼者。说理的流行歌曲也不多，因为一不小心就成了说教，写得好的当属《凡人歌》，说理歌到此也就见顶了。对唱的形式一般用在重唱中，二个或更多的人像互相对话一样地歌唱，经典之作首推李宗盛的《当爱已成往事》和《最近比较烦》。
        开始学作词时，还是以创作”叙事+抒情”的作品来锻炼自己，等真正有实力和灵感时再创作其他类型的歌词吧。学习写作的过程总是这样的，先是模仿，然后是写作，最后经历不断地磨练和突破，才能进行原创的殿堂，创作出具有自己风格的作品。
        创作歌词要注意七个方面的问题：素材、题材、主题、形象、节奏、音韵、修辞。 [2]
        有了一个好的作品之后，还要给它起一个好的名字。好的名字是歌词不可分割的一部分，要能够反映歌词的主题，吸引人的注意和兴趣。采用的方法主要有：一、用这首歌中最经典的句子，如《有多少爱可以重来》；二、用这首歌最核心的意象，如《棋子》；三、用与这首歌有关的对象，如《阿姐鼓》；四、用能够反映作者创作意图的概念，如《恋曲1990》。
        从歌词文件的角度，可把歌词分为外挂歌词和内嵌歌词两类。外挂歌词实际是一个包含时间码和歌词文本的文本文件，内嵌歌词则是把外挂歌词的文本放入了音频文件的文件头中，但内嵌歌词的规格并不统一，如用Windows Media Player制作的内嵌歌词并不能被其它的歌词插件所支持。所以，我们所用的歌词制作工具最好能同时支持外挂歌词和内嵌歌词的制作，这样才会有最好的兼容性。
        赏析
        播报
        编辑
        一般从歌词的形象性、抒情性和歌唱性本质特征出发欣赏歌词，就汉语歌词而言，还要结合汉语词语的形象、节奏、音韵和修辞特点欣赏。 [2]
     
    """.trimIndent()
}