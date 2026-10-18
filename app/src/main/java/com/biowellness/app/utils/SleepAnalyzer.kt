package com.biowellness.app.utils

/**
 * Small actigraphy-style analyzer.
 *
 * Classifies 1-minute windows of real accelerometer-derived movement
 * magnitude as "still" or "moving" against a fixed threshold — the
 * same general principle consumer actigraphy wearables use, but with
 * far less validation and a single, ungrounded sensor placement (the
 * phone, not a wrist strap). This is a trend indicator, not a
 * clinical sleep stage classifier: it cannot distinguish light/deep/
 * REM sleep, and it will misread being still-but-awake as "asleep."
 */
object SleepAnalyzer {

    // A minute is classified "restless" if its average movement
    // magnitude (m/s^2 deviation from gravity) exceeds this. Tuned
    // against typical accelerometer noise floor for a phone resting
    // on a mattress — not clinically validated.
    private const val MOVEMENT_THRESHOLD = 0.35f
    private const val WINDOW_MS = 60_000L

    fun analyze(samples: List<SleepSample>, startMs: Long, endMs: Long): SleepSummary {
        val totalMinutes = ((endMs - startMs) / 60_000L).toInt().coerceAtLeast(0)

        if (samples.isEmpty() || totalMinutes <= 0) {
            return SleepSummary(
                startMs = startMs,
                endMs = endMs,
                totalMinutes = totalMinutes,
                estimatedSleepMinutes = 0,
                estimatedAwakeMinutes = totalMinutes,
                restlessPeriods = 0,
                sleepQuality = 0,
                sampleCount = samples.size
            )
        }

        // Bucket samples into 1-minute windows and average movement per window.
        val windows = LinkedHashMap<Long, MutableList<Float>>()
        for (s in samples) {
            val bucket = (s.timestampMs - startMs) / WINDOW_MS
            windows.getOrPut(bucket) { mutableListOf() }.add(s.movement)
        }

        val windowAverages = windows.values.map { it.average().toFloat() }
        val stillWindows = windowAverages.count { it < MOVEMENT_THRESHOLD }

        // A "restless period" is a transition from a still window into
        // a moving window — a rough proxy for a waking/turning moment.
        var restlessPeriods = 0
        var prevStill = true
        for (avg in windowAverages) {
            val isStill = avg < MOVEMENT_THRESHOLD
            if (!isStill && prevStill) restlessPeriods++
            prevStill = isStill
        }

        val estimatedSleepMinutes = stillWindows
        val estimatedAwakeMinutes = (totalMinutes - estimatedSleepMinutes).coerceAtLeast(0)

        val stillRatio = if (windowAverages.isNotEmpty())
            stillWindows.toFloat() / windowAverages.size else 0f
        val quality = ((stillRatio * 100f) - (restlessPeriods * 2f)).toInt().coerceIn(0, 100)

        return SleepSummary(
            startMs = startMs,
            endMs = endMs,
            totalMinutes = totalMinutes,
            estimatedSleepMinutes = estimatedSleepMinutes,
            estimatedAwakeMinutes = estimatedAwakeMinutes,
            restlessPeriods = restlessPeriods,
            sleepQuality = quality,
            sampleCount = samples.size
        )
    }
}
