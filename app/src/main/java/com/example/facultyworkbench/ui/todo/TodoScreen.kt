package com.example.facultyworkbench.ui.todo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.facultyworkbench.data.entity.TodoEntity
import com.example.facultyworkbench.data.repository.FacultyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val priorityLabels = listOf("普通", "重要", "紧急")
private val priorityColors = listOf(
    androidx.compose.ui.graphics.Color(0xFF74777F),
    androidx.compose.ui.graphics.Color(0xFFFF9800),
    androidx.compose.ui.graphics.Color(0xFFE53935)
)

private val categoryOptions = listOf("默认", "工作", "生活", "学习", "其他")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    repository: FacultyRepository,
    snackbarHostState: SnackbarHostState
) {
    val todos by repository.allTodos.collectAsState(initial = emptyList())
    val categories by repository.todoCategories.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showDialog by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<TodoEntity?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) } // null=全部
    var selectedStatus by remember { mutableStateOf<Int?>(null) } // null=全部 0=未完成 1=已完成
    var showClearDialog by remember { mutableStateOf(false) }

    // 导入相关：暂存待导入的 JSON 字符串，弹出"合并/替换"确认
    var pendingImportJson by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val json = repository.exportTodosJson()
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    "已导出 ${todos.size} 条待办"
                } catch (e: Exception) {
                    "导出失败：${e.message}"
                }
            }
            snackbarHostState.showSnackbar(result)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    }
                } catch (e: Exception) {
                    null
                }
            }
            if (result != null) {
                pendingImportJson = result
            } else {
                snackbarHostState.showSnackbar("读取文件失败")
            }
        }
    }

    val filteredTodos = remember(todos, selectedCategory, selectedStatus) {
        todos.filter { todo ->
            (selectedCategory == null || todo.category == selectedCategory) &&
                when (selectedStatus) {
                    0 -> !todo.isCompleted
                    1 -> todo.isCompleted
                    else -> true
                }
        }
    }

    val totalCount = todos.size
    val completedCount = todos.count { it.isCompleted }
    val progressPercent = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("待办") },
                actions = {
                    IconButton(onClick = {
                        val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        exportLauncher.launch("todos_$date.json")
                    }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "导出")
                    }
                    IconButton(onClick = {
                        importLauncher.launch(arrayOf("application/json", "*/*"))
                    }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "导入")
                    }
                    if (completedCount > 0) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "清除已完成")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingTodo = null
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "新增")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 进度概览
            if (totalCount > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "完成进度 $completedCount / $totalCount",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "${(progressPercent * 100).toInt()}%",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progressPercent },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 分类筛选
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("全部分类") }
                )
                (categories.ifEmpty { categoryOptions }).distinct().forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat) }
                    )
                }
            }

            // 状态筛选
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = { selectedStatus = null },
                    label = { Text("全部 $totalCount") }
                )
                FilterChip(
                    selected = selectedStatus == 0,
                    onClick = { selectedStatus = if (selectedStatus == 0) null else 0 },
                    label = { Text("待办 ${totalCount - completedCount}") }
                )
                FilterChip(
                    selected = selectedStatus == 1,
                    onClick = { selectedStatus = if (selectedStatus == 1) null else 1 },
                    label = { Text("已完成 $completedCount") }
                )
            }

            if (filteredTodos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (todos.isEmpty()) "暂无待办，点击右下角 + 添加" else "没有符合条件的待办",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTodos, key = { it.id }) { todo ->
                        TodoItem(
                            todo = todo,
                            onToggle = {
                                scope.launch {
                                    repository.updateTodoCompleted(todo.id, !todo.isCompleted)
                                    snackbarHostState.showSnackbar(if (todo.isCompleted) "已标记为未完成" else "已完成")
                                }
                            },
                            onEdit = {
                                editingTodo = todo
                                showDialog = true
                            },
                            onDelete = {
                                scope.launch {
                                    repository.deleteTodo(todo)
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
        TodoEditDialog(
            todo = editingTodo,
            onDismiss = { showDialog = false },
            onSave = { title, note, category, priority, dueDate ->
                scope.launch {
                    if (editingTodo != null) {
                        repository.updateTodo(
                            editingTodo!!.copy(
                                title = title,
                                note = note,
                                category = category,
                                priority = priority,
                                dueDate = dueDate
                            )
                        )
                    } else {
                        repository.insertTodo(
                            TodoEntity(
                                title = title,
                                note = note,
                                category = category,
                                priority = priority,
                                dueDate = dueDate
                            )
                        )
                    }
                    showDialog = false
                    snackbarHostState.showSnackbar("已保存")
                }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清除已完成") },
            text = { Text("确定要删除所有已完成的待办吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.deleteCompletedTodos()
                        showClearDialog = false
                        snackbarHostState.showSnackbar("已清除 $completedCount 项已完成待办")
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }

    // 导入确认弹窗：合并追加 / 替换全部
    pendingImportJson?.let { json ->
        AlertDialog(
            onDismissRequest = { pendingImportJson = null },
            title = { Text("导入待办清单") },
            text = {
                Column {
                    Text("请选择导入方式：")
                    Spacer(Modifier.height(8.dp))
                    Text("• 合并追加：保留现有待办，追加导入的内容", style = MaterialTheme.typography.bodyMedium)
                    Text("• 替换全部：清空现有待办后导入", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                val count = repository.importTodosJson(json, merge = true)
                                pendingImportJson = null
                                snackbarHostState.showSnackbar("已合并导入 $count 条待办")
                            } catch (e: Exception) {
                                pendingImportJson = null
                                snackbarHostState.showSnackbar("导入失败：${e.message}")
                            }
                        }
                    }) { Text("合并追加") }
                    TextButton(onClick = {
                        scope.launch {
                            try {
                                val count = repository.importTodosJson(json, merge = false)
                                pendingImportJson = null
                                snackbarHostState.showSnackbar("已替换导入 $count 条待办")
                            } catch (e: Exception) {
                                pendingImportJson = null
                                snackbarHostState.showSnackbar("导入失败：${e.message}")
                            }
                        }
                    }) { Text("替换全部") }
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportJson = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun TodoItem(
    todo: TodoEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (todo.isCompleted)
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
            Checkbox(checked = todo.isCompleted, onCheckedChange = { onToggle() })
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
                    color = if (todo.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                if (todo.note.isNotBlank()) {
                    Text(
                        todo.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 优先级标签
                    Box(
                        modifier = Modifier
                            .background(priorityColors[todo.priority].copy(alpha = 0.15f), MaterialTheme.shapes.small)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            priorityLabels[todo.priority],
                            color = priorityColors[todo.priority],
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    // 分类标签
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), MaterialTheme.shapes.small)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            todo.category,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    if (todo.dueDate != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                .format(java.util.Date(todo.dueDate)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoEditDialog(
    todo: TodoEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int, Long?) -> Unit
) {
    var title by remember(todo) { mutableStateOf(todo?.title ?: "") }
    var note by remember(todo) { mutableStateOf(todo?.note ?: "") }
    var priority by remember(todo) { mutableStateOf(todo?.priority ?: 0) }
    var category by remember(todo) { mutableStateOf(todo?.category ?: "默认") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var dueDate by remember(todo) { mutableStateOf(todo?.dueDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (todo == null) "新增待办" else "编辑待办") },
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
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注") },
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
                Text("分类", style = MaterialTheme.typography.labelLarge)
                Box {
                    TextButton(onClick = { categoryExpanded = true }) { Text(category) }
                    DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        categoryOptions.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("截止日期", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showDatePicker = true }) {
                        Text(
                            if (dueDate != null) dateFormat.format(Date(dueDate!!)) else "选择日期",
                            color = if (dueDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (dueDate != null) {
                        TextButton(onClick = { dueDate = null }) {
                            Text("清除", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (title.isNotBlank()) onSave(title, note, category, priority, dueDate) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dueDate = it }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
