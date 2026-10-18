package com.biowellness.app.utils

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * BleHeartRateManager
 *
 * Connects to a REAL external heart-rate sensor (a chest strap or
 * smartwatch) over Bluetooth LE, using the standard Bluetooth SIG
 * Heart Rate Service (0x180D) / Heart Rate Measurement characteristic
 * (0x2A37). This is the actual "smartwatch-style" sensor path: a
 * dedicated PPG or ECG sensor inside the connected device — not the
 * phone's camera, and not a simulation.
 *
 * Any device implementing the standard Bluetooth Heart Rate Profile
 * (most chest straps — Polar H10, Wahoo TICKR — and many watches that
 * expose this service) works here with no per-device/vendor code.
 *
 * If the connected device also reports R-R intervals (many chest
 * straps do; most optical wrist devices don't), those are exposed too
 * and are far more reliable than camera-derived R-R timing — that's
 * what feeds the higher-confidence path in RhythmAnalyzer and gives
 * genuinely accurate HRV instead of a camera approximation.
 */
class BleHeartRateManager(private val context: Context) {

    enum class ConnectionState { DISCONNECTED, SCANNING, CONNECTING, CONNECTED, FAILED }

    data class Reading(
        val heartRate: Int,
        val rrIntervalsMs: List<Float>, // empty if this device doesn't report RR
        val timestampMs: Long
    )

    data class ScannedDevice(val name: String, val address: String)

    private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
    val state: StateFlow<ConnectionState> = _state

    private val _devices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val devices: StateFlow<List<ScannedDevice>> = _devices

    private val _lastReading = MutableStateFlow<Reading?>(null)
    val lastReading: StateFlow<Reading?> = _lastReading

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName

    private var gatt: BluetoothGatt? = null
    private var scanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null

    companion object {
        val HEART_RATE_SERVICE: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_MEASUREMENT: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val CCC_DESCRIPTOR: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private fun adapter(): BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private fun hasPermissions(): Boolean {
        // The UI layer is responsible for requesting these at runtime
        // (see BleConnectScreen) — this is a final guard so we never
        // call a BLE API without permission already granted.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!hasPermissions()) { _state.value = ConnectionState.FAILED; return }
        val a = adapter()
        if (a == null || !a.isEnabled) { _state.value = ConnectionState.FAILED; return }

        stopScan()
        _devices.value = emptyList()
        _state.value = ConnectionState.SCANNING

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(HEART_RATE_SERVICE))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val cb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val dev = result.device
                val name = try { dev.name } catch (_: SecurityException) { null } ?: "Unknown heart rate device"
                val addr = dev.address
                if (_devices.value.none { it.address == addr }) {
                    _devices.value = _devices.value + ScannedDevice(name, addr)
                }
            }
            override fun onScanFailed(errorCode: Int) {
                _state.value = ConnectionState.FAILED
            }
        }
        scanCallback = cb
        scanner = a.bluetoothLeScanner
        scanner?.startScan(listOf(filter), settings, cb)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanCallback?.let { scanner?.stopScan(it) }
        scanCallback = null
        if (_state.value == ConnectionState.SCANNING) _state.value = ConnectionState.DISCONNECTED
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String) {
        if (!hasPermissions()) { _state.value = ConnectionState.FAILED; return }
        stopScan()
        val a = adapter() ?: return
        val device = try { a.getRemoteDevice(address) } catch (_: IllegalArgumentException) { return }
        _state.value = ConnectionState.CONNECTING

        gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        _connectedDeviceName.value = try { device.name } catch (_: SecurityException) { null } ?: address
                        g.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        _state.value = ConnectionState.DISCONNECTED
                        _connectedDeviceName.value = null
                    }
                }
            }

            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) { _state.value = ConnectionState.FAILED; return }
                val svc = g.getService(HEART_RATE_SERVICE)
                val ch = svc?.getCharacteristic(HEART_RATE_MEASUREMENT)
                if (ch == null) { _state.value = ConnectionState.FAILED; return }
                g.setCharacteristicNotification(ch, true)
                val ccc = ch.getDescriptor(CCC_DESCRIPTOR)
                if (ccc != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        g.writeDescriptor(ccc, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    } else {
                        @Suppress("DEPRECATION")
                        ccc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        @Suppress("DEPRECATION")
                        g.writeDescriptor(ccc)
                    }
                }
                _state.value = ConnectionState.CONNECTED
            }

            // API 33+ callback (delivers the value directly).
            override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                parseHeartRateMeasurement(value)?.let { _lastReading.value = it }
            }

            // Pre-33 callback path (value read from the characteristic).
            @Suppress("DEPRECATION")
            override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return // handled by the overload above
                val value = characteristic.value ?: return
                parseHeartRateMeasurement(value)?.let { _lastReading.value = it }
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        _state.value = ConnectionState.DISCONNECTED
        _connectedDeviceName.value = null
    }

    /**
     * Parses the standard Bluetooth SIG "Heart Rate Measurement"
     * characteristic (org.bluetooth.characteristic.heart_rate_measurement).
     *
     * Byte 0: flags
     *   bit 0: 0 = HR in byte 1 (uint8), 1 = HR in bytes 1-2 (uint16 LE)
     *   bits 1-2: sensor contact status (not used here)
     *   bit 3: energy expended present (uint16, skipped if present)
     *   bit 4: one or more RR-Interval values present (uint16 LE each,
     *          in units of 1/1024 second)
     */
    private fun parseHeartRateMeasurement(value: ByteArray): Reading? {
        if (value.isEmpty()) return null
        val flags = value[0].toInt() and 0xFF
        var offset = 1

        val hr: Int
        if (flags and 0x01 != 0) {
            if (value.size < offset + 2) return null
            hr = (value[offset].toInt() and 0xFF) or ((value[offset + 1].toInt() and 0xFF) shl 8)
            offset += 2
        } else {
            if (value.size < offset + 1) return null
            hr = value[offset].toInt() and 0xFF
            offset += 1
        }

        // Skip Energy Expended field if present (uint16).
        if (flags and 0x08 != 0) offset += 2

        val rrList = mutableListOf<Float>()
        if (flags and 0x10 != 0) {
            while (offset + 1 < value.size) {
                val raw = (value[offset].toInt() and 0xFF) or ((value[offset + 1].toInt() and 0xFF) shl 8)
                // Units are 1/1024 second per the BLE spec — convert to ms.
                rrList.add(raw * 1000f / 1024f)
                offset += 2
            }
        }

        if (hr <= 0) return null
        return Reading(heartRate = hr, rrIntervalsMs = rrList, timestampMs = System.currentTimeMillis())
    }
}
