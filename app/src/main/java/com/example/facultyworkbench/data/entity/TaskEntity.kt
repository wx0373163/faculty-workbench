package com.example.facultyworkbench.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 通用任务实体，用于"今日任务"分界面。
 * type 区分任务来源：teaching(教学)、research(科研)、general(通用)。
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val priority: Int = 0, // 0=普通 1=重要 2=紧急
    val dueDate: Long? = null, // 截止时间戳（毫秒），null 表示无截止
    val type: String = "general", // teaching | research | general | todo
    val status: Int = 0, // 0=待办 1=进行中 2=已完成
    val progress: Int = 0, // 0-100，已完成时显示 100
    val createdAt: Long = System.currentTimeMillis(),
    // 同步来源：当任务由待办/科研事项自动生成时，记录原始来源类型与 id
    val sourceType: String = "", // "" | "todo" | "research"
    val sourceId: Long = 0
) {
    val isDone: Boolean get() = status == 2
}
