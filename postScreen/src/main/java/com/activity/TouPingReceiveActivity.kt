package com.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Point
import android.media.MediaPlayer
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.view.TextureView
import android.view.WindowManager
import android.widget.ScrollView
import android.widget.TextView
import com.Ext.bindMediaToTexture
import com.Ext.prepareMediaPlayer
import com.base.base.BaseActivity
import com.base.base.BaseApplication
import com.decode.ScreenDecoder
import com.screen.receive.WebSocketReceiver
import com.translate.postscreen.R
import java.io.File
import java.util.Collections


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

    private var playingIndex = 0;
    private var currentMediaPlayer: MediaPlayer = MediaPlayer()
    private var nextMediaPlayer: MediaPlayer = MediaPlayer()

    override fun initView() {
        File(BaseApplication.getInstance().filesDir, "receive").listFiles()?.apply {
            for (file in this) {
                file.delete()
            }
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val currentTextureView = findViewById<TextureView>(R.id.currentTextureView)
        val nextTextureView = findViewById<TextureView>(R.id.nextTextureView)
        currentMediaPlayer.bindMediaToTexture(currentTextureView)
        nextMediaPlayer.bindMediaToTexture(nextTextureView)
        currentMediaPlayer.setOnInfoListener { _, _, _ ->
            val path = getNextPath()
            if (TextUtils.isEmpty(path)) {
                appendLog("next 无法准备：${"path = empty"}")
                return@setOnInfoListener false
            }
            nextMediaPlayer.prepareMediaPlayer(path)
            nextMediaPlayer.start()
            BaseApplication.getInstance().handler.postDelayed({
                nextMediaPlayer.seekTo(0)
                nextMediaPlayer.pause()
                appendLog("next 准备完毕：${path}")
            }, 16)
            return@setOnInfoListener false
        }
        nextMediaPlayer.setOnInfoListener { _, _, _ ->
            val path = getNextPath()
            if (TextUtils.isEmpty(path)) {
                appendLog("current 无法准备：${"path = empty"}")
                return@setOnInfoListener false
            }
            currentMediaPlayer.prepareMediaPlayer(path)
            currentMediaPlayer.start()
            BaseApplication.getInstance().handler.postDelayed({
                currentMediaPlayer.seekTo(0)
                currentMediaPlayer.pause()
                appendLog("current 准备完毕：${path}")
            }, 16)
            return@setOnInfoListener false
        }
        currentMediaPlayer.setOnCompletionListener {
            val currentPosition = it.currentPosition
            if (currentPosition > 0) {
                switchTextureView(currentTextureView, nextTextureView, 1)
                nextMediaPlayer.start()
                appendLog("current 播放完毕，开始播放 next：${""}")
                deletePlayedFile()
            }
        }
        nextMediaPlayer.setOnCompletionListener {
            val currentPosition = it.currentPosition
            if (currentPosition > 0) {
                switchTextureView(currentTextureView, nextTextureView, 0)
                currentMediaPlayer.start()
                appendLog("next 播放完毕，开始播放 current：${""}")
                deletePlayedFile()
            }
        }
        intent.getStringExtra("ip")?.also { ip ->
            (BaseApplication.getInstance().topActivity as? TouPingReceiveActivity)?.findViewById<TextView>(R.id.info)?.append("开始连接到服务端:${ip}\n")
            WebSocketReceiver.onReceiveFileComplete = {
                appendLog("接收到文件：${it.path}")
                val size = it.parentFile?.listFiles()?.size?:0
                if (size > 3 && playingIndex == 0) {
                    it.parentFile?.listFiles()?.apply {
                        val files = arrayListOf<File>(*this)
                        files.sortWith { p0, p1 ->
                            val p0Time = p0?.lastModified() ?: 0
                            val p1Time = p1?.lastModified() ?: 0
                            (p0Time - p1Time).toInt()
                        }
                        BaseApplication.getInstance().handler.post {
                            currentMediaPlayer.prepareMediaPlayer(files[0].path)
                            currentMediaPlayer.start()
                            appendLog("current 开始播放文件：${files[0].path}")
                            playingIndex = 1
                        }
                    }
                }
            }
            WebSocketReceiver.init(ip)
            appendLog("${ip}已初始化，请等待")
        }
    }

    private fun deletePlayedFile() {
        File(BaseApplication.getInstance().filesDir, "receive").listFiles()?.apply {
            val files = arrayListOf<File>(*this)
            files.sortWith { p0, p1 ->
                val p0Time = p0?.lastModified() ?: 0
                val p1Time = p1?.lastModified() ?: 0
                (p0Time - p1Time).toInt()
            }
            if (files.size > 0) {
                files[0].delete()
                appendLog("删除播放过的文件：${files[0].path}")
            }
        }
    }

    private fun getNextPath(): String {
        File(BaseApplication.getInstance().filesDir, "receive").listFiles()?.apply {
            val files = arrayListOf<File>(*this)
            if (files.size <= 0) {
                appendLog("当前共有文件：${"empty"}")
                return ""
            }
            files.sortWith { p0, p1 ->
                val p0Time = p0?.lastModified() ?: 0
                val p1Time = p1?.lastModified() ?: 0
                (p0Time - p1Time).toInt()
            }
            val sb = StringBuilder()
            for (file in files) {
                sb.append(file.name).append(",")
            }
            appendLog("当前共有文件：${sb.substring(0, sb.length - 1)}")
            return files[0].path
        }
        return ""
    }

    private fun switchTextureView(
        currentTextureView: TextureView,
        nextTextureView: TextureView,
        index: Int
    ) {
        if (index == 0) {
            resetSize(currentTextureView)
            hideView(nextTextureView)
        } else {
            resetSize(nextTextureView)
            hideView(currentTextureView)
        }
    }

    private fun hideView(videoView: TextureView) {
        val params = videoView.layoutParams
        params.height = 1
        params.width = 1
        videoView.layoutParams = params
    }

    private fun resetSize(videoView: TextureView) {
        val currentRatio = getScreenWidth().toFloat() / getScreenHeight()
        val videoRatio = 1920f / 1080
        val params = videoView.layoutParams
        if (currentRatio > videoRatio) {
            params.height = getScreenHeight()
            params.width = (getScreenHeight() * videoRatio).toInt()
        } else {
            params.width = getScreenWidth()
            params.height = (getScreenWidth() / videoRatio).toInt()
        }
        videoView.layoutParams = params
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
//        ScreenDecoder.release()
//        RecordDecoder.release()
        WebSocketReceiver.release()
        super.onDestroy()
    }


    /**
     * Return the width of screen, in pixel.
     *
     * @return the width of screen, in pixel
     */
    private fun getScreenWidth(): Int {
        val wm = BaseApplication.getInstance().getSystemService(WINDOW_SERVICE) as WindowManager
            ?: return BaseApplication.getInstance().resources.displayMetrics.widthPixels
        val point = Point()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            wm.defaultDisplay.getRealSize(point)
        } else {
            wm.defaultDisplay.getSize(point)
        }
        return point.x
    }

    /**
     * Return the height of screen, in pixel.
     *
     * @return the height of screen, in pixel
     */
    private fun getScreenHeight(): Int {
        val wm = BaseApplication.getInstance().getSystemService(WINDOW_SERVICE) as WindowManager
            ?: return BaseApplication.getInstance().resources.displayMetrics.heightPixels
        val point = Point()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            wm.defaultDisplay.getRealSize(point)
        } else {
            wm.defaultDisplay.getSize(point)
        }
        return point.y
    }

    private fun appendLog(text: String) {
        Log.i("zune appendLog: ", text)
        BaseApplication.getInstance().handler.post {
            findViewById<TextView>(R.id.info).apply {
                append("$text${"\n"}")
                (parent as ScrollView).fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }
}