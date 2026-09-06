package com.example.facultyworkbench.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.facultyworkbench.data.entity.CourseEntity
import com.example.facultyworkbench.data.entity.TaskEntity
import com.example.facultyworkbench.data.repository.FacultyRepository
import com.example.facultyworkbench.ui.theme.courseColors

private val taskPriorityColors = listOf(
    Color(0xFF74777F), // 普通 灰
    Color(0xFFFF9800), // 重要 橙
    Color(0xFFE53935)  // 紧急 红
)

private val dayNames = listOf("一", "二", "三", "四", "五", "六", "日")
private const val maxSections = 10

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(repository: FacultyRepository) {
    val courses by repository.allCourses.collectAsState(initial = emptyList())
    val tasks by repository.allTasks.collectAsState(initial = emptyList())
    var selectedCourse by remember { mutableStateOf<CourseEntity?>(null) }
    var selectedDayTasks by remember { mutableStateOf<Pair<Int, List<TaskEntity>>?>(null) }

    // 本周一 00:00 到下周一 00:00
    val weekStart = remember {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            val dow = get(java.util.Calendar.DAY_OF_WEEK) // 周日=1 ... 周六=7
            val offset = (dow + 5) % 7 // 周一为 0
            add(java.util.Calendar.DAY_OF_YEAR, -offset)
        }.timeInMillis
    }
    val weekEnd = weekStart + 7 * 24 * 60 * 60 * 1000L

    // 本周任务按星期几分组（1=周一 ... 7=周日）
    val tasksByDay = remember(tasks, weekStart, weekEnd) {
        val map = mutableMapOf<Int, MutableList<TaskEntity>>()
        tasks.forEach { task ->
            val due = task.dueDate ?: return@forEach
            if (due !in weekStart until weekEnd) return@forEach
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = due }
            val dow = cal.get(java.util.Calendar.DAY_OF_WEEK)
            val day = ((dow + 5) % 7) + 1 // 转为周一=1 ... 周日=7
            map.getOrPut(day) { mutableListOf() }.add(task)
        }
        map
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("课表") })

        if (courses.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无课程，请先在「教学」中添加课程", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // 表头：星期
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Box(modifier = Modifier.width(40.dp)) { }
                dayNames.forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f).padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(day, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    }
                }
            }

            // 节次行
            for (section in 1..maxSections) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.width(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$section", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    for (day in 1..7) {
                        val course = courses.firstOrNull {
                            it.dayOfWeek == day && section in it.startSection until (it.startSection + it.sectionCount)
                        }
                        when {
                            course == null -> {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(2.dp)
                                        .height(56.dp)
                                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                                )
                            }
                            section == course.startSection -> {
                                // 课程起始节次：显示课程名与地点
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(2.dp)
                                        .height(56.dp)
                                        .background(
                                            courseColors[course.color % courseColors.size].copy(alpha = 0.85f),
                                            MaterialTheme.shapes.small
                                        )
                                        .clickable { selectedCourse = course }
                                        .padding(4.dp)
                                ) {
                                    Column {
                                        Text(
                                            course.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White,
                                            maxLines = 2
                                        )
                                        Text(
                                            course.location.ifBlank { "—" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    }
                                }
                            }
                            else -> {
                                // 课程中间节次：同色延续块
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(2.dp)
                                        .height(56.dp)
                                        .background(
                                            courseColors[course.color % courseColors.size].copy(alpha = 0.85f),
                                            MaterialTheme.shapes.small
                                        )
                                        .clickable { selectedCourse = course }
                                )
                            }
                        }
                    }
                }
            }
            // 本周任务行
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                    Text("任务", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                for (day in 1..7) {
                    val dayTasks = tasksByDay[day] ?: emptyList()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp)
                            .height(48.dp)
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                            .clickable { if (dayTasks.isNotEmpty()) selectedDayTasks = day to dayTasks }
                            .padding(4.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        if (dayTasks.isEmpty()) {
                            Text("—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        } else {
                            Column {
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    dayTasks.take(5).forEach { task ->
                                        Box(
                                            modifier = Modifier
                                                .width(8.dp)
                                                .height(8.dp)
                                                .background(taskPriorityColors[task.priority.coerceIn(0, 2)], CircleShape)
                                        )
                                    }
                                    if (dayTasks.size > 5) {
                                        Text("+${dayTasks.size - 5}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Text("${dayTasks.size}项", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        }
    }

    selectedCourse?.let { course ->
        AlertDialog(
            onDismissRequest = { selectedCourse = null },
            title = { Text(course.name) },
            text = {
                Column {
                    Text("班级：${course.className.ifBlank { "—" }}")
                    Text("时间：周${dayNames[course.dayOfWeek - 1]} 第${course.startSection}-${course.startSection + course.sectionCount - 1}节")
                    Text("地点：${course.location.ifBlank { "—" }}")
                    if (course.weeks.isNotBlank()) Text("周次：${course.weeks}")
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedCourse = null }) { Text("关闭") }
            }
        )
    }

    selectedDayTasks?.let { (day, dayTasks) ->
        val statusLabels = listOf("待办", "进行中", "已完成")
        AlertDialog(
            onDismissRequest = { selectedDayTasks = null },
            title = { Text("周${dayNames[day - 1]} 任务（${dayTasks.size}）") },
            text = {
                Column {
                    dayTasks.sortedBy { it.status }.forEach { task ->
                        Text(
                            "• ${task.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                statusLabels[task.status],
                                color = taskPriorityColors[task.priority.coerceIn(0, 2)],
                                style = MaterialTheme.typography.labelSmall
                            )
                            if (task.dueDate != null) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(task.dueDate)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDayTasks = null }) { Text("关闭") }
            }
        )
    }
}
