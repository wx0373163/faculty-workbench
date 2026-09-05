package com.example.facultyworkbench

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.facultyworkbench.data.db.FacultyDatabase
import com.example.facultyworkbench.data.repository.FacultyRepository
import com.example.facultyworkbench.ui.theme.FacultyWorkbenchTheme
import com.example.facultyworkbench.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: FacultyRepository
    private val scope = CoroutineScope(Dispatchers.Main)

    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val semesterStartDateKey = longPreferencesKey("semester_start_date")
    private val semesterEndDateKey = longPreferencesKey("semester_end_date")
    private val reminderEnabledKey = booleanPreferencesKey("daily_reminder_enabled")
    private val reminderHourKey = intPreferencesKey("daily_reminder_hour")
    private val reminderMinuteKey = intPreferencesKey("daily_reminder_minute")

    private lateinit var themeModeFlow: StateFlow<ThemeMode>
    private lateinit var semesterStartDateFlow: StateFlow<Long?>
    private lateinit var semesterEndDateFlow: StateFlow<Long?>
    private lateinit var reminderEnabledFlow: StateFlow<Boolean>
    private lateinit var reminderHourFlow: StateFlow<Int>
    private lateinit var reminderMinuteFlow: StateFlow<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = FacultyDatabase.getDatabase(applicationContext)
        repository = FacultyRepository(
            taskDao = db.taskDao(),
            courseDao = db.courseDao(),
            researchTaskDao = db.researchTaskDao(),
            profileDao = db.profileDao()
        )

        themeModeFlow = settingsDataStore.data
            .map { prefs ->
                when (prefs[themeModeKey]) {
                    "light" -> ThemeMode.LIGHT
                    "dark" -> ThemeMode.DARK
                    else -> ThemeMode.SYSTEM
                }
            }
            .stateIn(scope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

        semesterStartDateFlow = settingsDataStore.data
            .map { prefs -> prefs[semesterStartDateKey] }
            .stateIn(scope, SharingStarted.Eagerly, null)

        semesterEndDateFlow = settingsDataStore.data
            .map { prefs -> prefs[semesterEndDateKey] }
            .stateIn(scope, SharingStarted.Eagerly, null)

        reminderEnabledFlow = settingsDataStore.data
            .map { prefs -> prefs[reminderEnabledKey] ?: false }
            .stateIn(scope, SharingStarted.Eagerly, false)

        reminderHourFlow = settingsDataStore.data
            .map { prefs -> prefs[reminderHourKey] ?: 8 }
            .stateIn(scope, SharingStarted.Eagerly, 8)

        reminderMinuteFlow = settingsDataStore.data
            .map { prefs -> prefs[reminderMinuteKey] ?: 0 }
            .stateIn(scope, SharingStarted.Eagerly, 0)

        setContent {
            val themeMode by themeModeFlow.collectAsState()
            val semesterStartDate by semesterStartDateFlow.collectAsState()
            val semesterEndDate by semesterEndDateFlow.collectAsState()
            val reminderEnabled by reminderEnabledFlow.collectAsState()
            val reminderHour by reminderHourFlow.collectAsState()
            val reminderMinute by reminderMinuteFlow.collectAsState()
            FacultyWorkbenchTheme(themeMode = themeMode) {
                FacultyApp(
                    repository = repository,
                    themeMode = themeMode,
                    onThemeChange = { mode ->
                        scope.launch {
                            settingsDataStore.edit { prefs ->
                                prefs[themeModeKey] = when (mode) {
                                    ThemeMode.LIGHT -> "light"
                                    ThemeMode.DARK -> "dark"
                                    ThemeMode.SYSTEM -> "system"
                                }
                            }
                        }
                    },
                    semesterStartDate = semesterStartDate,
                    onSemesterStartDateChange = { date ->
                        scope.launch {
                            settingsDataStore.edit { prefs ->
                                if (date == null) prefs.remove(semesterStartDateKey)
                                else prefs[semesterStartDateKey] = date
                            }
                        }
                    },
                    semesterEndDate = semesterEndDate,
                    onSemesterEndDateChange = { date ->
                        scope.launch {
                            settingsDataStore.edit { prefs ->
                                if (date == null) prefs.remove(semesterEndDateKey)
                                else prefs[semesterEndDateKey] = date
                            }
                        }
                    },
                    reminderEnabled = reminderEnabled,
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute,
                    onReminderConfigChange = { enabled, hour, minute ->
                        scope.launch {
                            settingsDataStore.edit { prefs ->
                                prefs[reminderEnabledKey] = enabled
                                prefs[reminderHourKey] = hour
                                prefs[reminderMinuteKey] = minute
                            }
                        }
                    }
                )
            }
        }
    }
}
