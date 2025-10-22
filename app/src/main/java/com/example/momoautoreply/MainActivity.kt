package com.example.momoautoreply

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Context

class MainActivity : AppCompatActivity() {
    private val pref by lazy { getSharedPreferences("momo_bot", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // load example settings
        edit_message.setText(pref.getString("msg0", "hi! 帅哥你好吗?"))

        btn_accessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        btn_open_momo.setOnClickListener {
            val pkg = edit_target_package.text.toString().ifEmpty { "com.immomo.momo" }
            val launch = packageManager.getLaunchIntentForPackage(pkg)
            if (launch != null) startActivity(launch) else {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))
                startActivity(intent)
            }
        }

        btn_save.setOnClickListener {
            val editor = pref.edit()
            editor.putString("target_package", edit_target_package.text.toString())
            editor.putString("msg0", edit_message.text.toString())
            editor.putInt("interval_seconds", edit_interval.text.toString().toIntOrNull() ?: 2)
            editor.apply()
        }

        // load stored
        edit_target_package.setText(pref.getString("target_package", "com.immomo.momo"))
        edit_interval.setText(pref.getInt("interval_seconds", 2).toString())
    }
}
