package com.example.facultyworkbench.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Today : Screen("today", "今日\n任务", Icons.Default.Checklist)
    data object Todo : Screen("todo", "待办", Icons.Default.TaskAlt)
    data object Teaching : Screen("teaching", "教学", Icons.Default.School)
    data object Research : Screen("research", "科研", Icons.Default.Science)
    data object Schedule : Screen("schedule", "课表", Icons.Default.CalendarMonth)
    data object Profile : Screen("profile", "我的", Icons.Default.AccountCircle)

    companion object {
        val bottomItems = listOf(Today, Todo, Teaching, Research, Schedule, Profile)
    }
}
