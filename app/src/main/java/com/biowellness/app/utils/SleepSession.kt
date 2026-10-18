package com.biowellness.app.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class SleepSessionPhase { IDLE, TRACKING, COMPLETE }

data class SleepSample(val timestampMs: Long, val movement: Float)

/**
 * IMPORTANT — what this is and isn't:
 *
 * This is a phone-accelerometer movement proxy (a lightweight form of
 * "actigraphy," the same general principle used by many consumer
 * sleep-tracking wearables), NOT clinical polysomnography. It cannot
 * detect sleep stages (REM/light/deep), and accuracy depends heavily
 * on the phone being placed on the mattress close to the sleeper and
 * undisturbed by other movement sources (partner, pets, charging
 * cable, etc). Treat the numbers as a rough trend indicator, not a
 * diagnosis.
 */
data class SleepSummary(
    val startMs: Long,
    val endMs: Long,
    val totalMinutes: Int,
    val estimatedSleepMinutes: Int,
    val estimatedAwakeMinutes: Int,
    val restlessPeriods: Int,
    val sleepQuality: Int,
    val sampleCount: Int
)

/**
 * Process-wide holder for live sleep-tracking state.
 *
 * SleepTrackingService (a foreground service that keeps sampling the
 * accelerometer while the screen is off) writes to this object; the
 * ViewModel/UI read from it. Both live in the same process, so a
 * plain singleton StateFlow is enough here — no AIDL/Messenger IPC
 * needed for a same-process foreground service.
 */
object SleepSessionState {

    // ~12h at 1 sample per ~5s from the service's emit interval.
    private const val MAX_SAMPLES = 9_000

    private val _phase = MutableStateFlow(SleepSessionPhase.IDLE)
    val phase: StateFlow<SleepSessionPhase> = _phase

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds

    private val _currentMovement = MutableStateFlow(0f)
    val currentMovement: StateFlow<Float> = _currentMovement

    private val _summary = MutableStateFlow<SleepSummary?>(null)
    val summary: StateFlow<SleepSummary?> = _summary

    private val samples = ArrayList<SleepSample>()
    private var startMs = 0L

    @Synchronized
    fun start() {
        samples.clear()
        startMs = System.currentTimeMillis()
        _elapsedSeconds.value = 0
        _currentMovement.value = 0f
        _summary.value = null
        _phase.value = SleepSessionPhase.TRACKING
    }

    @Synchronized
    fun addSample(movement: Float) {
        if (_phase.value != SleepSessionPhase.TRACKING) return
        val now = System.currentTimeMillis()
        samples.add(SleepSample(now, movement))
        if (samples.size > MAX_SAMPLES) samples.removeAt(0)
        _currentMovement.value = movement
        _elapsedSeconds.value = ((now - startMs) / 1000L).toInt()
    }

    /** Called by the service when tracking ends (stop or destroy). */
    @Synchronized
    fun finish() {
        if (_phase.value != SleepSessionPhase.TRACKING) return
        val endMs = System.currentTimeMillis()
        _summary.value = SleepAnalyzer.analyze(samples.toList(), startMs, endMs)
        _phase.value = SleepSessionPhase.COMPLETE
    }

    @Synchronized
    fun reset() {
        samples.clear()
        _phase.value = SleepSessionPhase.IDLE
        _elapsedSeconds.value = 0
        _currentMovement.value = 0f
        _summary.value = null
    }
}
