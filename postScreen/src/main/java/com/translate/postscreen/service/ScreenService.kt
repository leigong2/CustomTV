package com.translate.postscreen.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.translate.postscreen.R
import com.translate.postscreen.SocketManager
import com.translate.postscreen.TouPingPostActivity


class ScreenService : Service() {
    private lateinit var mMediaProjectionManager: MediaProjectionManager
    private lateinit var mSocketManager: SocketManager
    private var mSampleRateInHZ = 8000
    private var minBufferSize: Int = 0
    private var audioRecord: AudioRecord? = null

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        mMediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        // 初始化服务器端
        mSocketManager = SocketManager()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val resultCode = intent.getIntExtra("code", -1)
        val resultData = intent.getParcelableExtra<Intent>("data")?:return super.onStartCommand(intent, flags, startId)
        startProject(resultCode, resultData)
        return super.onStartCommand(intent, flags, startId)
    }

    // 录屏开始后进行编码推流
    private fun startProject(resultCode: Int, data: Intent) {
        val mediaProjection = mMediaProjectionManager.getMediaProjection(
            resultCode,
            data
        ) ?: return
        mSocketManager.start(mediaProjection)
    }

    /**
     * 初始化录音器
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun initAudioRecord(resultCode: Int, intent: Intent) {
        minBufferSize = AudioRecord.getMinBufferSize(mSampleRateInHZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val mediaProjectionManager = baseContext.getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        //设置应用程序录制系统音频的能力
        val mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, intent)
        val builder = AudioRecord.Builder()
        builder.setAudioFormat(AudioFormat.Builder()
            .setSampleRate(mSampleRateInHZ) //设置采样率（一般为可选的三个-> 8000Hz 、16000Hz、44100Hz）
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO) //音频通道的配置，可选的有-> AudioFormat.CHANNEL_IN_MONO 单声道，CHANNEL_IN_STEREO为双声道，立体声道，选择单声道就行
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build()) //音频数据的格式，可选的有-> AudioFormat.ENCODING_PCM_8BIT，AudioFormat.ENCODING_PCM_16BIT
            .setBufferSizeInBytes(minBufferSize) //设置最小缓存区域
        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA) //设置捕获多媒体音频
            .addMatchingUsage(AudioAttributes.USAGE_GAME) //设置捕获游戏音频
            .build()
        //将 AudioRecord 设置为录制其他应用播放的音频
        builder.setAudioPlaybackCaptureConfig(config)
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                audioRecord = builder.build()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("录音器错误", "录音器初始化失败")
        }
        //做完准备工作，就可以开始录音了
    }

    private fun createNotificationChannel() {
        val builder = Notification.Builder(this.applicationContext)
        val nfIntent = Intent(this, TouPingPostActivity::class.java)
        builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        })).setLargeIcon(BitmapFactory.decodeResource(this.resources, R.mipmap.ic_launcher)) // 设置下拉列表中的图标(大图标)
            .setSmallIcon(R.mipmap.ic_launcher) // 设置状态栏内的小图标
            .setContentText("is screening......") // 设置上下文内容
            .setWhen(System.currentTimeMillis()) // 设置该通知发生的时间
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("notification_id")
            // 前台服务notification适配
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel("notification_id", "notification_name", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
        val notification = builder.build()
        startForeground(110, notification)
    }

    override fun onDestroy() {
        mSocketManager.close()
        super.onDestroy()
    }
}