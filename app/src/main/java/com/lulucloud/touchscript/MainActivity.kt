package com.lulucloud.touchscript

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lulucloud.touchscript.app.AppContainer
import com.lulucloud.touchscript.app.AppViewModelFactory
import com.lulucloud.touchscript.navigation.TouchWorkshopApp
import com.lulucloud.touchscript.ui.theme.TouchScriptTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer: AppContainer = (application as TouchWorkshopApplication).appContainer
        setContent {
            TouchScriptTheme {
                TouchWorkshopApp(
                    appViewModelFactory = AppViewModelFactory(appContainer),
                    appContainer = appContainer
                )
            }
        }
    }
}
