package com.customtv.webplay

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.Toast
import android.widget.VideoView
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

    var oa: ObjectAnimator? = null

    private fun loadPosition(position: Int) {
        ivLoading = findViewById(R.id.ivLoading)
        webView = findViewById(R.id.webView)
        initWebView(webView)
        if (liveUrls.size <= currentPosition || currentPosition == -1) {
            Toast.makeText(this, "播放完毕", Toast.LENGTH_SHORT).show()
            currentPosition = if (currentPosition == -1) 0 else liveUrls.size - 1
            return
        }
        webView.alpha = 0f
        oa = showLoading(ivLoading)
//        webView.loadUrl(liveUrls[position])
        webView.apply {
            startPlay(liveUrls[position]) {
                lifecycleScope.launch {
                    delay(1000)
                    webView.alpha = 1f
                    hideLoading(ivLoading, oa)
                }
            }
        }
    }

    private fun startPlayByVideoView(url: String) {
        val videoView = findViewById<VideoView>(R.id.video_view)
        videoView.visibility = View.VISIBLE
        videoView.setVideoURI(Uri.parse(url))
        videoView.setOnPreparedListener {
            it.setVolume(1f, 1f)
            videoView.setVideoCenterCrop(it)
            videoView.start()
        }
    }
    private fun VideoView.setVideoCenterCrop(mp: MediaPlayer) {
        val videoWidth: Int = mp.videoWidth
        val videoHeight: Int = mp.videoHeight

        val screenWidth: Int = (parent as View).measuredWidth
        val screenHeight: Int = (parent as View).measuredHeight

        // 计算VideoView的缩放比例
        val scaleX = screenWidth.toFloat() / videoWidth;
        val scaleY = screenHeight.toFloat() / videoHeight;
        val scale = Math.max(scaleX, scaleY);

        // 设置VideoView的新宽高
        val layoutParams = layoutParams;
        layoutParams.width = (videoWidth * scale).toInt();
        layoutParams.height = (videoHeight * scale).toInt()
        this.layoutParams = layoutParams
    }

    private fun initWebView(webView: WebView) {
        val settings = webView.settings
        settings.domStorageEnabled = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        settings.allowFileAccess = false
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        settings.javaScriptEnabled = true
        settings.loadWithOverviewMode = true
        settings.setSupportMultipleWindows(false)
        settings.useWideViewPort = true
        webView.isHorizontalScrollBarEnabled = false
        webView.isVerticalScrollBarEnabled = false
        settings.saveFormData = false
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.databaseEnabled = true
        settings.setSupportZoom(true)
        webView.visibility = View.VISIBLE
        webView.webViewClient = MyWebViewClient(this)
    }
    internal class MyWebViewClient(private val webViewPlayActivity: WebViewPlayActivity) : WebViewClient() {
        override fun onLoadResource(view: WebView, url: String) {
            super.onLoadResource(view, url)
            if (url.endsWith("_1_td.m3u8") || url.endsWith("_1td.m3u8") || url.endsWith("_1/index.m3u8?BR=td")) {
                if (webViewPlayActivity.oa != null) {
                    hideLoading(webViewPlayActivity.findViewById(R.id.ivLoading), webViewPlayActivity.oa)
                }
                view.visibility = View.GONE
                view.destroy()
                webViewPlayActivity.startPlayByVideoView(url)
            }
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            return super.shouldOverrideUrlLoading(view, request)
        }

        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            if (url.endsWith(".m3u8")) {
                println()
            }
            return super.shouldOverrideUrlLoading(view, url)
        }

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            return super.onRenderProcessGone(view, detail)
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
        }
    }

}