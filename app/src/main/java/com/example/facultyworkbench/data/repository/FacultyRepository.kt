package com.example.facultyworkbench.data.repository

import com.example.facultyworkbench.data.backup.BackupData
import com.example.facultyworkbench.data.backup.BackupSerializer
import com.example.facultyworkbench.data.dao.CourseDao
import com.example.facultyworkbench.data.dao.ProfileDao
import com.example.facultyworkbench.data.dao.ResearchTaskDao
import com.example.facultyworkbench.data.dao.TaskDao
import com.example.facultyworkbench.data.dao.TodoDao
import com.example.facultyworkbench.data.entity.CourseEntity
import com.example.facultyworkbench.data.entity.ProfileEntity
import com.example.facultyworkbench.data.entity.ResearchTaskEntity
import com.example.facultyworkbench.data.entity.TaskEntity
import com.example.facultyworkbench.data.entity.TodoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class FacultyRepository(
    private val taskDao: TaskDao,
    private val courseDao: CourseDao,
    private val researchTaskDao: ResearchTaskDao,
    private val profileDao: ProfileDao,
    private val todoDao: TodoDao
) {
    // Tasks
    val allTasks: Flow<List<TaskEntity>> = taskDao.getAll()
    suspend fun insertTask(task: TaskEntity): Long = taskDao.insert(task)
    suspend fun updateTask(task: TaskEntity) = taskDao.update(task)
    suspend fun deleteTask(task: TaskEntity) {
        taskDao.delete(task)
        // 反向同步：若任务来自待办/科研，同时删除来源项
        when (task.sourceType) {
            "todo" -> {
                val todo = todoDao.getAll().first().firstOrNull { it.id == task.sourceId }
                if (todo != null) todoDao.delete(todo)
            }
            "research" -> {
                val r = researchTaskDao.getAll().first().firstOrNull { it.id == task.sourceId }
                if (r != null) researchTaskDao.delete(r)
            }
        }
    }
    suspend fun updateTaskStatus(id: Long, status: Int, progress: Int) {
        val task = taskDao.getById(id) ?: return
        taskDao.updateStatusAndProgress(id, status, progress)
        // 反向同步：若任务来自待办/科研，同步更新来源的完成状态
        when (task.sourceType) {
            "todo" -> todoDao.updateCompleted(task.sourceId, status == 2)
            "research" -> researchTaskDao.updateStatusAndProgress(task.sourceId, status, progress)
        }
    }

    // Courses
    val allCourses: Flow<List<CourseEntity>> = courseDao.getAll()
    suspend fun insertCourse(course: CourseEntity): Long = courseDao.insert(course)
    suspend fun updateCourse(course: CourseEntity) = courseDao.update(course)
    suspend fun deleteCourse(course: CourseEntity) = courseDao.delete(course)

    // Research tasks
    val allResearchTasks: Flow<List<ResearchTaskEntity>> = researchTaskDao.getAll()
    val researchCategories: Flow<List<String>> = researchTaskDao.getCategories()
    suspend fun insertResearchTask(task: ResearchTaskEntity): Long {
        val id = researchTaskDao.insert(task)
        syncResearchToTask(task.copy(id = id))
        return id
    }
    suspend fun updateResearchTask(task: ResearchTaskEntity) {
        researchTaskDao.update(task)
        syncResearchToTask(task)
    }
    suspend fun deleteResearchTask(task: ResearchTaskEntity) {
        researchTaskDao.delete(task)
        taskDao.deleteBySource("research", task.id)
    }
    suspend fun updateResearchStatus(id: Long, status: Int, progress: Int) {
        researchTaskDao.updateStatusAndProgress(id, status, progress)
        val task = taskDao.getBySource("research", id)
        if (task != null) {
            taskDao.updateStatusAndProgress(task.id, status, progress)
        }
    }
    suspend fun deleteCompletedResearchTasks() {
        val completed = researchTaskDao.getAll().first().filter { it.status == 2 }
        completed.forEach { taskDao.deleteBySource("research", it.id) }
        researchTaskDao.deleteCompleted()
    }

    /** 将科研事项同步（新增或更新）到今日任务表 */
    private suspend fun syncResearchToTask(r: ResearchTaskEntity) {
        val existing = taskDao.getBySource("research", r.id)
        val task = TaskEntity(
            id = existing?.id ?: 0,
            title = r.title,
            description = r.note,
            priority = r.priority,
            dueDate = r.dueDate,
            type = "research",
            status = r.status,
            progress = r.progress,
            createdAt = r.createdAt,
            sourceType = "research",
            sourceId = r.id
        )
        if (existing != null) taskDao.update(task) else taskDao.insert(task)
    }

    // Profile
    val profile: Flow<ProfileEntity?> = profileDao.get()
    suspend fun upsertProfile(profile: ProfileEntity) = profileDao.upsert(profile)

    // Todos
    val allTodos: Flow<List<TodoEntity>> = todoDao.getAll()
    val todoCategories: Flow<List<String>> = todoDao.getCategories()
    suspend fun insertTodo(todo: TodoEntity): Long {
        val id = todoDao.insert(todo)
        syncTodoToTask(todo.copy(id = id))
        return id
    }
    suspend fun updateTodo(todo: TodoEntity) {
        todoDao.update(todo)
        syncTodoToTask(todo)
    }
    suspend fun deleteTodo(todo: TodoEntity) {
        todoDao.delete(todo)
        taskDao.deleteBySource("todo", todo.id)
    }
    suspend fun updateTodoCompleted(id: Long, isCompleted: Boolean) {
        todoDao.updateCompleted(id, isCompleted)
        val task = taskDao.getBySource("todo", id)
        if (task != null) {
            taskDao.updateStatusAndProgress(task.id, if (isCompleted) 2 else 0, if (isCompleted) 100 else 0)
        }
    }
    suspend fun deleteCompletedTodos() {
        // 先找到已完成待办对应的同步任务并删除
        val completed = todoDao.getAll().first().filter { it.isCompleted }
        completed.forEach { taskDao.deleteBySource("todo", it.id) }
        todoDao.deleteCompleted()
    }

    /** 将待办事项同步（新增或更新）到今日任务表 */
    private suspend fun syncTodoToTask(todo: TodoEntity) {
        val existing = taskDao.getBySource("todo", todo.id)
        val task = TaskEntity(
            id = existing?.id ?: 0,
            title = todo.title,
            description = todo.note,
            priority = todo.priority,
            dueDate = todo.dueDate,
            type = "todo",
            status = if (todo.isCompleted) 2 else 0,
            progress = if (todo.isCompleted) 100 else 0,
            createdAt = todo.createdAt,
            sourceType = "todo",
            sourceId = todo.id
        )
        if (existing != null) taskDao.update(task) else taskDao.insert(task)
    }

    // 待办清单导入/导出（JSON）
    suspend fun exportTodosJson(): String = BackupSerializer.todosToJson(todoDao.getAll().first())

    /**
     * 从 JSON 字符串导入待办清单。
     * @param json JSON 数组字符串
     * @param merge true=追加合并（保留现有），false=替换现有全部
     * @return 导入的条数
     */
    suspend fun importTodosJson(json: String, merge: Boolean): Int {
        val todos = BackupSerializer.todosFromJson(json)
        if (!merge) {
            todoDao.deleteAll()
            taskDao.deleteBySourceType("todo")
        }
        // 导入时重置 id，避免主键冲突；按当前列表顺序赋予 order
        var order = if (merge) todoDao.getAll().first().size else 0
        todos.forEach {
            val id = todoDao.insert(it.copy(id = 0, order = order++))
            syncTodoToTask(it.copy(id = id, order = order - 1))
        }
        return todos.size
    }

    // 备份导出/导入
    suspend fun exportData(): BackupData = BackupData(
        profile = profile.first(),
        tasks = taskDao.getAll().first(),
        courses = courseDao.getAll().first(),
        researchTasks = researchTaskDao.getAll().first(),
        todos = todoDao.getAll().first()
    )

    suspend fun importData(data: BackupData) {
        taskDao.deleteAll()
        courseDao.deleteAll()
        researchTaskDao.deleteAll()
        todoDao.deleteAll()
        if (data.tasks.isNotEmpty()) taskDao.insertAll(data.tasks)
        if (data.courses.isNotEmpty()) courseDao.insertAll(data.courses)
        if (data.researchTasks.isNotEmpty()) researchTaskDao.insertAll(data.researchTasks)
        if (data.todos.isNotEmpty()) todoDao.insertAll(data.todos)
        data.profile?.let { profileDao.upsert(it) }
    }
}
