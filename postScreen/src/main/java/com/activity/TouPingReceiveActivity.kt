package com.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import android.widget.TextView
import android.widget.VideoView
import com.base.base.BaseActivity
import com.base.base.BaseApplication
import com.decode.ScreenDecoder
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import com.screen.receive.WebSocketReceiver
import com.translate.postscreen.R
import java.io.File


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
    private lateinit var firstPlayerView:VideoView
    private lateinit var secondPlayerView:VideoView

    override fun initView() {
        File(BaseApplication.getInstance().filesDir, "receive").listFiles()?.apply {
            for (file in this) {
                file.delete()
            }
        }
        firstPlayerView = findViewById(R.id.firstPlayerView)
//        secondPlayerView = findViewById(R.id.secondPlayerView)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        intent.getStringExtra("ip")?.also { ip ->
            resetSize(firstPlayerView)
//            resetSize(secondPlayerView)
            firstPlayerView.setOnCompletionListener { playSingleNext() }
//            secondPlayerView.setOnCompletionListener { playNext() }
            (BaseApplication.getInstance().topActivity as? TouPingReceiveActivity)?.findViewById<TextView>(R.id.info)?.append("开始连接到服务端:${ip}\n")
            WebSocketReceiver.onReceiveFileComplete = {
                val size = it.parentFile?.listFiles()?.size?:0
                if (size >= 3 && playingIndex == 0) {
                    it.parentFile?.listFiles()?.apply {
                        val files = arrayListOf<File>(*this)
                        files.sortBy { it.lastModified() }
                        BaseApplication.getInstance().handler.post {
                            firstPlayerView.setVideoPath(files[0].path)
//                            secondPlayerView.setVideoPath(files[1].path)
                            firstPlayerView.seekTo(0)
                            firstPlayerView.start()
                            playingIndex = 1
                        }
                    }
                }
            }
            WebSocketReceiver.init(ip)
        }
    }

    private fun playSingleNext() {
        File(BaseApplication.getInstance().filesDir, "receive").listFiles()?.apply {
            val files = arrayListOf<File>(*this)
            files.sortBy { it.lastModified() }
            firstPlayerView.setVideoPath(files[1].path)
            firstPlayerView.seekTo(0)
            firstPlayerView.start()
            files[0].delete()
        }
    }

    private fun resetSize(videoView: VideoView) {
        val currentRatio = getScreenWidth().toFloat() / getScreenHeight()
        val videoRatio = 1080f / 1920
        val params = videoView.layoutParams
        if (currentRatio < videoRatio) {
            params.height = getScreenHeight()
            params.width = (getScreenHeight() * videoRatio).toInt()
        } else {
            params.width = getScreenWidth()
            params.height = (getScreenWidth() / videoRatio).toInt()
        }
        videoView.layoutParams = params
    }

    private fun playNext() {
        File(BaseApplication.getInstance().filesDir, "receive").listFiles()?.apply {
            val files = arrayListOf<File>(*this)
            files.sortBy { it.lastModified() }
            if (playingIndex == 1) {
                val f = firstPlayerView.layoutParams
                f.height = 1
                f.width = 1
                firstPlayerView.layoutParams = f
                resetSize(secondPlayerView)
                secondPlayerView.seekTo(0)
                secondPlayerView.start()
                playingIndex = 2
                firstPlayerView.setVideoPath(files[2].path)
                Log.i("zunePlayer: ", "firstPlayerView: path = ${files[2].path}");
                firstPlayerView.seekTo(0)
            } else {
                resetSize(firstPlayerView)
                val s = secondPlayerView.layoutParams
                s.height = 1
                s.width = 1
                secondPlayerView.layoutParams = s
                firstPlayerView.seekTo(0)
                firstPlayerView.start()
                playingIndex = 1
                secondPlayerView.setVideoPath(files[2].path)
                Log.i("zunePlayer: ", "secondPlayerView: path = ${files[2].path}");
                secondPlayerView.seekTo(0)
            }
            files[0].delete()
        }
    }

    private fun playFile(
        firstPlayer: SimpleExoPlayer,
        secondPlayer: SimpleExoPlayer,
        position: Int,
        rootView: ViewGroup
    ) {
//        BaseApplication.getInstance().handler.post {
//            if (position == 0) {
//                firstPlayer.seekTo(0)
//                firstPlayer.play()
//                Log.i("zunePlayer: ", "playFile: ${position}, firstPlayer， $preCount");
//            } else {
//                secondPlayer.seekTo(0)
//                secondPlayer.play()
//                Log.i("zunePlayer: ", "playFile: ${position}, secondPlayer， $preCount");
//            }
//            val firstPlayerView = findViewById<PlayerView>(R.id.firstPlayerView)
//            val secondPlayerView = findViewById<PlayerView>(R.id.secondPlayerView)
//            firstPlayerView.hideController()
//            secondPlayerView.hideController()
//            val firstView: View = rootView.getChildAt(0)
//            val secondView: View = rootView.getChildAt(1)
//            if (position == 0) {
//                firstView.alpha = 1f
//                secondView.alpha = 0f
//            } else {
//                firstView.alpha = 1f
//                secondView.alpha = 0f
//            }
//            val delay = if (position == 0) firstPlayer.duration else secondPlayer.duration
//            Log.i("zunePlayer: ", "delay: ${delay}");
//            BaseApplication.getInstance().handler.postDelayed({
//                File(BaseApplication.getInstance().filesDir, "receive").listFiles()?.apply {
//                    val files = arrayListOf<File>(*this)
//                    files.sortBy { it.lastModified() }
//                    if (files.size < preCount) {
//                        return@apply
//                    }
//                    firstPlayerView.hideController()
//                    secondPlayerView.hideController()
//                    if (position == 0) {
//                        val file = files[preCount++]
//                        prepareFile(file, firstPlayer, firstPlayerView)
//                        Log.i("zunePlayer: ", "prepare: ${file.name}, firstPlayer， $preCount");
//                        playFile(firstPlayer, secondPlayer, 1, rootView)
//                    } else {
//                        val file = files[preCount++]
//                        prepareFile(file, secondPlayer, secondPlayerView)
//                        Log.i("zunePlayer: ", "prepare: ${file.name}, secondPlayer， $preCount");
//                        playFile(firstPlayer, secondPlayer, 0, rootView)
//                    }
//                }
//            }, delay)
//        }
    }

    private fun prepareFile(file: File, player: SimpleExoPlayer, playerView: PlayerView) {
        val mediaItem: MediaItem = MediaItem.fromUri(Uri.fromFile(file))
        player.setMediaItem(mediaItem)
        player.prepare()
        playerView.hideController()
    }

    private fun switchPlayerView(rootView: ViewGroup) {
        val firstView: View = rootView.getChildAt(0)
        val firstLayoutParams = firstView.layoutParams
        val secondView: View = rootView.getChildAt(1)
        val secondLayoutParams = secondView.layoutParams
        secondView.layoutParams = firstLayoutParams
        firstView.layoutParams = secondLayoutParams
        if (firstView.id == R.id.firstPlayerView) {
            rootView.removeView(secondView)
            rootView.addView(secondView, 0)
        } else {
            rootView.removeView(firstView)
            rootView.addView(firstView, 1)
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
    fun getScreenWidth(): Int {
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
    fun getScreenHeight(): Int {
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
}