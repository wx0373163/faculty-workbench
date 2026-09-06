package com.example.facultyworkbench.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.facultyworkbench.data.entity.CourseEntity
import com.example.facultyworkbench.data.entity.TaskEntity
import com.example.facultyworkbench.data.repository.FacultyRepository
import com.example.facultyworkbench.ui.theme.courseColors
import kotlinx.coroutines.launch

private val priorityLabels = listOf("普通", "重要", "紧急")
private val priorityColors = listOf(
    androidx.compose.ui.graphics.Color(0xFF74777F),
    androidx.compose.ui.graphics.Color(0xFFFF9800),
    androidx.compose.ui.graphics.Color(0xFFE53935)
)

// 任务状态：0=待办 1=进行中 2=已完成
private val statusLabels = listOf("待办", "进行中", "已完成")
private val statusColors = listOf(
    androidx.compose.ui.graphics.Color(0xFF74777F), // 待办 灰
    androidx.compose.ui.graphics.Color(0xFF1976D2), // 进行中 蓝
    androidx.compose.ui.graphics.Color(0xFF43A047)  // 已完成 绿
)

/** 根据进度推导状态 */
private fun statusFromProgress(progress: Int): Int = when {
    progress <= 0 -> 0
    progress >= 100 -> 2
    else -> 1
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayTasksScreen(
    repository: FacultyRepository,
    snackbarHostState: SnackbarHostState,
    onNavigateToSchedule: () -> Unit = {}
) {
    val tasks by repository.allTasks.collectAsState(initial = emptyList())
    val courses by repository.allCourses.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    // 今日课程（按星期几过滤，按开始节次排序）
    val todayCourses = remember(courses) {
        val cal = java.util.Calendar.getInstance()
        val dow = cal.get(java.util.Calendar.DAY_OF_WEEK) // 周日=1 ... 周六=7
        val today = ((dow + 5) % 7) + 1 // 周一=1 ... 周日=7
        courses.filter { it.dayOfWeek == today }.sortedBy { it.startSection }
    }
    var selectedStatus by remember { mutableStateOf<Int?>(null) } // null=全部

    // 过滤"今日任务"：截止日期为今天，或无截止日期的未完成任务
    val todayTasks = remember(tasks) {
        val startOfToday = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfToday = startOfToday + 24 * 60 * 60 * 1000
        tasks.filter { task ->
            val due = task.dueDate
            (due == null) || (due in startOfToday until endOfToday)
        }
    }

    val filteredTasks = remember(todayTasks, selectedStatus) {
        if (selectedStatus == null) todayTasks else todayTasks.filter { it.status == selectedStatus }
    }

    // 各状态数量，用于筛选标签联动显示
    val countAll = todayTasks.size
    val countTodo = todayTasks.count { it.status == 0 }
    val countDoing = todayTasks.count { it.status == 1 }
    val countDone = todayTasks.count { it.status == 2 }
    val statusCounts = listOf(countTodo, countDoing, countDone)

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("今日任务") })
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = { selectedStatus = null },
                    label = { Text("全部 $countAll") }
                )
                statusLabels.forEachIndexed { idx, label ->
                    FilterChip(
                        selected = selectedStatus == idx,
                        onClick = { selectedStatus = idx },
                        label = { Text("$label ${statusCounts[idx]}") }
                    )
                }
            }

            if (todayCourses.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clickable { onNavigateToSchedule() }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "今日课程（${todayCourses.size}）",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "查看课表 ›",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        todayCourses.forEach { course ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clickable { onNavigateToSchedule() },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(32.dp)
                                        .background(
                                            courseColors[course.color % courseColors.size],
                                            MaterialTheme.shapes.small
                                        )
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        course.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "第${course.startSection}-${course.startSection + course.sectionCount - 1}节 · ${course.location.ifBlank { "未设置地点" }}${if (course.className.isNotBlank()) " · ${course.className}" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("今日暂无任务，请在「待办」「教学」「科研」中添加", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                    TaskItem(
                        task = task,
                        onToggleDone = {
                            scope.launch {
                                if (task.isDone) {
                                    // 恢复：根据进度回到待办或进行中
                                    repository.updateTaskStatus(task.id, statusFromProgress(task.progress), task.progress)
                                    snackbarHostState.showSnackbar("已恢复")
                                } else {
                                    // 标记完成
                                    repository.updateTaskStatus(task.id, 2, task.progress)
                                    snackbarHostState.showSnackbar("已完成")
                                }
                            }
                        },
                        onDelete = {
                            scope.launch {
                                repository.deleteTask(task)
                                snackbarHostState.showSnackbar("已删除")
                            }
                        }
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun TaskItem(
    task: TaskEntity,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isDone)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = task.isDone, onCheckedChange = { onToggleDone() })
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null
                )
                if (task.description.isNotBlank()) {
                    Text(
                        task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        priorityLabels[task.priority],
                        color = priorityColors[task.priority],
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        statusLabels[task.status],
                        color = statusColors[task.status],
                        style = MaterialTheme.typography.labelLarge
                    )
                    if (task.dueDate != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
                                .format(java.util.Date(task.dueDate)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                val displayProgress = if (task.isDone) 100 else task.progress
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { displayProgress / 100f },
                        modifier = Modifier.weight(1f).height(6.dp),
                        color = statusColors[task.status]
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "$displayProgress%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
