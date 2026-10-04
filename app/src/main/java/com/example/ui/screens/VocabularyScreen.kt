package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ArticleType
import com.example.data.model.LessonData
import com.example.data.model.VocabularyItem
import com.example.ui.components.ArticleBadge
import com.example.ui.components.AudioSpeechButtons
import com.example.ui.components.FlipFlashcard
import com.example.ui.theme.SuccessGreen

enum class VocabFilter {
    ALL,
    NEED_REVIEW,
    LEARNED
}

@Composable
fun VocabularyScreen(
    allLessons: List<LessonData>,
    initialLessonId: String,
    learnedWords: Set<String>,
    onToggleLearned: (String, Boolean) -> Unit,
    onPlayAudio: (String, Boolean) -> Unit, // text, isSlow
    modifier: Modifier = Modifier
) {
    if (allLessons.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("هیچ درسی موجود نیست.")
        }
        return
    }

    var selectedLessonId by remember(initialLessonId, allLessons) {
        val found = allLessons.find { it.id == initialLessonId }
        mutableStateOf(found?.id ?: allLessons.first().id)
    }

    val currentLesson: LessonData = allLessons.find { it.id == selectedLessonId } ?: allLessons.first()
    val lessonIndex = allLessons.indexOf(currentLesson).coerceAtLeast(0)

    var activeFilter by remember { mutableStateOf(VocabFilter.ALL) }
    var currentIndex by remember(selectedLessonId, activeFilter) { mutableIntStateOf(0) }
    var isFlipped by remember(selectedLessonId, currentIndex) { mutableStateOf(false) }
    var isListViewMode by remember { mutableStateOf(false) }

    val lessonWords: List<VocabularyItem> = currentLesson.vocabulary

    // Filter words
    val filteredWords: List<VocabularyItem> = remember(lessonWords, activeFilter, learnedWords, currentLesson.id) {
        when (activeFilter) {
            VocabFilter.ALL -> lessonWords
            VocabFilter.NEED_REVIEW -> lessonWords.filter { !learnedWords.contains("${currentLesson.id}_${it.word}") }
            VocabFilter.LEARNED -> lessonWords.filter { learnedWords.contains("${currentLesson.id}_${it.word}") }
        }
    }

    val safeIndex = if (filteredWords.isNotEmpty()) currentIndex.coerceIn(0, filteredWords.size - 1) else 0
    val currentWord: VocabularyItem? = filteredWords.getOrNull(safeIndex)

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("vocabulary_screen")
    ) {
        // Dynamic Lesson Selector Tabs
        ScrollableTabRow(
            selectedTabIndex = lessonIndex,
            edgePadding = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            allLessons.forEach { lesson ->
                Tab(
                    selected = selectedLessonId == lesson.id,
                    onClick = {
                        selectedLessonId = lesson.id
                        currentIndex = 0
                        isFlipped = false
                    },
                    text = {
                        Text(
                            text = "درس ${lesson.number}",
                            fontWeight = if (selectedLessonId == lesson.id) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Subheader: Lesson Title and Mode Toggle (Cards vs List)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentLesson.titleDari,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = currentLesson.titleGerman,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = { isListViewMode = !isListViewMode },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = if (isListViewMode) Icons.Default.ViewAgenda else Icons.Default.ViewList,
                    contentDescription = "تغییر حالت نمایش",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = activeFilter == VocabFilter.ALL,
                onClick = {
                    activeFilter = VocabFilter.ALL
                    currentIndex = 0
                },
                label = { Text("همه (${lessonWords.size})") }
            )

            val unlearnedCount = lessonWords.count { !learnedWords.contains("${currentLesson.id}_${it.word}") }
            FilterChip(
                selected = activeFilter == VocabFilter.NEED_REVIEW,
                onClick = {
                    activeFilter = VocabFilter.NEED_REVIEW
                    currentIndex = 0
                },
                label = { Text("نیاز به مرور ($unlearnedCount)") }
            )

            val learnedCount = lessonWords.count { learnedWords.contains("${currentLesson.id}_${it.word}") }
            FilterChip(
                selected = activeFilter == VocabFilter.LEARNED,
                onClick = {
                    activeFilter = VocabFilter.LEARNED
                    currentIndex = 0
                },
                label = { Text("یاد گرفته شده ($learnedCount)") }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (isListViewMode) {
            // List View
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredWords, key = { it.word }) { wordItem ->
                    val wordKey = "${currentLesson.id}_${wordItem.word}"
                    val isLearned = learnedWords.contains(wordKey)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (wordItem.article.isNotBlank()) {
                                        val artType = when (wordItem.article.lowercase()) {
                                            "der" -> ArticleType.DER
                                            "die" -> ArticleType.DIE
                                            "das" -> ArticleType.DAS
                                            else -> ArticleType.NONE
                                        }
                                        ArticleBadge(article = artType, showGenderLabel = false)
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        Text(
                                            text = wordItem.word,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    AudioSpeechButtons(
                                        textToSpeak = if (wordItem.article.isNotBlank()) "${wordItem.article} ${wordItem.word}" else wordItem.word,
                                        onPlayAudio = onPlayAudio,
                                        size = 32
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "تلفظ: ${wordItem.pronunciationPersianScript}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = wordItem.meaningDari,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = { onToggleLearned(wordKey, !isLearned) },
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isLearned) SuccessGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isLearned) SuccessGreen else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Flashcard Mode
            if (currentWord != null) {
                val wordKey = "${currentLesson.id}_${currentWord.word}"
                val isLearned = learnedWords.contains(wordKey)
                val example = currentLesson.exampleSentences.getOrNull(safeIndex % currentLesson.exampleSentences.size.coerceAtLeast(1))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Counter and Shuffle bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "واژه ${safeIndex + 1} از ${filteredWords.size}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        IconButton(
                            onClick = {
                                if (filteredWords.size > 1) {
                                    var newIdx: Int
                                    do {
                                        newIdx = (0 until filteredWords.size).random()
                                    } while (newIdx == safeIndex)
                                    currentIndex = newIdx
                                    isFlipped = false
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "تصادفی",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    FlipFlashcard(
                        item = currentWord,
                        example = example,
                        isFlipped = isFlipped,
                        onFlip = { isFlipped = !isFlipped },
                        isLearned = isLearned,
                        onMarkLearned = { learned -> onToggleLearned(wordKey, learned) },
                        onPlayAudio = onPlayAudio,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Previous / Next Nav
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = {
                                if (safeIndex > 0) {
                                    currentIndex = safeIndex - 1
                                    isFlipped = false
                                }
                            },
                            enabled = safeIndex > 0,
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "قبلی",
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Text(
                            text = "${safeIndex + 1} / ${filteredWords.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        FilledIconButton(
                            onClick = {
                                if (safeIndex < filteredWords.size - 1) {
                                    currentIndex = safeIndex + 1
                                    isFlipped = false
                                }
                            },
                            enabled = safeIndex < filteredWords.size - 1,
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "بعدی",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("هیچ واژه‌ای در این دسته یافت نشد.")
                }
            }
        }
    }
}
