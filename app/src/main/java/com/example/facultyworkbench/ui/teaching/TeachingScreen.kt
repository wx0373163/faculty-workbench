package com.example.facultyworkbench.ui.teaching

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.facultyworkbench.data.entity.CourseEntity
import com.example.facultyworkbench.data.repository.FacultyRepository
import com.example.facultyworkbench.ui.theme.courseColors
import kotlinx.coroutines.launch

private val dayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachingScreen(
    repository: FacultyRepository,
    snackbarHostState: SnackbarHostState
) {
    val courses by repository.allCourses.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var editingCourse by remember { mutableStateOf<CourseEntity?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("教学") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editingCourse = null; showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "新增课程")
            }
        }
    ) { padding ->
        if (courses.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无课程，点击右下角 + 添加", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(courses, key = { it.id }) { course ->
                    CourseItem(
                        course = course,
                        onEdit = { editingCourse = course; showDialog = true },
                        onDelete = {
                            scope.launch {
                                repository.deleteCourse(course)
                                snackbarHostState.showSnackbar("已删除课程")
                            }
                        }
                    )
                }
            }
        }
    }

    if (showDialog) {
        CourseEditDialog(
            course = editingCourse,
            onDismiss = { showDialog = false },
            onSave = { name, className, dayOfWeek, startSection, sectionCount, location, weeks ->
                scope.launch {
                    if (editingCourse != null) {
                        repository.updateCourse(
                            editingCourse!!.copy(
                                name = name, className = className, dayOfWeek = dayOfWeek,
                                startSection = startSection, sectionCount = sectionCount,
                                location = location, weeks = weeks
                            )
                        )
                    } else {
                        repository.insertCourse(
                            CourseEntity(
                                name = name, className = className, dayOfWeek = dayOfWeek,
                                startSection = startSection, sectionCount = sectionCount,
                                location = location, weeks = weeks,
                                color = (courses.size) % courseColors.size
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
private fun CourseItem(
    course: CourseEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = courseColors[course.color % courseColors.size].copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onEdit() }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(course.name, style = MaterialTheme.typography.titleMedium)
                if (course.className.isNotBlank()) {
                    Text(course.className, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${dayNames[course.dayOfWeek - 1]} 第${course.startSection}-${course.startSection + course.sectionCount - 1}节 · ${course.location.ifBlank { "未设置地点" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (course.weeks.isNotBlank()) {
                    Text("周次：${course.weeks}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun CourseEditDialog(
    course: CourseEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Int, Int, String, String) -> Unit
) {
    var name by remember(course) { mutableStateOf(course?.name ?: "") }
    var className by remember(course) { mutableStateOf(course?.className ?: "") }
    var dayOfWeek by remember(course) { mutableStateOf(course?.dayOfWeek ?: 1) }
    var startSectionText by remember(course) { mutableStateOf((course?.startSection ?: 1).toString()) }
    var sectionCountText by remember(course) { mutableStateOf((course?.sectionCount ?: 2).toString()) }
    var location by remember(course) { mutableStateOf(course?.location ?: "") }
    var weeks by remember(course) { mutableStateOf(course?.weeks ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (course == null) "新增课程" else "编辑课程") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("课程名称") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(className, { className = it }, label = { Text("授课班级") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("星期", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    dayNames.forEachIndexed { idx, d ->
                        TextButton(
                            onClick = { dayOfWeek = idx + 1 },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text(
                                d,
                                color = if (dayOfWeek == idx + 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    OutlinedTextField(
                        value = startSectionText,
                        onValueChange = { startSectionText = it.filter { c -> c.isDigit() } },
                        label = { Text("开始节次") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = sectionCountText,
                        onValueChange = { sectionCountText = it.filter { c -> c.isDigit() } },
                        label = { Text("持续节次") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(location, { location = it }, label = { Text("上课地点") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(weeks, { weeks = it }, label = { Text("周次（如 1-16）") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    val startSection = startSectionText.toIntOrNull()?.coerceIn(1, 10) ?: 1
                    var sectionCount = sectionCountText.toIntOrNull()?.coerceIn(1, 10) ?: 2
                    if (startSection + sectionCount > 11) sectionCount = 11 - startSection
                    onSave(name, className, dayOfWeek, startSection, sectionCount, location, weeks)
                }
            }) {
                Text("保存")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
