package com.biowellness.app.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.camera.core.CameraControl
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.biowellness.app.data.models.*
import com.biowellness.app.data.repository.BpLogRepository
import com.biowellness.app.data.repository.ProfileRepository
import com.biowellness.app.utils.*
import com.biowellness.app.utils.AiChatEngine
import com.biowellness.app.utils.ChatMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import androidx.core.content.edit

class WellnessViewModel(app: Application) : AndroidViewModel(app) {

    // ── Profile ────────────────────────────────────────────────────────
    private val repo = ProfileRepository(app)
    val savedProfile: StateFlow<UserProfile> = repo.profile

    // ── Scan ───────────────────────────────────────────────────────────
    private val _scanState  = MutableStateFlow(ScanState.IDLE)
    val scanState: StateFlow<ScanState> = _scanState

    private val _progress   = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _scanTimer  = MutableStateFlow(0)
    val scanTimer: StateFlow<Int> = _scanTimer

    private val _message    = MutableStateFlow("Place finger firmly over camera lens + flash")
    val message: StateFlow<String> = _message

    // ── Live sensor data (SENSOR-DRIVEN — not profile-based) ───────────
    private val _liveData   = MutableStateFlow(LiveSensorData())
    val liveData: StateFlow<LiveSensorData> = _liveData

    private val _liveAnalysis = MutableStateFlow(LiveAnalysis())
    val liveAnalysis: StateFlow<LiveAnalysis> = _liveAnalysis

    // ── Torch ──────────────────────────────────────────────────────────
    private val _torchOn    = MutableStateFlow(false)
    val torchOn: StateFlow<Boolean> = _torchOn
    private var cameraControl: CameraControl? = null

    // ── Report ─────────────────────────────────────────────────────────
    private val _report     = MutableStateFlow<WellnessReport?>(null)
    val report: StateFlow<WellnessReport?> = _report

    // ── Audio ──────────────────────────────────────────────────────────
    private val audioPlayer = AudioPlayer(app)
    private val _playingName = MutableStateFlow<String?>(null)
    val playingName: StateFlow<String?> = _playingName
    private val _volume     = MutableStateFlow(0.7f)
    val volume: StateFlow<Float> = _volume

    // ── Internal jobs ──────────────────────────────────────────────────
    private var timerJob:   Job? = null
    private var timeoutJob: Job? = null
    private var startMs     = 0L

    // A scan that never reaches a valid reading within this window is
    // treated as a failed measurement (poor contact / bad lighting),
    // not silently replaced with invented numbers.
    private val scanTimeoutMs = 90_000L

    // ── Camera analyzer ────────────────────────────────────────────────
    // This is the ONLY source of heart rate / HRV / breathing data.
    // There is no simulated/demo fallback: if the real PPG signal never
    // reaches PpgProcessor's readiness threshold, the scan ends in
    // ScanState.ERROR with an explanatory message instead of fabricating
    // a result.
    val imageAnalyzer = ImageAnalysis.Analyzer { proxy ->
        if (_scanState.value == ScanState.SCANNING) {
            viewModelScope.launch(Dispatchers.Default) {
                val res = PpgProcessor.processFrame(proxy)
                val cur = _liveData.value
                val quality = when {
                    res.confidence > 80 -> SignalQuality.EXCELLENT
                    res.confidence > 60 -> SignalQuality.GOOD
                    res.confidence > 35 -> SignalQuality.MODERATE
                    res.confidence > 10 -> SignalQuality.POOR
                    else                -> SignalQuality.UNAVAILABLE
                }
                val updated = cur.copy(
                    heartRate     = if (res.heartRate > 0) res.heartRate else cur.heartRate,
                    hrv           = if (res.hrv > 0f) res.hrv else cur.hrv,
                    spo2          = 0f, // unavailable — see PpgProcessor.spo2()
                    breathingRate = if (res.breathingRate > 0f) res.breathingRate else cur.breathingRate,
                    ppgConfidence = res.confidence,
                    ppgQuality    = quality,
                    ppgSamples    = res.rawSamples.takeLast(80),
                    rrIntervalsMs = res.rrIntervalsMs,
                    hrSource      = HrSource.CAMERA
                )
                _liveData.value     = updated
                _liveAnalysis.value = Analyzer.analyze(updated)
                _progress.value     = res.progress
                updateMsg(res.progress, res.confidence)
                if (res.isReady && res.heartRate > 0) finalizeScan(updated)
            }
        }
        proxy.close()
    }

