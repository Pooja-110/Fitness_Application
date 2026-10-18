package com.biowellness.app.utils

import androidx.camera.core.ImageProxy
import kotlin.math.PI
import kotlin.math.sqrt

/**
 * PpgProcessor
 *
 * Extracts a camera-based PPG signal from the Y plane.
 *
 * IMPORTANT:
 * Camera PPG is an estimation and should not be treated as a
 * clinical-grade measurement. SpO2 in particular is only an estimate
 * from this single-channel signal.
 *
 * Thread safety:
 * - CameraX may call processFrame() repeatedly.
 * - Processing can happen on a background coroutine.
 * - The signal/timestamp buffers are protected by samplesLock.
 * - All DSP calculations operate on immutable snapshots.
 */
object PpgProcessor {

    // ---------------------------------------------------------------------
    // Shared PPG buffers
    // ---------------------------------------------------------------------

    private val samplesLock = Any()

    private val signal = ArrayList<Float>()
    private val stamps = ArrayList<Long>()

    // ---------------------------------------------------------------------
    // Configuration
    // ---------------------------------------------------------------------

    private const val BUF_MAX = 1800       // ~60 sec @ 30 FPS
    private const val MIN_SHOW = 450       // ~15 sec @ 30 FPS
    private const val MIN_READY = 1800    // ~60 sec @ 30 FPS

    // Minimum distance between detected beats.
    // 333 ms ~= 180 BPM maximum.
    private const val REFRACT_MS = 333L

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    /**
     * Clears all collected PPG data.
     *
     * Safe to call from any thread.
     */
    fun reset() {
        synchronized(samplesLock) {
            signal.clear()
            stamps.clear()
        }
    }

    /**
     * Processes one CameraX frame.
     *
     * The image is expected to be supplied by the CameraX analyzer.
     */
    fun processFrame(image: ImageProxy): PpgResult {

        val y = extractY(image)
        val now = System.currentTimeMillis()

        // Update the shared buffers atomically.
        synchronized(samplesLock) {

            signal.add(y)
            stamps.add(now)

            while (signal.size > BUF_MAX) {
                signal.removeAt(0)
                stamps.removeAt(0)
            }
        }

        // -------------------------------------------------------------
        // IMPORTANT:
        // Never process the live mutable ArrayLists.
        // Create one consistent snapshot containing signal + timestamps.
        // -------------------------------------------------------------

        val snapshot = synchronized(samplesLock) {
            PpgSnapshot(
                signal = signal.toList(),
                stamps = stamps.toList()
            )
        }

        val data = snapshot.signal
        val times = snapshot.stamps

        if (data.isEmpty()) {
            return PpgResult(
                isReady = false,
                progress = 0f,
                confidence = 0,
                rawSamples = emptyList()
            )
        }

        val progress =
            (data.size.toFloat() / MIN_READY)
                .coerceAtMost(1f)

        val conf = confidence(data)

        // -------------------------------------------------------------
        // Not enough data yet
        // -------------------------------------------------------------

        if (data.size < MIN_SHOW) {
            return PpgResult(
                isReady = false,
                progress = progress,
                confidence = conf,
                rawSamples = data.takeLast(80)
            )
        }

        // -------------------------------------------------------------
        // Process only the immutable snapshot.
        // -------------------------------------------------------------

        val heartRate = heartRate(data, times)
        val hrv = hrv(data, times)
        val spo2 = spo2(data)
        val breathing = breathing(data, times)
        val rr = rrIntervals(data, times)

        return PpgResult(
            isReady = data.size >= MIN_READY,
            progress = progress,
            heartRate = heartRate,
            hrv = hrv,
            spo2 = spo2,
            breathingRate = breathing,
            confidence = conf,
            rawSamples = data.takeLast(80),
            rrIntervalsMs = rr
        )
    }

    // ---------------------------------------------------------------------
    // Snapshot
    // ---------------------------------------------------------------------

