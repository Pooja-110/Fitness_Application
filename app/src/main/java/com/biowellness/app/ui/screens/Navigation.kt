package com.biowellness.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.biowellness.app.viewmodel.WellnessViewModel

object Route {
    const val HOME      = "home"
    const val PROFILE   = "profile"
    const val SCAN      = "scan"
    const val DASHBOARD = "dashboard"
    const val REPORT    = "report"
    const val FREQ      = "frequencies"
    const val AI_CHAT   = "ai_chat"
    const val SLEEP     = "sleep_tracking"
    const val BLE_HR    = "ble_heart_rate"
    const val GYM       = "gym"
    const val BP_LOG    = "bp_log"
}

@Composable
fun AppNavGraph(vm: WellnessViewModel = viewModel()) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Route.HOME) {
        composable(Route.HOME)      { HomeScreen(nav, vm) }
        composable(Route.PROFILE)   { ProfileScreen(nav, vm) }
        composable(Route.SCAN)      { ScanScreen(nav, vm) }
        composable(Route.DASHBOARD) { DashboardScreen(nav, vm) }
        composable(Route.REPORT)    { ReportScreen(nav, vm) }
        composable(Route.FREQ)      { FrequenciesScreen(nav, vm) }
        composable(Route.AI_CHAT)   { AiChatScreen(nav, vm) }
        composable(Route.SLEEP)     { SleepScreen(nav, vm) }
        composable(Route.BLE_HR)    { BleConnectScreen(nav, vm) }
        composable(Route.GYM)       { GymScreen(nav, vm) }
        composable("exercise_detail/{id}") { entry ->
            ExerciseDetailScreen(nav, entry.arguments?.getString("id") ?: "")
        }
        composable(Route.BP_LOG)    { BpLogScreen(nav, vm) }
    }
}
