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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    var showDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
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

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("今日任务") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingTask = null
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "新增")
            }
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
                    label = { Text("全部") }
                )
                statusLabels.forEachIndexed { idx, label ->
                    FilterChip(
                        selected = selectedStatus == idx,
                        onClick = { selectedStatus = idx },
                        label = { Text(label) }
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
                    Text("今日暂无任务，点击右下角 + 添加", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        onEdit = {
                            editingTask = task
                            showDialog = true
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

    if (showDialog) {
        TaskEditDialog(
            task = editingTask,
            onDismiss = { showDialog = false },
            onSave = { title, desc, priority, dueDate, type, progress ->
                scope.launch {
                    val status = statusFromProgress(progress)
                    if (editingTask != null) {
                        repository.updateTask(
                            editingTask!!.copy(
                                title = title,
                                description = desc,
                                priority = priority,
                                dueDate = dueDate,
                                type = type,
                                status = status,
                                progress = progress
                            )
                        )
                    } else {
                        repository.insertTask(
                            TaskEntity(
                                title = title,
                                description = desc,
                                priority = priority,
                                dueDate = dueDate,
                                type = type,
                                status = status,
                                progress = progress
                            )
                        )
                    }
                    showDialog = false
                    snackbarHostState.showSnackbar("已保存")
                }
            }
        )
    }
}

@Composable
private fun TaskItem(
    task: TaskEntity,
    onToggleDone: () -> Unit,
    onEdit: () -> Unit,
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
                .clickable { onEdit() }
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
                            java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
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

@Composable
private fun TaskEditDialog(
    task: TaskEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Long?, String, Int) -> Unit
) {
    var title by remember(task) { mutableStateOf(task?.title ?: "") }
    var desc by remember(task) { mutableStateOf(task?.description ?: "") }
    var priority by remember(task) { mutableStateOf(task?.priority ?: 0) }
    var type by remember(task) { mutableStateOf(task?.type ?: "general") }
    var expanded by remember { mutableStateOf(false) }
    // 0=无截止 1=今天 2=明天 3=下周
    var dueOption by remember(task) { mutableStateOf(if (task?.dueDate == null) 0 else 1) }
    var progress by remember(task) { mutableStateOf(task?.progress ?: 0) }

    fun resolveDueDate(): Long? {
        val cal = java.util.Calendar.getInstance()
        return when (dueOption) {
            0 -> null
            1 -> cal.timeInMillis
            2 -> { cal.add(java.util.Calendar.DAY_OF_YEAR, 1); cal.timeInMillis }
            3 -> { cal.add(java.util.Calendar.DAY_OF_YEAR, 7); cal.timeInMillis }
            else -> null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (task == null) "新增任务" else "编辑任务") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("描述") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text("优先级", style = MaterialTheme.typography.labelLarge)
                Row {
                    priorityLabels.forEachIndexed { idx, label ->
                        TextButton(onClick = { priority = idx }) {
                            Text(label, color = if (priority == idx) priorityColors[idx] else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("类型", style = MaterialTheme.typography.labelLarge)
                Box {
                    TextButton(onClick = { expanded = true }) { Text(type) }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf("教学", "科研", "通用").forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = {
                                    type = when (t) {
                                        "教学" -> "teaching"
                                        "科研" -> "research"
                                        else -> "general"
                                    }
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("截止日期", style = MaterialTheme.typography.labelLarge)
                Row {
                    listOf("无截止", "今天", "明天", "下周").forEachIndexed { idx, label ->
                        TextButton(onClick = { dueOption = idx }) {
                            Text(label, color = if (dueOption == idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                val derivedStatus = statusFromProgress(progress)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("进度", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        statusLabels[derivedStatus],
                        color = statusColors[derivedStatus],
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Slider(
                    value = progress.toFloat(),
                    onValueChange = { progress = it.toInt() },
                    valueRange = 0f..100f,
                    steps = 99
                )
                Text("${progress}%", style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = { if (title.isNotBlank()) onSave(title, desc, priority, resolveDueDate(), type, progress) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
