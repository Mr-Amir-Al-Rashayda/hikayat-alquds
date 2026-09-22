package ps.hikayatalquds.narration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sentence splitting is what makes narration pausable and highlightable, and it
 * has to work for Arabic punctuation as well as Latin.
 *
 * Tested as pure logic because the interesting failures - losing a sentence,
 * splitting mid-word, ignoring the Arabic question mark - have nothing to do
 * with the speech engine.
 */
class NarrationSegmentationTest {

    private fun split(text: String) = NarrationController.splitIntoSegments(text)

    @Test
    fun `an empty narration produces no segments`() {
        assertTrue(split("").isEmpty())
        assertTrue(split("    ").isEmpty())
    }

    @Test
    fun `segments are numbered from zero without gaps`() {
        val segments = split(
            "The first sentence is long enough to stand on its own here. " +
                "The second sentence is also long enough to stand alone here. " +
                "And the third sentence is likewise long enough to stand alone.",
        )
        assertEquals(segments.indices.toList(), segments.map { it.index })
    }

    @Test
    fun `no text is lost when splitting`() {
        val text = "Enter through the gate. Follow the lane uphill. The spring is on your left."
        val rejoined = split(text).joinToString(" ") { it.text }
        // Word-for-word, ignoring how whitespace was normalised.
        assertEquals(
            text.split(Regex("\\s+")).filter { it.isNotBlank() },
            rejoined.split(Regex("\\s+")).filter { it.isNotBlank() },
        )
    }

    @Test
    fun `splits on the Arabic question mark`() {
        val segments = split(
            "ما تاريخ هذا المكان وكيف تطور عبر العصور المتتابعة؟ " +
                "يمتد السجل المراجع من العصر الروماني حتى اليوم الحاضر.",
        )
        assertEquals(2, segments.size)
        assertTrue(segments[0].text.endsWith("؟"))
    }

    @Test
    fun `newlines become sentence breaks rather than being read aloud`() {
        val segments = split("Chronology\nThe survey recorded the spring in 1864.")
        assertTrue(segments.none { it.text.contains("\n") })
        assertTrue(segments.any { it.text.contains("1864") })
    }

    @Test
    fun `markdown marks are stripped before speaking`() {
        val segments = split("**Bold** heading with a `code span` and a bullet point here now.")
        val spoken = segments.joinToString(" ") { it.text }
        listOf("*", "#", "`", "_", "•").forEach { mark ->
            assertTrue("$mark reached the speech engine", !spoken.contains(mark))
        }
    }

    @Test
    fun `very short fragments are merged so highlighting is not jumpy`() {
        val segments = split("Yes. No. Maybe. This sentence is a good deal longer than those.")
        assertTrue("expected fragments to be merged, got ${segments.size}", segments.size <= 2)
    }

    @Test
    fun `an over-long sentence is broken at word boundaries`() {
        val long = List(200) { "word$it" }.joinToString(" ") + "."
        val segments = split(long)
        assertTrue("should have been chunked", segments.size > 1)
        segments.forEach { segment ->
            assertTrue("chunk too long: ${segment.text.length}", segment.text.length <= 330)
            assertTrue("chunk starts mid-word", segment.text.trimStart().startsWith("word"))
        }
        // And still no words lost.
        assertEquals(
            200,
            segments.joinToString(" ") { it.text }
                .split(Regex("\\s+"))
                .count { it.startsWith("word") },
        )
    }

    @Test
    fun `Arabic commas and semicolons do not split a sentence`() {
        val segments = split("زرعت العائلات هذه المدرجات لأجيال، وسقتها من العين؛ ثم تغيّر الطريق.")
        assertEquals(1, segments.size)
    }

    @Test
    fun `runs of whitespace are collapsed`() {
        val segments = split("First   sentence    here.\n\n\nSecond    sentence there.")
        segments.forEach { segment ->
            assertTrue("double space survived", !segment.text.contains("  "))
        }
    }
}
