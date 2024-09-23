package com.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.TextView
import com.Config.withH265
import com.encode.ScreenEncoder
import com.translate.postscreen.R
import com.activity.TouPingPostActivity
import com.activity.TouPingReceiveActivity
import com.base.base.BaseApplication
import com.encode.RecordEncoder


class ScreenService : Service() {

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val resultCode = intent.getIntExtra("code", -1)
        val resultData = intent.getParcelableExtra<Intent>("data") ?: return super.onStartCommand(
            intent,
            flags,
            startId
        )
        startProject(resultCode, resultData)
        return super.onStartCommand(intent, flags, startId)
    }

    // 录屏开始后进行编码推流
    private fun startProject(resultCode: Int, data: Intent) {
        Log.e("我是一条鱼：", "录屏服务开启" )
        val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionManager.getMediaProjection(resultCode, data
        )?.apply {
            ScreenEncoder.start(this, withH265)
        }
    }

    private fun createNotificationChannel() {
        val builder = Notification.Builder(this.applicationContext)
        val nfIntent = Intent(this, TouPingPostActivity::class.java)
        builder.setContentIntent(
            PendingIntent.getActivity(
                this, 0, nfIntent, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )
        ).setLargeIcon(
            BitmapFactory.decodeResource(
                this.resources,
                R.mipmap.ic_launcher
            )
        ) // 设置下拉列表中的图标(大图标)
            .setSmallIcon(R.mipmap.ic_launcher) // 设置状态栏内的小图标
            .setContentText("is screening......") // 设置上下文内容
            .setWhen(System.currentTimeMillis()) // 设置该通知发生的时间
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("notification_id")
            // 前台服务notification适配
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                "notification_id",
                "notification_name",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
        val notification = builder.build()
        startForeground(110, notification)
    }

    override fun onDestroy() {
        ScreenEncoder.close()
        super.onDestroy()
    }
}