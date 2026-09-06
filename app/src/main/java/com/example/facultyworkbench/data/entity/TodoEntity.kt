package com.example.facultyworkbench.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 待办清单实体，用于"待办清单"分界面。
 * 支持分类、优先级、截止日期与完成状态。
 */
@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val category: String = "默认", // 分类标签
    val priority: Int = 0, // 0=普通 1=重要 2=紧急
    val dueDate: Long? = null, // 截止时间戳（毫秒），null 表示无截止
    val isCompleted: Boolean = false,
    val order: Int = 0, // 排序权重，越大越靠前
    val createdAt: Long = System.currentTimeMillis()
)
