package com.zune.customtv.utils

import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * @author wangzhilong
 * @date 2022/7/27 027
 */
object WebUtils {
    fun initWebView(webView: WebView) {
        val settings = webView.settings
        settings.domStorageEnabled = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        settings.allowFileAccess = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.javaScriptEnabled = true
        settings.loadWithOverviewMode = true
        settings.setSupportMultipleWindows(false)
//        settings.setAppCachePath(APP_CACHE_DIRNAME);
        //        settings.setAppCachePath(APP_CACHE_DIRNAME);
        settings.useWideViewPort = true
        webView.isHorizontalScrollBarEnabled = false
        webView.isVerticalScrollBarEnabled = false
        settings.saveFormData = false
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.databaseEnabled = true
        settings.setSupportZoom(true)
    }
}