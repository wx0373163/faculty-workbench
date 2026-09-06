package com.example.facultyworkbench.data.backup

import com.example.facultyworkbench.data.entity.CourseEntity
import com.example.facultyworkbench.data.entity.ProfileEntity
import com.example.facultyworkbench.data.entity.ResearchTaskEntity
import com.example.facultyworkbench.data.entity.TaskEntity
import com.example.facultyworkbench.data.entity.TodoEntity

/**
 * 全量备份数据结构，用于 JSON 导入/导出。
 */
data class BackupData(
    val version: Int = 2,
    val exportTime: Long = System.currentTimeMillis(),
    val profile: ProfileEntity?,
    val tasks: List<TaskEntity>,
    val courses: List<CourseEntity>,
    val researchTasks: List<ResearchTaskEntity>,
    val todos: List<TodoEntity> = emptyList()
)
