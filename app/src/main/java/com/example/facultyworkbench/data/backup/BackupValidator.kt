package com.example.facultyworkbench.data.backup

import com.example.facultyworkbench.data.entity.CourseEntity
import com.example.facultyworkbench.data.entity.ResearchTaskEntity
import com.example.facultyworkbench.data.entity.TaskEntity

/**
 * 单表校验结果。
 */
data class TableValidationResult(
    val tableName: String,
    val total: Int,
    val valid: Int,
    val invalid: Int,
    /** 前若干条异常的可读描述，便于用户预览问题。 */
    val issues: List<String>
)

/**
 * 整份备份的校验结果。
 */
data class BackupValidationResult(
    val tables: List<TableValidationResult>
) {
    val totalRecords: Int get() = tables.sumOf { it.total }
    val totalInvalid: Int get() = tables.sumOf { it.invalid }
    val hasIssues: Boolean get() = totalInvalid > 0
}

/**
 * 对备份数据进行字段映射与取值范围校验，帮助用户在导入前发现问题。
 */
object BackupValidator {

    fun validate(data: BackupData): BackupValidationResult {
        return BackupValidationResult(
            tables = listOf(
                validateTasks(data.tasks),
                validateCourses(data.courses),
                validateResearchTasks(data.researchTasks)
            )
        )
    }

    /**
     * 返回仅包含有效记录的备份数据副本，用于"跳过异常项"导入。
     */
    fun filterValid(data: BackupData): BackupData {
        return data.copy(
            tasks = data.tasks.filter { isTaskValid(it) },
            courses = data.courses.filter { isCourseValid(it) },
            researchTasks = data.researchTasks.filter { isResearchValid(it) }
        )
    }

    // ---- 有效性谓词 ----
    private fun isTaskValid(t: TaskEntity): Boolean =
        t.title.isNotBlank() &&
            t.priority in 0..2 &&
            t.status in 0..2 &&
            t.progress in 0..100 &&
            (t.dueDate == null || t.dueDate > 0L)

    private fun isCourseValid(c: CourseEntity): Boolean =
        c.name.isNotBlank() &&
            c.dayOfWeek in 1..7 &&
            c.startSection in 1..12 &&
            c.sectionCount in 1..12 &&
            c.startSection + c.sectionCount - 1 <= 12

    private fun isResearchValid(r: ResearchTaskEntity): Boolean =
        r.title.isNotBlank() &&
            r.priority in 0..2 &&
            r.status in 0..2 &&
            r.progress in 0..100 &&
            (r.dueDate == null || r.dueDate > 0L)

    // ---- 校验实现 ----
    private fun validateTasks(tasks: List<TaskEntity>): TableValidationResult {
        val issues = mutableListOf<String>()
        var invalid = 0
        tasks.forEachIndexed { idx, t ->
            val problems = mutableListOf<String>()
            if (t.title.isBlank()) problems.add("标题为空")
            if (t.priority !in 0..2) problems.add("优先级=${t.priority}(应0-2)")
            if (t.status !in 0..2) problems.add("状态=${t.status}(应0-2)")
            if (t.progress !in 0..100) problems.add("进度=${t.progress}(应0-100)")
            if (t.dueDate != null && t.dueDate <= 0L) problems.add("截止时间无效")
            if (problems.isNotEmpty()) {
                invalid++
                if (issues.size < 5) {
                    issues.add("#${idx + 1}「${t.title.ifBlank { "(空)" }}」: ${problems.joinToString("，")}")
                }
            }
        }
        return TableValidationResult("今日任务", tasks.size, tasks.size - invalid, invalid, issues)
    }

    private fun validateCourses(courses: List<CourseEntity>): TableValidationResult {
        val issues = mutableListOf<String>()
        var invalid = 0
        courses.forEachIndexed { idx, c ->
            val problems = mutableListOf<String>()
            if (c.name.isBlank()) problems.add("课程名为空")
            if (c.dayOfWeek !in 1..7) problems.add("星期=${c.dayOfWeek}(应1-7)")
            if (c.startSection !in 1..12) problems.add("开始节次=${c.startSection}(应1-12)")
            if (c.sectionCount !in 1..12) problems.add("持续节次=${c.sectionCount}(应1-12)")
            if (c.startSection in 1..12 && c.sectionCount in 1..12 &&
                c.startSection + c.sectionCount - 1 > 12
            ) {
                problems.add("节次越界(结束节=${c.startSection + c.sectionCount - 1})")
            }
            if (problems.isNotEmpty()) {
                invalid++
                if (issues.size < 5) {
                    issues.add("#${idx + 1}「${c.name.ifBlank { "(空)" }}」: ${problems.joinToString("，")}")
                }
            }
        }
        return TableValidationResult("教学课程", courses.size, courses.size - invalid, invalid, issues)
    }

    private fun validateResearchTasks(tasks: List<ResearchTaskEntity>): TableValidationResult {
        val issues = mutableListOf<String>()
        var invalid = 0
        tasks.forEachIndexed { idx, r ->
            val problems = mutableListOf<String>()
            if (r.title.isBlank()) problems.add("标题为空")
            if (r.priority !in 0..2) problems.add("优先级=${r.priority}(应0-2)")
            if (r.status !in 0..2) problems.add("状态=${r.status}(应0-2)")
            if (r.progress !in 0..100) problems.add("进度=${r.progress}(应0-100)")
            if (r.dueDate != null && r.dueDate <= 0L) problems.add("截止时间无效")
            if (problems.isNotEmpty()) {
                invalid++
                if (issues.size < 5) {
                    issues.add("#${idx + 1}「${r.title.ifBlank { "(空)" }}」: ${problems.joinToString("，")}")
                }
            }
        }
        return TableValidationResult("科研待办", tasks.size, tasks.size - invalid, invalid, issues)
    }
}
