package com.example.facultyworkbench.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 科研待办实体，用于"科研待办"分界面。
 */
@Entity(tableName = "research_tasks")
data class ResearchTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String = "论文", // 论文/项目/实验/申报/其他
    val priority: Int = 0, // 0=普通 1=重要 2=紧急
    val dueDate: Long? = null,
    val note: String = "",
    val progress: Int = 0, // 0-100 进度
    val status: Int = 0, // 0=待办 1=进行中 2=已完成
    val createdAt: Long = System.currentTimeMillis()
) {
    val isDone: Boolean get() = status == 2
}
