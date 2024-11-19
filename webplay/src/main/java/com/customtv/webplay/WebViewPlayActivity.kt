package com.customtv.webplay

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.WebView
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.customtv.webplay.WebConstant.liveUrls
import com.customtv.webplay.WebViewUtils.hideLoading
import com.customtv.webplay.WebViewUtils.showLoading
import com.customtv.webplay.WebViewUtils.startPlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WebViewPlayActivity : AppCompatActivity() {
    private lateinit var ivLoading: ImageView
    private lateinit var webView: WebView
    private var currentPosition = 0

    companion object {
        fun start(context: Context, position: Int) {
            val intent = Intent(context, WebViewPlayActivity::class.java)
            intent.putExtra("position", position)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview_play)
        currentPosition = intent.getIntExtra("position", 0)
        loadPosition(currentPosition)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                Toast.makeText(this, "播放下一个", Toast.LENGTH_SHORT).show()
                loadPosition(++currentPosition)
            }

            KeyEvent.KEYCODE_DPAD_UP -> {
                Toast.makeText(this, "播放上一个", Toast.LENGTH_SHORT).show()
                loadPosition(--currentPosition)
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun loadPosition(position: Int) {
        ivLoading = findViewById(R.id.ivLoading)
        webView = findViewById(R.id.webView)
        if (liveUrls.size <= currentPosition || currentPosition == -1) {
            Toast.makeText(this, "播放完毕", Toast.LENGTH_SHORT).show()
            currentPosition = if (currentPosition == -1) 0 else liveUrls.size - 1
            return
        }
        webView.alpha = 0f
        val oa = showLoading(ivLoading)
        webView.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                startPlay(liveUrls[position]) {
                    lifecycleScope.launch {
                        delay(1000)
                        webView.alpha = 1f
                        hideLoading(ivLoading, oa)
                    }
                }
            }
        }
    }
}