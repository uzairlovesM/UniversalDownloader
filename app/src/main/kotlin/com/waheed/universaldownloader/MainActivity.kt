package com.waheed.universaldownloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.rememberNavController
import com.waheed.universaldownloader.data.settings.PinManager
import com.waheed.universaldownloader.ui.navigation.NavRoutes
import com.waheed.universaldownloader.ui.navigation.UDNavGraph
import com.waheed.universaldownloader.ui.theme.UniversalDownloaderTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var pinManager: PinManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UniversalDownloaderTheme {
                val navController = rememberNavController()
                val startDestination = remember {
                    if (pinManager.isPinSet()) NavRoutes.PIN_LOCK_VERIFY else NavRoutes.SPLASH
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                val app = application as UDApplication

                DisposableEffectPinRecheck(
                    onResumeCheck = {
                        if (app.requiresPinRecheck && pinManager.isPinSet()) {
                            app.clearPinRecheckFlag()
                            navController.navigate(NavRoutes.PIN_LOCK_VERIFY) {
                                launchSingleTop = true
                            }
                        }
                    }
                )

                UDNavGraph(navController = navController, startDestination = startDestination)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun DisposableEffectPinRecheck(onResumeCheck: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onResumeCheck()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
