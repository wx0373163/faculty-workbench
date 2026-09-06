package com.example.facultyworkbench.ui.profile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.facultyworkbench.data.backup.BackupData
import com.example.facultyworkbench.data.backup.BackupSerializer
import com.example.facultyworkbench.data.backup.BackupValidator
import com.example.facultyworkbench.data.entity.ProfileEntity
import com.example.facultyworkbench.data.repository.FacultyRepository
import com.example.facultyworkbench.ui.theme.ThemeMode
import com.example.facultyworkbench.util.CalendarSync
import com.example.facultyworkbench.util.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    repository: FacultyRepository,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    semesterStartDate: Long?,
    onSemesterStartDateChange: (Long?) -> Unit,
    semesterEndDate: Long?,
    onSemesterEndDateChange: (Long?) -> Unit,
    reminderEnabled: Boolean,
    reminderHour: Int,
    reminderMinute: Int,
    onReminderConfigChange: (Boolean, Int, Int) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val profile by repository.profile.collectAsState(initial = null)
    val tasks by repository.allTasks.collectAsState(initial = emptyList())
    val courses by repository.allCourses.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showEdit by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var department by remember { mutableStateOf(profile?.department ?: "") }
    var title by remember { mutableStateOf(profile?.title ?: "") }
    var pendingImport by remember { mutableStateOf<BackupData?>(null) }
    var showSemesterDatePicker by remember { mutableStateOf(false) }
    var showSemesterEndDatePicker by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onReminderConfigChange(true, reminderHour, reminderMinute)
            ReminderScheduler.scheduleDailyReminder(context, reminderHour, reminderMinute)
        } else {
            scope.launch { snackbarHostState.showSnackbar("未授予通知权限，提醒无法生效") }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val data = repository.exportData()
                    val json = BackupSerializer.toJson(data)
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    "已导出 JSON 备份"
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
                    val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: return@withContext "读取文件失败"
                    BackupSerializer.fromJson(json)
                } catch (e: Exception) {
                    "解析失败：${e.message}"
                }
            }
            when (result) {
                is BackupData -> pendingImport = result
                is String -> snackbarHostState.showSnackbar(result)
            }
        }
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.WRITE_CALENDAR] == true &&
            result[Manifest.permission.READ_CALENDAR] == true
        if (granted) {
            performCalendarSync(context, tasks, courses, semesterStartDate, semesterEndDate, scope, snackbarHostState)
        } else {
            scope.launch { snackbarHostState.showSnackbar("未获得日历权限，无法同步") }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("我的") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 个人信息卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("个人信息", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(8.dp))
                    Text("姓名：${profile?.name?.ifBlank { "未设置" } ?: "未设置"}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("院系：${profile?.department?.ifBlank { "未设置" } ?: "未设置"}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("职称：${profile?.title?.ifBlank { "未设置" } ?: "未设置"}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        name = profile?.name ?: ""
                        department = profile?.department ?: ""
                        title = profile?.title ?: ""
                        showEdit = true
                    }) { Text("编辑") }
                }
            }

            // 主题设置
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("主题模式", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        ThemeMode.SYSTEM to "跟随系统",
                        ThemeMode.LIGHT to "浅色",
                        ThemeMode.DARK to "深色"
                    ).forEach { (mode, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = { onThemeChange(mode) }
                            )
                            Text(label)
                        }
                    }
                }
            }

            // 学期设置
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("学期设置", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "学期起始日：${semesterStartDate?.let { formatDate(it) } ?: "未设置（默认以本周为第 1 周）"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "学期截止日：${semesterEndDate?.let { formatDate(it) } ?: "未设置"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Row {
                        TextButton(onClick = { showSemesterDatePicker = true }) { Text("设置起始日") }
                        if (semesterStartDate != null) {
                            TextButton(onClick = { onSemesterStartDateChange(null) }) { Text("清除") }
                        }
                        TextButton(onClick = { showSemesterEndDatePicker = true }) { Text("设置截止日") }
                        if (semesterEndDate != null) {
                            TextButton(onClick = { onSemesterEndDateChange(null) }) { Text("清除") }
                        }
                    }
                }
            }

            // 提醒设置
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("每日提醒", style = MaterialTheme.typography.titleMedium)
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                                    if (hasPermission) {
                                        onReminderConfigChange(true, reminderHour, reminderMinute)
                                        ReminderScheduler.scheduleDailyReminder(context, reminderHour, reminderMinute)
                                    } else {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    onReminderConfigChange(false, reminderHour, reminderMinute)
                                    ReminderScheduler.cancelDailyReminder(context)
                                }
                            }
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (reminderEnabled) "每天 ${String.format("%02d:%02d", reminderHour, reminderMinute)} 提醒今日待办"
                        else "开启后每天定时推送今日待办任务",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (reminderEnabled) {
                        Spacer(Modifier.height(4.dp))
                        Row {
                            TextButton(onClick = { showReminderTimePicker = true }) { Text("修改提醒时间") }
                            TextButton(onClick = {
                                onReminderConfigChange(false, reminderHour, reminderMinute)
                                ReminderScheduler.cancelDailyReminder(context)
                            }) { Text("关闭提醒") }
                        }
                    }
                }
            }

            // 数据管理
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("数据管理", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        val date = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                        exportLauncher.launch("faculty_backup_$date.json")
                    }) { Text("导出 JSON 备份") }
                    TextButton(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }) {
                        Text("导入 JSON 备份")
                    }
                    TextButton(onClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            performCalendarSync(context, tasks, courses, semesterStartDate, semesterEndDate, scope, snackbarHostState)
                        } else {
                            calendarPermissionLauncher.launch(
                                arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
                            )
                        }
                    }) { Text("同步到系统日历") }
                    TextButton(onClick = {
                        scope.launch {
                            val result = exportDatabase(context)
                            snackbarHostState.showSnackbar(result)
                        }
                    }) { Text("导出数据库文件(.db)") }
                }
            }

            val currentVerName = com.example.facultyworkbench.util.UpdateChecker.getCurrentVersionName(context)
            Text(
                "教师工作台 v${currentVerName.ifEmpty { "1.0" }}\n本地数据存储，支持离线",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showEdit) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("编辑个人信息") },
            text = {
                Column {
                    OutlinedTextField(name, { name = it }, label = { Text("姓名") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(department, { department = it }, label = { Text("院系") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(title, { title = it }, label = { Text("职称") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.upsertProfile(ProfileEntity(name = name, department = department, title = title))
                        showEdit = false
                        snackbarHostState.showSnackbar("已保存")
                    }
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showEdit = false }) { Text("取消") } }
        )
    }

    pendingImport?.let { data ->
        val validation = remember(data) { BackupValidator.validate(data) }
        var skipInvalid by remember(data) { mutableStateOf(false) }
        val statusLabels = listOf("待办", "进行中", "已完成")
        val priorityLabels = listOf("普通", "重要", "紧急")

        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(if (validation.hasIssues) "导入备份（含异常）" else "确认导入备份") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("导入将覆盖当前所有数据（任务、课程、科研待办、个人信息），此操作不可撤销。")
                    Spacer(Modifier.height(8.dp))

                    // 校验摘要
                    Text("字段校验：", style = MaterialTheme.typography.titleSmall)
                    validation.tables.forEach { t ->
                        Text(
                            "• ${t.tableName}：共 ${t.total} 条，有效 ${t.valid}，异常 ${t.invalid}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (t.invalid > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 异常详情
                    if (validation.hasIssues) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "发现 ${validation.totalInvalid} 条异常记录，仍可导入（异常字段将按原值写入）：",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        validation.tables.forEach { t ->
                            t.issues.forEach { issue ->
                                Text("  · $issue", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // 跳过异常项开关
                    if (validation.hasIssues) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = skipInvalid, onCheckedChange = { skipInvalid = it })
                            Text("跳过异常项（仅导入 ${validation.totalRecords - validation.totalInvalid} 条有效记录）", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // 数据预览
                    Spacer(Modifier.height(12.dp))
                    Text("数据预览（每表前 2 条）：", style = MaterialTheme.typography.titleSmall)
                    if (data.tasks.isNotEmpty()) {
                        Text("【今日任务】", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        data.tasks.take(2).forEach { t ->
                            Text(
                                "  · ${t.title}｜${priorityLabels[t.priority.coerceIn(0, 2)]}｜${statusLabels[t.status.coerceIn(0, 2)]}｜进度${t.progress}%",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    if (data.courses.isNotEmpty()) {
                        Text("【教学课程】", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        data.courses.take(2).forEach { c ->
                            val endSec = (c.startSection + c.sectionCount - 1).coerceAtMost(12)
                            Text(
                                "  · ${c.name}｜周${c.dayOfWeek} 第${c.startSection}-${endSec}节｜${c.location.ifBlank { "无地点" }}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    if (data.researchTasks.isNotEmpty()) {
                        Text("【科研待办】", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        data.researchTasks.take(2).forEach { r ->
                            Text(
                                "  · ${r.title}｜${r.category}｜${statusLabels[r.status.coerceIn(0, 2)]}｜进度${r.progress}%",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            val toImport = if (skipInvalid) BackupValidator.filterValid(data) else data
                            repository.importData(toImport)
                            val imported = toImport.tasks.size + toImport.courses.size + toImport.researchTasks.size
                            val skipped = if (skipInvalid) validation.totalInvalid else 0
                            snackbarHostState.showSnackbar(
                                when {
                                    skipped > 0 -> "导入完成（${imported} 条，已跳过 $skipped 条异常）"
                                    validation.hasIssues -> "导入完成（含 ${validation.totalInvalid} 条异常）"
                                    else -> "导入成功"
                                }
                            )
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("导入失败：${e.message}")
                        }
                        pendingImport = null
                    }
                }) {
                    Text(
                        when {
                            skipInvalid -> "导入有效记录"
                            validation.hasIssues -> "仍要导入"
                            else -> "确认导入"
                        }
                    )
                }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("取消") } }
        )
    }

    if (showSemesterDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = semesterStartDate ?: System.currentTimeMillis()
        )
        AlertDialog(
            onDismissRequest = { showSemesterDatePicker = false },
            title = { Text("选择学期起始日") },
            text = { DatePicker(state = datePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onSemesterStartDateChange(it) }
                    showSemesterDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showSemesterDatePicker = false }) { Text("取消") } }
        )
    }

    if (showSemesterEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = semesterEndDate ?: System.currentTimeMillis()
        )
        AlertDialog(
            onDismissRequest = { showSemesterEndDatePicker = false },
            title = { Text("选择学期截止日") },
            text = { DatePicker(state = datePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onSemesterEndDateChange(it) }
                    showSemesterEndDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showSemesterEndDatePicker = false }) { Text("取消") } }
        )
    }

    if (showReminderTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showReminderTimePicker = false },
            title = { Text("设置提醒时间") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    onReminderConfigChange(true, timePickerState.hour, timePickerState.minute)
                    ReminderScheduler.scheduleDailyReminder(context, timePickerState.hour, timePickerState.minute)
                    showReminderTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showReminderTimePicker = false }) { Text("取消") } }
        )
    }
}

private fun performCalendarSync(
    context: Context,
    tasks: List<com.example.facultyworkbench.data.entity.TaskEntity>,
    courses: List<com.example.facultyworkbench.data.entity.CourseEntity>,
    semesterStartDate: Long?,
    semesterEndDate: Long?,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    scope.launch {
        val result = withContext(Dispatchers.IO) {
            try {
                CalendarSync.syncAll(context, tasks, courses, semesterStartDate, semesterEndDate)
            } catch (e: Exception) {
                CalendarSync.SyncResult(0, 0, 0, "同步失败：${e.message}")
            }
        }
        snackbarHostState.showSnackbar(result.message)
    }
}

private fun exportDatabase(context: Context): String {
    return try {
        val dbFile = context.getDatabasePath("faculty_workbench.db")
        if (!dbFile.exists()) return "数据库不存在"
        val exportDir = context.getExternalFilesDir(null) ?: context.filesDir
        val destFile = File(exportDir, "faculty_workbench_backup_${System.currentTimeMillis()}.db")
        FileInputStream(dbFile).use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        "已导出到：${destFile.absolutePath}"
    } catch (e: Exception) {
        "导出失败：${e.message}"
    }
}

private fun formatDate(millis: Long): String =
    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(millis))
