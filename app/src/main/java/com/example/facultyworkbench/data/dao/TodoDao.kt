package com.example.facultyworkbench.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.facultyworkbench.data.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY isCompleted ASC, priority DESC, dueDate ASC, `order` DESC, createdAt DESC")
    fun getAll(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE category = :category ORDER BY isCompleted ASC, priority DESC, dueDate ASC, `order` DESC, createdAt DESC")
    fun getByCategory(category: String): Flow<List<TodoEntity>>

    @Query("SELECT DISTINCT category FROM todos ORDER BY category")
    fun getCategories(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: TodoEntity): Long

    @Update
    suspend fun update(todo: TodoEntity)

    @Delete
    suspend fun delete(todo: TodoEntity)

    @Query("UPDATE todos SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateCompleted(id: Long, isCompleted: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(todos: List<TodoEntity>)

    @Query("DELETE FROM todos")
    suspend fun deleteAll()

    @Query("DELETE FROM todos WHERE isCompleted = 1")
    suspend fun deleteCompleted()
}
