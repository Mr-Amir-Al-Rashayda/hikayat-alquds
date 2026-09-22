package ps.hikayatalquds.ui.connections

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ps.hikayatalquds.domain.model.AppLanguage
import ps.hikayatalquds.domain.model.LocalizedList
import ps.hikayatalquds.domain.model.LocalizedText
import ps.hikayatalquds.domain.model.Location
import ps.hikayatalquds.domain.model.Memory
import ps.hikayatalquds.domain.model.MemoryStatus

/**
 * The constellation claims that every line on it is a real relationship in the
 * archive. These tests hold it to that: no edge without a shared theme or an
 * attached memory, and no node invented to balance the picture.
 */
class ConstellationGraphTest {

    private fun place(id: String, themes: List<String>) = Location(
        id = id,
        name = LocalizedText(id, "ع-$id"),
        city = "Jerusalem",
        country = "Palestine",
        latitude = 31.78,
        longitude = 35.23,
        description = LocalizedText("about $id", "عن $id"),
        culturalImportance = LocalizedText.Empty,
        historicalSummary = LocalizedText.Empty,
        landmarks = LocalizedList.Empty,
        storyTitle = LocalizedText.Empty,
        story = LocalizedText.Empty,
        walkthrough = LocalizedText.Empty,
        coverImage = null,
        coverImageCredit = null,
        coverImageLicense = null,
        coverImageSourceUrl = null,
        contentFile = null,
        categoryLabels = themes,
        interests = emptyList(),
        citations = emptyList(),
    )

    private fun memory(id: String, locationId: String) = Memory(
        id = id,
        referenceCode = null,
        locationId = locationId,
        title = "Memory $id",
        content = "A remembered thing.",
        contributorName = null,
        status = MemoryStatus.APPROVED,
        submittedAt = "2026-09-18T00:00:00.000Z",
    )

    private val places = listOf(
        place("muslim-quarter", listOf("حارة تاريخية", "سوق تراثي")),
        place("silwan", listOf("حي مقدسي")),
        place("bab-al-amud", listOf("سوق تراثي")),
    )

    private val memories = listOf(
        memory("m1", "silwan"),
        memory("m2", "silwan"),
        memory("m3", "muslim-quarter"),
    )

    private fun graph(language: AppLanguage = AppLanguage.ARABIC) =
        ConnectionsViewModel.buildGraph(places, memories, language)

    @Test
    fun `an empty archive draws nothing rather than an empty ring`() {
        val empty = ConnectionsViewModel.buildGraph(emptyList(), emptyList(), AppLanguage.ARABIC)
        assertTrue(empty.nodes.isEmpty())
        assertTrue(empty.edges.isEmpty())
    }

    @Test
    fun `there is one node per place, per theme and per memory and nothing more`() {
        val result = graph()
        assertEquals(3, result.locationCount)
        assertEquals(3, result.themeCount) // three distinct themes across the places
        assertEquals(3, result.memoryCount)
        assertEquals(9, result.nodes.size)
    }

    @Test
    fun `every edge joins two nodes that exist`() {
        val result = graph()
        result.edges.forEach { edge ->
            assertTrue("dangling edge from ${edge.fromId}", result.byId.containsKey(edge.fromId))
            assertTrue("dangling edge to ${edge.toId}", result.byId.containsKey(edge.toId))
        }
    }

    @Test
    fun `a place is joined to exactly the themes it carries`() {
        val result = graph()
        places.forEach { location ->
            val joined = result.edges
                .filter { it.fromId == "location:${location.id}" && !it.isMemory }
                .map { it.toId.removePrefix("theme:") }
                .toSet()
            assertEquals(location.categoryLabels.toSet(), joined)
        }
    }

    @Test
    fun `a memory hangs off the place it was contributed about and nowhere else`() {
        val result = graph()
        memories.forEach { item ->
            val parents = result.edges
                .filter { it.toId == "memory:${item.id}" }
                .map { it.fromId }
            assertEquals(listOf("location:${item.locationId}"), parents)
        }
    }

    @Test
    fun `no edge is invented between two places`() {
        val result = graph()
        assertFalse(
            "places should only connect through a theme",
            result.edges.any {
                it.fromId.startsWith("location:") && it.toId.startsWith("location:")
            },
        )
    }

    @Test
    fun `a place with no memory has no memory node, so a thin archive looks thin`() {
        val result = graph()
        val orphan = result.nodes.filter { it.kind == NodeKind.MEMORY && it.locationId == "bab-al-amud" }
        assertTrue(orphan.isEmpty())
    }

    @Test
    fun `nodes carry the language they were built for`() {
        val arabic = graph(AppLanguage.ARABIC)
        val english = graph(AppLanguage.ENGLISH)
        val arabicName = arabic.byId["location:silwan"]!!.label
        val englishName = english.byId["location:silwan"]!!.label
        assertEquals("ع-silwan", arabicName)
        assertEquals("silwan", englishName)
    }

    @Test
    fun `a long title is trimmed for the canvas but the full text is kept`() {
        val long = memory("m4", "silwan").copy(
            title = "A remembered afternoon in the terraced gardens below the wall",
        )
        val result = ConnectionsViewModel.buildGraph(places, listOf(long), AppLanguage.ENGLISH)
        val node = result.byId["memory:m4"]!!
        assertTrue("label should be trimmed", node.shortLabel.length <= 18)
        assertTrue("full title should survive", node.label.length > node.shortLabel.length)
    }

    @Test
    fun `memories around one place are fanned apart rather than stacked`() {
        val result = graph()
        val silwanMemories = result.nodes
            .filter { it.kind == NodeKind.MEMORY && it.locationId == "silwan" }
            .map { it.angle }
        assertEquals(2, silwanMemories.size)
        assertTrue("two memories share an angle", silwanMemories[0] != silwanMemories[1])
    }

    @Test
    fun `a memory node knows which place to open`() {
        val result = graph()
        assertEquals("silwan", result.byId["memory:m1"]!!.locationId)
        // A theme belongs to several places, so it opens none of them.
        assertEquals(null, result.byId["theme:سوق تراثي"]!!.locationId)
    }
}
