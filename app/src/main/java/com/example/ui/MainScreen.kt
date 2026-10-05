package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.GrammarRepository
import com.example.data.repository.UnifiedCourseRepository
import com.example.data.storage.UserProgressManager
import com.example.ui.screens.DialoguesScreen
import com.example.ui.screens.GeminiChatScreen
import com.example.ui.screens.GrammarScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MuseScreen
import com.example.ui.screens.NumbersScreen
import com.example.ui.screens.PracticeScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.VocabularyScreen
import com.example.util.TtsManager

enum class NavDestination(
    val titleDari: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("خانه", Icons.Filled.Home, Icons.Outlined.Home, "nav_item_home"),
    GRAMMAR("گرامر", Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "nav_item_grammar"),
    VOCABULARY("لغات", Icons.Filled.Book, Icons.Outlined.Book, "nav_item_vocab"),
    PRACTICE("تمرین", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter, "nav_item_practice"),
    QUIZ("آزمون", Icons.Filled.Psychology, Icons.Outlined.Psychology, "nav_item_quiz"),
    DIALOGUES("گفتگو", Icons.Filled.Chat, Icons.Outlined.Chat, "nav_item_dialogues"),
    GEMINI_CHAT("چت جیمنای", Icons.Filled.SmartToy, Icons.Outlined.SmartToy, "nav_item_gemini_chat"),
    MUSE("موس", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_item_muse")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    ttsManager: TtsManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val progressManager = remember { UserProgressManager.getInstance(context) }
    val courseRepository = remember { UnifiedCourseRepository.getInstance(context) }
    val grammarRepository = remember { GrammarRepository.getInstance(context) }

    val allLessons by courseRepository.lessonsFlow.collectAsState()
    val overrideNumbers by courseRepository.overrideNumbersFlow.collectAsState()
    val allGrammarTopics by grammarRepository.grammarTopicsFlow.collectAsState()
    val overrideGrammarNumbers by grammarRepository.overrideNumbersFlow.collectAsState()
    val learnedWords by progressManager.learnedWordsFlow.collectAsState()
    val quizHighScore by progressManager.quizHighScoreFlow.collectAsState()
    val savedGeminiKey by progressManager.geminiApiKeyFlow.collectAsState()

    var currentTab by remember { mutableStateOf(NavDestination.HOME) }
    var selectedLessonIdForVocab by remember { mutableStateOf("lesson_1") }
    var selectedLessonSection by remember { mutableIntStateOf(0) }
    var selectedLessonIdForPractice by remember { mutableStateOf("lesson_1") }
    var isNumbersScreenOpen by remember { mutableStateOf(false) }

    // Always RTL layout for Dari UI
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (isNumbersScreenOpen) {
            BackHandler { isNumbersScreenOpen = false }
        } else if (currentTab != NavDestination.HOME) {
            BackHandler { currentTab = NavDestination.HOME }
        }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = if (isNumbersScreenOpen) "تمرین اعداد آلمانی" else "آلمانی بیاموز",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    actions = {
                        if (!isNumbersScreenOpen) {
                            IconButton(
                                onClick = { isNumbersScreenOpen = true },
                                modifier = Modifier.testTag("top_bar_numbers_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = "اعداد ۰ تا ۱۰۰",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            },
            bottomBar = {
                if (!isNumbersScreenOpen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavDestination.values().forEach { destination ->
                            val isSelected = currentTab == destination
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentTab = destination },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                        contentDescription = destination.titleDari,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = destination.titleDari,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.testTag(destination.testTag)
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isNumbersScreenOpen) {
                    NumbersScreen(
                        onPlayAudio = { text, isSlow -> ttsManager.speak(text, isSlow) },
                        onBack = { isNumbersScreenOpen = false }
                    )
                } else {
                    when (currentTab) {
                        NavDestination.HOME -> {
                            HomeScreen(
                                allLessons = allLessons,
                                learnedWords = learnedWords,
                                quizHighScore = quizHighScore,
                                overrideLessonNumbers = overrideNumbers,
                                onNavigateToLessonVocab = { lessonId ->
                                    selectedLessonIdForVocab = lessonId
                                    selectedLessonSection = 0
                                    currentTab = NavDestination.VOCABULARY
                                },
                                onNavigateToLessonPractice = { lessonId ->
                                    selectedLessonIdForVocab = lessonId
                                    selectedLessonSection = 3
                                    currentTab = NavDestination.VOCABULARY
                                },
                                onNavigateToNumbers = { isNumbersScreenOpen = true },
                                onNavigateToQuiz = { currentTab = NavDestination.QUIZ },
                                onNavigateToMuse = { currentTab = NavDestination.MUSE },
                                onImportJson = { json -> courseRepository.importJson(json) },
                                onDeleteCustomLesson = { lessonId -> courseRepository.deleteCustomLesson(lessonId) },
                                onNavigateToGrammar = { currentTab = NavDestination.GRAMMAR }
                            )
                        }

                        NavDestination.GRAMMAR -> {
                            GrammarScreen(
                                allTopics = allGrammarTopics,
                                overrideTopicNumbers = overrideGrammarNumbers,
                                onImportJson = { json -> grammarRepository.importJson(json) },
                                onDeleteCustomTopic = { topicId -> grammarRepository.deleteCustomTopic(topicId) },
                                onPlayAudio = { text, isSlow -> ttsManager.speak(text, isSlow) },
                                onNavigateToLessons = { currentTab = NavDestination.HOME }
                            )
                        }

                        NavDestination.VOCABULARY -> {
                            VocabularyScreen(
                                allLessons = allLessons,
                                initialLessonId = selectedLessonIdForVocab,
                                learnedWords = learnedWords,
                                onToggleLearned = { wordKey, learned ->
                                    progressManager.setWordLearned(wordKey, learned)
                                },
                                onPlayAudio = { text, isSlow -> ttsManager.speak(text, isSlow) },
                                initialSection = selectedLessonSection
                            )
                        }

                        NavDestination.PRACTICE -> {
                            PracticeScreen(
                                allLessons = allLessons,
                                initialLessonId = selectedLessonIdForPractice,
                                onPlayAudio = { text, isSlow -> ttsManager.speak(text, isSlow) }
                            )
                        }

                        NavDestination.QUIZ -> {
                            QuizScreen(
                                allLessons = allLessons,
                                onSaveQuizScore = { score, total ->
                                    progressManager.saveQuizResult(score, total)
                                },
                                onPlayAudio = { text, isSlow -> ttsManager.speak(text, isSlow) }
                            )
                        }

                        NavDestination.DIALOGUES -> {
                            DialoguesScreen(
                                allLessons = allLessons,
                                onPlayAudio = { text, isSlow -> ttsManager.speak(text, isSlow) }
                            )
                        }

                        NavDestination.GEMINI_CHAT -> {
                            val effectiveKey = progressManager.getEffectiveGeminiApiKey()
                            GeminiChatScreen(
                                currentApiKey = effectiveKey,
                                onSaveApiKey = { key -> progressManager.saveGeminiApiKey(key) },
                                onImportLessonJson = { json -> courseRepository.importLesson(json) },
                                onPlayAudio = { text, isSlow -> ttsManager.speak(text, isSlow) },
                                onImportGrammarJson = { json -> grammarRepository.importTopic(json) }
                            )
                        }

                        NavDestination.MUSE -> {
                            MuseScreen(
                                onImportJson = { json -> courseRepository.importJson(json) }
                            )
                        }
                    }
                }
            }
        }
    }
}
