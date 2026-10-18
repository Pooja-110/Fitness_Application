package com.biowellness.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.biowellness.app.data.models.BpLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stores manually-entered blood pressure readings only. This app has no
 * way to measure blood pressure from any phone or watch sensor —
 * accurate BP needs a calibrated cuff. Nothing here is computed.
 */
class BpLogRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("biowellness_bp_log", Context.MODE_PRIVATE)

    private val _entries = MutableStateFlow(load())
    val entries: StateFlow<List<BpLogEntry>> = _entries

    private fun load(): List<BpLogEntry> {
        val raw = prefs.getString("entries", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                BpLogEntry(
                    systolic = o.optInt("sys", 0),
                    diastolic = o.optInt("dia", 0),
                    pulse = o.optInt("pulse", 0),
                    timestampMs = o.optLong("ts", System.currentTimeMillis()),
                    note = o.optString("note", "")
                )
            }.sortedByDescending { it.timestampMs }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persist(list: List<BpLogEntry>) {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(JSONObject().apply {
                put("sys", e.systolic); put("dia", e.diastolic); put("pulse", e.pulse)
                put("ts", e.timestampMs); put("note", e.note)
            })
        }
        prefs.edit().putString("entries", arr.toString()).apply()
    }

    fun add(entry: BpLogEntry) {
        val updated = (_entries.value + entry).sortedByDescending { it.timestampMs }
        _entries.value = updated; persist(updated)
    }

    fun delete(entry: BpLogEntry) {
        val updated = _entries.value.filterNot { it.timestampMs == entry.timestampMs }
        _entries.value = updated; persist(updated)
    }
}
