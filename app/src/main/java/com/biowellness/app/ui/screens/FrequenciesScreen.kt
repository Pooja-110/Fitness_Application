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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.biowellness.app.data.models.AudioItem
import com.biowellness.app.ui.theme.*
import com.biowellness.app.viewmodel.WellnessViewModel

@Composable
fun FrequenciesScreen(nav: NavHostController, vm: WellnessViewModel) {
    val report      by vm.report.collectAsState()
    val playingName by vm.playingName.collectAsState()
    val volume      by vm.volume.collectAsState()

    var activeBreathing by remember { mutableStateOf(false) }
    var breathPhase     by remember { mutableStateOf("PREPARE") }
    var breathCount     by remember { mutableStateOf(0) }
    var selectedTab     by remember { mutableStateOf(0) }

    LaunchedEffect(key1 = activeBreathing) {
        if (activeBreathing) {
            val phases = listOf("INHALE" to 4000L, "HOLD" to 4000L, "EXHALE" to 4000L, "HOLD" to 4000L)
            while (activeBreathing) {
                phases.forEach { (phase, dur) ->
                    if (!activeBreathing) return@LaunchedEffect
                    breathPhase = phase
                    kotlinx.coroutines.delay(dur)
                }
                breathCount++
            }
        } else {
            breathPhase = "PREPARE"
            breathCount = 0
        }
    }

    val breathInf   = rememberInfiniteTransition(label = "breath")
    val breathScale by breathInf.animateFloat(
        initialValue = 0.72f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathSc"
    )

    val r = report
    val tabs = listOf("Binaural", "Solfeggio", "Nature", "Noise")

    Column(modifier = Modifier.fillMaxSize().background(color = BackgroundDark)) {
        TopBar(title = "Recovery Frequencies", onBack = { nav.popBackStack() })

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {

            // Breathing exercise
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF0A2040), Color(0xFF050D1A)),
                            radius = 700f
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(width = 1.dp, color = TealAccent.copy(alpha = 0.3f), shape = RoundedCornerShape(20.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Air, contentDescription = "Breath", tint = TealAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "GUIDED BREATHING", style = MaterialTheme.typography.labelLarge, color = TealAccent, letterSpacing = 2.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = r?.breathingCadence ?: "Box Breathing (4-4-4-4)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // Animated circle
                    Box(contentAlignment = Alignment.Center) {
                        val animScale = if (activeBreathing) breathScale else 0.85f
                        Box(
                            modifier = Modifier
                                .size((168f * animScale).dp)
                                .background(color = TealAccent.copy(alpha = 0.04f), shape = CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size((124f * animScale).dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(TealAccent.copy(alpha = 0.18f), TealAccent.copy(alpha = 0.04f))
                                    ),
                                    shape = CircleShape
                                )
                                .border(width = 2.dp, color = TealAccent.copy(alpha = 0.45f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (activeBreathing) breathPhase else "START",
                                    color = if (activeBreathing) TealAccent else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    letterSpacing = 1.sp
                                )
                                if (activeBreathing && breathCount > 0) {
                                    Text(text = "Cycle $breathCount", color = TextMuted, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    val btnColor = if (activeBreathing) RedAlert else TealAccent
                    Button(
                        onClick = { activeBreathing = !activeBreathing },
                        colors = ButtonDefaults.buttonColors(containerColor = btnColor.copy(alpha = 0.12f)),
                        border = BorderStroke(width = 1.dp, color = btnColor.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = if (activeBreathing) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "Toggle",
                            modifier = Modifier.size(18.dp),
                            tint = btnColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (activeBreathing) "STOP" else "BEGIN BREATHING",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = btnColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Volume / now playing
            if (playingName != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(width = 1.dp, color = CyanPrimary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Vol", tint = CyanPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NOW PLAYING: $playingName",
                                style = MaterialTheme.typography.labelLarge,
                                color = CyanPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { vm.stopAudio() }, modifier = Modifier.size(36.dp)) {
                                Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop", tint = RedAlert, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.VolumeDown, contentDescription = "VDown", tint = TextMuted, modifier = Modifier.size(16.dp))
                            Slider(
                                value = volume,
                                onValueChange = { vm.setVolume(it) },
                                valueRange = 0f..1f,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = CyanPrimary,
                                    activeTrackColor = CyanPrimary,
                                    inactiveTrackColor = CardDarker
                                )
                            )
                            Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "VUp", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Tab row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = CardDark,
                contentColor = CyanPrimary,
                edgePadding = 0.dp,
                modifier = Modifier.clip(shape = RoundedCornerShape(12.dp))
            ) {
                tabs.forEachIndexed { i, tab ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        text = {
                            Text(
                                text = tab,
                                fontWeight = if (selectedTab == i) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val tabColor = when (selectedTab) { 0 -> CyanPrimary; 1 -> PurpleAccent; 2 -> GreenGood; else -> TealAccent }
            val tabDesc  = when (selectedTab) {
                0 -> "Use stereo headphones. Binaural beats require both channels at low volume. Evidence for their effects is limited — treat this as a relaxation aid, not a treatment."
                1 -> "Ambient tones, best experienced with headphones in a quiet space. \"Healing frequency\" claims for these tones aren't scientifically supported — they're offered here simply as calming background audio."
                2 -> "Ambient nature soundscapes some people find calming for rest and focus."
                else -> "Coloured noise for focus, sleep, and environmental distraction masking."
            }
            Text(
                text = tabDesc,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            val currentList: List<AudioItem> = when (selectedTab) {
                0 -> r?.binauralItems   ?: defaultBinaural()
                1 -> r?.solfeggioItems  ?: defaultSolfeggio()
                2 -> r?.natureItems     ?: defaultNature()
                3 -> r?.noiseItems      ?: defaultNoise()
                else -> emptyList()
            }

            currentList.forEach { item ->
                AudioCard(
                    item = item,
                    isPlaying = playingName == item.name,
                    primaryColor = tabColor,
                    onPlayToggle = { if (playingName == item.name) vm.stopAudio() else vm.playAudio(item) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Meditation guide
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(width = 1.dp, color = PurpleAccent.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.SelfImprovement, contentDescription = "Med", tint = PurpleAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MEDITATION GUIDE",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    listOf(
                        "Find a comfortable seated or lying position in a quiet space",
                        "Set a timer for 10–20 minutes. Begin with natural breathing",
                        "Play the recommended frequency at low volume with headphones",
                        "Focus attention on your breath — when thoughts arise, gently return",
                        "End slowly: take 3 deep breaths before opening your eyes",
                        "Consistency: 10 min daily yields measurably better HRV in 4–8 weeks"
                    ).forEachIndexed { i, text ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(color = PurpleAccent.copy(alpha = 0.15f), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "${i + 1}", color = PurpleAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 18.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Disclaimer
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1628)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "These frequencies are optional relaxation tools only. They do not cure, treat, or diagnose any medical condition. Always consult a healthcare professional for medical concerns.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        lineHeight = 17.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AudioCard(item: AudioItem, isPlaying: Boolean, primaryColor: Color, onPlayToggle: () -> Unit) {
    val inf   = rememberInfiniteTransition(label = "ac${item.name}")
    val scale by inf.animateFloat(
        initialValue = 1f, targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "acSc"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isPlaying) Modifier.scale(scale) else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) primaryColor.copy(alpha = 0.10f) else CardDark
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isPlaying) primaryColor.copy(alpha = 0.5f) else primaryColor.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = primaryColor.copy(alpha = if (isPlaying) 0.2f else 0.08f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.VolumeUp else Icons.Default.GraphicEq,
                    contentDescription = "Play",
                    tint = primaryColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name,     style = MaterialTheme.typography.titleSmall,  color = TextPrimary,   fontWeight = FontWeight.SemiBold)
                Text(text = item.frequency, style = MaterialTheme.typography.labelLarge, color = primaryColor)
                Text(text = item.benefit,  style = MaterialTheme.typography.bodyMedium,  color = TextSecondary, lineHeight = 16.sp)
            }
            IconButton(onClick = onPlayToggle) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Toggle",
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun defaultBinaural() = listOf(
    AudioItem("Delta Waves",  "0.5–4 Hz",  "Low-frequency tone some use for deep relaxation"),
    AudioItem("Theta Waves",  "4–8 Hz",    "Often used during meditation"),
    AudioItem("Alpha Waves",  "8–14 Hz",   "Relaxed, calm alertness"),
    AudioItem("Low Beta",     "14–20 Hz",  "Gentle background focus tone"),
    AudioItem("Gamma Waves",  "30–100 Hz", "Higher-frequency tone for active listening")
)

// Frequencies are real; the numerology/"healing" claims historically
// attached to solfeggio tones (e.g. "528 Hz = love frequency") aren't
// scientifically supported, so descriptions stick to what's actually
// true: these are tones some listeners find pleasant or calming.
private fun defaultSolfeggio() = listOf(
    AudioItem("396 Hz Tone",     "396 Hz", "A low, grounding tone some find calming"),
    AudioItem("528 Hz Tone",     "528 Hz", "A mid tone popular in relaxation playlists"),
    AudioItem("Natural Tuning",  "432 Hz", "An alternative tuning some prefer over standard 440 Hz"),
    AudioItem("639 Hz Tone",     "639 Hz", "A mid-range ambient background tone"),
    AudioItem("963 Hz Tone",     "963 Hz", "A high tone used in ambient relaxation tracks"),
    AudioItem("Schumann Resonance",  "7.83 Hz","Named after Earth's ELF resonance; used as a low ambient tone")
)

private fun defaultNature() = listOf(
    AudioItem("Rain",          "Continuous",    "A steady ambient sound many find calming"),
    AudioItem("Forest Sounds", "Broadband",     "Ambient outdoor soundscape"),
    AudioItem("Ocean Waves",   "~0.1 Hz rhythm","Rhythmic wave sound often used for relaxation"),
    AudioItem("Birdsong",      "Variable",      "Ambient outdoor sound some find calming"),
    AudioItem("Tibetan Bowls", "Harmonics",     "Resonant tones often used during meditation")
)

private fun defaultNoise() = listOf(
    AudioItem("Brown Noise", "Low-frequency heavy", "Deep, low-frequency masking noise"),
    AudioItem("Pink Noise",  "Balanced spectrum",   "Balanced-spectrum masking noise, popular for sleep"),
    AudioItem("White Noise", "Flat spectrum",       "Flat-spectrum noise for focus and distraction masking"),
    AudioItem("Grey Noise",  "Equal loudness",       "Perceptually flat masking, gentler on sensitive ears")
)
