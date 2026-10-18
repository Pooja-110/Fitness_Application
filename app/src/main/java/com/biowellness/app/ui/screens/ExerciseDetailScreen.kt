package com.biowellness.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.biowellness.app.ui.theme.*
import com.biowellness.app.utils.Equipment
import com.biowellness.app.utils.ExerciseLibrary

@Composable
fun ExerciseDetailScreen(nav: NavHostController, exerciseId: String) {
    val ex = ExerciseLibrary.find(exerciseId)

    Column(modifier = Modifier.fillMaxSize().background(color = BackgroundDark)) {
        TopBar(title = ex?.name ?: "Exercise", onBack = { nav.popBackStack() })

        if (ex == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Exercise not found", color = TextMuted)
            }
        } else {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1F1C)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(width = 1.dp, color = TealAccent.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ExercisePosePair(start = ex.startPose, end = ex.endPose, modifier = Modifier.fillMaxWidth().height(150.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Illustrative form diagram — start position → end position",
                            style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(text = "Works: ${ex.primaryMuscles.joinToString(", ")}", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Equipment: ${if (ex.equipment == Equipment.NONE) "none" else "dumbbells"}", color = TextMuted, fontSize = 13.sp)

                Spacer(modifier = Modifier.height(16.dp))
                Section("SETS & REPS", TealAccent) {
                    Text(text = ex.setsRepsGuidance, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 19.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Section("HOW TO DO IT", TealAccent) {
                    ex.instructions.forEachIndexed { i, step ->
                        Row(modifier = Modifier.padding(vertical = 3.dp)) {
                            Text(text = "${i + 1}. ", color = TealAccent, fontWeight = FontWeight.Bold)
                            Text(text = step, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 19.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Section("FORM CUES", GreenGood) {
                    ex.formCues.forEach { cue ->
                        Row(modifier = Modifier.padding(vertical = 3.dp)) {
                            Text(text = "✓ ", color = GreenGood)
                            Text(text = cue, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 19.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Section("COMMON MISTAKES", OrangeWarn) {
                    ex.commonMistakes.forEach { m ->
                        Row(modifier = Modifier.padding(vertical = 3.dp)) {
                            Text(text = "✗ ", color = OrangeWarn)
                            Text(text = m, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 19.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Stop if you feel sharp pain, dizziness or chest discomfort. This is general fitness education, not a personalised programme — if you're new to exercise, pregnant, or managing an injury or health condition, check with a doctor first.",
                    style = MaterialTheme.typography.labelSmall, color = TextMuted, lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun Section(title: String, accent: Color, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(width = 1.dp, color = accent.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = accent, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}
