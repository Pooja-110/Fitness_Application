package com.biowellness.app.ui.screens

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.biowellness.app.data.models.ScanState
import com.biowellness.app.ui.theme.*
import com.biowellness.app.utils.BleHeartRateManager
import com.biowellness.app.viewmodel.WellnessViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * Connects to a real external heart-rate sensor (chest strap or
 * watch) over Bluetooth LE — the genuine non-camera sensor path. See
 * BleHeartRateManager for the GATT Heart Rate Profile implementation.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BleConnectScreen(nav: NavHostController, vm: WellnessViewModel) {
    val bleState   by vm.bleState.collectAsState()
    val devices    by vm.bleDevices.collectAsState()
    val deviceName by vm.bleDeviceName.collectAsState()
    val liveData   by vm.liveData.collectAsState()
    val scanState  by vm.scanState.collectAsState()

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        listOf(android.Manifest.permission.BLUETOOTH, android.Manifest.permission.ACCESS_FINE_LOCATION)
    }
    val permState = rememberMultiplePermissionsState(permissions = permissions)

    LaunchedEffect(key1 = scanState) {
        if (scanState == ScanState.COMPLETE) {
            nav.navigate(Route.REPORT) { popUpTo(Route.HOME) }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (bleState == BleHeartRateManager.ConnectionState.SCANNING) vm.bleStopScan()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(color = BackgroundDark)) {
        TopBar(title = "Heart Rate Device", onBack = {
            if (scanState == ScanState.SCANNING) vm.resetScan()
            nav.popBackStack()
        })

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1628)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Bluetooth, contentDescription = "BT", tint = TealAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "WHAT THIS DOES", style = MaterialTheme.typography.labelLarge, color = TealAccent, letterSpacing = 1.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This connects to a real heart-rate sensor in a chest strap or watch (any device supporting the standard Bluetooth Heart Rate service). " +
                               "It reads the device's own PPG/ECG sensor directly — no camera, no phone touch required. " +
                               "If your device reports beat-to-beat intervals, HRV here is computed from real data instead of a camera approximation.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            if (!permState.allPermissionsGranted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(width = 1.dp, color = TealAccent.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.BluetoothDisabled, contentDescription = "BT", tint = TealAccent, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Bluetooth Permission Required", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Needed to scan for and connect to nearby heart-rate devices.",
                            style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(onClick = { permState.launchMultiplePermissionRequest() },
                            colors = ButtonDefaults.buttonColors(containerColor = TealAccent), shape = RoundedCornerShape(12.dp)) {
                            Text(text = "Grant Permission", color = BackgroundDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                return@Column
            }

            when (bleState) {
                BleHeartRateManager.ConnectionState.CONNECTED -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = TealAccent.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(width = 1.dp, color = TealAccent.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.Favorite, contentDescription = "HR", tint = RedAlert, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "${liveData.heartRate}", style = MaterialTheme.typography.headlineLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text(text = "BPM — live from ${deviceName ?: "device"}", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                            if (liveData.rrIntervalsMs.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "HRV (real R-R data): ${liveData.hrv.toInt()} ms", style = MaterialTheme.typography.bodyMedium, color = TealAccent)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    if (scanState == ScanState.SCANNING) {
                        Button(
                            onClick = { vm.bleFinishLiveSession() },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(text = "FINISH & GENERATE REPORT", fontWeight = FontWeight.Bold, color = BackgroundDark)
                        }
                    } else {
                        Button(
                            onClick = { vm.bleStartLiveSession() },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start", tint = BackgroundDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "START LIVE SESSION", fontWeight = FontWeight.Bold, color = BackgroundDark)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { vm.bleDisconnect() },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(width = 1.dp, color = TextMuted.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(text = "Disconnect", color = TextMuted)
                    }
                }

                BleHeartRateManager.ConnectionState.CONNECTING -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = TealAccent)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Connecting...", color = TextSecondary)
                        }
                    }
                }

                else -> {
                    Button(
                        onClick = { vm.bleStartScan() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Bluetooth, contentDescription = "Scan", tint = BackgroundDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (bleState == BleHeartRateManager.ConnectionState.SCANNING) "SCANNING..." else "SCAN FOR DEVICES",
                            fontWeight = FontWeight.Bold, color = BackgroundDark)
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    if (devices.isEmpty() && bleState == BleHeartRateManager.ConnectionState.SCANNING) {
                        Text(text = "Looking for nearby heart-rate devices — make sure yours is powered on and worn/attached.",
                            style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                    }
                    if (bleState == BleHeartRateManager.ConnectionState.FAILED) {
                        Text(text = "Couldn't scan — check that Bluetooth is on and permissions are granted.",
                            style = MaterialTheme.typography.bodyMedium, color = OrangeWarn)
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    devices.forEach { d ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                .clickable { vm.bleConnect(d.address) },
                            colors = CardDefaults.cardColors(containerColor = CardDark),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Watch, contentDescription = "Device", tint = TealAccent, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = d.name, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                    Text(text = d.address, color = TextMuted, style = MaterialTheme.typography.labelSmall)
                                }
                                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Connect", tint = TextMuted)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
