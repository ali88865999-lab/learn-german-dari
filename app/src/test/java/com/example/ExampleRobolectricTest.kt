package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.LessonData
import com.example.data.repository.BuiltInCourseData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("آلمانی بیاموز", appName)
    }

    @Test
    fun `verify unified schema has 8 built-in lessons with mandatory pronunciation and translation`() {
        assertEquals(8, BuiltInCourseData.lessons.size)
        for (lesson in BuiltInCourseData.lessons) {
            assertTrue("Lesson ${lesson.number} must have >= 15 vocabulary items", lesson.vocabulary.size >= 15)
            for (vocab in lesson.vocabulary) {
                assertTrue(vocab.word.isNotBlank())
                assertTrue("Pronunciation in Persian script is required for ${vocab.word}", vocab.pronunciationPersianScript.isNotBlank())
                assertTrue("Meaning in Dari is required for ${vocab.word}", vocab.meaningDari.isNotBlank())
            }

            assertTrue("Lesson ${lesson.number} must have exercises", lesson.exercises.isNotEmpty())
            for (ex in lesson.exercises) {
                assertEquals("Each exercise must have exactly 4 options", 4, ex.options.size)
                assertTrue("Exercise correctAnswer must be in options", ex.options.contains(ex.correctAnswer))
                assertTrue("Exercise must have pronunciation", ex.pronunciation.isNotBlank())
                assertTrue("Exercise must have Dari translation", ex.translationDari.isNotBlank())
                assertTrue("Exercise must have Dari explanation", ex.explanationDari.isNotBlank())
            }
        }
    }

    @Test
    fun `verify lesson parser rejects missing pronunciation or translation`() {
        // Missing pronunciation in vocabulary
        val invalidJson = """
            {
              "id": "test_invalid",
              "number": 9,
              "titleGerman": "Test",
              "titleDari": "تست",
              "vocabulary": [
                {
                  "article": "der",
                  "word": "Test",
                  "pronunciationPersianScript": "",
                  "meaningDari": "تست"
                }
              ]
            }
        """.trimIndent()

        val result = LessonData.parseAndValidate(invalidJson)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("تلفظ") == true)
    }

    @Test
    fun `verify lesson parser succeeds for valid schema`() {
        val validJson = """
            {
              "id": "test_valid_9",
              "number": 9,
              "titleGerman": "Reisen",
              "titleDari": "سفر کردن",
              "vocabulary": [
                {
                  "article": "der",
                  "word": "Zug",
                  "pronunciationPersianScript": "تسوگ",
                  "meaningDari": "قطار"
                }
              ],
              "exampleSentences": [
                {
                  "german": "Der Zug kommt pünktlich.",
                  "pronunciation": "دِر تسوگ کومت پونکتلیش.",
                  "meaningDari": "قطار به موقع می‌رسد."
                }
              ],
              "exercises": [
                {
                  "type": "multiple-choice",
                  "question": "___ Zug fährt nach Berlin.",
                  "pronunciation": "دِر تسوگ فِرت ناخ برلین.",
                  "translationDari": "قطار به سمت برلین حرکت می‌کند.",
                  "options": ["Der", "Die", "Das", "Den"],
                  "correctAnswer": "Der",
                  "explanationDari": "واژه Zug مذکر است و با حرف تعریف Der می‌آید."
                }
              ],
              "dialogues": [
                {
                  "lines": [
                    {
                      "speaker": "احمد",
                      "german": "Wann fährt der Zug?",
                      "pronunciation": "وان فِرت دِر تسوگ؟",
                      "meaningDari": "قطار چه ساعتی حرکت می‌کند؟"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val result = LessonData.parseAndValidate(validJson)
        assertTrue(result.isSuccess)
        val lesson = result.getOrNull()
        assertNotNull(lesson)
        assertEquals("test_valid_9", lesson?.id)
        assertEquals("Reisen", lesson?.titleGerman)
        assertEquals("سفر کردن", lesson?.titleDari)
        assertEquals(1, lesson?.vocabulary?.size)
        assertEquals("Zug", lesson?.vocabulary?.first()?.word)
        assertEquals("تسوگ", lesson?.vocabulary?.first()?.pronunciationPersianScript)
        assertEquals(4, lesson?.exercises?.first()?.options?.size)
    }

    @Test
    fun `verify source field parsing for muse lessons`() {
        val museJson = """
            {
              "id": "muse_lesson_1",
              "number": 9,
              "source": "muse",
              "titleGerman": "Im Restaurant",
              "titleDari": "در رستورانت",
              "vocabulary": [
                {
                  "article": "die",
                  "word": "Suppe",
                  "pronunciationPersianScript": "زوپه",
                  "meaningDari": "شوربا / سوپ"
                }
              ]
            }
        """.trimIndent()

        val result = LessonData.parseAndValidate(museJson)
        assertTrue(result.isSuccess)
        val lesson = result.getOrNull()
        assertNotNull(lesson)
        assertEquals("muse", lesson?.source)
    }
}
