package com.zhangteng.projectionscreensender

import android.annotation.TargetApi
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
import com.base.base.BaseApplication
import com.zhangteng.projectionscreensender.controller.ScreenVideoVideoController
import com.zhangteng.projectionscreensender.controller.StreamController
import com.zhangteng.projectionscreensender.record.ScreenRecordActivity
import java.lang.ref.WeakReference


class ScreenService : Service() {
    lateinit var mStreamController: StreamController
    private lateinit var mMediaProjectionManage: MediaProjectionManager
    companion object {
        @JvmStatic
        var self:WeakReference<ScreenService>? = null
    }
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        self = WeakReference(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val resultCode = intent.getIntExtra("code", -1)
        val resultData = intent.getParcelableExtra<Intent>("data")?:return super.onStartCommand(intent, flags, startId)
        val videoController = ScreenVideoVideoController(mMediaProjectionManage, resultCode, resultData)
        mStreamController = StreamController(videoController)
        ProjectionScreenActivity.getSelf?.get()?.requestRecordSuccess()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotificationChannel() {
        val builder = Notification.Builder(this.applicationContext)
        val nfIntent = Intent(this, ProjectionScreenActivity::class.java)
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
        super.onDestroy()
        self = null
    }
}