package com.example.alldemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alldemo.ui.DemoLauncherViewModel
import com.example.alldemo.ui.navigation.AppNavigation
import com.example.alldemo.ui.theme.AllDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AllDemoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: DemoLauncherViewModel = viewModel()
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}
