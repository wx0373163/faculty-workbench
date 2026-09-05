package com.example.facultyworkbench.util

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.example.facultyworkbench.data.entity.CourseEntity
import com.example.facultyworkbench.data.entity.TaskEntity
import java.util.Calendar
import java.util.TimeZone

/**
 * 系统日历同步工具：将任务（按截止日期）与课程（按周次重复）写入系统日历。
 */
object CalendarSync {

    data class SyncResult(
        val syncedTasks: Int,
        val syncedCourses: Int,
        val skippedTasks: Int,
        val message: String
    )

    /** 每节次的开始时间（分钟，从 0 点起算），采用常见高校作息。 */
    private val sectionStartMinutes = intArrayOf(
        8 * 60,       // 1: 08:00
        8 * 60 + 55,  // 2: 08:55
        10 * 60,      // 3: 10:00
        10 * 60 + 55, // 4: 10:55
        14 * 60,      // 5: 14:00
        14 * 60 + 55, // 6: 14:55
        16 * 60,      // 7: 16:00
        16 * 60 + 55, // 8: 16:55
        19 * 60,      // 9: 19:00
        19 * 60 + 55, // 10: 19:55
        20 * 60 + 50, // 11: 20:50
        21 * 60 + 45  // 12: 21:45
    )

    /** 获取第一个可见日历的 ID，没有则返回 null。 */
    private fun getPrimaryCalendarId(context: Context): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE} = 1",
            null,
            null
        )
        return cursor?.use {
            if (it.moveToFirst()) it.getLong(0) else null
        }
    }

    /**
     * 同步全部任务与课程到系统日历。
     * semesterStartDate 为学期起始日（null 则以本周为第 1 周）。
     * semesterEndDate 为学期截止日（用于限制课程重复周数，null 则不限制）。
     */
    fun syncAll(
        context: Context,
        tasks: List<TaskEntity>,
        courses: List<CourseEntity>,
        semesterStartDate: Long?,
        semesterEndDate: Long? = null
    ): SyncResult {
        val calendarId = getPrimaryCalendarId(context)
            ?: return SyncResult(0, 0, tasks.count { it.dueDate != null }, "未找到可用日历，请先在系统日历中添加账户")

        // 第 1 周的周一 00:00
        val week1Monday = Calendar.getInstance().apply {
            if (semesterStartDate != null) timeInMillis = semesterStartDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            val dow = get(Calendar.DAY_OF_WEEK) // 周日=1 ... 周六=7
            val offset = (dow + 5) % 7 // 周一为 0
            add(Calendar.DAY_OF_YEAR, -offset)
        }.timeInMillis

        // 学期总周数（按截止日限制，null 则不限制）
        val semesterWeeks = if (semesterStartDate != null && semesterEndDate != null && semesterEndDate > semesterStartDate) {
            val days = ((semesterEndDate - semesterStartDate) / (1000L * 60 * 60 * 24)).toInt()
            (days / 7) + 1
        } else null

        var syncedTasks = 0
        var skippedTasks = 0
        tasks.forEach { task ->
            val due = task.dueDate
            if (due == null) {
                skippedTasks++
                return@forEach
            }
            if (insertTaskEvent(context, calendarId, task, due)) syncedTasks++ else skippedTasks++
        }

        var syncedCourses = 0
        courses.forEach { course ->
            if (insertCourseEvent(context, calendarId, course, week1Monday, semesterWeeks)) syncedCourses++
        }

        val total = syncedTasks + syncedCourses
        return SyncResult(
            syncedTasks = syncedTasks,
            syncedCourses = syncedCourses,
            skippedTasks = skippedTasks,
            message = if (total == 0) "没有可同步的数据"
            else "已同步 $total 项（任务 $syncedTasks，课程 $syncedCourses）" +
                if (skippedTasks > 0) "，跳过 $skippedTasks 条无截止日期任务" else ""
        )
    }

    private fun insertTaskEvent(context: Context, calendarId: Long, task: TaskEntity, dueDate: Long): Boolean {
        val start = dueDate
        val end = dueDate + 60 * 60 * 1000L // 默认 1 小时
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, "[教师工作台] ${task.title}")
            put(CalendarContract.Events.DESCRIPTION, task.description)
            put(CalendarContract.Events.DTSTART, start)
            put(CalendarContract.Events.DTEND, end)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.HAS_ALARM, 1)
        }
        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) ?: return false
        val eventId = uri.lastPathSegment?.toLongOrNull() ?: return false

        // 到期提醒
        val reminder = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.MINUTES, 0)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        }
        context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminder)
        return true
    }

    private fun insertCourseEvent(
        context: Context,
        calendarId: Long,
        course: CourseEntity,
        week1MondayMillis: Long,
        semesterWeeks: Int?
    ): Boolean {
        val weekCount = parseWeekCount(course.weeks).let {
            if (semesterWeeks != null) it.coerceAtMost(semesterWeeks) else it
        }
        if (weekCount <= 0) return false

        val startSection = course.startSection.coerceIn(1, 12)
        val startMinutes = sectionStartMinutes[startSection - 1]
        val durationMinutes = course.sectionCount.coerceIn(1, 12) * 45 +
            (course.sectionCount.coerceIn(1, 12) - 1) * 10

        val startCal = Calendar.getInstance().apply { timeInMillis = week1MondayMillis }
        startCal.add(Calendar.DAY_OF_YEAR, course.dayOfWeek.coerceIn(1, 7) - 1)
        startCal.add(Calendar.MINUTE, startMinutes)
        val startMillis = startCal.timeInMillis
        val endMillis = startMillis + durationMinutes * 60 * 1000L

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, course.name)
            put(CalendarContract.Events.EVENT_LOCATION, course.location)
            put(
                CalendarContract.Events.DESCRIPTION,
                "${course.className}｜周次：${course.weeks.ifBlank { "未设置" }}｜节次：$startSection-${startSection + course.sectionCount - 1}"
            )
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.RRULE, "FREQ=WEEKLY;COUNT=$weekCount")
            put(CalendarContract.Events.HAS_ALARM, 1)
        }
        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) ?: return false
        val eventId = uri.lastPathSegment?.toLongOrNull() ?: return false

        // 课前 15 分钟提醒
        val reminder = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.MINUTES, 15)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        }
        context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminder)
        return true
    }

    /** 解析周次字符串（如 "1-16"、"1,3,5"、"1-8,10-16"、"单周"、"双周"），返回周数。 */
    private fun parseWeekCount(weeks: String): Int {
        if (weeks.isBlank()) return 16
        val trimmed = weeks.trim()
        if (trimmed == "单周" || trimmed == "双周") return 8

        val set = mutableSetOf<Int>()
        trimmed.split(",").forEach { part ->
            val p = part.trim()
            if (p.contains("-")) {
                val parts = p.split("-").mapNotNull { it.trim().toIntOrNull() }
                if (parts.size == 2) for (w in parts[0]..parts[1]) set.add(w)
            } else {
                p.toIntOrNull()?.let { set.add(it) }
            }
        }
        return if (set.isEmpty()) 16 else set.size
    }
}
