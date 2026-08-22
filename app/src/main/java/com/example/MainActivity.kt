package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var selectedTab by remember { mutableStateOf(0) }
                val isServiceRunning by VpsService.isRunning.collectAsState()
                val temp by VpsService.batteryTemp.collectAsState()
                val battery by VpsService.batteryLevel.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Host") },
                                label = { Text("Host") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.Terminal, contentDescription = "Console") },
                                label = { Text("Console") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (selectedTab == 0) {
                            VpsDashboard(
                                isServiceRunning = isServiceRunning,
                                temp = temp,
                                battery = battery,
                                onToggleService = { checked ->
                                    if (checked) {
                                        val intent = Intent(this@MainActivity, VpsService::class.java)
                                        startForegroundService(intent)
                                    } else {
                                        val intent = Intent(this@MainActivity, VpsService::class.java)
                                        stopService(intent)
                                    }
                                }
                            )
                        } else {
                            VpsConsole()
                        }
                    }
                }
            }
        }
    }
}