    private data class PpgSnapshot(
        val signal: List<Float>,
        val stamps: List<Long>
    )

    // ---------------------------------------------------------------------
    // Signal extraction
    // ---------------------------------------------------------------------

    private fun extractY(image: ImageProxy): Float {

        return try {

            val plane = image.planes[0]

            val buffer = plane.buffer

            /*
             * Duplicate the buffer so we don't modify the original
             * ImageProxy buffer position.
             */
            val duplicate = buffer.duplicate()

            val bytes = ByteArray(duplicate.remaining())

            duplicate.get(bytes)

            val stride = plane.rowStride
            val pixelStride = plane.pixelStride

            val width = image.width
            val height = image.height

            var sum = 0.0
            var count = 0

            var row = 0

            while (row < height) {

                var col = 0

                while (col < width) {

                    val index =
                        row * stride + col * pixelStride

                    if (index >= 0 && index < bytes.size) {

                        sum +=
                            (bytes[index].toInt() and 0xFF)

                        count++
                    }

                    col += 4
                }

                row += 4
            }

            if (count == 0) {
                0f
            } else {
                (sum / count).toFloat()
            }

        } catch (_: Exception) {
            0f
        }
    }

    // ---------------------------------------------------------------------
    // Confidence
    // ---------------------------------------------------------------------

    private fun confidence(
        data: List<Float>
    ): Int {

        if (data.size < 30) {

            return (
                    data.size / 30f * 15f
                    ).toInt()
        }

        val recent =
            data.takeLast(
                minOf(300, data.size)
            )

        if (recent.isEmpty()) {
            return 0
        }

        val mean = recent.average()

        val std = recent.std()

        val pi =
            if (mean > 0.0) {
                std / mean * 100.0
            } else {
                0.0
            }

        return when {

            pi < 0.1 -> 10

            pi < 0.5 -> 25

            pi < 1.0 -> 50

            pi < 3.0 -> 80

            pi < 8.0 -> 90

            else -> 40

        }.coerceIn(0, 95)
    }

    // ---------------------------------------------------------------------
    // Heart Rate
    // ---------------------------------------------------------------------

    private fun heartRate(
        data: List<Float>,
        timestamps: List<Long>
    ): Int {

        if (data.size < 10) {
            return 0
        }

        val bp = bandpass(data)

        val peakIndexes =
            peaks(
                data = bp,
                timestamps = timestamps
            )

        if (peakIndexes.size < 4) {
            return 0
        }

        val times =
            peakIndexes.mapNotNull { index ->

                timestamps.getOrNull(index)
            }

        if (times.size < 4) {
            return 0
        }

        val rr =
            times.zipWithNext { a, b ->
                (b - a).toFloat()
            }

        val valid =
            rr.filter {
                it in 250f..2000f
            }

        if (valid.size < 3) {
            return 0
        }

        val sorted = valid.sorted()

        val median =
            sorted[sorted.size / 2]

        if (median <= 0f) {
            return 0
        }

        return (
                60000f / median
                ).toInt().coerceIn(40, 200)
    }

    /**
     * Real R-R (beat-to-beat) intervals in milliseconds, from the same
     * peak-detection pass as heartRate()/hrv(). Camera-derived R-R
     * timing is noisier than a dedicated chest strap or watch, so this
     * is used only for a coarse, clearly-labeled screening heuristic
     * (see RhythmAnalyzer) — never presented as a diagnosis.
     */
    private fun rrIntervals(
        data: List<Float>,
        timestamps: List<Long>
    ): List<Float> {
        if (data.size < 10) return emptyList()
        val bp = bandpass(data)
        val peakIndexes = peaks(data = bp, timestamps = timestamps)
        val times = peakIndexes.mapNotNull { timestamps.getOrNull(it) }
        if (times.size < 4) return emptyList()
        return times.zipWithNext { a, b -> (b - a).toFloat() }.filter { it in 250f..2000f }
    }

