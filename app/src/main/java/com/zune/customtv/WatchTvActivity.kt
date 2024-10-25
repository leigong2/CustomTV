package com.zune.customtv

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.view.KeyEvent
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.base.base.BaseActivity
import com.base.base.BaseApplication
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.zune.customtv.bean.TvBean
import com.zune.customtv.bean.UrlBean
import com.zune.customtv.fragment.WatchTvFragment
import com.zune.customtv.utils.SurfaceVideoView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException


class WatchTvActivity : BaseActivity() {

    companion object {
        fun start(context: Context, position: Int) {
            val intent = Intent(context, WatchTvActivity::class.java)
            intent.putExtra("position", position)
            context.startActivity(intent)
        }
    }

    private lateinit var tvChangeVideo: TextView
    private var data : ArrayList<TvBean> = arrayListOf()
    private var position = 0
    private var mData: ArrayList<UrlBean> = arrayListOf()
    private var mCurrentPosition = 0
    private var mediaPlayer: MediaPlayer? = null
    private var name: String? = null
    private var timeoutDuration = 10000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutId)
        initView()
        BaseApplication.getInstance().handler.postDelayed({
//            playNext()
        }, 5000)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                Toast.makeText(this, "播放下一个", Toast.LENGTH_SHORT).show()
                playNext()
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                Toast.makeText(this, "播放上一个", Toast.LENGTH_SHORT).show()
                playLast()
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun playLast() {
        if (position <= 0) {
            return
        }
        mData.clear()
        val p = data[--position]
        p.urls.sortBy {
            it.timeout
        }
        p.urls.let { mData.addAll(it) }
        name = p.name
        mCurrentPosition = 0
        mediaPlayer?.stop()
        mediaPlayer?.reset()
        try {
            timeoutDuration = 3000L
            mediaPlayer?.setDataSource(getCurrentUrl())
            mediaPlayer?.prepareAsync()
            loading()
        } catch (ignore: Exception) {
            changeNextSource()
        }
    }

    private fun playNext() {
        if (position >= data.size - 1) {
            return
        }
        mData.clear()
        val p = data[++position]
        p.urls.sortBy {
            it.timeout
        }
        p.urls.let { mData.addAll(it) }
        name = p.name
        mCurrentPosition = 0
        mediaPlayer?.stop()
        mediaPlayer?.reset()
        try {
            timeoutDuration = 3000L
            mediaPlayer?.setDataSource(getCurrentUrl())
            mediaPlayer?.prepareAsync()
            loading()
        } catch (ignore: Exception) {
            changeNextSource()
        }
    }

    override fun initView() {
        WatchTvFragment.mData.let { data.addAll(it) }
        position = intent.getIntExtra("position", 0)
        val p = data[position]
        p.urls.sortBy {
            it.timeout
        }
        p.urls.let { mData.addAll(it) }
        name = p.name
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = 0
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        tvChangeVideo = findViewById(R.id.tv_change_video)
        tvChangeVideo.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            v.setBackgroundResource(if (hasFocus) R.drawable.bg_select else R.drawable.bg_normal)
            (v as TextView).setTextColor(ContextCompat.getColor(this@WatchTvActivity, if (hasFocus) R.color.white else R.color.half_white))
        }
        tvChangeVideo.setOnClickListener { changeNextSource() }
        val surface: SurfaceVideoView = findViewById(R.id.player_view)
        surface.addCallBack()
        surface.callBack = { surfaceTexture ->
            mediaPlayer = MediaPlayer().apply {
                try {
                    setAudioStreamType(AudioManager.STREAM_MUSIC)
                    setSurface(Surface(surfaceTexture))
                    setDataSource(getCurrentUrl())
                    setOnPreparedListener { //这里可以调用start()方法开始播放视频
                        it.start()
                        loadComplete()
                    }
                    setOnErrorListener { mp, what, extra ->
                        println("我是一条鱼：setOnErrorListener ${what}")
                        changeNextSource()
                        false
                    }
                    prepareAsync()
                } catch (ignore: IOException) {
                }
                loading()
            }
        }
    }

    private var anrCount = 0

    @Deprecated(message = "找不到好的方法判断是否卡顿")
    private fun startLooper() {
        try {
            if (mediaPlayer?.isPlaying != true) {
                if (++anrCount >= 3) {
                    changeNextSource()
                    return
                }
            }
            lifecycleScope.launch {
                delay(1000)
                startLooper()
            }
        } catch (ignore: Exception) {
        }
    }

    private fun loading() {
        if (mediaPlayer?.isPlaying != true) {
            BaseApplication.getInstance().handler.postDelayed(r, timeoutDuration)
        }
    }

    private fun loadComplete() {
        BaseApplication.getInstance().handler.removeCallbacks(r)
        lifecycleScope.launch {
            tvChangeVideo.text = name?:""
        }
    }

    private val r = Runnable {
        if (isDestroyed || isFinishing || mediaPlayer?.isPlaying == true) {
            return@Runnable
        }
        changeNextSource()
    }

    private fun getCurrentUrl(): String {
        if (mData.size <= mCurrentPosition) {
            timeoutDuration += 3000L
            mCurrentPosition = 0
        }
        val data = mData[mCurrentPosition]
        println("我是一条鱼: 正在播放当前url = ${data.url}")
        return data.url
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_watch_play_tv
    }

    override fun onContentChanged() {
        super.onContentChanged()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    override fun finish() {
        super.finish()
        mediaPlayer?.stop()
        mediaPlayer?.release()
    }

    private fun changeNextSource() {
        loadComplete()
        anrCount = 0
        removeUrl(getCurrentUrl())
        ++mCurrentPosition
        val currentUrl = getCurrentUrl()
        tvChangeVideo.text = String.format("视频卡顿切换视频源\n%s", currentUrl)
        mediaPlayer?.stop()
        mediaPlayer?.reset()
        try {
            mediaPlayer?.setDataSource(currentUrl)
            mediaPlayer?.prepareAsync()
            loading()
        } catch (ignore: Exception) {
            changeNextSource()
        }
    }

    private fun removeUrl(currentUrl: String) {
        MainScope().launch {
            withContext(Dispatchers.IO) {
                val file = File(BaseApplication.getInstance().getExternalFilesDir(""), "tvLiveM3u8.json");
                val fr = FileReader(file)
                val list = GsonBuilder().create().fromJson<MutableMap<String, MutableList<MutableMap<String, Any>>>>(
                    fr.readText(),
                    object : TypeToken<MutableMap<String, MutableList<MutableMap<String, String>>>>() {}.type
                )
                fr.close()
                for (key in list.keys) {
                    val value = list[key] ?: arrayListOf()
                    for (mutableMap in value) {
                        val url = mutableMap["url"] ?: ""
                        if (url == currentUrl) {
                            mutableMap["timeout"] = 3000
                            break
                        }
                    }
                }
                for (datum in WatchTvFragment.mData) {
                    for (url in datum.urls) {
                        if (url.url == currentUrl) {
                            url.timeout = 3000
                            break
                        }
                    }
                }
                val json = GsonBuilder().create().toJson(list)
                val fw = FileWriter(file)
                fw.write(json)
                fw.close()
            }
        }
    }
}