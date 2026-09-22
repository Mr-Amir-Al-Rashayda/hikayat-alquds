package ps.hikayatalquds.domain.narrative

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ps.hikayatalquds.domain.model.AppLanguage
import ps.hikayatalquds.domain.model.AudienceMode
import ps.hikayatalquds.domain.model.GenerationOrigin
import ps.hikayatalquds.domain.model.LocalizedList
import ps.hikayatalquds.domain.model.LocalizedText
import ps.hikayatalquds.domain.model.Location
import ps.hikayatalquds.domain.model.StoryTone
import ps.hikayatalquds.domain.model.TimelineEvent

/**
 * The narrator is where this project's central promise is either kept or
 * broken, so these tests are about *what it must not do* as much as what it
 * does: no sentence may appear that is not in the reviewed record, and a
 * question the record does not answer must come back as a refusal rather than
 * an invention.
 */
class OnDeviceNarratorTest {

    private val narrator = OnDeviceNarrator()

    private val silwan = Location(
        id = "silwan",
        name = LocalizedText("Silwan", "سلوان"),
        city = "Jerusalem",
        country = "Palestine",
        latitude = 31.7712,
        longitude = 35.2366,
        description = LocalizedText(
            "A Jerusalem village on the slope below the Old City, known for its spring and terraced gardens.",
            "قرية مقدسية على المنحدر أسفل البلدة القديمة، تُعرف بعينها وبساتينها المدرَّجة.",
        ),
        culturalImportance = LocalizedText(
            "Families have farmed these terraces for generations.",
            "زرعت العائلات هذه المدرجات لأجيال.",
        ),
        historicalSummary = LocalizedText(
            "Ordnance surveyors mapped the spring in 1864. Photographs record the gardens into the 1960s.",
            "رسم المساحون العين سنة 1864. وتوثق الصور البساتين حتى الستينيات.",
        ),
        landmarks = LocalizedList(
            listOf("Ain Silwan", "The terraced gardens"),
            listOf("عين سلوان", "البساتين المدرَّجة"),
        ),
        storyTitle = LocalizedText("Silwan: the spring below the walls", "سلوان: العين تحت الأسوار"),
        story = LocalizedText(
            "Walk down from the Old City wall and the ground drops into terraces.",
            "انزل من سور البلدة القديمة فتهبط الأرض إلى مدرجات.",
        ),
        walkthrough = LocalizedText(
            "Start at the spring. Follow the water channel along the slope.",
            "ابدأ من العين. اتبع قناة الماء على المنحدر.",
        ),
        coverImage = "images/jerusalem/silwan.jpg",
        coverImageCredit = "A photographer",
        coverImageLicense = "CC BY-SA 4.0",
        coverImageSourceUrl = "https://commons.wikimedia.org/",
        contentFile = "content/ai-ready/silwan-summary.md",
        categoryLabels = listOf("حي مقدسي"),
        interests = emptyList(),
        citations = emptyList(),
    )

    private val timeline = listOf(
        TimelineEvent(
            id = "tl-silwan-01",
            locationId = "silwan",
            periodLabel = LocalizedText("1864", "١٨٦٤"),
            description = LocalizedText(
                "The Ordnance Survey of Jerusalem recorded the spring and the village.",
                "سجّل مسح القدس العين والقرية.",
            ),
            sortYear = 1864,
            sortOrder = 0,
            isTradition = false,
            sourceFile = "content/ai-ready/silwan-summary.md",
        ),
    )

    /** Every sentence the reviewed record contains, as a comparison corpus. */
    private fun corpus(language: AppLanguage) = listOf(
        silwan.description[language],
        silwan.culturalImportance[language],
        silwan.historicalSummary[language],
        silwan.story[language],
        silwan.walkthrough[language],
    ).joinToString(" ") + " " + silwan.landmarks[language].joinToString(" ")

    @Test
    fun `every audience produces a narrative and declares itself on-device`() {
        AudienceMode.entries.forEach { audience ->
            val story = narrator.generate(silwan, audience, StoryTone.STORYTELLING, AppLanguage.ENGLISH)
            assertTrue("$audience produced nothing", story.narrative.isNotBlank())
            assertEquals(GenerationOrigin.ON_DEVICE, story.origin)
            assertEquals(audience, story.audience)
            assertEquals("content/ai-ready/silwan-summary.md", story.sourceFile)
        }
    }

    @Test
    fun `every audience says what it did not claim`() {
        AudienceMode.entries.forEach { audience ->
            val story = narrator.generate(silwan, audience, StoryTone.NEUTRAL, AppLanguage.ARABIC)
            assertTrue(
                "$audience gave no uncertainty note",
                story.uncertaintyNotes.isNotEmpty() && story.uncertaintyNotes.all { it.isNotBlank() },
            )
        }
    }

