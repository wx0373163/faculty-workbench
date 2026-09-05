package com.example.facultyworkbench.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.facultyworkbench.util.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 开机完成后重新调度每日提醒闹钟（重启后 AlarmManager 闹钟会被清除）。
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderScheduler.scheduleFromSettings(context)
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}
