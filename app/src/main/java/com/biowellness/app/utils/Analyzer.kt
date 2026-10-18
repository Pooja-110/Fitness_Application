package com.biowellness.app.utils

import com.biowellness.app.data.models.*
import kotlin.math.sin

object Analyzer {

    fun analyze(s: LiveSensorData): LiveAnalysis {
        val hasPpg = s.heartRate > 0 && s.ppgConfidence > 25
        if (!hasPpg) return LiveAnalysis()

        val stress     = computeStress(s)
        val fatigue    = computeFatigue(s)
        val focus      = (60 + ((s.hrv - 30f) * 0.4f).toInt() - (stress * 0.3f).toInt()).coerceIn(0, 100)
        val relax      = (100 - stress).coerceIn(0, 100)
        val wellness   = computeWellness(s, stress, fatigue)
        val emotion    = emotionLabel(stress, fatigue)
        val eConf      = s.ppgConfidence.coerceIn(0, 85)

        return LiveAnalysis(
            stressLevel    = stress,
            fatigueScore   = fatigue,
            wellnessScore  = wellness,
            focusLevel     = focus,
            relaxationScore= relax,
            breathingQuality = breathLabel(s.breathingRate),
            emotionalState = emotion,
            emotionalConf  = eConf
        )
    }

    private fun computeStress(s: LiveSensorData): Int {
        var v = 0
        v += ((s.heartRate - 70) * 0.9f).toInt().coerceAtLeast(0)
        v += ((50f - s.hrv) * 0.7f).toInt().coerceAtLeast(0)
        v += ((s.breathingRate - 16f) * 1.5f).toInt().coerceAtLeast(0)
        return v.coerceIn(0, 100)
    }

    private fun computeFatigue(s: LiveSensorData): Int {
        val h = ((50f - s.hrv.coerceAtMost(80f)) * 0.8f).toInt().coerceAtLeast(0)
        val r = if (s.heartRate > 90 || s.heartRate < 52) 20 else 0
        val b = if (s.breathingRate > 20 || s.breathingRate < 10) 15 else 0
        return (h + r + b).coerceIn(0, 100)
    }

    private fun computeWellness(s: LiveSensorData, stress: Int, fatigue: Int): Int {
        val a = ((s.hrv / 80f) * 30f).toInt().coerceIn(0, 30)
        val b = if (s.heartRate in 55..85) 25 else if (s.heartRate in 50..95) 15 else 5
        // SpO2 is not measurable from this device's camera sensor — it
        // contributes nothing rather than a fabricated bonus/penalty.
        val c = if (s.spo2 > 0f) ((s.spo2 - 90f) * 2f).toInt().coerceIn(0, 20) else 0
        val d = if (s.breathingRate in 12f..18f) 15 else 5
        return (a + b + c + d - stress / 5 - fatigue / 10).coerceIn(0, 100)
    }

    private fun emotionLabel(stress: Int, fatigue: Int): String = when {
        stress < 20 && fatigue < 30 -> "Calm & Relaxed"
        stress < 35                 -> "Balanced"
        stress in 35..55            -> "Mildly Stressed"
        stress in 56..70            -> "Stressed"
        fatigue > 60                -> "Fatigued"
        else                        -> "High Tension"
    }

    private fun breathLabel(br: Float): String = when {
        br <= 0            -> "Measuring..."
        br < 10f           -> "Slow (~${br.toInt()} br/min)"
        br in 10f..18f     -> "Optimal (~${br.toInt()} br/min)"
        br in 18f..22f     -> "Mildly elevated (~${br.toInt()} br/min)"
        else               -> "Elevated (~${br.toInt()} br/min)"
    }
}

