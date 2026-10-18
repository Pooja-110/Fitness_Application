package com.biowellness.app.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.biowellness.app.data.models.RiskSeverity
import com.biowellness.app.ui.theme.*
import com.biowellness.app.viewmodel.WellnessViewModel

@Composable
fun ReportScreen(nav: NavHostController, vm: WellnessViewModel) {
    val report   by vm.report.collectAsState()
    val analysis by vm.liveAnalysis.collectAsState()
    val liveData by vm.liveData.collectAsState()

    if (report == null) {
        Box(modifier = Modifier.fillMaxSize().background(color = BackgroundDark), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.Assessment, contentDescription = "No report", tint = TextMuted, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "No report yet", color = TextMuted, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { nav.navigate(Route.SCAN) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)) {
                    Text(text = "Run a Scan", color = BackgroundDark)
                }
            }
        }
        return
    }

    val r = report!!
    var expanded by remember { mutableStateOf<String?>("summary") }

    Column(modifier = Modifier.fillMaxSize().background(color = BackgroundDark)) {
        TopBar(
            title = "Wellness Report",
            // Back goes to Home (popUpTo already cleared Scan from stack)
            onBack = { nav.navigate(Route.HOME) { popUpTo(Route.HOME) { inclusive = false } } },
            actions = {
                IconButton(onClick = { nav.navigate(Route.FREQ) }) {
                    Icon(imageVector = Icons.Default.GraphicEq, contentDescription = "Freq", tint = TealAccent)
                }
            }
        )

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {

            // Hero summary
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(brush = Brush.linearGradient(listOf(Color(0xFF061428), Color(0xFF0A1F3C))), shape = RoundedCornerShape(20.dp))
                    .border(width = 1.dp, color = CyanPrimary.copy(alpha = 0.25f), shape = RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.HealthAndSafety, contentDescription = "Sum",
                            tint = CyanPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "SCAN SUMMARY", style = MaterialTheme.typography.labelLarge, color = TextMuted, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Chip(text = "${r.scanSeconds}s", color = CyanPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Chip(text = "Sensor-Only", color = TealAccent)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        ScoreCircle(label = "Wellness", value = analysis.wellnessScore, color = CyanPrimary, size = 72)
                        ScoreCircle(label = "Stress",   value = analysis.stressLevel,
                            color = if (analysis.stressLevel > 60) RedAlert else OrangeWarn, size = 72)
                        ScoreCircle(label = "Focus",    value = analysis.focusLevel, color = TealAccent, size = 72)
                        ScoreCircle(label = "Relax",    value = analysis.relaxationScore, color = GreenGood, size = 72)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = Color(0xFF1A3A5C))
                    Spacer(modifier = Modifier.height(10.dp))
                    SumRow(label = "Emotion",    value = analysis.emotionalState,                                            color = PurpleAccent)
                    Spacer(modifier = Modifier.height(5.dp))
                    SumRow(label = "Heart Rate", value = if (liveData.heartRate > 0) "${liveData.heartRate} BPM" else "—",  color = CyanPrimary)
                    Spacer(modifier = Modifier.height(5.dp))
                    SumRow(label = "HRV",        value = if (liveData.hrv > 0f) "${liveData.hrv.toInt()} ms" else "—",     color = TealAccent)
                    Spacer(modifier = Modifier.height(5.dp))
                    SumRow(label = "SpO₂",       value = "Not available on this device",                                    color = TextMuted)
                    Spacer(modifier = Modifier.height(5.dp))
                    SumRow(label = "Source",     value = "Real-Time Camera PPG (60-second window)",                         color = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // SpO₂ accuracy note
            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1628)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "Note", tint = TealAccent, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SpO₂ (blood oxygen) requires comparing RED and INFRARED light absorption — a phone's single-channel " +
                               "camera physically cannot measure it, so no oxygen saturation reading is shown. " +
                               "HR accuracy improves with a full 60-second window and stable finger placement.",
                        style = MaterialTheme.typography.bodyMedium, color = TextMuted, lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            SectionCard(title = "CARDIOVASCULAR", icon = Icons.Default.Favorite, color = CyanPrimary,
                expanded = expanded == "cardio", onToggle = { expanded = if (expanded == "cardio") null else "cardio" }) {
                Text(text = r.hrText, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 20.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))

            SectionCard(title = "HRV ANALYSIS", icon = Icons.Default.Timeline, color = TealAccent,
                expanded = expanded == "hrv", onToggle = { expanded = if (expanded == "hrv") null else "hrv" }) {
                Text(text = r.hrvText, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 20.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))

            SectionCard(title = "STRESS ANALYSIS", icon = Icons.Default.Psychology, color = RedAlert,
                expanded = expanded == "stress", onToggle = { expanded = if (expanded == "stress") null else "stress" }) {
                Text(text = r.stressText, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 20.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))

            SectionCard(title = "RECOVERY & FATIGUE", icon = Icons.Default.BatteryFull, color = GreenGood,
                expanded = expanded == "recovery", onToggle = { expanded = if (expanded == "recovery") null else "recovery" }) {
                Text(text = r.recoveryText, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 20.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = r.fatigueText, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 20.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))

            SectionCard(title = "BREATHING ANALYSIS", icon = Icons.Default.Air, color = Color(0xFF64FFDA),
                expanded = expanded == "breathing", onToggle = { expanded = if (expanded == "breathing") null else "breathing" }) {
                Text(text = r.breathingText, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 20.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))

            SectionCard(title = "SCAN INDICATORS", icon = Icons.Default.Warning, color = OrangeWarn,
                expanded = expanded == "risks", onToggle = { expanded = if (expanded == "risks") null else "risks" }) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardDarker), shape = RoundedCornerShape(10.dp)) {
                    Text(text = "⚠  Probability estimates only — not diagnostic.", modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodyMedium, color = OrangeWarn.copy(alpha = 0.85f))
                }
                Spacer(modifier = Modifier.height(10.dp))
                r.risks.forEach { risk ->
                    val rc = when (risk.severity) { RiskSeverity.HIGH -> RedAlert; RiskSeverity.MODERATE -> OrangeWarn; else -> GreenGood }
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .background(color = rc.copy(alpha = 0.06f), shape = RoundedCornerShape(10.dp))
                        .border(width = 1.dp, color = rc.copy(alpha = 0.2f), shape = RoundedCornerShape(10.dp))
                        .padding(12.dp)) {
                        Box(modifier = Modifier.size(8.dp).background(color = rc, shape = CircleShape).align(Alignment.CenterVertically))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = risk.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                                Text(text = risk.severity.name, color = rc, style = MaterialTheme.typography.labelSmall)
                            }
                            Text(text = risk.probability, color = rc.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                            risk.signals.forEach { sig -> Text(text = "• $sig", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 18.sp) }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            SectionCard(title = "RECOMMENDATIONS", icon = Icons.Default.TipsAndUpdates, color = GreenGood,
                expanded = expanded == "recs", onToggle = { expanded = if (expanded == "recs") null else "recs" }) {
                BulletList(title = "💧 Hydration",  items = r.hydrationTips,  color = CyanPrimary)
                Spacer(modifier = Modifier.height(10.dp))
                BulletList(title = "🥗 Diet",       items = r.dietTips,       color = GreenGood)
                Spacer(modifier = Modifier.height(10.dp))
                BulletList(title = "🏃 Exercise",   items = r.exerciseTips,   color = TealAccent)
                Spacer(modifier = Modifier.height(10.dp))
                BulletList(title = "😴 Sleep",      items = r.sleepTips,      color = PurpleAccent)
                Spacer(modifier = Modifier.height(10.dp))
                BulletList(title = "🌬 Breathing",  items = r.breathingTips,  color = Color(0xFF64FFDA))
                Spacer(modifier = Modifier.height(10.dp))
                BulletList(title = "🧘 Meditation", items = r.meditationTips, color = OrangeWarn)
                Spacer(modifier = Modifier.height(10.dp))
                BulletList(title = "🌿 Lifestyle",  items = r.lifestyleTips,  color = YellowColor)
            }
            Spacer(modifier = Modifier.height(8.dp))

            SectionCard(title = "RELAXATION AUDIO", icon = Icons.Default.GraphicEq, color = YellowColor,
                expanded = expanded == "audio", onToggle = { expanded = if (expanded == "audio") null else "audio" }) {
                Text(text = r.breathingCadence, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 18.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = { nav.navigate(Route.FREQ) }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = YellowColor.copy(alpha = 0.15f)),
                    border = BorderStroke(width = 1.dp, color = YellowColor.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)) {
                    Icon(imageVector = Icons.Default.GraphicEq, contentDescription = "Freq", tint = YellowColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Open Frequencies Player", color = YellowColor)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            SectionCard(title = "WHEN TO CONSULT A PROFESSIONAL", icon = Icons.Default.MedicalServices, color = RedAlert,
                expanded = expanded == "consult", onToggle = { expanded = if (expanded == "consult") null else "consult" }) {
                r.consultFlags.forEach { flag ->
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Icon(imageVector = Icons.Default.MedicalServices, contentDescription = "Flag", tint = RedAlert, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = flag, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 18.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            SectionCard(title = "CONFIDENCE SUMMARY", icon = Icons.Default.VerifiedUser, color = TextSecondary,
                expanded = expanded == "conf", onToggle = { expanded = if (expanded == "conf") null else "conf" }) {
                Text(text = r.confidenceText, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 20.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Scan Again button — KEY FIX for back navigation
            Button(
                onClick = {
                    vm.resetScan()
                    nav.navigate(Route.SCAN) { popUpTo(Route.HOME) { inclusive = false } }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Rescan", modifier = Modifier.size(18.dp), tint = BackgroundDark)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "SCAN AGAIN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = BackgroundDark)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { nav.navigate(Route.AI_CHAT) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                border = BorderStroke(width = 1.dp, color = CyanPrimary.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI",
                    modifier = Modifier.size(18.dp), tint = CyanPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Ask AI to explain my results", color = CyanPrimary, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF071020)),
                shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(imageVector = Icons.Default.HealthAndSafety, contentDescription = "Disclaimer", tint = TextMuted, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "AI report from camera PPG. NOT a medical diagnosis. Profile NOT used for measurements.",
                        style = MaterialTheme.typography.bodyMedium, color = TextMuted, lineHeight = 17.sp)
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
