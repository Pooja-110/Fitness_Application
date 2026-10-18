package com.biowellness.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biowellness.app.data.models.SignalQuality
import com.biowellness.app.ui.theme.*

// ── Quality helpers ────────────────────────────────────────────────────────

fun qualityColor(q: SignalQuality): Color = when (q) {
    SignalQuality.EXCELLENT -> GreenGood
    SignalQuality.GOOD      -> TealAccent
    SignalQuality.MODERATE  -> OrangeWarn
    SignalQuality.POOR      -> RedAlert
    else                    -> TextMuted
}

// ── Top bar ────────────────────────────────────────────────────────────────

@Composable
fun TopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = TextSecondary
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        actions()
    }
}

// ── Section chip ───────────────────────────────────────────────────────────

@Composable
fun Chip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Collapsible section card ───────────────────────────────────────────────

@Composable
fun SectionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(14.dp))
                content()
            }
        }
    }
}

// ── Score circle ───────────────────────────────────────────────────────────

@Composable
fun ScoreCircle(label: String, value: Int, color: Color, size: Int = 72) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(size.dp)
                .background(color = color.copy(alpha = 0.10f), shape = CircleShape)
                .border(
                    width = 2.dp,
                    color = color.copy(alpha = 0.45f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$value",
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size * 0.29f).sp
                )
                if (size >= 70) {
                    Text(
                        text = "/100",
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

// ── Progress bar row ───────────────────────────────────────────────────────

@Composable
fun BarRow(label: String, value: Int, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Text(
                text = "$value%",
                style = MaterialTheme.typography.labelLarge,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = value / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = CardDarker
        )
    }
}

// ── Signal quality badge ───────────────────────────────────────────────────

@Composable
fun QualityBadge(quality: SignalQuality, modifier: Modifier = Modifier) {
    val color = qualityColor(quality)
    Box(
        modifier = modifier
            .background(color = BackgroundDark.copy(alpha = 0.85f), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color = color, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = quality.name,
                color = color,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── PPG waveform card ──────────────────────────────────────────────────────

@Composable
fun WaveformCard(
    samples: List<Float>,
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    onDrawBehind {
                        if (samples.size < 2) return@onDrawBehind
                        val path  = Path()
                        val min   = samples.min()
                        val max   = samples.max()
                        val range = (max - min).coerceAtLeast(1f)
                        samples.forEachIndexed { i, v ->
                            val x = i * size.width / (samples.size - 1)
                            val y = size.height - ((v - min) / range) * size.height * 0.8f - size.height * 0.1f
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path = path, color = color, style = Stroke(width = 2f))
                    }
                }
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                letterSpacing = 1.sp
            )
        }
    }
}

// ── Vital metric card (for dashboard) ─────────────────────────────────────

@Composable
fun VitalCard(
    label: String,
    value: String,
    unit: String,
    icon: ImageVector,
    color: Color,
    conf: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 9.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = (conf / 100f).coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = color.copy(alpha = 0.7f),
                trackColor = CardDarker
            )
            Text(
                text = "Conf: $conf%",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 9.sp
            )
        }
    }
}

// ── Bullet list ────────────────────────────────────────────────────────────

@Composable
fun BulletList(title: String, items: List<String>, color: Color) {
    if (items.isEmpty()) return
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = color,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(5.dp))
    items.forEach { item ->
        Row(modifier = Modifier.padding(vertical = 2.dp)) {
            Text(text = "› ", color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                text = item,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}
