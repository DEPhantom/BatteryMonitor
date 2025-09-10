package com.example.batterymonitor


import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import com.example.batterymonitor.ui.theme.BatteryMonitorTheme
import android.view.WindowManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import android.app.KeyguardManager
import android.os.PowerManager

class TCPNotificationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 ) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } // end if()
        else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        } // end else

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager

            if (keyguardManager.isKeyguardLocked) {
                keyguardManager.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        super.onDismissSucceeded()
                        // 解鎖成功
                    }

                    override fun onDismissCancelled() {
                        super.onDismissCancelled()
                        // 使用者取消
                    }

                    override fun onDismissError() {
                        super.onDismissError()
                        // 發生錯誤
                    }
                })
            }

        } // end if
        else {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
                "MyApp::WakeUpTag"
            )
            wakeLock.acquire(5000)
            wakeLock.release()
        } // end else

        setContent {

            BatteryMonitorTheme {
                Scaffold(modifier = Modifier.fillMaxSize() ) { innerPadding ->
                    WakeUpScreen( modifier = Modifier.padding(innerPadding) )
                } // Scaffold
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 5000 )



    } // end onCreate

    override fun onDestroy() {
        super.onDestroy()

    } // end onDestroy()

} // end TCPNotificationActivity

@Composable
fun WakeUpScreen(modifier: Modifier = Modifier) {

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    val screenRatio = screenWidth.toFloat() / screenHeight.toFloat()

    var heightRatio = 1.0f
    if ( screenRatio > 0.8649f ) {
        heightRatio = 1.0f
    }
    else {
        heightRatio = screenRatio*1.1561f
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {

        Image(
            painter = painterResource(id = R.drawable.yuigahama_yui),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(heightRatio),
            contentScale = ContentScale.Crop
        )
    }
} // end WakeUpScreen()