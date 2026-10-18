package com.biowellness.app.ui.screens

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.biowellness.app.ui.theme.*
import com.biowellness.app.utils.SleepSessionPhase
import com.biowellness.app.viewmodel.WellnessViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SleepScreen(nav: NavHostController, vm: WellnessViewModel) {
    val context = LocalContext.current
    val phase    by vm.sleepPhase.collectAsState()
    val elapsed  by vm.sleepElapsedSeconds.collectAsState()
    val movement by vm.sleepCurrentMovement.collectAsState()
    val summary  by vm.sleepSummary.collectAsState()

    val notifPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(permission = android.Manifest.permission.POST_NOTIFICATIONS)
    } else null

    Column(modifier = Modifier.fillMaxSize().background(color = BackgroundDark)) {
        TopBar(title = "Sleep Tracking", onBack = { nav.popBackStack() })

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {

            // What this actually is
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1628)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = TealAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "HOW THIS WORKS", style = MaterialTheme.typography.labelLarge, color = TealAccent, letterSpacing = 1.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This uses your phone's motion sensor to track how still you are, the same basic principle " +
                               "consumer sleep-tracking wearables use. It is NOT clinical sleep-stage tracking — it cannot detect " +
                               "REM/light/deep sleep, only movement vs. stillness. For best results, place the phone on the " +
                               "mattress near you, plugged in, with the screen off.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (notifPerm != null && !notifPerm.status.isGranted && phase == SleepSessionPhase.IDLE) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(width = 1.dp, color = TealAccent.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Allow notifications so you can see tracking is active overnight.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(onClick = { notifPerm.launchPermissionRequest() },
                            colors = ButtonDefaults.buttonColors(containerColor = TealAccent)) {
                            Text(text = "Allow Notifications", color = BackgroundDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            when (phase) {
                SleepSessionPhase.IDLE -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(width = 1.dp, color = TealAccent.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.Bedtime, contentDescription = "Sleep", tint = TealAccent, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Ready to track", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Start before you go to sleep, stop when you wake up.",
                                style = MaterialTheme.typography.bodyMedium, color = TextMuted, textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { vm.startSleepTracking(context) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start", tint = BackgroundDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "START SLEEP TRACKING", fontWeight = FontWeight.Bold, color = BackgroundDark)
                    }
                }

                SleepSessionPhase.TRACKING -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = TealAccent.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(width = 1.dp, color = TealAccent.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            val h = elapsed / 3600
                            val m = (elapsed % 3600) / 60
                            val s = elapsed % 60
                            Text(
                                text = "%02d:%02d:%02d".format(h, m, s),
                                style = MaterialTheme.typography.headlineMedium,
                                color = TealAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "Tracking movement...", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Live accelerometer reading — makes clear this is
                            // actually sampling the motion sensor right now,
                            // not just counting time.
                            Text(text = "LIVE MOTION SENSOR READING", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "%.3f".format(movement) + " m/s²",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (movement < 0.35f) GreenGood else OrangeWarn,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = (movement / 2f).coerceIn(0f, 1f),
                                modifier = Modifier.fillMaxWidth(0.8f).height(6.dp).clip(shape = RoundedCornerShape(3.dp)),
                                color = if (movement < 0.35f) GreenGood else OrangeWarn,
                                trackColor = CardDarker
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (movement < 0.35f) "Currently still" else "Currently moving",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (movement < 0.35f) GreenGood else OrangeWarn,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Reading updates every ~5s from the accelerometer",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { vm.stopSleepTracking(context) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        border = BorderStroke(width = 1.dp, color = RedAlert.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop", tint = RedAlert)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "STOP & SEE RESULTS", fontWeight = FontWeight.Bold, color = RedAlert)
                    }
                }

                SleepSessionPhase.COMPLETE -> {
                    val s = summary
                    if (s == null) {
                        Text(text = "No data was recorded for this session.", color = TextMuted)
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CardDark),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(width = 1.dp, color = TealAccent.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "SESSION SUMMARY", style = MaterialTheme.typography.labelLarge, color = TextMuted, letterSpacing = 1.5.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                SumRow(label = "Total tracked", value = "${s.totalMinutes / 60}h ${s.totalMinutes % 60}m", color = TextPrimary)
                                Spacer(modifier = Modifier.height(6.dp))
                                SumRow(label = "Estimated still time", value = "${s.estimatedSleepMinutes / 60}h ${s.estimatedSleepMinutes % 60}m", color = TealAccent)
                                Spacer(modifier = Modifier.height(6.dp))
                                SumRow(label = "Estimated restless time", value = "${s.estimatedAwakeMinutes / 60}h ${s.estimatedAwakeMinutes % 60}m", color = OrangeWarn)
                                Spacer(modifier = Modifier.height(6.dp))
                                SumRow(label = "Restless periods", value = "${s.restlessPeriods}", color = TextSecondary)
                                Spacer(modifier = Modifier.height(6.dp))
                                SumRow(label = "Stillness score", value = "${s.sleepQuality}/100", color = GreenGood)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "\"Estimated still time\" is a movement proxy for sleep, not a verified sleep-stage measurement.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { vm.resetSleepSummary() },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(text = "TRACK ANOTHER NIGHT", fontWeight = FontWeight.Bold, color = BackgroundDark)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SumRow(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label: ", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = color, modifier = Modifier.weight(1f))
    }
}
