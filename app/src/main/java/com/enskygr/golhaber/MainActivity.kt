package com.enskygr.golhaber

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.enskygr.golhaber.notify.Notifier
import com.enskygr.golhaber.notify.Scheduler
import com.enskygr.golhaber.ui.MainScreen
import com.enskygr.golhaber.ui.MainViewModel
import com.enskygr.golhaber.ui.theme.GolHaberTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    private val askNotif = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.refreshPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && !Notifier.canNotify(this)) {
            askNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        Scheduler.kick(this)
        setContent {
            GolHaberTheme {
                MainScreen(vm, onAskPermission = {
                    if (Build.VERSION.SDK_INT >= 33) askNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
                })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshPermission()
    }
}
