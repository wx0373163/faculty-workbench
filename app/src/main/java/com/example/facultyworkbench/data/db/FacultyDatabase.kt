package com.example.facultyworkbench.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.facultyworkbench.data.dao.CourseDao
import com.example.facultyworkbench.data.dao.ProfileDao
import com.example.facultyworkbench.data.dao.ResearchTaskDao
import com.example.facultyworkbench.data.dao.TaskDao
import com.example.facultyworkbench.data.entity.CourseEntity
import com.example.facultyworkbench.data.entity.ProfileEntity
import com.example.facultyworkbench.data.entity.ResearchTaskEntity
import com.example.facultyworkbench.data.entity.TaskEntity

@Database(
    entities = [TaskEntity::class, CourseEntity::class, ResearchTaskEntity::class, ProfileEntity::class],
    version = 3,
    exportSchema = false
)
abstract class FacultyDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun courseDao(): CourseDao
    abstract fun researchTaskDao(): ResearchTaskDao
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var INSTANCE: FacultyDatabase? = null

        fun getDatabase(context: Context): FacultyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FacultyDatabase::class.java,
                    "faculty_workbench.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
