package com.activity

import android.content.Context
import android.content.Intent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import com.base.base.BaseActivity
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
                        ScreenDecoder.start(ip, holder.surface)
                    }

                    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
                    }

                    override fun surfaceDestroyed(p0: SurfaceHolder) {
                    }
                })
            }
        }
    }

    override fun onDestroy() {
        ScreenDecoder.release()
        super.onDestroy()
    }
}