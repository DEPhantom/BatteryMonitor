package com.example.batterymonitor
import android.app.ActivityOptions
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.BatteryState
import android.media.AudioAttributes
import android.Manifest
import android.app.AlarmManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

data class BatteryInfo( val batteryPct: Int, val batteryState: String )

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "fcm_default_channel"
        private const val NOTIFICATION_ID = 1
    }

    private fun getNewBattery() : BatteryInfo {
        val context: Context = this
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        // val batteryStatus = BMApp.instance.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        batteryStatus?.run {
            val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = getIntExtra(BatteryManager.EXTRA_STATUS, -1)

            val batteryPct = level * 100f / scale
            val statusText = when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING ->  " (充電中)"
                BatteryManager.BATTERY_STATUS_FULL -> " (充飽)"
                else -> ""
            }

            return BatteryInfo( batteryPct.toInt(), statusText)

        }

        return BatteryInfo( 0, "Unknow" )

    } // end getNewBattery()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "接收到訊息來自: ${remoteMessage.from}")

        if (remoteMessage.data.isNotEmpty()) {
            if ( isDeviceLocked(this) ) { // call yuigahama intent
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && hasFullScreenIntentPermission() ) { // android 14+
                    showFullScreenNotification()

                } // end if
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ) { // android 10+
                    showFullScreenNotification()

                } // end else if()
                else {
                    val intent = Intent(this, TCPNotificationActivity::class.java ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }

                    this.startActivity(intent)

                } // end else


            } // end if()

            handleDataMessage(remoteMessage.data)
        } // end if()

    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "新的 FCM Token: $token")
        val prefs = getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()

    }

    private fun handleDataMessage(data: Map<String, String>) {
        val context: Context = this
        // get ip
        val ip = data["ip"]
        // get battery
        val result = getNewBattery()
        // read current token
        val prefs = context.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("fcm_token", null)

        // send ip and battery
        val intent = Intent().apply {
            action = "com.example.batterymonitor.ACTION_FROM_FCM"
            putExtra("ip", ip)
            putExtra("batteryPct", result.batteryPct)
            putExtra("fcm_token", token)
            setPackage(packageName)  // 讓它變成 explicit broadcast
        }

        context.sendBroadcast(intent)

    } // end handleDataMessage()

    private fun isDeviceLocked(context: Context): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        return keyguardManager.isKeyguardLocked

    } // end isDeviceLocked()

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun hasFullScreenIntentPermission(): Boolean {
        val notificationManager = getSystemService(NotificationManager::class.java)
        return notificationManager.canUseFullScreenIntent()
    }

    private fun showFullScreenNotification() {

        try {

            val fullScreenIntent = Intent(this, TCPNotificationActivity::class.java)
            val fullScreenPendingIntent = PendingIntent.getActivity(this, 0,
                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE )

            val builder: NotificationCompat.Builder = NotificationCompat.Builder(this,
                "YuigahamaYuiChannel"
            )
                .setSmallIcon(R.drawable.crest_notification)
                .setContentTitle("Ya hello")
                .setContentText("Yuigahama Yui Working")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(fullScreenPendingIntent, true)


            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                val soundUri = Uri.parse("android.resource://${packageName}/raw/h2r")
                builder.setSound(soundUri)
            }

            val notification: Notification = builder.build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ 需要檢查通知權限
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    NotificationManagerCompat.from(this).notify(1728, notification)
                }
            } else {
                // Android 12 及以下直接發通知，不用檢查權限
                NotificationManagerCompat.from(this).notify(1000, notification)
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "Fullscreen notification error")
        }

    } // showFullScreenNotification()

    private fun sendTokenToServer(token: String) {
        // 實作將 token 傳送到你的伺服器的邏輯
        // 例如使用 Retrofit 或其他網路庫
        Log.d(TAG, "應將 token 傳送到伺服器: $token")
    }
}