    // ---------------------------------------------------------------------
    // HRV
    // ---------------------------------------------------------------------

    private fun hrv(
        data: List<Float>,
        timestamps: List<Long>
    ): Float {

        if (data.size < 10) {
            return 0f
        }

        val bp = bandpass(data)

        val peakIndexes =
            peaks(
                data = bp,
                timestamps = timestamps
            )

        val times =
            peakIndexes.mapNotNull { index ->
                timestamps.getOrNull(index)
            }

        if (times.size < 6) {
            return 0f
        }

        val rr =
            times.zipWithNext { a, b ->
                (b - a).toFloat()
            }.filter {
                it in 300f..1500f
            }

        if (rr.size < 5) {
            return 0f
        }

        val squaredDifferences =
            rr.zipWithNext { a, b ->

                val difference = b - a

                difference * difference
            }

        if (squaredDifferences.isEmpty()) {
            return 0f
        }

        val rmssd =
            sqrt(
                squaredDifferences.average()
            ).toFloat()

        return rmssd.coerceIn(5f, 200f)
    }

    // ---------------------------------------------------------------------
    // SpO2
    // ---------------------------------------------------------------------

    /**
     * SpO2 (blood oxygen saturation) is NOT computed here.
     *
     * Real pulse oximetry requires comparing absorption at two
     * distinct light wavelengths (red AND infrared) to derive the
     * ratio-of-ratios that maps to oxygen saturation. A phone's rear
     * camera behind a single-color flashlight only gives us one
     * channel of intensity data — there is no physical way to derive
     * a real SpO2 value from that alone.
     *
     * The previous implementation here bucketed signal noise
     * (a perfusion-index-like ratio) into a narrow 95–98% range and
     * presented it as an "estimate." That number was not actually
     * correlated with oxygen saturation in any validated way, so it
     * has been removed rather than kept as a fake approximation.
     * This function always reports "unavailable" (0f); the UI treats
     * 0f as "not supported on this device" for SpO2.
     */
    private fun spo2(
        @Suppress("UNUSED_PARAMETER") data: List<Float>
    ): Float = 0f

    // ---------------------------------------------------------------------
    // Breathing Rate
    // ---------------------------------------------------------------------

    private fun breathing(
        data: List<Float>,
        timestamps: List<Long>
    ): Float {

        if (data.size < MIN_SHOW) {
            return 0f
        }

        if (timestamps.size != data.size) {
            return 0f
        }

        val filtered =
            lowpass(
                data = data,
                cut = 0.5f,
                sr = 30f
            )

        val peakIndexes =
            peaks(
                data = filtered,
                timestamps = timestamps
            )

        val times =
            peakIndexes.mapNotNull { index ->
                timestamps.getOrNull(index)
            }

        if (times.size < 3) {
            return 0f
        }

        val duration =
            times.last() - times.first()

        if (duration <= 0L) {
            return 0f
        }

        val minutes =
            duration / 60000f

        if (minutes <= 0f) {
            return 0f
        }

        val rate =
            times.size.toFloat() / minutes

        return rate.coerceIn(6f, 30f)
    }

    // ---------------------------------------------------------------------
    // Bandpass
    // ---------------------------------------------------------------------

    private fun bandpass(
        data: List<Float>
    ): List<Float> {

        if (data.isEmpty()) {
            return emptyList()
        }

        /*
         * Make a stable snapshot.
         *
         * The caller already normally passes a snapshot, but keeping
         * this protection here makes the function safe if reused later.
         */
        val snapshot = data.toList()

        if (snapshot.isEmpty()) {
            return emptyList()
        }

        val fw =
            maxOf(
                3,
                (snapshot.size * 0.015f).toInt()
            )

        val sw =
            maxOf(
                15,
                (snapshot.size * 0.08f).toInt()
            )

        return List(snapshot.size) { i ->

            val fastStart =
                maxOf(0, i - fw)

            val fastEnd =
                minOf(
                    snapshot.size,
                    i + fw + 1
                )

            val slowStart =
                maxOf(0, i - sw)

            val slowEnd =
                minOf(
                    snapshot.size,
                    i + sw + 1
                )

            val fastAverage =
                snapshot
                    .subList(
                        fastStart,
                        fastEnd
                    )
                    .average()
                    .toFloat()

            val slowAverage =
                snapshot
                    .subList(
                        slowStart,
                        slowEnd
                    )
                    .average()
                    .toFloat()

            fastAverage - slowAverage
        }
    }