object ReportBuilder {
    fun build(s: LiveSensorData, a: LiveAnalysis, secs: Int): WellnessReport {
        val hr    = s.heartRate
        val hrv   = s.hrv
        val spo2  = s.spo2
        val br    = s.breathingRate
        val conf  = s.ppgConfidence
        val stress= a.stressLevel
        val rhythmSource = if (s.hrSource == HrSource.BLUETOOTH_DEVICE)
            RhythmAnalyzer.RhythmSource.BLUETOOTH_DEVICE else RhythmAnalyzer.RhythmSource.CAMERA
        val rhythm = RhythmAnalyzer.screen(s.rrIntervalsMs, rhythmSource)

        return WellnessReport(
            scanSeconds    = secs,
            hrText         = hrText(hr, hrv, spo2, conf),
            hrvText        = hrvText(hrv, conf),
            stressText     = stressText(stress, hrv, br, conf),
            recoveryText   = recoveryText(hrv, spo2, conf),
            fatigueText    = fatigueText(a.fatigueScore, conf),
            breathingText  = breathText(br, spo2, conf),
            bioAgeText     = bioAgeText(hrv, stress, spo2, conf),
            confidenceText = confText(s),
            risks          = buildRisks(hr, hrv, spo2, br, stress, rhythm),
            dietTips       = dietTips(stress),
            hydrationTips  = listOf("A common rule of thumb is roughly 30–35 ml of fluid per kg of body weight per day; needs vary with heat and exercise", "Drink to thirst and drink more around workouts", "Pale yellow urine usually indicates good hydration"),
            exerciseTips   = exerciseTips(stress, a.fatigueScore),
            sleepTips      = listOf("Aim for 7+ hours; keep a consistent schedule", "A cool, dark, quiet bedroom helps", "Try to wind down away from bright screens before bed"),
            breathingTips  = listOf("Box Breathing (4-4-4-4): 4 s in, hold, out, hold", "Coherent Breathing: 5 breaths/min (6 s in, 6 s out)", "4-7-8: inhale 4 s, hold 7 s, exhale 8 s"),
            meditationTips = listOf("Start with 5–10 minutes of mindfulness daily", "A short body-scan or breathing practice before sleep can help you wind down", "Loving-kindness (Metta) — emotional resilience"),
            lifestyleTips  = listOf("Time outdoors most days is associated with better mood", "1 hr/day digital detox", "Gratitude journaling — a few items nightly"),
            consultFlags   = consultFlags(hr, hrv, spo2, stress, rhythm),
            binauralItems  = binauralList(stress),
            solfeggioItems = solfeggioList(),
            natureItems    = natureList(),
            noiseItems     = noiseList(),
            breathingCadence = if (stress > 50) "Coherent Breathing: 5 breaths/min (6 s in, 6 s out)" else "Box Breathing (4-4-4-4)"
        )
    }

    private fun hrText(hr: Int, hrv: Float, spo2: Float, conf: Int) = buildString {
        appendLine("Heart Rate: $hr BPM  |  HRV: ${hrv.toInt()} ms  |  Signal quality score: $conf%")
        appendLine()
        appendLine(when {
            hr < 50       -> "HR: Below typical resting range — possible bradycardia pattern."
            hr in 50..100 -> "HR: Within normal resting range."
            hr in 101..120 -> "HR: Mildly elevated — possible stress, activity or dehydration."
            else          -> "HR: Significantly elevated — rest and re-scan recommended."
        })
        appendLine("SpO₂: Not available — a phone camera cannot measure blood oxygen (requires red + infrared sensors).")
        append("Source: Real-time camera PPG (not profile data).")
    }

    private fun hrvText(hrv: Float, conf: Int) = buildString {
        appendLine("HRV (RMSSD): ${hrv.toInt()} ms  |  Signal quality score: $conf%")
        appendLine()
        appendLine(when {
            hrv >= 60 -> "EXCELLENT — Strong parasympathetic activity and cardiac adaptability."
            hrv >= 40 -> "NORMAL — Balanced autonomic function within healthy range."
            hrv >= 20 -> "BELOW AVERAGE — Possible physiological stress or fatigue."
            else      -> "LOW — Significant sympathetic dominance. Rest and recovery advised."
        })
        append("Note: HRV varies a lot with age, fitness and time of day — compare against your own baseline rather than a single number. Camera PPG HRV is an estimate; ECG is the clinical standard.")
    }

    private fun stressText(stress: Int, hrv: Float, br: Float, conf: Int) = buildString {
        val label = when { stress < 20 -> "LOW"; stress < 40 -> "MILD"; stress < 60 -> "MODERATE"; stress < 80 -> "ELEVATED"; else -> "HIGH" }
        appendLine("Stress Index: $stress/100  |  Level: $label  |  Signal quality score: $conf%")
        appendLine()
        appendLine("Contributing live signals:")
        appendLine("• HRV: ${hrv.toInt()} ms — ${if (hrv < 35) "suppressed (stress indicator)" else "normal"}")
        appendLine("• Breathing: ${br.toInt()} br/min — ${if (br > 18) "elevated" else "normal"}")
        append("Stress index is a rough heuristic from this scan's heart rate, HRV and breathing — not a clinical stress measure.")
    }

    private fun recoveryText(hrv: Float, @Suppress("UNUSED_PARAMETER") spo2: Float, conf: Int): String {
        // Recovery score is HRV-based only — SpO2 isn't measurable here,
        // so it no longer factors into the score at all.
        val score = (hrv / 80f * 100f).toInt().coerceIn(0, 100)
        return "Recovery Score: $score/100  |  Signal quality score: $conf%\n\n" +
               "HRV-based recovery: ${if (hrv > 50) "Good" else if (hrv > 30) "Moderate" else "Limited"}\n" +
               "SpO₂: Not available on this device.\n\n" +
               "Source: Real-time PPG sensor."
    }

