package com.example.facultyworkbench.data.backup

import com.example.facultyworkbench.data.entity.CourseEntity
import com.example.facultyworkbench.data.entity.ProfileEntity
import com.example.facultyworkbench.data.entity.ResearchTaskEntity
import com.example.facultyworkbench.data.entity.TaskEntity
import com.example.facultyworkbench.data.entity.TodoEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * 使用 Android 内置 org.json 进行备份数据的序列化/反序列化，无需额外依赖。
 */
object BackupSerializer {

    fun toJson(data: BackupData): String {
        val root = JSONObject()
        root.put("version", data.version)
        root.put("exportTime", data.exportTime)

        data.profile?.let { root.put("profile", profileToJson(it)) }
        root.put("tasks", JSONArray(data.tasks.map { taskToJson(it) }))
        root.put("courses", JSONArray(data.courses.map { courseToJson(it) }))
        root.put("researchTasks", JSONArray(data.researchTasks.map { researchToJson(it) }))
        root.put("todos", JSONArray(data.todos.map { todoToJson(it) }))

        return root.toString(2)
    }

    /** 仅序列化待办清单列表为 JSON 数组字符串。 */
    fun todosToJson(todos: List<TodoEntity>): String {
        return JSONArray(todos.map { todoToJson(it) }).toString(2)
    }

    /** 从 JSON 数组字符串反序列化待办清单列表。 */
    fun todosFromJson(json: String): List<TodoEntity> {
        val arr = JSONArray(json)
        val list = mutableListOf<TodoEntity>()
        for (i in 0 until arr.length()) {
            list.add(todoFromJson(arr.getJSONObject(i)))
        }
        return list
    }

    fun fromJson(json: String): BackupData {
        val root = JSONObject(json)
        val version = root.optInt("version", 1)
        val exportTime = root.optLong("exportTime", 0L)

        val profile = if (root.isNull("profile")) null else profileFromJson(root.getJSONObject("profile"))

        val tasks = mutableListOf<TaskEntity>()
        val tasksArr = root.optJSONArray("tasks")
        if (tasksArr != null) {
            for (i in 0 until tasksArr.length()) {
                tasks.add(taskFromJson(tasksArr.getJSONObject(i)))
            }
        }

        val courses = mutableListOf<CourseEntity>()
        val coursesArr = root.optJSONArray("courses")
        if (coursesArr != null) {
            for (i in 0 until coursesArr.length()) {
                courses.add(courseFromJson(coursesArr.getJSONObject(i)))
            }
        }

        val researchTasks = mutableListOf<ResearchTaskEntity>()
        val researchArr = root.optJSONArray("researchTasks")
        if (researchArr != null) {
            for (i in 0 until researchArr.length()) {
                researchTasks.add(researchFromJson(researchArr.getJSONObject(i)))
            }
        }

        val todos = mutableListOf<TodoEntity>()
        val todosArr = root.optJSONArray("todos")
        if (todosArr != null) {
            for (i in 0 until todosArr.length()) {
                todos.add(todoFromJson(todosArr.getJSONObject(i)))
            }
        }

        return BackupData(
            version = version,
            exportTime = exportTime,
            profile = profile,
            tasks = tasks,
            courses = courses,
            researchTasks = researchTasks,
            todos = todos
        )
    }

    // ---- TaskEntity ----
    private fun taskToJson(t: TaskEntity) = JSONObject().apply {
        put("id", t.id)
        put("title", t.title)
        put("description", t.description)
        put("priority", t.priority)
        put("dueDate", t.dueDate ?: JSONObject.NULL)
        put("type", t.type)
        put("status", t.status)
        put("progress", t.progress)
        put("createdAt", t.createdAt)
        put("sourceType", t.sourceType)
        put("sourceId", t.sourceId)
    }

    private fun taskFromJson(o: JSONObject) = TaskEntity(
        id = o.optLong("id", 0L),
        title = o.optString("title", ""),
        description = o.optString("description", ""),
        priority = o.optInt("priority", 0),
        dueDate = if (o.isNull("dueDate")) null else o.optLong("dueDate"),
        type = o.optString("type", "general"),
        status = o.optInt("status", 0),
        progress = o.optInt("progress", 0),
        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
        sourceType = o.optString("sourceType", ""),
        sourceId = o.optLong("sourceId", 0L)
    )

    // ---- CourseEntity ----
    private fun courseToJson(c: CourseEntity) = JSONObject().apply {
        put("id", c.id)
        put("name", c.name)
        put("className", c.className)
        put("dayOfWeek", c.dayOfWeek)
        put("startSection", c.startSection)
        put("sectionCount", c.sectionCount)
        put("location", c.location)
        put("weeks", c.weeks)
        put("color", c.color)
    }

    private fun courseFromJson(o: JSONObject) = CourseEntity(
        id = o.optLong("id", 0L),
        name = o.optString("name", ""),
        className = o.optString("className", ""),
        dayOfWeek = o.optInt("dayOfWeek", 1),
        startSection = o.optInt("startSection", 1),
        sectionCount = o.optInt("sectionCount", 2),
        location = o.optString("location", ""),
        weeks = o.optString("weeks", ""),
        color = o.optInt("color", 0)
    )

    // ---- ResearchTaskEntity ----
    private fun researchToJson(r: ResearchTaskEntity) = JSONObject().apply {
        put("id", r.id)
        put("title", r.title)
        put("category", r.category)
        put("priority", r.priority)
        put("dueDate", r.dueDate ?: JSONObject.NULL)
        put("note", r.note)
        put("progress", r.progress)
        put("status", r.status)
        put("createdAt", r.createdAt)
    }

    private fun researchFromJson(o: JSONObject) = ResearchTaskEntity(
        id = o.optLong("id", 0L),
        title = o.optString("title", ""),
        category = o.optString("category", "论文"),
        priority = o.optInt("priority", 0),
        dueDate = if (o.isNull("dueDate")) null else o.optLong("dueDate"),
        note = o.optString("note", ""),
        progress = o.optInt("progress", 0),
        status = o.optInt("status", 0),
        createdAt = o.optLong("createdAt", System.currentTimeMillis())
    )

    // ---- TodoEntity ----
    private fun todoToJson(t: TodoEntity) = JSONObject().apply {
        put("id", t.id)
        put("title", t.title)
        put("note", t.note)
        put("category", t.category)
        put("priority", t.priority)
        put("dueDate", t.dueDate ?: JSONObject.NULL)
        put("isCompleted", t.isCompleted)
        put("order", t.order)
        put("createdAt", t.createdAt)
    }

    private fun todoFromJson(o: JSONObject) = TodoEntity(
        id = o.optLong("id", 0L),
        title = o.optString("title", ""),
        note = o.optString("note", ""),
        category = o.optString("category", "默认"),
        priority = o.optInt("priority", 0),
        dueDate = if (o.isNull("dueDate")) null else o.optLong("dueDate"),
        isCompleted = o.optBoolean("isCompleted", false),
        order = o.optInt("order", 0),
        createdAt = o.optLong("createdAt", System.currentTimeMillis())
    )

    // ---- ProfileEntity ----
    private fun profileToJson(p: ProfileEntity) = JSONObject().apply {
        put("id", p.id)
        put("name", p.name)
        put("department", p.department)
        put("title", p.title)
    }

    private fun profileFromJson(o: JSONObject) = ProfileEntity(
        id = o.optInt("id", 1),
        name = o.optString("name", ""),
        department = o.optString("department", ""),
        title = o.optString("title", "")
    )
}
