package com.slowatcoding.finio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge to edge: the note paper runs under both system bars, as the PWA's does in
        // standalone mode; headers and the tab bar pad themselves by the insets.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { AppContent() }
    }
}
