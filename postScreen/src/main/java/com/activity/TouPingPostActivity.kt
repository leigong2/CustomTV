package com.activity

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.view.View
import android.view.WindowManager
import com.base.base.BaseActivity
import com.base.base.BaseApplication
import com.decode.AACPlayer
import com.encode.RecordEncoder
import com.encode.ScreenEncoder
import com.service.ScreenService
import com.translate.postscreen.R
import java.io.File


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
        File(BaseApplication.getInstance().getExternalFilesDir(""), "record").listFiles()?.forEach {
            it.delete()
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        findViewById<View>(R.id.start).setOnClickListener {
            startProjection()
        }
    }

    // 请求开始录屏
    private fun startProjection() {
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

    override fun finish() {
        super.finish()
        ScreenEncoder.close()
        RecordEncoder.close()
    }
}