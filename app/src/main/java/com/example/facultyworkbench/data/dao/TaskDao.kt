package com.example.facultyworkbench.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.facultyworkbench.data.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY status ASC, priority DESC, dueDate ASC")
    fun getAll(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("UPDATE tasks SET status = :status, progress = :progress WHERE id = :id")
    suspend fun updateStatusAndProgress(id: Long, status: Int, progress: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    @Query("SELECT * FROM tasks WHERE dueDate IS NOT NULL AND dueDate BETWEEN :start AND :end AND status != 2 ORDER BY priority DESC, dueDate ASC")
    suspend fun getPendingTasksDueBetween(start: Long, end: Long): List<TaskEntity>

    // 按同步来源查找/删除，用于待办/科研事项与今日任务的联动
    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE sourceType = :sourceType AND sourceId = :sourceId LIMIT 1")
    suspend fun getBySource(sourceType: String, sourceId: Long): TaskEntity?

    @Query("DELETE FROM tasks WHERE sourceType = :sourceType AND sourceId = :sourceId")
    suspend fun deleteBySource(sourceType: String, sourceId: Long)

    @Query("DELETE FROM tasks WHERE sourceType = :sourceType")
    suspend fun deleteBySourceType(sourceType: String)
}
