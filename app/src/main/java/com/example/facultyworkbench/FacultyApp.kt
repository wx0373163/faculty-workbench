package com.example.facultyworkbench

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.facultyworkbench.data.repository.FacultyRepository
import com.example.facultyworkbench.ui.navigation.Screen
import com.example.facultyworkbench.ui.research.ResearchScreen
import com.example.facultyworkbench.ui.schedule.ScheduleScreen
import com.example.facultyworkbench.ui.teaching.TeachingScreen
import com.example.facultyworkbench.ui.today.TodayTasksScreen
import com.example.facultyworkbench.ui.profile.ProfileScreen
import com.example.facultyworkbench.ui.theme.ThemeMode

@Composable
fun FacultyApp(
    repository: FacultyRepository,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    semesterStartDate: Long?,
    onSemesterStartDateChange: (Long?) -> Unit,
    semesterEndDate: Long?,
    onSemesterEndDateChange: (Long?) -> Unit,
    reminderEnabled: Boolean,
    reminderHour: Int,
    reminderMinute: Int,
    onReminderConfigChange: (Boolean, Int, Int) -> Unit
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                Screen.bottomItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Today.route) {
                TodayTasksScreen(
                    repository = repository,
                    snackbarHostState = snackbarHostState,
                    onNavigateToSchedule = {
                        navController.navigate(Screen.Schedule.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Teaching.route) {
                TeachingScreen(repository = repository, snackbarHostState = snackbarHostState)
            }
            composable(Screen.Research.route) {
                ResearchScreen(repository = repository, snackbarHostState = snackbarHostState)
            }
            composable(Screen.Schedule.route) {
                ScheduleScreen(repository = repository)
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    repository = repository,
                    themeMode = themeMode,
                    onThemeChange = onThemeChange,
                    semesterStartDate = semesterStartDate,
                    onSemesterStartDateChange = onSemesterStartDateChange,
                    semesterEndDate = semesterEndDate,
                    onSemesterEndDateChange = onSemesterEndDateChange,
                    reminderEnabled = reminderEnabled,
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute,
                    onReminderConfigChange = onReminderConfigChange,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}
