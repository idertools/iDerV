package com.example

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ContainerState(val label: String, val description: String) {
    RUNNING("RUNNING", "Kontainer aktif bekerja dan memproses data secara normal."),
    PAUSED("PAUSED", "Kontainer dibekukan sementara di RAM (0% CPU). Menghemat baterai & suhu HP, data tetap aman, dan bisa dilanjutkan seketika."),
    STOPPED("STOPPED", "Kontainer dimatikan secara penuh.")
}

data class VpsContainer(
    val id: String,
    val name: String,
    val serviceType: String,
    var state: ContainerState
)

@Composable
fun VpsDashboard(
    isServiceRunning: Boolean,
    temp: Float,
    battery: Int,
    onToggleService: (Boolean) -> Unit
) {
    var sshEnabled by remember { mutableStateOf(false) }
    val localIp = remember { NetworkUtils.getLocalIpAddress() }
    var showPausedInfoDialog by remember { mutableStateOf(false) }
    var selectedContainerForDialog by remember { mutableStateOf<VpsContainer?>(null) }

    // Dynamic containers state list
    val containers = remember {
        mutableStateListOf(
            VpsContainer("c1", "AI-Assistant", "Python 3.11 / LLM Agent", ContainerState.RUNNING),
            VpsContainer("c2", "Nginx Web Server", "Port 80/443 (HTTP Reverse Proxy)", ContainerState.RUNNING),
            VpsContainer("c3", "Database", "PostgreSQL / SQLite Storage", ContainerState.PAUSED)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeaderSection(
                isServiceRunning = isServiceRunning,
                onToggleService = onToggleService
            )
        }
        item {
            StatsGrid(temp, battery)
        }
        item {
            SshPushCard(
                enabled = sshEnabled,
                ipAddress = localIp,
                onToggle = { sshEnabled = it }
            )
        }
        item {
            ContainersSection(
                containers = containers,
                onInfoClick = { showPausedInfoDialog = true },
                onContainerClick = { container -> selectedContainerForDialog = container },
                onToggleState = { container ->
                    val index = containers.indexOfFirst { it.id == container.id }
                    if (index != -1) {
                        val newState = when (containers[index].state) {
                            ContainerState.RUNNING -> ContainerState.PAUSED
                            ContainerState.PAUSED -> ContainerState.RUNNING
                            ContainerState.STOPPED -> ContainerState.RUNNING
                        }
                        containers[index] = containers[index].copy(state = newState)
                    }
                }
            )
        }
        item {
            AiAgentGatewayCard(localIp = localIp)
        }
        item {
            GuideCard()
        }
    }

    // Dialog Explanation for "PAUSED"
    if (showPausedInfoDialog) {
        StatusExplanationDialog(onDismiss = { showPausedInfoDialog = false })
    }

    // Detail Action Dialog for a Container
    selectedContainerForDialog?.let { container ->
        ContainerActionDialog(
            container = container,
            onDismiss = { selectedContainerForDialog = null },
            onChangeState = { newState ->
                val index = containers.indexOfFirst { it.id == container.id }
                if (index != -1) {
                    containers[index] = containers[index].copy(state = newState)
                }
                selectedContainerForDialog = null
            }
        )
    }
}

/**
 * Header Section with a clear, color-changing PUSH BUTTON (Tombol Tekan)
 * Instead of an ambiguous slider switch.
 */
@Composable
private fun HeaderSection(
    isServiceRunning: Boolean,
    onToggleService: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "IDER VPS",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "Local Node",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Custom Push Button with tactile visual feedback and clear color changes
        PushButton(
            isActive = isServiceRunning,
            activeText = "AKTIF",
            inactiveText = "OFF",
            onClick = { onToggleService(!isServiceRunning) }
        )
    }
}

/**
 * Reusable Modern Push Button with clear color transition, status dot, and haptic/ripple feedback.
 */
