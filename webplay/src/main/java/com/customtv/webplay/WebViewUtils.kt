package com.customtv.webplay

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.util.DisplayMetrics
import android.view.View
import android.view.animation.LinearInterpolator
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity

object WebViewUtils {

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun WebView.startPlay(url: String, loadCompleteCallBack: () -> Unit) {
//        if (ContextCompat.checkSelfPermission(context as FragmentActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(context as FragmentActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), 1)
//        }
//        QbSdk.forceSysWebView()
//        val map = HashMap<String, Any>(2)
//        map[TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER] = true
//        map[TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE] = true
//        QbSdk.initTbsSettings(map)

        // 配置 WebView 设置
        val webSettings: WebSettings = getSettings()
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.loadsImagesAutomatically = false // 禁用自动加载图片
        webSettings.blockNetworkImage = true // 禁用网络图片加载
        webSettings.mediaPlaybackRequiresUserGesture = false
        webSettings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"
        // 启用缓存
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        // 启用 JavaScript 自动点击功能
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // X5内核代码
            webSettings.mixedContentMode = WebSettings.LOAD_NORMAL
        }
        // 设置 WebViewClient 和 WebChromeClient
        webViewClient = object : WebViewClient() {
            // X5内核代码
            override fun onReceivedSslError(
                webView: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                handler.proceed() // 忽略 SSL 错误
            }

            // 系统Webview内核代码
            // @Override
            // public void onReceivedSslError(WebView view, SslErrorHandler handler,
            // SslError error) {
            // handler.proceed(); // 忽略 SSL 错误
            // }
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                // 页面加载时执行 JavaScript 脚本
                view.evaluateJavascript(
                    """
                        function FastLoading() {
                             const fullscreenBtn = document.querySelector('#player_pagefullscreen_yes_player') || document.querySelector('.videoFull');
                             if (fullscreenBtn) return;

                             // 清空所有图片的 src 属性，阻止图片加载
                             Array.from(document.getElementsByTagName('img')).forEach(img => {
                                 img.src = '';
                             });

                             // 清空特定的脚本 src 属性
                             const scriptKeywords = ['login', 'index', 'daohang', 'grey', 'jquery'];
                             Array.from(document.getElementsByTagName('script')).forEach(script => {
                                 if (scriptKeywords.some(keyword => script.src.includes(keyword))) {
                                     script.src = '';
                                 }
                             });

                             // 清空具有特定 class 的 div 内容
                             const classNames = ['newmap', 'newtopbz', 'newtopbzTV', 'column_wrapper'];
                             classNames.forEach(className => {
                                 Array.from(document.getElementsByClassName(className)).forEach(div => {
                                     div.innerHTML = '';
                                 });
                             });

                             // 递归调用 FastLoading，每 4ms 触发一次
                             setTimeout(FastLoading, 4);
                         }

                     FastLoading();
                """.trimIndent()
                ) { value: String? -> }
                super.onPageStarted(view, url, favicon)
            }

            // 设置 WebViewClient，监听页面加载完成事件
            override fun onPageFinished(view: WebView, url: String) {
                if (url == "about:blank") {
                    return
                }
                view.evaluateJavascript(
                    """
                         function AutoFullscreen(){
                             var fullscreenBtn = document.querySelector('#player_pagefullscreen_yes_player')||document.querySelector('.videoFull');
                             if(fullscreenBtn!=null){
                                //alert(fullscreenBtn)
                              fullscreenBtn.click();
                              document.querySelector('video').volume=1;
                             }else{
                                 setTimeout(
                                    ()=>{ AutoFullscreen();}
                                ,16);
                             }
                         }
                    AutoFullscreen()
                    """.trimIndent()
                ) { value: String? ->
                    loadCompleteCallBack.invoke()
                }
            }
        }
        // 禁用缩放
        webSettings.setSupportZoom(false)
        webSettings.builtInZoomControls = false
        webSettings.displayZoomControls = false
        // 在 Android TV 上，需要禁用焦点自动导航
        isFocusable = false
        // 开启无图（X5内核）
//        if (QbSdk.canLoadX5(context)) {
//            settingsExtension.setPicModel(IX5WebSettingsExtension.PicModel_NoPic)
//        }
        // 设置 WebView 客户端
        webChromeClient = WebChromeClient()
        loadUrl(url)
        setInitialScale(getMinimumScale())
    }

    private fun WebView.getMinimumScale(): Int {
        val displayMetrics = DisplayMetrics()
        (context as FragmentActivity).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // 计算缩放比例，使用 double 类型进行计算
        val scale = Math.min(screenWidth.toDouble() / 1920.0, screenHeight.toDouble() / 1080.0) * 100
        // 四舍五入并转为整数
        return Math.round(scale).toInt()
    }

    @JvmStatic
    fun showLoading(ivLoading: View): ObjectAnimator {
        val oa = ObjectAnimator.ofFloat(ivLoading, "rotation", 0f, 360f)
        com.base.base.BaseApplication.getInstance().handler.post {
            ivLoading.visibility = View.VISIBLE
            oa.duration = 1000
            oa.repeatCount = ValueAnimator.INFINITE
            oa.interpolator = LinearInterpolator()
            oa.start()
        }
        return oa
    }

    @JvmStatic
    fun hideLoading(ivLoading: View, oa: ObjectAnimator?) {
        com.base.base.BaseApplication.getInstance().handler.post {
            ivLoading.visibility = View.GONE
            oa?.cancel()
        }
    }
}