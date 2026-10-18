package com.biowellness.app.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * SleepTrackingService
 *
 * A foreground service (so it keeps running with the screen off) that
 * samples the device accelerometer and feeds real movement-magnitude
 * readings into SleepSessionState. There is no simulated data here —
 * if the accelerometer is unavailable, no samples are produced and
 * the resulting summary will reflect that (near-zero sample count).
 *
 * This is intentionally a simple phone-on-the-mattress movement
 * tracker, not a wearable-grade sleep stage detector. See
 * SleepAnalyzer for what the resulting numbers do and don't mean.
 */
class SleepTrackingService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // Accumulate raw magnitude readings and emit one averaged sample
    // every emitIntervalMs, rather than storing every raw event.
    private val magnitudeBuffer = mutableListOf<Float>()
    private var lastEmitMs = 0L
    private val emitIntervalMs = 5_000L

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        SleepSessionState.start()
        magnitudeBuffer.clear()
        lastEmitMs = System.currentTimeMillis()
        val sensor = accelerometer
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        // Deviation from resting gravity (~9.81 m/s^2) approximates
        // how much the phone is being moved/jostled.
        val magnitude = abs(sqrt((x * x + y * y + z * z).toDouble()).toFloat() - SensorManager.GRAVITY_EARTH)
        magnitudeBuffer.add(magnitude)

        val now = System.currentTimeMillis()
        if (now - lastEmitMs >= emitIntervalMs) {
            val avg = if (magnitudeBuffer.isEmpty()) 0f else magnitudeBuffer.average().toFloat()
            magnitudeBuffer.clear()
            SleepSessionState.addSample(avg)
            lastEmitMs = now
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        SleepSessionState.finish()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val channelId = "sleep_tracking"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Sleep tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Shown while a sleep-tracking session is running" }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Sleep tracking active")
            .setContentText("Using the motion sensor to estimate rest — keep the phone on the bed near you.")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 4201
    }
}
