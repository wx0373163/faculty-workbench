package com.example.facultyworkbench.ui.research

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import com.example.facultyworkbench.data.entity.ResearchTaskEntity
import com.example.facultyworkbench.data.repository.FacultyRepository
import kotlinx.coroutines.launch

private val priorityLabels = listOf("普通", "重要", "紧急")
private val priorityColors = listOf(
    androidx.compose.ui.graphics.Color(0xFF74777F),
    androidx.compose.ui.graphics.Color(0xFFFF9800),
    androidx.compose.ui.graphics.Color(0xFFE53935)
)
private val categories = listOf("论文", "项目", "实验", "申报", "其他")

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
fun ResearchScreen(
    repository: FacultyRepository,
    snackbarHostState: SnackbarHostState
) {
    val tasks by repository.allResearchTasks.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<ResearchTaskEntity?>(null) }
    var selectedStatus by remember { mutableStateOf<Int?>(null) } // null=全部

    val filteredTasks = remember(tasks, selectedStatus) {
        if (selectedStatus == null) tasks else tasks.filter { it.status == selectedStatus }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("科研待办") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editingTask = null; showDialog = true }) {
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
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无科研待办，点击右下角 + 添加", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                    ResearchItem(
                        task = task,
                        onToggleDone = {
                            scope.launch {
                                if (task.isDone) {
                                    repository.updateResearchStatus(task.id, statusFromProgress(task.progress), task.progress)
                                    snackbarHostState.showSnackbar("已恢复")
                                } else {
                                    repository.updateResearchStatus(task.id, 2, task.progress)
                                    snackbarHostState.showSnackbar("已完成")
                                }
                            }
                        },
                        onEdit = { editingTask = task; showDialog = true },
                        onDelete = {
                            scope.launch {
                                repository.deleteResearchTask(task)
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
        ResearchEditDialog(
            task = editingTask,
            onDismiss = { showDialog = false },
            onSave = { title, category, priority, dueDate, note, progress ->
                scope.launch {
                    val status = statusFromProgress(progress)
                    if (editingTask != null) {
                        repository.updateResearchTask(
                            editingTask!!.copy(
                                title = title, category = category, priority = priority,
                                dueDate = dueDate, note = note, progress = progress, status = status
                            )
                        )
                    } else {
                        repository.insertResearchTask(
                            ResearchTaskEntity(
                                title = title, category = category, priority = priority,
                                dueDate = dueDate, note = note, progress = progress, status = status
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
private fun ResearchItem(
    task: ResearchTaskEntity,
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
            modifier = Modifier.fillMaxWidth().clickable { onEdit() }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = task.isDone, onCheckedChange = { onToggleDone() })
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "[${task.category}] ${priorityLabels[task.priority]}",
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
                            java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault()).format(java.util.Date(task.dueDate)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (task.note.isNotBlank()) {
                    Text(task.note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                val displayProgress = if (task.isDone) 100 else task.progress
                LinearProgressIndicator(
                    progress = { displayProgress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = statusColors[task.status]
                )
                Text("进度 $displayProgress%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ResearchEditDialog(
    task: ResearchTaskEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Long?, String, Int) -> Unit
) {
    var title by remember(task) { mutableStateOf(task?.title ?: "") }
    var category by remember(task) { mutableStateOf(task?.category ?: "论文") }
    var priority by remember(task) { mutableStateOf(task?.priority ?: 0) }
    var note by remember(task) { mutableStateOf(task?.note ?: "") }
    var progress by remember(task) { mutableStateOf(task?.progress ?: 0) }
    var catExpanded by remember { mutableStateOf(false) }
    var dueOption by remember(task) { mutableStateOf(if (task?.dueDate == null) 0 else 1) }

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
        title = { Text(if (task == null) "新增科研待办" else "编辑科研待办") },
        text = {
            Column {
                OutlinedTextField(title, { title = it }, label = { Text("标题") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("类别", style = MaterialTheme.typography.labelLarge)
                Box {
                    TextButton(onClick = { catExpanded = true }) { Text(category) }
                    DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        categories.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c) },
                                onClick = { category = c; catExpanded = false }
                            )
                        }
                    }
                }
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
                OutlinedTextField(note, { note = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth())
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
                Spacer(Modifier.height(8.dp))
                Text("截止日期", style = MaterialTheme.typography.labelLarge)
                Row {
                    listOf("无截止", "今天", "明天", "下周").forEachIndexed { idx, label ->
                        TextButton(onClick = { dueOption = idx }) {
                            Text(label, color = if (dueOption == idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (title.isNotBlank()) onSave(title, category, priority, resolveDueDate(), note, progress) }) {
                Text("保存")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
