package com.biowellness.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.sp
import com.biowellness.app.utils.PoseKey

/**
 * Simple vector line-art stick-figure diagrams of exercise start/end
 * positions. These are illustrative diagrams drawn from fixed
 * coordinates — NOT photographs.
 */
private data class BodyPose(
    val head: Offset, val neck: Offset, val hip: Offset,
    val elbow: Offset, val hand: Offset, val knee: Offset, val foot: Offset,
    val elbow2: Offset? = null, val hand2: Offset? = null,
    val knee2: Offset? = null, val foot2: Offset? = null
)

private fun o(x: Float, y: Float) = Offset(x, y)

private fun pose(key: PoseKey): BodyPose = when (key) {
    PoseKey.STANDING -> BodyPose(o(.5f,.12f), o(.5f,.22f), o(.5f,.55f), o(.35f,.38f), o(.3f,.5f), o(.45f,.75f), o(.42f,.95f), o(.65f,.38f), o(.7f,.5f), o(.55f,.75f), o(.58f,.95f))
    PoseKey.ARMS_DOWN -> BodyPose(o(.5f,.12f), o(.5f,.22f), o(.5f,.55f), o(.38f,.45f), o(.35f,.58f), o(.45f,.75f), o(.42f,.95f), o(.62f,.45f), o(.65f,.58f), o(.55f,.75f), o(.58f,.95f))
    PoseKey.ARMS_UP_OUT -> BodyPose(o(.5f,.14f), o(.5f,.24f), o(.5f,.55f), o(.3f,.2f), o(.15f,.08f), o(.35f,.75f), o(.25f,.95f), o(.7f,.2f), o(.85f,.08f), o(.65f,.75f), o(.75f,.95f))
    PoseKey.SQUAT_DOWN -> BodyPose(o(.5f,.32f), o(.5f,.4f), o(.5f,.62f), o(.3f,.5f), o(.15f,.55f), o(.35f,.75f), o(.32f,.95f), o(.7f,.5f), o(.85f,.55f), o(.65f,.75f), o(.68f,.95f))
    PoseKey.LUNGE_FORWARD -> BodyPose(o(.55f,.12f), o(.55f,.22f), o(.55f,.5f), o(.45f,.35f), o(.4f,.48f), o(.65f,.68f), o(.78f,.92f), knee2 = o(.4f,.75f), foot2 = o(.22f,.95f))
    PoseKey.PLANK -> BodyPose(o(.1f,.35f), o(.22f,.42f), o(.65f,.48f), o(.22f,.6f), o(.22f,.78f), o(.85f,.5f), o(.95f,.55f))
    PoseKey.PUSHUP_DOWN -> BodyPose(o(.1f,.55f), o(.22f,.58f), o(.65f,.62f), o(.16f,.68f), o(.22f,.78f), o(.85f,.64f), o(.95f,.66f))
    PoseKey.LYING_FLAT -> BodyPose(o(.15f,.55f), o(.25f,.55f), o(.55f,.55f), o(.25f,.62f), o(.2f,.68f), o(.7f,.4f), o(.85f,.55f))
    PoseKey.BRIDGE_UP -> BodyPose(o(.15f,.55f), o(.25f,.55f), o(.55f,.38f), o(.25f,.62f), o(.2f,.68f), o(.7f,.45f), o(.85f,.6f))
    PoseKey.SITUP_UP -> BodyPose(o(.38f,.35f), o(.43f,.4f), o(.55f,.55f), o(.4f,.42f), o(.46f,.42f), o(.68f,.4f), o(.82f,.55f))
    PoseKey.HINGE_DEADLIFT -> BodyPose(o(.25f,.28f), o(.3f,.35f), o(.55f,.55f), o(.35f,.5f), o(.4f,.7f), o(.55f,.75f), o(.5f,.95f))
    PoseKey.ARM_CURL_UP -> BodyPose(o(.5f,.12f), o(.5f,.22f), o(.5f,.55f), o(.35f,.42f), o(.38f,.26f), o(.45f,.75f), o(.42f,.95f), o(.65f,.42f), o(.62f,.26f), o(.55f,.75f), o(.58f,.95f))
    PoseKey.SHOULDER_PRESS_UP -> BodyPose(o(.5f,.14f), o(.5f,.24f), o(.5f,.55f), o(.35f,.25f), o(.32f,.08f), o(.45f,.75f), o(.42f,.95f), o(.65f,.25f), o(.68f,.08f), o(.55f,.75f), o(.58f,.95f))
    PoseKey.MOUNTAIN_CLIMBER -> BodyPose(o(.1f,.35f), o(.22f,.4f), o(.55f,.45f), o(.22f,.58f), o(.22f,.75f), o(.45f,.55f), o(.4f,.65f), knee2 = o(.75f,.46f), foot2 = o(.9f,.5f))
}

private fun DrawScope.drawPose(p: BodyPose, color: Color) {
    val w = size.width
    val h = size.height
    fun pt(v: Offset) = Offset(v.x * w, v.y * h)
    val stroke = (w * 0.045f).coerceAtLeast(4f)
    fun line(a: Offset, b: Offset) =
        drawLine(color = color, start = pt(a), end = pt(b), strokeWidth = stroke, cap = StrokeCap.Round)

    line(p.neck, p.hip)
    line(p.neck, p.elbow); line(p.elbow, p.hand)
    if (p.elbow2 != null && p.hand2 != null) { line(p.neck, p.elbow2); line(p.elbow2, p.hand2) }
    line(p.hip, p.knee); line(p.knee, p.foot)
    if (p.knee2 != null && p.foot2 != null) { line(p.hip, p.knee2); line(p.knee2, p.foot2) }
    drawCircle(color = color, radius = w * 0.07f, center = pt(p.head))
}

@Composable
fun ExerciseIllustration(poseKey: PoseKey, modifier: Modifier = Modifier, color: Color = Color(0xFF2DD4BF)) {
    Canvas(modifier = modifier) { drawPose(pose(poseKey), color) }
}

/** Start pose and end pose side by side with an arrow between them. */
@Composable
fun ExercisePosePair(start: PoseKey, end: PoseKey, modifier: Modifier = Modifier, color: Color = Color(0xFF2DD4BF)) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        ExerciseIllustration(poseKey = start, modifier = Modifier.weight(1f).aspectRatio(1f), color = color)
        Text(text = "→", fontSize = 22.sp, color = color)
        ExerciseIllustration(poseKey = end, modifier = Modifier.weight(1f).aspectRatio(1f), color = color)
    }
}
