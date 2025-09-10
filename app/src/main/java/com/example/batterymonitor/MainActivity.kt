package com.example.batterymonitor

import android.Manifest
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.batterymonitor.ui.theme.BatteryMonitorTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.os.Build
import com.google.firebase.messaging.FirebaseMessaging
import com.google.android.gms.tasks.OnCompleteListener
import android.util.Log
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import android.net.Uri
import android.widget.ImageView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

class BatteryReceiver : BroadcastReceiver() {

    private var batteryPct = MutableStateFlow(0)     // private value
    val batteryPct_state: StateFlow<Int> = batteryPct           // read-only state

    override fun onReceive(context: Context?, intent: Intent?) {
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        val batteryPct_f = level * 100 / scale.toFloat()
        batteryPct.value = batteryPct_f.toInt()

    } // end onReceive()

} // end BatteryReceiver

class FCMtokenManager {

    private var token = MutableStateFlow("No token")     // private value
    val token_state: StateFlow<String> = token           // read-only state

    fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCMtokenManager", "Fetch FCM token Failed", task.exception)
                return@OnCompleteListener
            }

            // 取得新的 FCM registration token
            val fetch_token = task.result
            token.value = fetch_token
            Log.d("FCMtokenManager", "FCM Token: $fetch_token")
        }) // addOnCompleteListener()

    } // getFCMToken()

} // end FCMtokenManager

class MainActivity : ComponentActivity() {

    private fun Initialized_FCM() {
        val prefs = getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
        val isInitialized = prefs.getBoolean("initialized", false)

        if ( !isInitialized ) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FCMtokenManager", "Fetch FCM token Failed", task.exception)
                    return@OnCompleteListener
                }

                // 取得新的 FCM registration token
                val fetch_token = task.result
                prefs.edit {
                    putBoolean("initialized", true)
                    putString("fcm_token", fetch_token)
                }

            }) // addOnCompleteListener()

        } // end if()

    } // end Initialized_FCM()

    /*
    private fun updateBatteryLevel() {
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        batteryStatus?.run {
            val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = getIntExtra(BatteryManager.EXTRA_STATUS, -1)

            val batteryPct = level * 100f / scale
            val statusText = when (status) {
                BatteryManager.BATTERY_STATUS_CHARGING -> " (充電中)"
                BatteryManager.BATTERY_STATUS_FULL -> " (充飽)"
                else -> ""
            }

            // batteryText.text = "電量: %.1f%%%s".format(batteryPct, statusText)
        }
    } // end updateBatteryLevel()
    */

    private val batteryReceiver = BatteryReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        splashScreen.setKeepOnScreenCondition {
            false
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {

            BatteryMonitorTheme {
                Scaffold(modifier = Modifier.fillMaxSize() ) { innerPadding ->
                    AppNavigation(innerPadding, batteryReceiver)
                } // Scaffold
            }
        }

        // start foreground
        val intent = Intent(this, TCPConnectionService::class.java) // Build the intent for the service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        }
        else {
            startService(intent)
        }
        // 考慮包裝成function

        // init fcm token
        Initialized_FCM()

        // register boardcastReceiver
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, intentFilter)

        requestFullScreenIntentPermission()

    } // end onCreate

    override fun onDestroy() {
        super.onDestroy()
        // handler.removeCallbacks(updateRunnable)
        runCatching { unregisterReceiver(batteryReceiver) }
    } // end onDestroy()

    private fun requestFullScreenIntentPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )

            } // end if

        } // end if

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14
            val notificationManager = getSystemService(NotificationManager::class.java)
            if (!notificationManager.canUseFullScreenIntent()) {

                val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.parse("package:$packageName")
                }


                AlertDialog.Builder(this)
                    .setTitle("需要全屏通知權限")
                    .setMessage("為了確保您能及時收到重要訊息，請允許此應用使用全屏通知。")
                    .setPositiveButton("前往設定") { _, _ ->
                        startActivity(intent)
                    }
                    .setNegativeButton("稍後再說", null)
                    .show()
            }

        } // end if()

    } // requestFullScreenIntentPermission()

} // end MainActivity

@Composable
fun BatteryShow(batteryPct: Int, modifier: Modifier = Modifier) {
    var imageResource = R.drawable.power_1
    if ( batteryPct == 100) {
        imageResource = R.drawable.power_100
    } else if ( batteryPct > 74 ) {
        imageResource = R.drawable.power_1
    } else if ( batteryPct > 26  ) {
        imageResource = R.drawable.power_2
    } else if ( batteryPct > 15  ) {
        imageResource = R.drawable.power_3
    } else {
        imageResource = R.drawable.power_5
    }

    Row(
        modifier = Modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(imageResource),
            contentDescription = null,
            modifier = Modifier.size(100.dp)
        )
        Text(
            text = "$batteryPct%",
            textAlign = TextAlign.Left,
            fontSize = 24.sp,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp)
        )

    }
}

@Composable
fun HomeScreen(batteryPct_state: StateFlow<Int>, modifier: Modifier = Modifier) {
    val batteryPct by batteryPct_state.collectAsState()
    val fcm_token_manager = remember { FCMtokenManager() }
    val fcm_token by fcm_token_manager.token_state.collectAsState()
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    Column(modifier
        .fillMaxSize()
        .padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 16.dp)) {
        BatteryShow(
            batteryPct = batteryPct,
            modifier = modifier
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Now token is: $fcm_token",
            textAlign = TextAlign.Start,
            fontSize = 24.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(onClick = { fcm_token_manager.getFCMToken() }) {
                Text(text = "Update")
            }
        } // Row
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(onClick = {
                coroutineScope.launch {
                    val clipEntry = ClipEntry(ClipData.newPlainText("fcm_token", fcm_token))
                    clipboard.setClipEntry(clipEntry)
                }
            }) {
                Text("Copy Token")
            }
        } // Row

    } // Column
}

@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(Unit) {
        delay(2000)
        navController.navigate("main") {
            popUpTo("splash") { inclusive = true } // 清掉 splash 畫面
        }
    }

    Box(modifier = Modifier.fillMaxSize(), // 讓容器填滿畫面
        contentAlignment = Alignment.Center) {

        AndroidView(
            factory = { context ->
                ImageView(context).apply {
                    setImageResource(R.drawable.splash_background)
                    scaleType = ImageView.ScaleType.FIT_XY // 或 CENTER_CROP，看你需求
                }
            },
            modifier = Modifier.matchParentSize() // 讓它填滿整個父容器
        )

        Image(
            painter = painterResource(id = R.drawable.security),
            contentDescription = "Logo",
            modifier = Modifier
                .size(150.dp)
        )
    }

}

@Composable
fun AppNavigation(innerPadding: PaddingValues, batteryReceiver: BatteryReceiver ) {
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFd1386a), Color(0xFF114357))
    )
    val navController = rememberNavController()

    NavHost(navController, startDestination = "splash") {
        composable("splash") { SplashScreen(navController) }
        composable("main") {
            Box( modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(innerPadding) ) {
                HomeScreen(
                    batteryPct_state = batteryReceiver.batteryPct_state,
                    modifier = Modifier.padding(innerPadding)
                )
            } // Box
        }
    }
}

/*
@Preview(showBackground = true)
@Composable
fun AppPreview() {
    BatteryMonitorTheme {
        HomeScreen(
            batteryPct_state = batteryReceiver.batteryPct_state,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

 */