    fun setCameraControl(ctrl: CameraControl) { cameraControl = ctrl }
    fun toggleTorch() = setTorch(!_torchOn.value)
    fun setTorch(on: Boolean) { _torchOn.value = on; cameraControl?.enableTorch(on) }

    // ── Scan control ───────────────────────────────────────────────────
    fun startScan() {
        if (_scanState.value == ScanState.SCANNING) return
        PpgProcessor.reset()
        _scanState.value = ScanState.SCANNING
        _progress.value  = 0f
        _report.value    = null
        _liveData.value  = LiveSensorData()
        startMs          = System.currentTimeMillis()
        _message.value   = "Place finger firmly over camera lens + flash"
        setTorch(true)

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var t = 0
            while (_scanState.value == ScanState.SCANNING) { _scanTimer.value = t++; delay(1000L) }
        }

        // No simulated fallback. If a genuine reading isn't reached in
        // time, stop and tell the user rather than inventing a result.
        timeoutJob?.cancel()
        timeoutJob = viewModelScope.launch {
            delay(scanTimeoutMs)
            if (_scanState.value == ScanState.SCANNING) failScan()
        }
    }

    private fun failScan() {
        timerJob?.cancel()
        timeoutJob?.cancel()
        setTorch(false)
        PpgProcessor.reset()
        _scanState.value = ScanState.ERROR
        _message.value = if (_liveData.value.ppgConfidence < 20)
            "Signal too weak to measure. Make sure your finger fully covers both the camera lens and flash, and hold still."
        else
            "Couldn't get a stable reading in time. Reposition your finger and try again."
    }

    private suspend fun finalizeScan(data: LiveSensorData) {
        timerJob?.cancel()
        timeoutJob?.cancel()
        _scanState.value = ScanState.ANALYZING
        _message.value   = "Generating wellness report from this scan's live readings..."
        setTorch(false)
        delay(1000L)
        val dur = ((System.currentTimeMillis() - startMs) / 1000).toInt().coerceIn(5, 300)
        val analysis = Analyzer.analyze(data)
        _liveAnalysis.value = analysis
        _report.value = ReportBuilder.build(data, analysis, dur)
        _scanState.value = ScanState.COMPLETE
    }

    private fun updateMsg(p: Float, confidence: Int) {
        if (confidence in 1..20 && p > 0.15f) {
            _message.value = "Weak signal — reposition your finger to fully cover the lens and flash..."
            return
        }
        _message.value = when {
            p < 0.08f -> "Calibrating — keep finger still over lens + flash..."
            p < 0.20f -> "Signal detected — measuring cardiac pulse..."
            p < 0.35f -> "Measuring inter-beat intervals..."
            p < 0.50f -> "Computing HRV (60-second window)..."
            p < 0.65f -> "Analysing heart rate variability trend..."
            p < 0.80f -> "Measuring breathing rate from low-frequency oscillations..."
            p < 0.92f -> "Validating cardiac rhythm..."
            else       -> "Finalising analysis from this scan's readings..."
        }
    }

    /**
     * Reset to IDLE.
     * Navigation fix: ReportScreen uses popUpTo(Route.HOME) so pressing back
     * from the report goes HOME, not back to a stale ScanScreen.
     */
    fun resetScan() {
        timerJob?.cancel(); timeoutJob?.cancel()
        PpgProcessor.reset()
        setTorch(false)
        _scanState.value    = ScanState.IDLE
        _progress.value     = 0f
        _scanTimer.value    = 0
        _liveData.value     = LiveSensorData()
        _liveAnalysis.value = LiveAnalysis()
        _message.value      = "Place finger firmly over camera lens + flash"
    }

    // ── Profile ────────────────────────────────────────────────────────
    fun saveProfile(p: UserProfile) = repo.save(p)
    fun deleteProfile()              = repo.delete()

    // ── Audio ──────────────────────────────────────────────────────────
    fun playAudio(item: AudioItem) { audioPlayer.play(item); _playingName.value = item.name }
    fun stopAudio()                { audioPlayer.stop();     _playingName.value = null }
    fun setVolume(v: Float)        { _volume.value = v.coerceIn(0f,1f); audioPlayer.setPlayerVolume(_volume.value) }

    // ── Sleep tracking (real accelerometer via foreground service) ──────
    // The service runs independently of the ViewModel/Activity lifecycle
    // (so tracking survives the screen turning off) and publishes live
    // state through the process-wide SleepSessionState singleton.
    val sleepPhase: StateFlow<SleepSessionPhase> = SleepSessionState.phase
    val sleepElapsedSeconds: StateFlow<Int> = SleepSessionState.elapsedSeconds
    val sleepCurrentMovement: StateFlow<Float> = SleepSessionState.currentMovement
    val sleepSummary: StateFlow<SleepSummary?> = SleepSessionState.summary

    fun startSleepTracking(context: Context) {
        val intent = Intent(context, SleepTrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopSleepTracking(context: Context) {
        context.stopService(Intent(context, SleepTrackingService::class.java))
    }

    fun resetSleepSummary() {
        SleepSessionState.reset()
    }

    // ── Bluetooth heart-rate device (real external sensor) ──────────────
    // This is the genuine non-camera sensor path: a real PPG/ECG sensor
    // inside a connected chest strap or watch, read over standard BLE
    // GATT Heart Rate Profile. See BleHeartRateManager.
    private val bleManager = BleHeartRateManager(app.applicationContext)
    val bleState: StateFlow<BleHeartRateManager.ConnectionState> = bleManager.state
    val bleDevices: StateFlow<List<BleHeartRateManager.ScannedDevice>> = bleManager.devices
    val bleDeviceName: StateFlow<String?> = bleManager.connectedDeviceName

    private var bleCollectJob: Job? = null
    private val bleRrBuffer = ArrayDeque<Float>()

    fun bleStartScan() = bleManager.startScan()
    fun bleStopScan()  = bleManager.stopScan()
    fun bleConnect(address: String) = bleManager.connect(address)

    fun bleDisconnect() {
        bleCollectJob?.cancel()
        bleManager.disconnect()
        bleRrBuffer.clear()
        if (_liveData.value.hrSource == HrSource.BLUETOOTH_DEVICE) {
            _liveData.value = LiveSensorData()
            _liveAnalysis.value = LiveAnalysis()
        }
    }

    /**
     * Begins treating the connected BLE device as the live heart-rate
     * source: readings replace camera PPG entirely (no camera/torch
     * involved), HRV is computed from the device's own real R-R
     * intervals when it reports them, and the wellness pipeline reuses
     * the same Analyzer used for camera scans.
     */
    fun bleStartLiveSession() {
        bleRrBuffer.clear()
        _liveData.value = LiveSensorData(hrSource = HrSource.BLUETOOTH_DEVICE)
        _liveAnalysis.value = LiveAnalysis()
        _scanState.value = ScanState.SCANNING
        _message.value = "Reading live data from ${bleDeviceName.value ?: "connected device"}..."
        startMs = System.currentTimeMillis()

        bleCollectJob?.cancel()
        bleCollectJob = viewModelScope.launch {
            bleManager.lastReading.collect { reading ->
                if (reading == null) return@collect
                if (reading.rrIntervalsMs.isNotEmpty()) {
                    bleRrBuffer.addAll(reading.rrIntervalsMs)
                    while (bleRrBuffer.size > 120) bleRrBuffer.removeFirst()
                }
                val hrv = if (bleRrBuffer.size >= 6) computeRmssd(bleRrBuffer.toList()) else _liveData.value.hrv
                val updated = _liveData.value.copy(
                    heartRate = reading.heartRate,
                    hrv = if (hrv > 0f) hrv else _liveData.value.hrv,
                    breathingRate = _liveData.value.breathingRate, // BLE HR devices don't report this
                    ppgConfidence = 90, // a connected device reading is real, not a camera-confidence estimate
                    ppgQuality = SignalQuality.EXCELLENT,
                    rrIntervalsMs = bleRrBuffer.toList(),
                    hrSource = HrSource.BLUETOOTH_DEVICE
                )
                _liveData.value = updated
                _liveAnalysis.value = Analyzer.analyze(updated)
            }
        }
    }

    /** Ends the live BLE session and builds a report from the collected data, same as a camera scan. */
    fun bleFinishLiveSession() {
        bleCollectJob?.cancel()
        viewModelScope.launch {
            val data = _liveData.value
            if (data.heartRate <= 0) { _scanState.value = ScanState.IDLE; return@launch }
            _scanState.value = ScanState.ANALYZING
            delay(500L)
            val dur = ((System.currentTimeMillis() - startMs) / 1000).toInt().coerceIn(5, 3600)
            val analysis = Analyzer.analyze(data)
            _liveAnalysis.value = analysis
            _report.value = ReportBuilder.build(data, analysis, dur)
            _scanState.value = ScanState.COMPLETE
        }
    }

    private fun computeRmssd(rr: List<Float>): Float {
        if (rr.size < 2) return 0f
        val diffs = rr.zipWithNext { a, b -> (b - a) * (b - a) }
        return kotlin.math.sqrt(diffs.average()).toFloat().coerceIn(0f, 200f)
    }

    // ── Manual blood pressure log ──────────────────────────────────────
    // No phone/watch sensor can measure blood pressure without a
    // calibrated cuff — this only stores readings the user types in.
    private val bpRepo = BpLogRepository(app)
    val bpEntries: StateFlow<List<BpLogEntry>> = bpRepo.entries
    fun addBpEntry(entry: BpLogEntry) = bpRepo.add(entry)
    fun deleteBpEntry(entry: BpLogEntry) = bpRepo.delete(entry)

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel(); timeoutJob?.cancel()
        bleCollectJob?.cancel(); bleManager.disconnect()
        audioPlayer.release(); setTorch(false)
    }
    // ── AI Chat ────────────────────────────────────────────────────────
    private val _apiKey           = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey

    private val _chatMessages     = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val _chatLoading      = MutableStateFlow(false)
    val chatLoading: StateFlow<Boolean> = _chatLoading

    private val _showApiKeyDialog = MutableStateFlow(false)
    val showApiKeyDialog: StateFlow<Boolean> = _showApiKeyDialog

    fun loadApiKey() {
        val prefs = getApplication<android.app.Application>()
            .getSharedPreferences("biowellness_ai", android.content.Context.MODE_PRIVATE)
        _apiKey.value = prefs.getString("api_key", "") ?: ""
    }

    fun setApiKey(key: String) {
        _apiKey.value = key.trim()
        _showApiKeyDialog.value = false
        getApplication<android.app.Application>()
            .getSharedPreferences("biowellness_ai", android.content.Context.MODE_PRIVATE)
            .edit { putString("api_key", key.trim()) }
    }

    fun openApiKeyDialog()  { _showApiKeyDialog.value = true }
    fun closeApiKeyDialog() { _showApiKeyDialog.value = false }
    fun clearChat()         { _chatMessages.value = emptyList() }

    fun sendChatMessage(message: String) {
        val keyToUse = if (apiKey.value.trim().startsWith("gsk_")) apiKey.value.trim() else AiChatEngine.DEFAULT_API_KEY

        _chatMessages.value = _chatMessages.value + ChatMessage(role = "user", content = message)
        _chatLoading.value = true

        AiChatEngine.send(
            apiKey = keyToUse,
            userMessage = message,
            history = _chatMessages.value.dropLast(1),
            liveAnalysis = liveAnalysis.value,
            report = report.value,
            onSuccess = { response ->
                _chatLoading.value = false
                _chatMessages.value = _chatMessages.value + ChatMessage(role = "assistant", content = response)
            },
            onError = { error ->
                _chatLoading.value = false
                _chatMessages.value = _chatMessages.value + ChatMessage(role = "assistant", content = error, isError = true)
            }
        )
    }
}
