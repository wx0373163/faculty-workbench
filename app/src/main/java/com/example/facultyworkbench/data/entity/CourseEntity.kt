package com.example.facultyworkbench.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 教学课程实体，用于"教学"与"日程课表"分界面。
 */
@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val className: String = "", // 授课班级
    val dayOfWeek: Int = 1, // 1=周一 ... 7=周日
    val startSection: Int = 1, // 开始节次
    val sectionCount: Int = 2, // 持续节次
    val location: String = "", // 上课地点
    val weeks: String = "", // 周次描述，如 "1-16"
    val color: Int = 0 // 课程显示颜色索引
)