    // ---------------------------------------------------------------------
    // Lowpass
    // ---------------------------------------------------------------------

    private fun lowpass(
        data: List<Float>,
        cut: Float,
        sr: Float
    ): List<Float> {

        if (data.isEmpty()) {
            return emptyList()
        }

        if (cut <= 0f || sr <= 0f) {
            return data.toList()
        }

        /*
         * Work on a stable snapshot.
         */
        val snapshot = data.toList()

        if (snapshot.isEmpty()) {
            return emptyList()
        }

        val a =
            (1f / sr) /
                    (
                            1f /
                                    (
                                            2f *
                                                    PI.toFloat() *
                                                    cut
                                            ) +
                                    1f / sr
                            )

        var previous = snapshot[0]

        val result =
            ArrayList<Float>(
                snapshot.size
            )

        result.add(previous)

        for (i in 1 until snapshot.size) {

            val value = snapshot[i]

            val filtered =
                previous +
                        a * (value - previous)

            result.add(filtered)

            previous = filtered
        }

        return result
    }

    // ---------------------------------------------------------------------
    // Peak detection
    // ---------------------------------------------------------------------

    /**
     * Peak detection.
     *
     * IMPORTANT:
     * timestamps are passed into this method instead of accessing
     * the global mutable stamps list.
     */
    private fun peaks(
        data: List<Float>,
        timestamps: List<Long>
    ): List<Int> {

        if (data.size < 3) {
            return emptyList()
        }

        if (timestamps.size != data.size) {
            return emptyList()
        }

        val mean =
            data.average()

        val std =
            data.std()

        val threshold =
            mean + std * 0.6

        val output =
            ArrayList<Int>()

        for (i in 1 until data.size - 1) {

            val current = data[i]
            val previous = data[i - 1]
            val next = data[i + 1]

            val isPeak =
                current > previous &&
                        current > next &&
                        current > threshold

            if (!isPeak) {
                continue
            }

            val currentTime =
                timestamps[i]

            if (output.isEmpty()) {

                output.add(i)

            } else {

                val previousIndex =
                    output.last()

                val previousTime =
                    timestamps[previousIndex]

                if (
                    currentTime - previousTime >=
                    REFRACT_MS
                ) {
                    output.add(i)
                }
            }
        }

        return output
    }

    // ---------------------------------------------------------------------
    // Math helpers
    // ---------------------------------------------------------------------

    private fun List<Float>.variance(): Double {

        if (size < 2) {
            return 0.0
        }

        val mean =
            average()

        var sum = 0.0

        for (value in this) {

            val difference =
                value - mean

            sum +=
                difference * difference
        }

        return sum / size
    }

    private fun List<Float>.std(): Double {
        return sqrt(variance())
    }
}

// -------------------------------------------------------------------------
// Result
// -------------------------------------------------------------------------

data class PpgResult(

    val isReady: Boolean,

    val progress: Float,

    val heartRate: Int = 0,

    val hrv: Float = 0f,

    val spo2: Float = 0f,

    val breathingRate: Float = 0f,

    val confidence: Int = 0,

    val rawSamples: List<Float> = emptyList(),

    // Real beat-to-beat intervals from this scan's own camera data —
    // see RhythmAnalyzer for what these are used for.
    val rrIntervalsMs: List<Float> = emptyList()
)