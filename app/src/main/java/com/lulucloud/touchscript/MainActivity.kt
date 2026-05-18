package com.lulucloud.touchscript

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lulucloud.touchscript.app.AppViewModelFactory
import com.lulucloud.touchscript.navigation.TouchWorkshopApp
import com.lulucloud.touchscript.ui.theme.TouchScriptTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TouchScriptTheme {
                TouchWorkshopApp(
                    appViewModelFactory = AppViewModelFactory(
                        (application as TouchWorkshopApplication).appContainer
                    )
                )
            }
        }
    }
}