    private fun fatigueText(fatigue: Int, conf: Int): String {
        val label = when { fatigue < 20 -> "MINIMAL"; fatigue < 40 -> "MILD"; fatigue < 60 -> "MODERATE"; fatigue < 80 -> "SIGNIFICANT"; else -> "HIGH" }
        return "Fatigue Score: $fatigue/100  |  Level: $label  |  Signal quality score: $conf%\n\n" +
               "A rough indicator estimated from HRV, heart rate and breathing during this scan — not a clinical fatigue measure."
    }

    private fun breathText(br: Float, @Suppress("UNUSED_PARAMETER") spo2: Float, conf: Int) = buildString {
        appendLine("Breathing Rate: ~${br.toInt()} br/min  |  Signal quality score: $conf%")
        appendLine()
        appendLine(when {
            br in 10f..18f -> "OPTIMAL — Consistent with diaphragmatic breathing and calm state."
            br < 10f       -> "SLOW — Below typical range."
            br in 18f..22f -> "MILDLY ELEVATED — Possible mild anxiety or recent activity."
            else           -> "ELEVATED — Box breathing practice recommended."
        })
        append("Breathing rate extracted from low-frequency PPG oscillations.")
    }

    private fun bioAgeText(@Suppress("UNUSED_PARAMETER") hrv: Float, @Suppress("UNUSED_PARAMETER") stress: Int,
                           @Suppress("UNUSED_PARAMETER") spo2: Float, @Suppress("UNUSED_PARAMETER") conf: Int): String =
        "Biological age can't be estimated reliably from a phone scan, so this app doesn't show one."

    private fun confText(s: LiveSensorData) = buildString {
        appendLine("Signal quality: ${s.ppgQuality.name}")
        appendLine("Source: ${if (s.hrSource == HrSource.BLUETOOTH_DEVICE) "connected Bluetooth heart-rate device" else "phone camera PPG"}")
        appendLine("SpO₂: not measured — needs a red + infrared sensor this phone doesn't have.")
        appendLine("Blood pressure: not measured — needs a calibrated cuff (use the BP Log to record cuff readings).")
        appendLine()
        append("Camera PPG is an estimate and is sensitive to movement, pressure and lighting. It is not a medical device.")
    }

    private fun buildRisks(
        hr: Int, hrv: Float, @Suppress("UNUSED_PARAMETER") spo2: Float, br: Float, stress: Int,
        rhythm: RhythmAnalyzer.RhythmScreeningResult?
    ): List<RiskItem> {
        val list = mutableListOf<RiskItem>()
        if (stress > 65) list.add(RiskItem("Chronic Stress Pattern", "Indicator only — not a probability", 72,
            listOf("Sustained stress signals", "HRV suppression: ${hrv.toInt()} ms"), RiskSeverity.HIGH))
        if (hrv < 25f) list.add(RiskItem("Autonomic Imbalance", "Indicator only — not a probability", 65,
            listOf("Low HRV: ${hrv.toInt()} ms"), RiskSeverity.MODERATE))
        if (hr > 100) list.add(RiskItem("Elevated Resting HR", "Indicator only — not a probability", 60,
            listOf("HR: $hr BPM at rest"), RiskSeverity.MODERATE))
        // No SpO2-based risk — this device cannot measure blood oxygen,
        // so no oxygenation risk claim is made either way.
        if (br > 20f) list.add(RiskItem("Hyperventilation Pattern", "Indicator only — not a probability", 50,
            listOf("Breathing: ${br.toInt()} br/min"), RiskSeverity.LOW))
        // Non-diagnostic screening flag only — see RhythmAnalyzer for
        // exactly what this does and doesn't mean. Never phrased as a
        // named condition or a probability of disease.
        if (rhythm != null && rhythm.flagged) {
            val pct = (rhythm.irregularBeatFraction * 100).toInt()
            list.add(RiskItem(
                "Irregular Rhythm Pattern Detected",
                "Screening flag — not a diagnosis",
                if (rhythm.confidence == RhythmAnalyzer.RhythmConfidence.MODERATE) 55 else 35,
                listOf(
                    "$pct% of ${rhythm.beatsAnalyzed} beats analysed showed larger-than-expected beat-to-beat variation",
                    "Source: ${if (rhythm.source == RhythmAnalyzer.RhythmSource.BLUETOOTH_DEVICE) "connected heart-rate device" else "camera PPG (lower reliability)"}",
                    "This does NOT identify a condition — it only flags a pattern worth having checked"
                ),
                RiskSeverity.MODERATE
            ))
        }
        if (list.isEmpty()) list.add(RiskItem("No Significant Indicators", "Indicator only — not a probability", 85,
            listOf("All live signals within acceptable ranges"), RiskSeverity.LOW))
        return list
    }

