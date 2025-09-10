package com.example.batterymonitor

import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationCompat
import android.os.IBinder
import android.os.Build
import android.util.Log
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.Executors


class TCPConnectionService : Service() {

    companion object {
        const val CHANNEL_ID = "ForegroundServiceChannel"
    }

    private val fcmReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            val executor: Executor = Executors.newSingleThreadExecutor()
            Log.d("FCMService", "foreground Got message")
            if (action == "com.example.batterymonitor.ACTION_FROM_FCM") {
                val ip = intent.getStringExtra("ip")
                val batteryPct = intent.getIntExtra("batteryPct", 0)
                val token = intent.getStringExtra("fcm_token")
                try {
                    executor.execute {
                        tcp_client.sendMessage( ip, batteryPct, token)
                    }

                } catch (e: IOException) {
                    e.printStackTrace()
                    Log.e("FileRead", "讀取檔案時出錯：${e.message}")
                }

            }
        }
    }

    private lateinit var tcp_client: TCPClient

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val filter = IntentFilter("com.example.batterymonitor.ACTION_FROM_FCM")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // API 26 以上呼叫可帶 flag 的方法
            registerReceiver(fcmReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            // 26 以下只能呼叫舊版方法，不帶 flag
            registerReceiver(fcmReceiver, filter)
        }

        tcp_client = TCPClient()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val builder: NotificationCompat.Builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TCP still working")
            .setContentText("TCP will send message to server")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val soundUri = Uri.parse("android.resource://${packageName}/raw/h2r")
            builder.setSound(soundUri)
        }

        val notification: Notification = builder.build()

        startForeground(1, notification)
        // Log.d("MyService", "已呼叫 startForeground")
        // 如果任務完成，呼叫 stopSelf()
        // stopSelf()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // 不需要綁定
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val soundUri = Uri.parse("android.resource://${packageName}/raw/h2r")

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "前景服務頻道",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setSound(soundUri, audioAttributes)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }

        // 25 below API don't need NotificationChannel()

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(fcmReceiver)
    }
}