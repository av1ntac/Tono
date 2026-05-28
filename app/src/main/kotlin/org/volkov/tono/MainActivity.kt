package org.volkov.tono

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import org.volkov.tono.ui.TonoScreen
import org.volkov.tono.ui.theme.TonoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TonoTheme {
                TonoScreen(modifier = Modifier.safeDrawingPadding())
            }
        }
    }
}
