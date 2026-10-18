package com.biowellness.app.data.models

data class UserProfile(
    val age: String = "",
    val gender: String = "",
    val height: String = "",
    val weight: String = "",
    val activityLevel: String = "",
    val sleepHours: String = "",
    val stressLevel: String = "",
    val smokingStatus: String = "",
    val alcoholUse: String = "",
    val medicalHistory: String = ""
) {
    val isEmpty: Boolean get() = age.isBlank() && gender.isBlank() && height.isBlank() && weight.isBlank()
}

enum class SignalQuality { EXCELLENT, GOOD, MODERATE, POOR, UNAVAILABLE }

data class LiveSensorData(
    val heartRate: Int = 0,
    val hrv: Float = 0f,
    val spo2: Float = 0f,
    val breathingRate: Float = 0f,
    val ppgConfidence: Int = 0,
    val ppgQuality: SignalQuality = SignalQuality.UNAVAILABLE,
    val torchEnabled: Boolean = false,
    val ppgSamples: List<Float> = emptyList(),
    // Real beat-to-beat intervals (ms), from either camera PPG or a
    // connected BLE heart-rate device — see RhythmAnalyzer.
    val rrIntervalsMs: List<Float> = emptyList(),
    val hrSource: HrSource = HrSource.CAMERA
)

enum class HrSource { CAMERA, BLUETOOTH_DEVICE }

data class LiveAnalysis(
    val stressLevel: Int = 0,
    val fatigueScore: Int = 0,
    val wellnessScore: Int = 0,
    val focusLevel: Int = 0,
    val relaxationScore: Int = 0,
    val breathingQuality: String = "",
    val emotionalState: String = "—",
    val emotionalConf: Int = 0
)

data class WellnessReport(
    val scanSeconds: Int = 0,
    val hrText: String = "",
    val hrvText: String = "",
    val stressText: String = "",
    val recoveryText: String = "",
    val fatigueText: String = "",
    val breathingText: String = "",
    val bioAgeText: String = "",
    val confidenceText: String = "",
    val risks: List<RiskItem> = emptyList(),
    val dietTips: List<String> = emptyList(),
    val hydrationTips: List<String> = emptyList(),
    val exerciseTips: List<String> = emptyList(),
    val sleepTips: List<String> = emptyList(),
    val breathingTips: List<String> = emptyList(),
    val meditationTips: List<String> = emptyList(),
    val lifestyleTips: List<String> = emptyList(),
    val consultFlags: List<String> = emptyList(),
    val binauralItems: List<AudioItem> = emptyList(),
    val solfeggioItems: List<AudioItem> = emptyList(),
    val natureItems: List<AudioItem> = emptyList(),
    val noiseItems: List<AudioItem> = emptyList(),
    val breathingCadence: String = "Box Breathing (4-4-4-4)"
)

data class RiskItem(val name: String, val probability: String, val conf: Int,
                    val signals: List<String>, val severity: RiskSeverity)
enum class RiskSeverity { LOW, MODERATE, HIGH }

data class AudioItem(val name: String, val frequency: String, val benefit: String)

enum class ScanState { IDLE, SCANNING, ANALYZING, COMPLETE, ERROR }

/**
 * A single manually-entered blood pressure reading.
 *
 * IMPORTANT: this app cannot measure blood pressure from any phone
 * sensor — that requires a calibrated cuff. Entries are typed in by
 * the user from their own validated BP monitor, purely for trend
 * tracking. Never computed or estimated by this app.
 */
data class BpLogEntry(
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int = 0,
    val timestampMs: Long = System.currentTimeMillis(),
    val note: String = ""
)
