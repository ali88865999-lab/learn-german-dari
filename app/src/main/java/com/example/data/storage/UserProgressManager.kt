package com.example.data.storage

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserProgressManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("german_learning_prefs", Context.MODE_PRIVATE)

    private val _learnedWordsFlow = MutableStateFlow<Set<String>>(emptySet())
    val learnedWordsFlow: StateFlow<Set<String>> = _learnedWordsFlow.asStateFlow()

    private val _quizHighScoreFlow = MutableStateFlow(0)
    val quizHighScoreFlow: StateFlow<Int> = _quizHighScoreFlow.asStateFlow()

    private val _quizzesTakenCountFlow = MutableStateFlow(0)
    val quizzesTakenCountFlow: StateFlow<Int> = _quizzesTakenCountFlow.asStateFlow()

    private val _geminiApiKeyFlow = MutableStateFlow("")
    val geminiApiKeyFlow: StateFlow<String> = _geminiApiKeyFlow.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val learnedSet = prefs.getStringSet(KEY_LEARNED_WORDS, emptySet()) ?: emptySet()
        _learnedWordsFlow.value = learnedSet
        _quizHighScoreFlow.value = prefs.getInt(KEY_QUIZ_HIGH_SCORE, 0)
        _quizzesTakenCountFlow.value = prefs.getInt(KEY_QUIZZES_TAKEN_COUNT, 0)
        _geminiApiKeyFlow.value = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
    }

    fun saveGeminiApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString(KEY_GEMINI_API_KEY, trimmed).apply()
        _geminiApiKeyFlow.value = trimmed
    }

    fun getEffectiveGeminiApiKey(): String {
        val userKey = _geminiApiKeyFlow.value.trim()
        if (userKey.isNotEmpty()) return userKey

        // Fallback to BuildConfig if configured
        val buildKey = com.example.BuildConfig.GEMINI_API_KEY
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey
        }
        return ""
    }

    fun isWordLearned(wordId: String): Boolean {
        return _learnedWordsFlow.value.contains(wordId)
    }

    fun toggleWordLearned(wordId: String): Boolean {
        val current = _learnedWordsFlow.value.toMutableSet()
        val isNowLearned: Boolean
        if (current.contains(wordId)) {
            current.remove(wordId)
            isNowLearned = false
        } else {
            current.add(wordId)
            isNowLearned = true
        }
        prefs.edit().putStringSet(KEY_LEARNED_WORDS, current).apply()
        _learnedWordsFlow.value = current
        return isNowLearned
    }

    fun setWordLearned(wordId: String, learned: Boolean) {
        val current = _learnedWordsFlow.value.toMutableSet()
        if (learned) {
            current.add(wordId)
        } else {
            current.remove(wordId)
        }
        prefs.edit().putStringSet(KEY_LEARNED_WORDS, current).apply()
        _learnedWordsFlow.value = current
    }

    fun saveQuizResult(score: Int, total: Int) {
        val currentHigh = _quizHighScoreFlow.value
        val newHigh = maxOf(currentHigh, score)
        val newCount = _quizzesTakenCountFlow.value + 1

        prefs.edit()
            .putInt(KEY_QUIZ_HIGH_SCORE, newHigh)
            .putInt(KEY_QUIZZES_TAKEN_COUNT, newCount)
            .apply()

        _quizHighScoreFlow.value = newHigh
        _quizzesTakenCountFlow.value = newCount
    }

    fun resetAllProgress() {
        prefs.edit().clear().apply()
        _learnedWordsFlow.value = emptySet()
        _quizHighScoreFlow.value = 0
        _quizzesTakenCountFlow.value = 0
    }

    companion object {
        private const val KEY_LEARNED_WORDS = "learned_words"
        private const val KEY_QUIZ_HIGH_SCORE = "quiz_high_score"
        private const val KEY_QUIZZES_TAKEN_COUNT = "quizzes_taken_count"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"

        @Volatile
        private var instance: UserProgressManager? = null

        fun getInstance(context: Context): UserProgressManager {
            return instance ?: synchronized(this) {
                instance ?: UserProgressManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