    private fun dietTips(stress: Int) = buildList {
        add("Build meals around vegetables, fruit, whole grains, legumes and lean protein")
        add("Include a protein source with each meal to support muscle repair after training")
        add("Limit added sugar and heavily processed foods")
        if (stress > 50) add("Caffeine late in the day can disrupt sleep — consider keeping it to the morning")
        add("Fish such as salmon or sardines 2 times a week is a common heart-healthy guideline")
    }

    private fun exerciseTips(stress: Int, fatigue: Int) = if (stress > 60 || fatigue > 60) listOf(
        "Today's scan suggests keeping it light: easy walking, gentle mobility or yoga",
        "Save hard sessions for a day when you feel recovered",
        "Prioritise sleep — most adults need 7+ hours",
        "Open the Gym section for low-intensity options like glute bridges and planks"
    ) else listOf(
        "General guideline for adults: 150–300 min/week of moderate aerobic activity (WHO)",
        "Strength training for all major muscle groups on 2 or more days/week",
        "Warm up 5–10 minutes before harder sessions and leave rest days between hard efforts",
        "Open the Gym section for illustrated form guides"
    )

    private fun consultFlags(
        hr: Int, hrv: Float, @Suppress("UNUSED_PARAMETER") spo2: Float, stress: Int,
        rhythm: RhythmAnalyzer.RhythmScreeningResult?
    ): List<String> {
        val flags = mutableListOf<String>()
        if (hr > 110 || hr < 45) flags.add("Heart rate outside normal range on repeated measurement")
        // No SpO2 consult flag — this device can't measure blood oxygen,
        // so it can't tell you whether it's low.
        if (stress > 80) flags.add("Persistently very high stress score")
        if (hrv < 10f)  flags.add("Very low HRV — possible autonomic system concern")
        if (rhythm != null && rhythm.flagged) {
            flags.add("Irregular beat-to-beat pattern flagged on this scan — not a diagnosis, but worth mentioning to a doctor if it keeps happening")
        }
        if (flags.isEmpty()) flags.add("No urgent clinical flags in this scan")
        flags.add("Always consult a licensed healthcare professional for persistent symptoms")
        flags.add("This app does NOT replace medical diagnosis or examination")
        return flags
    }

    private fun binauralList(stress: Int) = if (stress > 60) listOf(
        AudioItem("Delta Waves", "0.5–4 Hz", "Low-frequency tone some people use for deep relaxation"),
        AudioItem("Theta Waves", "4–8 Hz", "Often used during meditation for a calming effect"),
        AudioItem("Alpha Waves", "8–14 Hz", "Commonly used for relaxed, calm alertness")
    ) else listOf(
        AudioItem("Alpha Waves", "10 Hz", "Commonly used for calm focus"),
        AudioItem("Beta Waves", "15–20 Hz", "Often used for active concentration"),
        AudioItem("Gamma Waves", "40 Hz", "Used by some for a sense of mental clarity")
    )

    // Frequencies are real; the "healing"/numerology claims historically
    // attached to solfeggio tones (e.g. "528 Hz = love frequency") are
    // not scientifically supported, so the copy sticks to what's true:
    // these are tones some people find calming to listen to.
    private fun solfeggioList() = listOf(
        AudioItem("396 Hz Tone", "396 Hz", "A low, grounding tone some listeners find calming"),
        AudioItem("528 Hz Tone", "528 Hz", "A mid tone popular in relaxation playlists"),
        AudioItem("432 Hz Tuning", "432 Hz", "An alternative tuning some prefer over standard 440 Hz"),
        AudioItem("639 Hz Tone", "639 Hz", "A mid-range tone used for ambient background listening"),
        AudioItem("963 Hz Tone", "963 Hz", "A high tone used in some ambient relaxation tracks")
    )

    private fun natureList() = listOf(
        AudioItem("Rain", "Continuous", "A steady ambient sound many people find calming"),
        AudioItem("Forest Sounds", "Broadband", "Ambient outdoor soundscape for background listening"),
        AudioItem("Ocean Waves", "~0.1 Hz rhythm", "Rhythmic wave sound often used for relaxation"),
        AudioItem("Birdsong", "Variable", "Ambient outdoor sound some find calming"),
        AudioItem("Tibetan Bowls", "Variable harmonics", "Resonant tones often used during meditation")
    )

    private fun noiseList() = listOf(
        AudioItem("Brown Noise", "Low-frequency heavy", "Deep, low-frequency masking noise"),
        AudioItem("Pink Noise", "Balanced spectrum", "Balanced-spectrum masking noise, popular for sleep"),
        AudioItem("White Noise", "Flat spectrum", "Flat-spectrum noise for focus and distraction masking"),
        AudioItem("Grey Noise", "Equal loudness", "Perceptually flat masking noise, gentler on sensitive ears")
    )
}
