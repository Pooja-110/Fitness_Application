package com.biowellness.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.biowellness.app.data.models.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("biowellness_profile", Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(load())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    val hasProfile: Boolean get() = !_profile.value.isEmpty

    private fun load() = UserProfile(
        age            = prefs.getString("age", "") ?: "",
        gender         = prefs.getString("gender", "") ?: "",
        height         = prefs.getString("height", "") ?: "",
        weight         = prefs.getString("weight", "") ?: "",
        activityLevel  = prefs.getString("activity", "") ?: "",
        sleepHours     = prefs.getString("sleep", "") ?: "",
        stressLevel    = prefs.getString("stress", "") ?: "",
        smokingStatus  = prefs.getString("smoking", "") ?: "",
        alcoholUse     = prefs.getString("alcohol", "") ?: "",
        medicalHistory = prefs.getString("medical", "") ?: ""
    )

    fun save(p: UserProfile) {
        prefs.edit()
            .putString("age", p.age)
            .putString("gender", p.gender)
            .putString("height", p.height)
            .putString("weight", p.weight)
            .putString("activity", p.activityLevel)
            .putString("sleep", p.sleepHours)
            .putString("stress", p.stressLevel)
            .putString("smoking", p.smokingStatus)
            .putString("alcohol", p.alcoholUse)
            .putString("medical", p.medicalHistory)
            .apply()
        _profile.value = p
    }

    fun delete() {
        prefs.edit().clear().apply()
        _profile.value = UserProfile()
    }

    fun reload() { _profile.value = load() }
}
