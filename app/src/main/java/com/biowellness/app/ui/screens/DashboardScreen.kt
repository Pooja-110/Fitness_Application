package com.biowellness.app.ui.screens

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.biowellness.app.data.models.ScanState
import com.biowellness.app.ui.theme.*
import com.biowellness.app.viewmodel.WellnessViewModel
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DashboardScreen(nav: NavHostController, vm: WellnessViewModel) {
    val scanState by vm.scanState.collectAsState()
    val liveData  by vm.liveData.collectAsState()
    val analysis  by vm.liveAnalysis.collectAsState()
    val torchOn   by vm.torchOn.collectAsState()
    val scanTimer by vm.scanTimer.collectAsState()
    val camPerm   = rememberPermissionState(permission = android.Manifest.permission.CAMERA)

    val inf    = rememberInfiniteTransition(label = "live")
    val glow   by inf.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveGlow"
    )

    Column(modifier = Modifier.fillMaxSize().background(color = BackgroundDark)) {
        TopBar(
            title = "Live Dashboard",
            onBack = { nav.popBackStack() },
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

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {

            // Camera preview
            if (camPerm.status.isGranted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(horizontal = 16.dp)
                        .clip(shape = RoundedCornerShape(16.dp))
                        .border(
                            width = 2.dp,
                            color = qualityColor(liveData.ppgQuality).copy(alpha = 0.45f),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    CameraPreview(vm = vm)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, BackgroundDark.copy(alpha = 0.7f)),
                                    startY = 80f
                                )
                            )
                    )
                    QualityBadge(
                        quality = liveData.ppgQuality,
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                    )
                    if (scanState == ScanState.SCANNING) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(color = RedAlert.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).background(color = RedAlert.copy(alpha = glow), shape = CircleShape))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(text = "LIVE  ${scanTimer}s", color = RedAlert, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (scanState == ScanState.IDLE) {
                        Text(
                            text = "TAP START TO BEGIN",
                            modifier = Modifier.align(Alignment.Center),
                            color = TextMuted,
                            style = MaterialTheme.typography.labelLarge,
                            letterSpacing = 1.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Wellness scores hero
            if (analysis.wellnessScore > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(
                            brush = Brush.linearGradient(colors = listOf(Color(0xFF061428), Color(0xFF0D1F3C))),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(width = 1.dp, color = CyanPrimary.copy(alpha = 0.25f), shape = RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(text = "REAL-TIME WELLNESS", style = MaterialTheme.typography.labelLarge, color = TextMuted, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            ScoreCircle(label = "Wellness", value = analysis.wellnessScore, color = CyanPrimary, size = 64)
                            ScoreCircle(label = "Stress",   value = analysis.stressLevel,
                                color = if (analysis.stressLevel > 60) RedAlert else OrangeWarn, size = 64)
                            ScoreCircle(label = "Focus",    value = analysis.focusLevel,    color = TealAccent,  size = 64)
                            ScoreCircle(label = "Relax",    value = analysis.relaxationScore, color = GreenGood, size = 64)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Vital cards
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VitalCard(
                    label = "HEART RATE",
                    value = if (liveData.heartRate > 0) "${liveData.heartRate}" else "—",
                    unit = "BPM",
                    icon = Icons.Default.Favorite,
                    color = CyanPrimary,
                    conf = liveData.ppgConfidence,
                    modifier = Modifier.weight(1f)
                )
                VitalCard(
                    label = "HRV",
                    value = if (liveData.hrv > 0f) "${liveData.hrv.toInt()}" else "—",
                    unit = "ms",
                    icon = Icons.Default.Timeline,
                    color = TealAccent,
                    conf = liveData.ppgConfidence,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VitalCard(
                    label = "SpO₂",
                    value = if (liveData.spo2 > 0f) "${liveData.spo2.toInt()}" else "—",
                    unit = "%",
                    icon = Icons.Default.Air,
                    color = GreenGood,
                    conf = (liveData.ppgConfidence - 12).coerceAtLeast(0),
                    modifier = Modifier.weight(1f)
                )
                VitalCard(
                    label = "BREATHING",
                    value = if (liveData.breathingRate > 0f) "${liveData.breathingRate.toInt()}" else "—",
                    unit = "br/min",
                    icon = Icons.Default.Air,
                    color = Color(0xFF64FFDA),
                    conf = (liveData.ppgConfidence - 8).coerceAtLeast(0),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            // PPG waveform
            if (liveData.ppgSamples.isNotEmpty()) {
                WaveformCard(
                    samples = liveData.ppgSamples,
                    color = CyanPrimary,
                    label = "PPG WAVEFORM — LIVE",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Emotional state
            if (analysis.wellnessScore > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(width = 1.dp, color = PurpleAccent.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.SentimentNeutral, contentDescription = "Emotion", tint = PurpleAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "EMOTIONAL STATE", style = MaterialTheme.typography.labelLarge, color = TextMuted, letterSpacing = 1.5.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = analysis.emotionalState, style = MaterialTheme.typography.headlineSmall, color = PurpleAccent, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            Chip(text = "${analysis.emotionalConf}% conf", color = PurpleAccent)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        BarRow(label = "Fatigue",     value = analysis.fatigueScore,
                            color = if (analysis.fatigueScore > 60) RedAlert else OrangeWarn)
                        Spacer(modifier = Modifier.height(6.dp))
                        BarRow(label = "Relaxation",  value = analysis.relaxationScore, color = GreenGood)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (scanState) {
                    ScanState.IDLE -> {
                        Button(
                            onClick = { vm.startScan() },
                            modifier = Modifier.weight(1f).height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start", modifier = Modifier.size(18.dp), tint = BackgroundDark)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "START SCAN", fontWeight = FontWeight.Bold, color = BackgroundDark, fontSize = 13.sp)
                        }
                    }
                    ScanState.SCANNING -> {
                        OutlinedButton(
                            onClick = { vm.resetScan() },
                            modifier = Modifier.weight(1f).height(52.dp),
                            border = BorderStroke(width = 1.dp, color = RedAlert.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(18.dp), tint = RedAlert)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "STOP", fontWeight = FontWeight.Bold, color = RedAlert)
                        }
                    }
                    ScanState.COMPLETE -> {
                        Button(
                            onClick = { nav.navigate(Route.REPORT) },
                            modifier = Modifier.weight(1f).height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenGood),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Assessment, contentDescription = "Report", modifier = Modifier.size(18.dp), tint = BackgroundDark)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "VIEW REPORT", fontWeight = FontWeight.Bold, color = BackgroundDark, fontSize = 13.sp)
                        }
                    }
                    else -> {}
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
