package com.biowellness.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.biowellness.app.ui.theme.*
import com.biowellness.app.utils.*
import com.biowellness.app.viewmodel.WellnessViewModel

@Composable
fun GymScreen(nav: NavHostController, @Suppress("UNUSED_PARAMETER") vm: WellnessViewModel) {
    var selected by remember { mutableStateOf<ExerciseCategory?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(color = BackgroundDark)) {
        TopBar(title = "Gym & Exercises", onBack = { nav.popBackStack() })

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "General fitness education — not personalised medical or physical-therapy advice. If you're new to exercise, have an injury or a health condition, check with a doctor first.",
                style = MaterialTheme.typography.labelSmall, color = TextMuted, lineHeight = 15.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                CategoryChip(label = "All", selected = selected == null) { selected = null }
                ExerciseCategory.values().forEach { cat ->
                    CategoryChip(label = categoryName(cat), selected = selected == cat) { selected = cat }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            val list = selected?.let { ExerciseLibrary.byCategory(it) } ?: ExerciseLibrary.all
            list.forEach { ex ->
                ExerciseRow(ex) { nav.navigate("exercise_detail/${ex.id}") }
                Spacer(modifier = Modifier.height(10.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun categoryName(c: ExerciseCategory) = when (c) {
    ExerciseCategory.LEGS -> "Legs"
    ExerciseCategory.CHEST -> "Chest"
    ExerciseCategory.BACK -> "Back"
    ExerciseCategory.ARMS -> "Arms"
    ExerciseCategory.SHOULDERS -> "Shoulders"
    ExerciseCategory.CORE -> "Core"
    ExerciseCategory.CARDIO -> "Cardio"
    ExerciseCategory.FULL_BODY -> "Full Body"
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(shape = RoundedCornerShape(20.dp))
            .background(color = if (selected) TealAccent else CardDark)
            .border(width = 1.dp, color = if (selected) TealAccent else TextMuted.copy(alpha = 0.25f), shape = RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text = label, color = if (selected) BackgroundDark else TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun ExerciseRow(ex: Exercise, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(width = 1.dp, color = TealAccent.copy(alpha = 0.15f))
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).clip(shape = RoundedCornerShape(10.dp)).background(color = BackgroundDark),
                contentAlignment = Alignment.Center
            ) {
                ExerciseIllustration(poseKey = ex.endPose, modifier = Modifier.size(44.dp), color = TealAccent)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = ex.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(3.dp))
                Text(text = ex.primaryMuscles.joinToString(" · "), color = TextMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(3.dp))
                Row {
                    val (label, color) = when (ex.difficulty) {
                        Difficulty.BEGINNER -> "Beginner" to GreenGood
                        Difficulty.INTERMEDIATE -> "Intermediate" to OrangeWarn
                        Difficulty.ADVANCED -> "Advanced" to RedAlert
                    }
                    Text(text = label, fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold)
                    if (ex.equipment == Equipment.DUMBBELLS) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Dumbbells", fontSize = 11.sp, color = TextMuted)
                    }
                }
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Open", tint = TextMuted)
        }
    }
}
