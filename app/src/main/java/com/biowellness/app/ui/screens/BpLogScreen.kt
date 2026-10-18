package com.biowellness.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.biowellness.app.data.models.BpLogEntry
import com.biowellness.app.ui.theme.*
import com.biowellness.app.viewmodel.WellnessViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BpLogScreen(nav: NavHostController, vm: WellnessViewModel) {
    val entries by vm.bpEntries.collectAsState()
    var sys by remember { mutableStateOf("") }
    var dia by remember { mutableStateOf("") }
    var pulse by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(color = BackgroundDark)) {
        TopBar(title = "Blood Pressure Log", onBack = { nav.popBackStack() })

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1628)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = TealAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "This app can't measure blood pressure — no phone or watch sensor can do that reliably without a calibrated cuff. This is a manual log: enter readings from your own validated BP monitor to track the trend over time.",
                        style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 18.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(width = 1.dp, color = TealAccent.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "ADD READING", style = MaterialTheme.typography.labelLarge, color = TealAccent, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = sys, onValueChange = { sys = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Systolic") }, modifier = Modifier.weight(1f), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = dia, onValueChange = { dia = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Diastolic") }, modifier = Modifier.weight(1f), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = pulse, onValueChange = { pulse = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Pulse") }, modifier = Modifier.weight(1f), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    val s = sys.toIntOrNull()
                    val d = dia.toIntOrNull()
                    val valid = s != null && d != null && s in 60..260 && d in 30..180 && s > d
                    Button(
                        onClick = {
                            if (valid) {
                                vm.addBpEntry(BpLogEntry(systolic = s!!, diastolic = d!!, pulse = pulse.toIntOrNull() ?: 0))
                                sys = ""; dia = ""; pulse = ""
                            }
                        },
                        enabled = valid,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(text = "Save Reading", color = BackgroundDark, fontWeight = FontWeight.Bold) }
                    if (sys.isNotEmpty() && dia.isNotEmpty() && !valid) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Check the values: systolic should be 60–260 and higher than diastolic (30–180).",
                            style = MaterialTheme.typography.labelSmall, color = OrangeWarn)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            if (entries.isNotEmpty()) {
                val recent = entries.take(7)
                Text(text = "Average of last ${recent.size}: ${recent.map { it.systolic }.average().toInt()} / ${recent.map { it.diastolic }.average().toInt()} mmHg",
                    color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Averages are just arithmetic on what you entered. Ask a doctor how to interpret your numbers.",
                    style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Spacer(modifier = Modifier.height(14.dp))
            }

            Text(text = "HISTORY", style = MaterialTheme.typography.labelLarge, color = TextMuted, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            if (entries.isEmpty()) {
                Text(text = "No readings logged yet.", color = TextMuted, modifier = Modifier.padding(vertical = 12.dp))
            } else {
                val fmt = remember { SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault()) }
                entries.forEach { e ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "${e.systolic}/${e.diastolic} mmHg" + if (e.pulse > 0) "  ·  ${e.pulse} bpm" else "",
                                    color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                Text(text = fmt.format(Date(e.timestampMs)), color = TextMuted, fontSize = 12.sp)
                            }
                            IconButton(onClick = { vm.deleteBpEntry(e) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
