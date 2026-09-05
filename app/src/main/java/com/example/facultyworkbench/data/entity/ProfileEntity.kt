package com.example.facultyworkbench.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 教师个人信息实体，用于"我的"分界面。
 * 仅保存一条记录（id 固定为 1）。
 */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val department: String = "",
    val title: String = "" // 职称
)
