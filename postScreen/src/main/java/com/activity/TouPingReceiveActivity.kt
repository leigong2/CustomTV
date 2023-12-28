package com.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.TextView
import com.base.base.BaseActivity
import com.base.base.BaseApplication
import com.decode.RecordDecoder
import com.decode.ScreenDecoder
import com.translate.postscreen.R


class TouPingReceiveActivity : BaseActivity() {

    companion object {
        fun start(ip: String, context: Context) {
            val intent = Intent(context, TouPingReceiveActivity::class.java)
            intent.putExtra("ip", ip)
            context.startActivity(intent)
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_touping_receive
    }

    override fun initView() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        intent.getStringExtra("ip")?.also { ip ->
            findViewById<SurfaceView>(R.id.surfaceView).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        // 连接到服务端
                        (BaseApplication.getInstance().topActivity as? TouPingReceiveActivity)?.findViewById<TextView>(R.id.info)?.append("开始连接到服务端:${ip}\n")
                        ScreenDecoder.start(ip, holder.surface, false)
                    }

                    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
                    }

                    override fun surfaceDestroyed(p0: SurfaceHolder) {
                    }
                })
            }
//            RecordDecoder.start(ip)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 横屏
            ScreenDecoder.VIDEO_WIDTH = 2400
            ScreenDecoder.VIDEO_HEIGHT = 1080
        } else {
            // 竖屏
            ScreenDecoder.VIDEO_WIDTH = 1080
            ScreenDecoder.VIDEO_HEIGHT = 2400
        }
        ScreenDecoder.sendOrientation(newConfig.orientation)
        initView()
    }

    override fun onDestroy() {
        ScreenDecoder.release()
//        RecordDecoder.release()
        super.onDestroy()
    }
}