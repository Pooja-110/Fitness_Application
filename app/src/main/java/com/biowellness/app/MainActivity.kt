package com.biowellness.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.biowellness.app.ui.screens.AppNavGraph
import com.biowellness.app.ui.theme.BioWellnessTheme
import com.biowellness.app.ui.theme.BackgroundDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BioWellnessTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
                    AppNavGraph()
                }
            }
        }
    }
}
