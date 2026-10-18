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

@Composable
fun HomeScreen(nav: NavHostController, vm: WellnessViewModel) {
    val scanState    by vm.scanState.collectAsState()
    val report       by vm.report.collectAsState()
    val liveData     by vm.liveData.collectAsState()
    val liveAnalysis by vm.liveAnalysis.collectAsState()
    val savedProfile by vm.savedProfile.collectAsState()

    val inf = rememberInfiniteTransition(label = "pulse")
    val sc  by inf.animateFloat(
        initialValue = 0.93f, targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = BackgroundDark)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF061428), BackgroundDark)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(color = CyanPrimary.copy(alpha = 0.15f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MonitorHeart,
                        contentDescription = "Logo",
                        tint = CyanPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "BioWellness AI",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-Time Bio-Signal Engine",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { nav.navigate(Route.PROFILE) }) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Scan button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (scanState == ScanState.IDLE || scanState == ScanState.COMPLETE) {
                Box(
                    modifier = Modifier
                        .size((196f * sc).dp)
                        .background(color = CyanPrimary.copy(alpha = 0.04f), shape = CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size((158f * sc).dp)
                        .background(color = CyanPrimary.copy(alpha = 0.07f), shape = CircleShape)
                )
            }
            val btnColor = if (scanState == ScanState.COMPLETE) TealAccent else CyanPrimary
            Button(
                onClick = {
                    if (scanState == ScanState.COMPLETE) vm.resetScan()
                    nav.navigate(Route.SCAN)
                },
                modifier = Modifier.size(132.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = btnColor.copy(alpha = 0.12f)
                ),
                border = BorderStroke(width = 2.dp, color = btnColor)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (scanState == ScanState.COMPLETE)
                            Icons.Default.CheckCircle else Icons.Default.Fingerprint,
                        contentDescription = "Scan",
                        modifier = Modifier.size(40.dp),
                        tint = btnColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (scanState == ScanState.COMPLETE) "RE-SCAN" else "SCAN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = btnColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HomeStatCard(
                label = "HEART RATE",
                value = if (liveData.heartRate > 0) "${liveData.heartRate} BPM" else "—",
                icon = Icons.Default.Favorite,
                color = CyanPrimary,
                modifier = Modifier.weight(1f)
            )
            HomeStatCard(
                label = "STRESS",
                value = if (liveAnalysis.wellnessScore > 0) "${liveAnalysis.stressLevel}" else "—",
                icon = Icons.Default.Psychology,
                color = if (liveAnalysis.stressLevel > 60) RedAlert
                        else if (liveAnalysis.stressLevel > 30) OrangeWarn else GreenGood,
                modifier = Modifier.weight(1f)
            )
            HomeStatCard(
                label = "STATUS",
                value = when (scanState) {
                    ScanState.IDLE      -> "READY"
                    ScanState.SCANNING  -> "LIVE"
                    ScanState.ANALYZING -> "AI"
                    ScanState.COMPLETE  -> "DONE"
                    ScanState.ERROR     -> "ERR"
                },
                icon = Icons.Default.Sensors,
                color = when (scanState) {
                    ScanState.COMPLETE  -> TealAccent
                    ScanState.SCANNING  -> GreenGood
                    ScanState.ANALYZING -> OrangeWarn
                    else -> TextSecondary
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Report summary
        if (report != null) {
            val r = report!!
            val a = r
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(width = 1.dp, color = CyanPrimary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = "Report",
                            tint = CyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Latest Report",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Chip(text = "${r.scanSeconds}s scan", color = CyanPrimary)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ScoreCircle(label = "Wellness", value = liveAnalysis.wellnessScore, color = CyanPrimary, modifier = Modifier.weight(1f))
                        ScoreCircle(label = "Stress",   value = liveAnalysis.stressLevel,
                            color = if (liveAnalysis.stressLevel > 60) RedAlert else OrangeWarn, modifier = Modifier.weight(1f))
                        ScoreCircle(label = "Focus",    value = liveAnalysis.focusLevel,  color = TealAccent, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { nav.navigate(Route.REPORT) },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(width = 1.dp, color = CyanPrimary.copy(alpha = 0.4f))
                        ) {
                            Text(text = "Full Report", fontSize = 12.sp, color = CyanPrimary)
                        }
                        OutlinedButton(
                            onClick = { nav.navigate(Route.FREQ) },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(width = 1.dp, color = TealAccent.copy(alpha = 0.4f))
                        ) {
                            Text(text = "Frequencies", fontSize = 12.sp, color = TealAccent)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Module grid
        Text(
            text = "ANALYSIS MODULES",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = TextMuted,
            letterSpacing = 2.sp
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModuleCard("Gym & Exercises", Icons.Default.FitnessCenter, "Illustrated form guides", GreenGood, Modifier.weight(1f)) { nav.navigate(Route.GYM) }
                ModuleCard("AI Coach", Icons.Default.AutoAwesome, "Ask about workouts & health", CyanPrimary, Modifier.weight(1f)) { nav.navigate(Route.AI_CHAT) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModuleCard("Heart Rate", Icons.Default.Favorite, "Camera PPG measurement", RedAlert, Modifier.weight(1f)) { nav.navigate(Route.SCAN) }
                ModuleCard("HR Strap / Watch", Icons.Default.Bluetooth, "Live data over Bluetooth", TealAccent, Modifier.weight(1f)) { nav.navigate(Route.BLE_HR) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModuleCard("Sleep & Rest", Icons.Default.Bedtime, "Motion-based tracker", PurpleAccent, Modifier.weight(1f)) { nav.navigate(Route.SLEEP) }
                ModuleCard("BP Log", Icons.Default.MonitorHeart, "Manual cuff readings", OrangeWarn, Modifier.weight(1f)) { nav.navigate(Route.BP_LOG) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModuleCard("Live Dashboard", Icons.Default.Dashboard, "Real-time metrics", TealAccent, Modifier.weight(1f)) { nav.navigate(Route.DASHBOARD) }
                ModuleCard("Recovery Sounds", Icons.Default.GraphicEq, "Cool-down audio", TealAccent, Modifier.weight(1f)) { nav.navigate(Route.FREQ) }
            }
            ModuleCard("Wellness Report", Icons.Default.Assessment, "From your latest heart-rate scan", GreenGood, Modifier.fillMaxWidth()) {
                nav.navigate(if (report != null) Route.REPORT else Route.SCAN)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Profile banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { nav.navigate(Route.PROFILE) },
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                width = 1.dp,
                color = if (savedProfile.isEmpty) OrangeWarn.copy(alpha = 0.35f) else Color(0xFF1A3A5C)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = if (savedProfile.isEmpty) OrangeWarn else CyanPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (savedProfile.isEmpty) "No profile saved" else "Profile saved",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (savedProfile.isEmpty) OrangeWarn else TextPrimary
                    )
                    Text(
                        text = if (savedProfile.isEmpty)
                            "Add profile for personalised recommendations"
                        else
                            "Age: ${savedProfile.age.ifBlank { "—" }}  ·  ${savedProfile.gender.ifBlank { "—" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Go",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Disclaimer
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1628)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = "Ethics",
                    tint = TextMuted,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "All analysis is driven by live sensor data only. " +
                           "Profile personalises text — never physiological measurements. " +
                           "This app does not diagnose disease.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    lineHeight = 17.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun HomeStatCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(width = 1.dp, color = color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 8.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun ScoreCircle(label: String, value: Int, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(color = color.copy(alpha = 0.10f), shape = CircleShape)
                .border(width = 2.dp, color = color.copy(alpha = 0.4f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$value",
                style = MaterialTheme.typography.titleMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}

@Composable
fun ModuleCard(title: String, icon: ImageVector, subtitle: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(width = 1.dp, color = color.copy(alpha = 0.18f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}