@Composable
fun PushButton(
    isActive: Boolean,
    activeText: String = "AKTIF",
    inactiveText: String = "OFF",
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) ButtonActiveGreen else ButtonInactiveSlate,
        animationSpec = tween(durationMillis = 250),
        label = "btnColor"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .height(46.dp),
        color = bgColor,
        shadowElevation = if (isActive) 4.dp else 1.dp,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Glowing Indicator Dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isActive) Color.White else Color.White.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isActive) activeText else inactiveText,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun StatsGrid(temp: Float, battery: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Thermostat,
            label = "Temp",
            value = "${String.format("%.1f", temp)}°C",
            color = TempColor
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Memory,
            label = "CPU",
            value = "34%",
            color = CpuColor
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.BatteryFull,
            label = "Battery",
            value = "$battery%",
            color = BatteryColor
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { 0.6f },
                modifier = Modifier
                    .height(4.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
        }
    }
}

/**
 * SSH Remote Access Card featuring a prominent PUSH BUTTON (instead of slider)
 * and an easy COPY BUTTON for the connection command!
 */
@Composable
private fun SshPushCard(
    enabled: Boolean,
    ipAddress: String,
    onToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val sshCommand = "ssh root@$ipAddress -p 2222"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SshCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = if (enabled) ButtonActiveGreen else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SSH Remote Access",
                            color = SshCardText,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Text(
                        text = if (enabled) "Port 2222 (Terbuka)" else "Port 2222 (Tertutup)",
                        color = if (enabled) ButtonActiveGreen else SshCardText.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Push Button for SSH
                PushButton(
                    isActive = enabled,
                    activeText = "AKTIF",
                    inactiveText = "NONAKTIF",
                    onClick = { onToggle(!enabled) }
                )
            }

            // Expanded SSH details when active with 1-Click Copy
            AnimatedVisibility(visible = enabled) {
                Column(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF00284A))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Perintah Akses Terminal:",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF001528))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sshCommand,
                            color = Color(0xFF68D391),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )

                        // 1-Click Copy Button
                        CopyButton(
                            textToCopy = sshCommand,
                            toastMessage = "Perintah SSH disalin!",
                            iconTint = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Containers section with Interactive status and explanation badge for "PAUSED".
 */
@Composable
private fun ContainersSection(
    containers: List<VpsContainer>,
    onInfoClick: () -> Unit,
    onContainerClick: (VpsContainer) -> Unit,
    onToggleState: (VpsContainer) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Containers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Info icon to clarify container states like PAUSED
                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HelpOutline,
                        contentDescription = "Arti Status Container",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            TextButton(
                onClick = onInfoClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Apa arti 'PAUSED'?",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        containers.forEach { container ->
            ContainerItem(
                container = container,
                onClick = { onContainerClick(container) },
                onToggleClick = { onToggleState(container) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ContainerItem(
    container: VpsContainer,
    onClick: () -> Unit,
    onToggleClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when (container.state) {
                                ContainerState.RUNNING -> Color(0xFFE8F5E9)
                                ContainerState.PAUSED -> Color(0xFFFFF8E1)
                                ContainerState.STOPPED -> Color(0xFFFFEBEE)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = when (container.state) {
                            ContainerState.RUNNING -> ButtonActiveGreenDark
                            ContainerState.PAUSED -> Color(0xFFB45309)
                            ContainerState.STOPPED -> Color(0xFFB91C1C)
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = container.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = container.serviceType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Interactive Status Badge
                StatusBadge(
                    state = container.state,
                    onClick = onToggleClick
                )

                // Quick Play/Pause Action Icon
                IconButton(
                    onClick = onToggleClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (container.state == ContainerState.RUNNING) {
                            Icons.Default.PauseCircle
                        } else {
                            Icons.Default.PlayCircle
                        },
                        contentDescription = if (container.state == ContainerState.RUNNING) "Pause" else "Resume",
                        tint = if (container.state == ContainerState.RUNNING) Color(0xFFD97706) else ButtonActiveGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(state: ContainerState, onClick: () -> Unit) {
    val (bgColor, textColor) = when (state) {
        ContainerState.RUNNING -> BadgeRunning to BadgeRunningText
        ContainerState.PAUSED -> BadgePaused to BadgePausedText
        ContainerState.STOPPED -> BadgeStopped to BadgeStoppedText
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = state.label,
                color = textColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/**
 * AI Agent Gateway Card with dedicated 1-CLICK COPY BUTTONS
 * for Gateway URL and Node Access Token!
 */
@Composable
private fun AiAgentGatewayCard(localIp: String) {
    val gatewayUrl = "http://$localIp:8080/api"
    val token = "ider-token-xyz123"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Agent Gateway",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gateway URL Row with Copy Button
            CopyableDataRow(
                label = "Gateway URL",
                value = gatewayUrl,
                toastMessage = "URL Gateway disalin!"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Node Access Token Row with Copy Button
            CopyableDataRow(
                label = "Node Access Token",
                value = token,
                toastMessage = "Token Akses disalin!"
            )
        }
    }
}

/**
 * Clean Row displaying label, value, and a dedicated 1-click Copy Button
 */
@Composable
fun CopyableDataRow(
    label: String,
    value: String,
    toastMessage: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF1F5F9))
                .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            CopyButton(
                textToCopy = value,
                toastMessage = toastMessage,
                iconTint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * 1-Click Copy Button with animated check feedback
 */
@Composable
fun CopyButton(
    textToCopy: String,
    toastMessage: String,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    IconButton(
        onClick = {
            clipboardManager.setText(AnnotatedString(textToCopy))
            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
            isCopied = true
            coroutineScope.launch {
                delay(1800)
                isCopied = false
            }
        },
        modifier = Modifier.size(36.dp)
    ) {
        if (isCopied) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Tersalin",
                tint = ButtonActiveGreen,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = "Salin ke Clipboard",
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Modal Dialog explaining what "PAUSED" means in Docker/Container architecture.
 */
@Composable
private fun StatusExplanationDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Arti Status Kontainer", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ButtonActiveGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("1. RUNNING (Berjalan)", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Kontainer sedang aktif memproses tugas. Memakai CPU & RAM secara wajar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider()

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD97706))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("2. PAUSED (Dijeda / Freeze)", fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                    }
                    Text(
                        "Artinya proses kontainer dibekukan sementara di RAM (penggunaan CPU 0%). Tujuannya menghemat baterai & menjaga suhu HP tetap dingin saat kontainer sedang tidak dipakai.\n\nKeuntungannya: Saat dilanjutkan (Resume), kontainer langsung aktif seketika tanpa perlu booting ulang!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider()

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDC2626))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("3. STOPPED (Dimatikan)", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Kontainer dimatikan total. Memerlukan waktu start ulang bila ingin digunakan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Mengerti")
            }
        }
    )
}

/**
 * Container Action Dialog allowing user to Resume, Pause, or Restart the selected container.
 */
@Composable
private fun ContainerActionDialog(
    container: VpsContainer,
    onDismiss: () -> Unit,
    onChangeState: (ContainerState) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Kelola: ${container.name}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Status saat ini: ${container.state.label}", fontWeight = FontWeight.Medium)
                Text(
                    text = container.state.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Ubah Status:", fontWeight = FontWeight.SemiBold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (container.state != ContainerState.RUNNING) {
                        Button(
                            onClick = { onChangeState(ContainerState.RUNNING) },
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonActiveGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Resume (Jalankan)")
                        }
                    }
                    if (container.state != ContainerState.PAUSED) {
                        OutlinedButton(
                            onClick = { onChangeState(ContainerState.PAUSED) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Jeda (Pause)")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
private fun GuideCard() {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cara Kerja & Panduan", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Aplikasi membuat sistem virtual tanpa akses Root, dan dijaga hidup 24 jam dengan Wakelock Android.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Cara Penggunaan:\n" +
                    "1. Tekan tombol push 'AKTIF' pada header untuk menyalakan node VPS.\n" +
                    "2. Gunakan tombol 'Salin' (Copy) pada Gateway URL atau Token untuk menghubungkan aplikasi Anda di iderg.my.id.\n" +
                    "3. Aktifkan SSH Remote Access dengan tombol push hijau untuk membuka port 2222.\n" +
                    "4. Kelola kontainer (Jalankan / Jeda) untuk menghemat RAM dan baterai HP.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
