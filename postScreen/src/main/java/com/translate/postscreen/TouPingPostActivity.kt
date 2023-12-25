package com.translate.postscreen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.base.base.BaseActivity
import com.translate.postscreen.service.ScreenService


/**
 * 发送投屏端
 */
class TouPingPostActivity : BaseActivity(){
    private lateinit var mediaProjectionManager: MediaProjectionManager
    companion object {
        fun start(context: Context) {
            val intent = Intent(context, TouPingPostActivity::class.java)
            context.startActivity(intent)
        }
    }
    override fun getLayoutId(): Int {
        return R.layout.activity_touping_post
    }

    override fun initView() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        findViewById<View>(R.id.start).setOnClickListener {
            startProjection()
        }
    }

    // 请求开始录屏
    private fun startProjection() {
        if (Build.VERSION.SDK_INT >= 23) {
            val permission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.RECORD_AUDIO)
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this@TouPingPostActivity, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
                return
            }
        }
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), 1001)
        mediaProjectionManager.createScreenCaptureIntent()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) {
            return
        }
        if (requestCode == 1001) {
            val service = Intent(this, ScreenService::class.java)
            service.putExtra("code", resultCode)
            service.putExtra("data", data)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(service)
            } else {
                startService(service)
            }
        }
    }
}