package com.biowellness.app.utils

import kotlin.math.abs

/**
 * RhythmAnalyzer
 *
 * IMPORTANT — read this before changing the wording anywhere this is used:
 *
 * This is a SCREENING heuristic, not a diagnosis, modeled on the same
 * general idea as "Irregular Rhythm Notifications" on consumer
 * wearables (Apple Watch, Fitbit, etc): look at how much consecutive
 * beat-to-beat (R-R) intervals vary, and if a large fraction of them
 * swing by more than a threshold amount across enough beats, surface
 * a prompt to get it checked by a professional. It never says what
 * condition might be present, never assigns a probability of disease,
 * and is not a replacement for an ECG or clinical exam.
 *
 * Input R-R intervals can come from either:
 *  - Camera PPG (noisier — motion, poor contact, and lighting all
 *    corrupt beat timing, so this signal alone is a weak input), or
 *  - A connected BLE heart rate strap/watch (millisecond-accurate
 *    R-R intervals — the same quality data real irregular-rhythm
 *    features are built on).
 *
 * The confidence of the flag reflects which source was used and how
 * many beats were available — camera-only data caps out at LOW.
 */
object RhythmAnalyzer {

    enum class RhythmSource { CAMERA, BLUETOOTH_DEVICE }

    data class RhythmScreeningResult(
        val flagged: Boolean,
        val irregularBeatFraction: Float, // 0f..1f
        val beatsAnalyzed: Int,
        val source: RhythmSource,
        val confidence: RhythmConfidence
    )

    enum class RhythmConfidence { LOW, MODERATE }

    // A beat-to-beat change larger than this fraction of the local
    // interval is counted as "irregular." This threshold is a coarse
    // screening cutoff, not a clinically validated diagnostic value.
    private const val IRREGULARITY_RATIO = 0.20f
    private const val MIN_BEATS_CAMERA = 25
    private const val MIN_BEATS_BLE = 40
    // Flag only if a meaningful share of beats are irregular — a couple
    // of noisy beats from finger movement shouldn't trigger anything.
    private const val FLAG_FRACTION_THRESHOLD = 0.15f

    fun screen(rrIntervalsMs: List<Float>, source: RhythmSource): RhythmScreeningResult? {
        val minBeats = if (source == RhythmSource.BLUETOOTH_DEVICE) MIN_BEATS_BLE else MIN_BEATS_CAMERA
        if (rrIntervalsMs.size < minBeats) return null

        var irregular = 0
        var compared = 0
        for (i in 1 until rrIntervalsMs.size) {
            val prev = rrIntervalsMs[i - 1]
            val curr = rrIntervalsMs[i]
            if (prev <= 0f) continue
            val delta = abs(curr - prev) / prev
            if (delta > IRREGULARITY_RATIO) irregular++
            compared++
        }
        if (compared == 0) return null

        val fraction = irregular.toFloat() / compared
        val confidence = if (source == RhythmSource.BLUETOOTH_DEVICE)
            RhythmConfidence.MODERATE else RhythmConfidence.LOW

        return RhythmScreeningResult(
            flagged = fraction >= FLAG_FRACTION_THRESHOLD,
            irregularBeatFraction = fraction,
            beatsAnalyzed = compared,
            source = source,
            confidence = confidence
        )
    }
}
