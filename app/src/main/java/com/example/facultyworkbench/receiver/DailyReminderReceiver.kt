package com.example.facultyworkbench.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.facultyworkbench.MainActivity
import com.example.facultyworkbench.R
import com.example.facultyworkbench.data.db.FacultyDatabase
import com.example.facultyworkbench.util.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 每日提醒接收器：到点后查询今日待办任务并发送通知，然后重设次日闹钟。
 */
class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                notifyTodayTasks(context)
            } finally {
                // 无论是否成功，都重新调度下一次提醒
                try {
                    ReminderScheduler.scheduleFromSettings(context)
                } catch (_: Exception) {
                }
                pendingResult.finish()
            }
        }
    }

    private suspend fun notifyTodayTasks(context: Context) {
        val channelId = "daily_reminder"
        ensureChannel(context, channelId)

        // 今日 00:00 ~ 23:59:59
        val now = Calendar.getInstance()
        val startCal = now.clone() as Calendar
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)
        val endCal = startCal.clone() as Calendar
        endCal.add(Calendar.DAY_OF_YEAR, 1)
        endCal.add(Calendar.MILLISECOND, -1)

        val tasks = try {
            FacultyDatabase.getDatabase(context)
                .taskDao()
                .getPendingTasksDueBetween(startCal.timeInMillis, endCal.timeInMillis)
        } catch (e: Exception) {
            emptyList()
        }

        val title = "教师工作台 · 今日提醒"
        val content = if (tasks.isEmpty()) {
            "今天没有待办任务，祝你工作顺利！"
        } else {
            val top = tasks.take(3).joinToString("、") { it.title }
            val more = if (tasks.size > 3) " 等" else ""
            "共 ${tasks.size} 项待办：$top$more"
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPi = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(context: Context, channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId,
                    "每日提醒",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "每日待办任务提醒"
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 20240905
    }
}
