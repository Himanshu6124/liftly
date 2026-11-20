package com.himanshu.liftly.android

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.himanshu.liftly.App
import com.himanshu.liftly.core.platform.NotificationPermission

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Asked lazily: the first time a rest timer is started.
            NotificationPermission.register { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }
        setContent { App() }
    }

    override fun onDestroy() {
        NotificationPermission.unregister()
        super.onDestroy()
    }
}
