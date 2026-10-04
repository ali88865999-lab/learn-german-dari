package com.example.data.repository

import android.content.Context
import com.example.data.model.LessonData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.io.File

class UnifiedCourseRepository private constructor(private val context: Context) {

    private val customLessonsFile = File(context.filesDir, "custom_lessons.json")

    private val _lessonsFlow = MutableStateFlow<List<LessonData>>(emptyList())
    val lessonsFlow: StateFlow<List<LessonData>> = _lessonsFlow.asStateFlow()

    init {
        reloadAllLessons()
    }

    fun reloadAllLessons() {
        val builtIn = BuiltInCourseData.lessons
        val custom = loadCustomLessons()
        _lessonsFlow.value = builtIn + custom
    }

    private fun loadCustomLessons(): List<LessonData> {
        if (!customLessonsFile.exists()) return emptyList()
        return try {
            val jsonText = customLessonsFile.readText()
            val array = JSONArray(jsonText)
            val list = mutableListOf<LessonData>()
            for (i in 0 until array.length()) {
                val parsed = LessonData.parseAndValidate(array.getJSONObject(i).toString())
                parsed.getOrNull()?.let { list.add(it) }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveCustomLessons(customLessons: List<LessonData>) {
        try {
            val array = JSONArray()
            customLessons.forEach { lesson ->
                array.put(org.json.JSONObject(lesson.toJson()))
            }
            customLessonsFile.writeText(array.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun importLesson(jsonString: String): Result<LessonData> {
        val parseResult = LessonData.parseAndValidate(jsonString)
        if (parseResult.isFailure) {
            return parseResult
        }

        val parsedLesson = parseResult.getOrThrow()

        // Ensure unique ID and correct lesson number
        val currentAll = _lessonsFlow.value
        val nextNumber = (currentAll.maxOfOrNull { it.number } ?: 8) + 1
        val finalLesson = parsedLesson.copy(
            id = if (parsedLesson.id.isBlank() || currentAll.any { it.id == parsedLesson.id }) {
                "custom_lesson_${System.currentTimeMillis()}"
            } else parsedLesson.id,
            number = if (parsedLesson.number <= 8) nextNumber else parsedLesson.number
        )

        val currentCustom = loadCustomLessons().toMutableList()
        currentCustom.add(finalLesson)
        saveCustomLessons(currentCustom)

        _lessonsFlow.value = BuiltInCourseData.lessons + currentCustom
        return Result.success(finalLesson)
    }

    fun deleteCustomLesson(lessonId: String): Boolean {
        val currentCustom = loadCustomLessons().toMutableList()
        val removed = currentCustom.removeAll { it.id == lessonId }
        if (removed) {
            saveCustomLessons(currentCustom)
            _lessonsFlow.value = BuiltInCourseData.lessons + currentCustom
        }
        return removed
    }

    companion object {
        @Volatile
        private var instance: UnifiedCourseRepository? = null

        fun getInstance(context: Context): UnifiedCourseRepository {
            return instance ?: synchronized(this) {
                instance ?: UnifiedCourseRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
