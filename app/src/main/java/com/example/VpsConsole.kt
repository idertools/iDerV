package com.example

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun VpsConsole() {
    var command by remember { mutableStateOf("") }
    val logs = remember {
        mutableStateListOf(
            "Welcome to Mobile VPS Console",
            "ider VPS local Node v1.0 [Online]",
            "Ketik 'help' untuk daftar perintah yang tersedia."
        )
    }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val localIp = remember { NetworkUtils.getLocalIpAddress() }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    fun executeCommand(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return

        logs.add("root@ider-vps:~# $trimmed")

        when (trimmed.lowercase()) {
            "help" -> {
                logs.add("Available commands:")
                logs.add("  status     - Tampilkan status sistem VPS")
                logs.add("  docker ps  - Tampilkan daftar kontainer aktif")
                logs.add("  ip a       - Tampilkan alamat IP lokal")
                logs.add("  uptime     - Waktu aktif node")
                logs.add("  clear      - Bersihkan layar konsol")
            }
            "clear" -> {
                logs.clear()
                logs.add("Konsol dibersihkan.")
            }
            "status" -> {
                logs.add("[SYS] IDER VPS Node: RUNNING")
                logs.add("[NET] Local IP: $localIp")
                logs.add("[SSH] Port 2222: ACTIVE")
                logs.add("[MEM] 1.8GB / 3.6GB Used")
            }
            "docker ps" -> {
                logs.add("CONTAINER ID   IMAGE                 STATUS    PORTS")
                logs.add("c1-ai-assist   agent-llm:latest      Up 2h     8080/tcp")
                logs.add("c2-nginx-web   nginx:alpine          Up 2h     80,443/tcp")
                logs.add("c3-db-server   postgres:16           Paused    5432/tcp")
            }
            "ip a", "ip" -> {
                logs.add("1: lo: <LOOPBACK,UP> inet 127.0.0.1/8")
                logs.add("2: wlan0: <BROADCAST,UP> inet $localIp/24")
            }
            "uptime" -> {
                logs.add(" 12:45:00 up 2:15, load average: 0.35, 0.40, 0.28")
            }
            else -> {
                logs.add("bash: $trimmed: command not found (ketik 'help' untuk panduan)")
            }
        }
        command = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121417))
    ) {
        // Console Header Bar with Copy & Clear buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E2229))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF10B981), shape = RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Terminal Session",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row {
                IconButton(
                    onClick = {
                        val fullLog = logs.joinToString("\n")
                        clipboardManager.setText(AnnotatedString(fullLog))
                        Toast.makeText(context, "Log terminal disalin!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Salin Log",
                        tint = Color(0xFFA0AEC0),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        logs.clear()
                        logs.add("Konsol dibersihkan.")
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Bersihkan Konsol",
                        tint = Color(0xFFA0AEC0),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Terminal Output Screen
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            state = listState
        ) {
            items(logs) { log ->
                val textColor = when {
                    log.startsWith("root@") -> Color(0xFF38BDF8)
                    log.startsWith("bash:") -> Color(0xFFF87171)
                    log.startsWith("[") -> Color(0xFFFBBF24)
                    else -> Color(0xFF4ADE80)
                }
                Text(
                    text = log,
                    color = textColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
            }
        }

        // Quick Command Shortcuts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF181B20))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("help", "status", "docker ps", "ip a", "uptime").forEach { cmd ->
                Surface(
                    onClick = { executeCommand(cmd) },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF282F3B)
                ) {
                    Text(
                        text = cmd,
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Terminal Input Field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E2229))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "root@ider-vps:~#",
                color = Color(0xFF38BDF8),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 6.dp)
            )
            TextField(
                value = command,
                onValueChange = { command = it },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color(0xFF4ADE80)
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                singleLine = true,
                placeholder = {
                    Text("Ketik perintah...", color = Color.Gray, fontSize = 13.sp)
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { executeCommand(command) })
            )
            IconButton(
                onClick = { executeCommand(command) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Kirim",
                    tint = Color(0xFF4ADE80)
                )
            }
        }
    }
}
