package com.biowellness.app.ui.screens

import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.biowellness.app.data.models.ScanState
import com.biowellness.app.ui.theme.*
import com.biowellness.app.viewmodel.WellnessViewModel
import com.google.accompanist.permissions.*
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanScreen(nav: NavHostController, vm: WellnessViewModel) {
    val scanState by vm.scanState.collectAsState()
    val progress  by vm.progress.collectAsState()
    val liveData  by vm.liveData.collectAsState()
    val analysis  by vm.liveAnalysis.collectAsState()
    val message   by vm.message.collectAsState()
    val torchOn   by vm.torchOn.collectAsState()
    val scanTimer by vm.scanTimer.collectAsState()

    val camPerm = rememberPermissionState(permission = android.Manifest.permission.CAMERA)

    // KEY FIX: navigate to Report with popUpTo(SCAN, inclusive=true)
    // so pressing Back from Report goes to Home, not back to stale Scan.
    LaunchedEffect(key1 = scanState) {
        if (scanState == ScanState.COMPLETE) {
            nav.navigate(Route.REPORT) {
                popUpTo(Route.SCAN) { inclusive = true }
            }
        }
    }

    val inf     = rememberInfiniteTransition(label = "hrp")
    val hrPulse by inf.animateFloat(
        initialValue = 1f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hrScale"
    )

    Column(modifier = Modifier.fillMaxSize().background(color = BackgroundDark)) {

        TopBar(
            title = "Bio-Signal Scanner",
            onBack = { vm.resetScan(); nav.popBackStack() },
            actions = {
                IconButton(onClick = { vm.toggleTorch() }) {
                    Icon(
                        imageVector = if (torchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                        contentDescription = "Torch",
                        tint = if (torchOn) YellowColor else TextMuted
                    )
                }
            }
        )

        if (!camPerm.status.isGranted) {
            PermissionScreen(
                icon = Icons.Default.CameraAlt,
                title = "Camera Required",
                body = "BioWellness AI uses your rear camera + torch to detect blood volume pulse (PPG) for heart rate and HRV. A 60-second scan window gives the steadiest reading. There is no simulated/demo mode — camera access is required to measure anything.",
                primaryLabel = "Grant Camera Permission",
                onPrimary = { camPerm.launchPermissionRequest() }
            )
        } else {
            // Camera preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .padding(horizontal = 16.dp)
                    .clip(shape = RoundedCornerShape(20.dp))
                    .border(
                        width = 2.dp,
                        color = qualityColor(liveData.ppgQuality).copy(alpha = 0.5f),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                CameraPreview(vm = vm)
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, BackgroundDark.copy(alpha = 0.65f)),
                            startY = 130f
                        )
                    )
                )
                if (scanState == ScanState.IDLE) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.Fingerprint, contentDescription = "Finger",
                            tint = CyanPrimary.copy(alpha = 0.9f), modifier = Modifier.size(72.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Cover lens AND flashlight with finger", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(text = "60-second scan for the steadiest reading", color = TextSecondary, fontSize = 12.sp)
                    }
                }
                QualityBadge(quality = liveData.ppgQuality,
                    modifier = Modifier.align(Alignment.TopStart).padding(10.dp))
                if (torchOn) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                            .background(color = YellowColor.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.FlashlightOn, contentDescription = "Torch",
                                tint = YellowColor, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "TORCH", color = YellowColor,
                                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (liveData.heartRate > 0) {
                    Box(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)
                            .background(color = BackgroundDark.copy(alpha = 0.85f), shape = RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Favorite, contentDescription = "HR",
                                tint = RedAlert, modifier = Modifier.size((12f * hrPulse).dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(text = "${liveData.heartRate} BPM", color = TextPrimary,
                                fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Waveform
            if (liveData.ppgSamples.isNotEmpty()) {
                WaveformCard(
                    samples = liveData.ppgSamples, color = CyanPrimary, label = "PPG WAVEFORM",
                    modifier = Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Progress
            if (scanState == ScanState.SCANNING || scanState == ScanState.ANALYZING) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(text = message, color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f), lineHeight = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "${(progress * 100).toInt()}%", color = CyanPrimary, fontWeight = FontWeight.Bold)
                                Text(text = "${scanTimer}s / 60s", color = TextMuted,
                                    style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth().clip(shape = RoundedCornerShape(4.dp)),
                            color = CyanPrimary,
                            trackColor = CardDarker
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Live metrics
            if (scanState == ScanState.SCANNING && liveData.heartRate > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LiveChip(label = "HR",     value = "${liveData.heartRate} BPM",          color = CyanPrimary,  modifier = Modifier.weight(1f))
                    LiveChip(label = "HRV",    value = "${liveData.hrv.toInt()} ms",         color = TealAccent,   modifier = Modifier.weight(1f))
                    LiveChip(label = "SpO₂",   value = "N/A",                                color = TextMuted,    modifier = Modifier.weight(1f))
                    LiveChip(label = "STRESS", value = "${analysis.stressLevel}",
                        color = if (analysis.stressLevel > 60) RedAlert else OrangeWarn,     modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Error state — real failure, not a fabricated result
            if (scanState == ScanState.ERROR) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0E00)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(width = 1.dp, color = OrangeWarn.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = "Error", tint = OrangeWarn, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "SCAN FAILED", style = MaterialTheme.typography.labelLarge, color = OrangeWarn, letterSpacing = 1.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = message, style = MaterialTheme.typography.bodyMedium, color = OrangeWarn.copy(alpha = 0.9f), lineHeight = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Why 60 seconds info
            if (scanState == ScanState.IDLE) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1628)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "WHY 60 SECONDS?", style = MaterialTheme.typography.labelSmall,
                            color = CyanPrimary, letterSpacing = 1.5.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Smart watches also use 30–120 s windows for accuracy\n" +
                                   "• HRV requires ≥20 consecutive heartbeat intervals\n" +
                                   "• Longer scan = more beats = more accurate median HR\n" +
                                   "• Cover BOTH camera lens and flashlight completely",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary, lineHeight = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            // Buttons
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (scanState) {
                    ScanState.IDLE -> {
                        Button(
                            onClick = { vm.startScan() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start",
                                modifier = Modifier.size(20.dp), tint = BackgroundDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "START 60-SECOND SCAN", fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp, color = BackgroundDark)
                        }
                        OutlinedButton(
                            onClick = { nav.navigate(Route.DASHBOARD) },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(width = 1.dp, color = TealAccent.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Dashboard, contentDescription = "Dashboard",
                                modifier = Modifier.size(16.dp), tint = TealAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Live Dashboard", color = TealAccent)
                        }
                        TextButton(onClick = { nav.navigate(Route.BLE_HR) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(imageVector = Icons.Default.Bluetooth, contentDescription = "BT", modifier = Modifier.size(14.dp), tint = TextMuted)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Have a heart-rate strap or watch? Connect it instead", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                    ScanState.SCANNING, ScanState.ANALYZING -> {
                        OutlinedButton(
                            onClick = { vm.resetScan(); nav.popBackStack() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            border = BorderStroke(width = 1.dp, color = RedAlert.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop",
                                modifier = Modifier.size(20.dp), tint = RedAlert)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "CANCEL SCAN", fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp, color = RedAlert)
                        }
                    }
                    ScanState.ERROR -> {
                        Button(
                            onClick = { vm.resetScan(); vm.startScan() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeWarn),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry",
                                modifier = Modifier.size(20.dp), tint = BackgroundDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "TRY AGAIN", fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp, color = BackgroundDark)
                        }
                        OutlinedButton(
                            onClick = { vm.resetScan(); nav.popBackStack() },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(width = 1.dp, color = TextMuted.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(text = "Back", color = TextMuted)
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun CameraPreview(vm: WellnessViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    AndroidView(
        factory = { ctx ->
            val pv = PreviewView(ctx)
            ProcessCameraProvider.getInstance(ctx).addListener({
                val cp = ProcessCameraProvider.getInstance(ctx).get()
                val prev = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
                val ia   = ImageAnalysis.Builder()
                    .setTargetResolution(android.util.Size(320, 240))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also { it.setAnalyzer(executor, vm.imageAnalyzer) }
                try {
                    cp.unbindAll()
                    val cam = cp.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, prev, ia)
                    vm.setCameraControl(cam.cameraControl)
                } catch (e: Exception) { e.printStackTrace() }
            }, ContextCompat.getMainExecutor(ctx))
            pv
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun LiveChip(label: String, value: String, color: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = CardDark), shape = RoundedCornerShape(10.dp)) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
            Text(text = value, style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PermissionScreen(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String, body: String,
    primaryLabel: String, onPrimary: () -> Unit,
    secondaryLabel: String? = null, onSecondary: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(80.dp).background(color = CyanPrimary.copy(alpha = 0.1f), shape = CircleShape),
                contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = title, tint = CyanPrimary, modifier = Modifier.size(44.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = title, style = MaterialTheme.typography.headlineSmall, color = TextPrimary,
                textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary,
                textAlign = TextAlign.Center, lineHeight = 20.sp)
            Spacer(modifier = Modifier.height(28.dp))
            Button(onClick = onPrimary, modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary), shape = RoundedCornerShape(14.dp)) {
                Text(text = primaryLabel, fontWeight = FontWeight.Bold, color = BackgroundDark)
            }
            if (secondaryLabel != null) {
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = onSecondary) {
                    Text(text = secondaryLabel, color = TextMuted, fontSize = 12.sp)
                }
            }
        }
    }
}
