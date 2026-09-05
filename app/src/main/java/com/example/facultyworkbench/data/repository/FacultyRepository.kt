package com.example.facultyworkbench.data.repository

import com.example.facultyworkbench.data.backup.BackupData
import com.example.facultyworkbench.data.dao.CourseDao
import com.example.facultyworkbench.data.dao.ProfileDao
import com.example.facultyworkbench.data.dao.ResearchTaskDao
import com.example.facultyworkbench.data.dao.TaskDao
import com.example.facultyworkbench.data.entity.CourseEntity
import com.example.facultyworkbench.data.entity.ProfileEntity
import com.example.facultyworkbench.data.entity.ResearchTaskEntity
import com.example.facultyworkbench.data.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class FacultyRepository(
    private val taskDao: TaskDao,
    private val courseDao: CourseDao,
    private val researchTaskDao: ResearchTaskDao,
    private val profileDao: ProfileDao
) {
    // Tasks
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAll()
    suspend fun insertTask(task: TaskEntity): Long = taskDao.insert(task)
    suspend fun updateTask(task: TaskEntity) = taskDao.update(task)
    suspend fun deleteTask(task: TaskEntity) = taskDao.delete(task)
    suspend fun updateTaskStatus(id: Long, status: Int, progress: Int) = taskDao.updateStatusAndProgress(id, status, progress)

    // Courses
    val allCourses: Flow<List<CourseEntity>> = courseDao.getAll()
    suspend fun insertCourse(course: CourseEntity): Long = courseDao.insert(course)
    suspend fun updateCourse(course: CourseEntity) = courseDao.update(course)
    suspend fun deleteCourse(course: CourseEntity) = courseDao.delete(course)

    // Research tasks
    val allResearchTasks: Flow<List<ResearchTaskEntity>> = researchTaskDao.getAll()
    suspend fun insertResearchTask(task: ResearchTaskEntity): Long = researchTaskDao.insert(task)
    suspend fun updateResearchTask(task: ResearchTaskEntity) = researchTaskDao.update(task)
    suspend fun deleteResearchTask(task: ResearchTaskEntity) = researchTaskDao.delete(task)
    suspend fun updateResearchStatus(id: Long, status: Int, progress: Int) = researchTaskDao.updateStatusAndProgress(id, status, progress)

    // Profile
    val profile: Flow<ProfileEntity?> = profileDao.get()
    suspend fun upsertProfile(profile: ProfileEntity) = profileDao.upsert(profile)

    // 备份导出/导入
    suspend fun exportData(): BackupData = BackupData(
        profile = profile.first(),
        tasks = taskDao.getAll().first(),
        courses = courseDao.getAll().first(),
        researchTasks = researchTaskDao.getAll().first()
    )

    suspend fun importData(data: BackupData) {
        taskDao.deleteAll()
        courseDao.deleteAll()
        researchTaskDao.deleteAll()
        if (data.tasks.isNotEmpty()) taskDao.insertAll(data.tasks)
        if (data.courses.isNotEmpty()) courseDao.insertAll(data.courses)
        if (data.researchTasks.isNotEmpty()) researchTaskDao.insertAll(data.researchTasks)
        data.profile?.let { profileDao.upsert(it) }
    }
}
