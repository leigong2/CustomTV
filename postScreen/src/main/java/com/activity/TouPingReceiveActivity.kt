package com.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import com.base.base.BaseActivity
import com.base.base.BaseApplication
import com.decode.ScreenDecoder
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
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

    private var preCount = 0;

    override fun initView() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        intent.getStringExtra("ip")?.also { ip ->
            val firstPlayerView = findViewById<PlayerView>(R.id.firstPlayerView)
            val firstPlayer = SimpleExoPlayer.Builder(BaseApplication.getInstance()).build()
            val secondPlayerView = findViewById<PlayerView>(R.id.secondPlayerView)
            val secondPlayer = SimpleExoPlayer.Builder(BaseApplication.getInstance()).build()
            firstPlayerView.player = firstPlayer
            secondPlayerView.player = secondPlayer
            firstPlayerView.hideController()
            secondPlayerView.hideController()
            firstPlayer.playWhenReady = true
            secondPlayer.playWhenReady = true
            File(BaseApplication.getInstance().filesDir, "receive").listFiles()?.apply {
                val files = arrayListOf<File>(*this)
                files.sortBy { it.lastModified() }
                prepareFile(files[0], firstPlayer, firstPlayerView)
                Log.i("zunePlayer: ", "prepare: ${files[0].name}, firstPlayer， $preCount");
                prepareFile(files[1], secondPlayer, secondPlayerView)
                Log.i("zunePlayer: ", "prepare: ${files[1].name}, secondPlayer， $preCount");
                ++preCount
                ++preCount
                playFile(firstPlayer, secondPlayer, 0, firstPlayerView.parent as ViewGroup)
            }
//            (BaseApplication.getInstance().topActivity as? TouPingReceiveActivity)?.findViewById<TextView>(R.id.info)?.append("开始连接到服务端:${ip}\n")
//            WebSocketReceiver.onReceiveFileComplete = {
//                val size = it.parentFile?.listFiles()?.size?:0
//                if (size >= 3 && playingIndex < 0) {
//                    it.parentFile?.listFiles()?.apply {
//                        val files = arrayListOf<File>(*this)
//                        files.sortBy { it.lastModified() }
//                        prepareFile(files[0], firstPlayer, firstPlayerView)
//                        prepareFile(files[1], secondPlayer, secondPlayerView)
//                        playFile(firstPlayer, secondPlayer, 0, firstPlayerView.parent as ViewGroup)
//                        playingIndex = 0
//                    }
//                }
//            }
//            WebSocketReceiver.init(ip)
        }
    }

    private fun playFile(firstPlayer: SimpleExoPlayer, secondPlayer: SimpleExoPlayer, position: Int, rootView: ViewGroup) {
        BaseApplication.getInstance().handler.post {
            if (position == 0) {
                firstPlayer.seekTo(0)
                firstPlayer.play()
                Log.i("zunePlayer: ", "playFile: ${position}, firstPlayer， $preCount");
            } else {
                secondPlayer.seekTo(0)
                secondPlayer.play()
                Log.i("zunePlayer: ", "playFile: ${position}, secondPlayer， $preCount");
            }
            val firstPlayerView = findViewById<PlayerView>(R.id.firstPlayerView)
            val secondPlayerView = findViewById<PlayerView>(R.id.secondPlayerView)
            firstPlayerView.hideController()
            secondPlayerView.hideController()
            val firstView: View = rootView.getChildAt(0)
            val secondView: View = rootView.getChildAt(1)
            if (position == 0) {
                firstView.alpha = 1f
                secondView.alpha = 0f
            } else {
                firstView.alpha = 1f
                secondView.alpha = 0f
            }
            val delay = if (position == 0) firstPlayer.duration else secondPlayer.duration
            Log.i("zunePlayer: ", "delay: ${delay}");
            BaseApplication.getInstance().handler.postDelayed({
                File(BaseApplication.getInstance().filesDir, "receive").listFiles()?.apply {
                    val files = arrayListOf<File>(*this)
                    files.sortBy { it.lastModified() }
                    if (files.size < preCount) {
                        return@apply
                    }
                    firstPlayerView.hideController()
                    secondPlayerView.hideController()
                    if (position == 0) {
                        val file = files[preCount++]
                        prepareFile(file, firstPlayer, firstPlayerView)
                        Log.i("zunePlayer: ", "prepare: ${file.name}, firstPlayer， $preCount");
                        playFile(firstPlayer, secondPlayer, 1, rootView)
                    } else {
                        val file = files[preCount++]
                        prepareFile(file, secondPlayer, secondPlayerView)
                        Log.i("zunePlayer: ", "prepare: ${file.name}, secondPlayer， $preCount");
                        playFile(firstPlayer, secondPlayer, 0, rootView)
                    }
                }
            }, delay)
        }
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
}