    @Test
    fun `the short retelling reuses whole sentences from the record`() {
        val story = narrator.generate(silwan, AudienceMode.SHORT, StoryTone.NEUTRAL, AppLanguage.ENGLISH)
        val source = corpus(AppLanguage.ENGLISH)
        // The short mode is pure extraction: every sentence must be present verbatim.
        story.narrative.split(Regex("(?<=[.!?])\\s+"))
            .map(String::trim)
            .filter { it.isNotBlank() }
            .forEach { sentence ->
                assertTrue("invented sentence: $sentence", source.contains(sentence))
            }
    }

    @Test
    fun `no retelling introduces a year that is not in the record`() {
        val years = Regex("\\b1[0-9]{3}\\b")
        val allowed = years.findAll(corpus(AppLanguage.ENGLISH)).map { it.value }.toSet()
        AudienceMode.entries.forEach { audience ->
            val story = narrator.generate(silwan, audience, StoryTone.NEUTRAL, AppLanguage.ENGLISH)
            years.findAll(story.narrative).forEach { match ->
                assertTrue(
                    "$audience invented the year ${match.value}",
                    match.value in allowed,
                )
            }
        }
    }

    @Test
    fun `the child retelling is gentler but still only names real landmarks`() {
        val story = narrator.generate(silwan, AudienceMode.CHILD, StoryTone.EMOTIONAL, AppLanguage.ENGLISH)
        assertTrue(story.narrative.contains("Ain Silwan"))
        assertTrue(
            "a child retelling should invite looking, not assert new history",
            story.narrative.contains("spot") || story.narrative.contains("Imagine"),
        )
    }

    @Test
    fun `the student retelling is sectioned`() {
        val story = narrator.generate(silwan, AudienceMode.STUDENT, StoryTone.EDUCATIONAL, AppLanguage.ENGLISH)
        listOf("Introduction", "Historical cause and sequence", "Reading the place").forEach {
            assertTrue("missing section $it", story.narrative.contains(it))
        }
    }

    @Test
    fun `Arabic retellings come back in Arabic`() {
        val story = narrator.generate(silwan, AudienceMode.HISTORIAN, StoryTone.NEUTRAL, AppLanguage.ARABIC)
        assertEquals(AppLanguage.ARABIC, story.language)
        assertTrue("expected Arabic text", story.narrative.any { it in '؀'..'ۿ' })
        assertTrue(story.narrative.contains("الإطار الزمني"))
    }

    @Test
    fun `the word count matches the narrative it describes`() {
        val story = narrator.generate(silwan, AudienceMode.TOURIST, StoryTone.NEUTRAL, AppLanguage.ENGLISH)
        val counted = story.narrative.split(Regex("\\s+")).count { it.isNotBlank() }
        assertEquals(counted, story.wordCount)
    }

    // --- the guide ----------------------------------------------------------

    @Test
    fun `answers a question the record covers, and cites what it used`() {
        val answer = narrator.answer(silwan, timeline, "What is the history of the spring?", AppLanguage.ENGLISH)
        assertTrue(answer.answeredFromSource)
        assertTrue(answer.excerpts.isNotEmpty())
        val source = corpus(AppLanguage.ENGLISH) + " " +
            timeline.joinToString(" ") { it.description[AppLanguage.ENGLISH] }
        answer.excerpts.forEach { excerpt ->
            assertTrue(
                "excerpt is not from the record: $excerpt",
                excerpt.split(" ").take(4).joinToString(" ").let(source::contains) ||
                    source.contains(excerpt),
            )
        }
    }

    @Test
    fun `refuses a question the record does not cover instead of inventing`() {
        val answer = narrator.answer(
            silwan,
            timeline,
            "How many buses run to the neighbourhood on a Tuesday?",
            AppLanguage.ENGLISH,
        )
        assertFalse(answer.answeredFromSource)
        assertTrue(answer.excerpts.isEmpty())
        assertTrue(answer.answer.contains("will not guess"))
        assertTrue(answer.uncertaintyNotes.isNotEmpty())
    }

    @Test
    fun `refuses in Arabic when asked in Arabic`() {
        val answer = narrator.answer(silwan, timeline, "كم عدد الحافلات يوم الثلاثاء؟", AppLanguage.ARABIC)
        assertFalse(answer.answeredFromSource)
        assertTrue(answer.answer.any { it in '؀'..'ۿ' })
    }

    @Test
    fun `an empty question is a refusal, not a dump of the record`() {
        val answer = narrator.answer(silwan, timeline, "   ", AppLanguage.ENGLISH)
        assertFalse(answer.answeredFromSource)
    }

    @Test
    fun `Arabic spelling variants still match the record`() {
        // Asked with a bare alef where the record uses hamza, and no diacritics.
        val answer = narrator.answer(silwan, timeline, "ما اهمية البساتين المدرجة؟", AppLanguage.ARABIC)
        assertTrue("alef and diacritic folding should still match", answer.answeredFromSource)
    }

    @Test
    fun `landmark questions are answered from the landmark list`() {
        val answer = narrator.answer(silwan, timeline, "Tell me about the terraced gardens", AppLanguage.ENGLISH)
        assertTrue(answer.answeredFromSource)
        assertTrue(answer.answer.contains("terrace", ignoreCase = true))
    }
}
