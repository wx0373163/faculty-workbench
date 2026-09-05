package com.example.facultyworkbench.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.facultyworkbench.receiver.DailyReminderReceiver
import com.example.facultyworkbench.settingsDataStore
import kotlinx.coroutines.flow.first

/**
 * 每日提醒闹钟调度工具。
 * - Android 12+ 优先使用精确闹钟（需 SCHEDULE_EXACT_ALARM 权限），否则降级为 setAndAllowWhileIdle。
 * - 每次触发后由 Receiver 重新调度次日闹钟，实现每日循环。
 */
object ReminderScheduler {

    private const val REQUEST_CODE = 20240905

    /** 从 DataStore 读取配置并调度（若已启用）。 */
    suspend fun scheduleFromSettings(context: Context) {
        val prefs = context.settingsDataStore.data.first()
        val enabled = prefs[booleanPreferencesKeyCompat("daily_reminder_enabled")] ?: false
        val hour = prefs[intPreferencesKeyCompat("daily_reminder_hour")] ?: 8
        val minute = prefs[intPreferencesKeyCompat("daily_reminder_minute")] ?: 0
        if (enabled) {
            scheduleDailyReminder(context, hour, minute)
        } else {
            cancelDailyReminder(context)
        }
    }

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context)

        val triggerAt = nextTriggerAtMillis(hour, minute)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (e: SecurityException) {
            // 缺少精确闹钟权限时降级
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPendingIntent(context))
    }

    /** 计算下一次触发时间（今天的指定时刻，若已过则为明天）。 */
    fun nextTriggerAtMillis(hour: Int, minute: Int): Long {
        val now = System.currentTimeMillis()
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyReminderReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    // DataStore key 的兼容辅助（避免在工具类中重复声明 key 常量）
    private fun booleanPreferencesKeyCompat(name: String) =
        androidx.datastore.preferences.core.booleanPreferencesKey(name)

    private fun intPreferencesKeyCompat(name: String) =
        androidx.datastore.preferences.core.intPreferencesKey(name)
}
