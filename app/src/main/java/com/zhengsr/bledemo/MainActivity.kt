package com.zhengsr.bledemo

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

/**
 * @author by zhengshaorui 2023/12/14
 * describe：
 */
class MainActivity :AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }

    fun openServer(view: View) {
        startActivity(Intent(this,ServerActivity::class.java))
    }
    fun openClient(view: View) {
        startActivity(Intent(this,ClientActivity::class.java))
    }
}