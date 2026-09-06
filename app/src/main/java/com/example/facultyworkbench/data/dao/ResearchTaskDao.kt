package com.example.facultyworkbench.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.facultyworkbench.data.entity.ResearchTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResearchTaskDao {
    @Query("SELECT * FROM research_tasks ORDER BY status ASC, priority DESC, dueDate ASC")
    fun getAll(): Flow<List<ResearchTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: ResearchTaskEntity): Long

    @Update
    suspend fun update(task: ResearchTaskEntity)

    @Delete
    suspend fun delete(task: ResearchTaskEntity)

    @Query("UPDATE research_tasks SET status = :status, progress = :progress WHERE id = :id")
    suspend fun updateStatusAndProgress(id: Long, status: Int, progress: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<ResearchTaskEntity>)

    @Query("DELETE FROM research_tasks")
    suspend fun deleteAll()

    @Query("DELETE FROM research_tasks WHERE status = 2")
    suspend fun deleteCompleted()

    @Query("SELECT DISTINCT category FROM research_tasks ORDER BY category")
    fun getCategories(): Flow<List<String>>
}
