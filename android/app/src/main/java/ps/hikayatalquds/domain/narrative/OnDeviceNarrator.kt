package ps.hikayatalquds.domain.narrative

import ps.hikayatalquds.domain.model.AppLanguage
import ps.hikayatalquds.domain.model.AudienceMode
import ps.hikayatalquds.domain.model.GeneratedStory
import ps.hikayatalquds.domain.model.GenerationOrigin
import ps.hikayatalquds.domain.model.GuideAnswer
import ps.hikayatalquds.domain.model.Location
import ps.hikayatalquds.domain.model.StoryTone
import ps.hikayatalquds.domain.model.TimelineEvent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retells the reviewed record for a chosen audience, entirely on the device.
 *
 * This is the app's answer to "what happens with no connection", and it is
 * bound by the same rule as the rest of the project: **it never invents a
 * historical fact**. Every sentence it emits is a sentence from the stored
 * reviewed record for that place, rearranged and framed for a student, a
 * child, a historian or someone with two minutes. The framing sentences it
 * adds of its own are about *reading* the place - where to look, what to keep
 * separate - never about what happened there.
 *
 * The guide works the same way: it retrieves from the record and, when the
 * record does not cover the question, says so instead of guessing. A refusal
 * is a correct answer here, not a failure.
 */
@Singleton
class OnDeviceNarrator @Inject constructor() {

    fun generate(
        location: Location,
        audience: AudienceMode,
        tone: StoryTone,
        language: AppLanguage,
    ): GeneratedStory {
        val arabic = language.isArabic
        val name = location.name[language]
        val summary = location.description[language]
        val history = location.historicalSummary[language]
        val importance = location.culturalImportance[language]
        val walkthrough = location.walkthrough[language].ifBlank { location.story[language] }
        val landmarks = location.landmarks[language]

        val narrative = when (audience) {
            AudienceMode.STUDENT -> if (arabic) {
                buildSection(
                    "مدخل" to summary,
                    "التسلسل التاريخي والسبب والنتيجة" to history,
                    "قراءة المكان" to walkthrough,
                    "خلاصة التعلّم" to
                        "اربط بين ما تراه وبين ${landmarks.joinToString("، ")}، وميّز بين التاريخ الموثق والذاكرة الشفوية.",
                )
            } else {
                buildSection(
                    "Introduction" to summary,
                    "Historical cause and sequence" to history,
                    "Reading the place" to walkthrough,
                    "Learning takeaway" to
                        "Connect what you see with ${landmarks.joinToString(", ")}, and keep documented history distinct from attributed oral memory.",
                )
            }

            AudienceMode.CHILD -> if (arabic) {
                "هيا نكتشف $name. $summary\n\n" +
                    "تخيّل ملمس الحجر تحت يدك وأصوات الخطوات في الزقاق. $importance\n\n" +
                    "ابحث بعينيك عن ${landmarks.take(3).joinToString("، و")}. تحرّك بهدوء، فهذا المكان بيتٌ لأناس يعيشون فيه كل يوم."
            } else {
                "Let us explore $name. $summary\n\n" +
                    "Imagine the cool stone beside your hand and the footsteps moving through the lane. $importance\n\n" +
                    "Can you spot ${landmarks.take(3).joinToString(", and ")}? Walk gently and remember that this heritage place is also someone's everyday neighbourhood."
            }

            AudienceMode.HISTORIAN -> if (arabic) {
                buildSection(
                    "الإطار الزمني" to history,
                    "الأدلة المكانية والعمرانية" to walkthrough,
                    "السياق الاجتماعي والثقافي" to importance,
                    "حدود السجل" to
                        "هذه صياغة استخراجية من السجل المراجع؛ لا تستكمل الفجوات بادعاءات غير موثقة.",
                )
            } else {
                buildSection(
                    "Chronology" to history,
                    "Spatial and architectural evidence" to walkthrough,
                    "Social and cultural context" to importance,
                    "Limits of the record" to
                        "This extractive account uses only the reviewed place record and does not fill evidentiary gaps.",
                )
            }

            // Five sentences, taken whole from the record - never a paraphrase,
            // because a paraphrase is where an invented fact would slip in.
            AudienceMode.SHORT -> listOf(summary, history, importance)
                .flatMap(::sentences)
                .take(5)
                .joinToString(" ")

            // A visitor wants orientation, not a lecture: what the place is, how
            // to walk it, why it matters. The same shape reads correctly in both
            // languages, so there is nothing to branch on.
            AudienceMode.TOURIST -> "$summary\n\n$walkthrough\n\n$importance"

            AudienceMode.GENERAL -> "${location.story[language]}\n\n$walkthrough"
        }.trim()

        return GeneratedStory(
            title = location.storyTitle[language].ifBlank { name },
            summary = summary,
            narrative = narrative,
            audience = audience,
            tone = tone,
            language = language,
            wordCount = narrative.split(Regex("\\s+")).count { it.isNotBlank() },
            uncertaintyNotes = listOf(
                if (arabic) {
                    "استجابة استخراجية على الجهاز؛ لم تُضف أي حقائق خارج سجل المكان المراجع."
                } else {
                    "Offline extractive response: no facts were added beyond the stored reviewed location record."
                },
            ),
            origin = GenerationOrigin.ON_DEVICE,
            sourceFile = location.contentFile,
            locationId = location.id,
        )
    }

    /**
     * Answers from the reviewed record by retrieval, or declines.
     *
     * Passages are scored on how many of the question's terms they contain,
     * normalised for Arabic spelling. Below [MINIMUM_RELEVANCE] the guide says
     * the record does not cover the question - which is the honest answer, and
     * the one the whole project is built around.
     */
    fun answer(
        location: Location,
        timeline: List<TimelineEvent>,
        question: String,
        language: AppLanguage,
    ): GuideAnswer {
        val arabic = language.isArabic
        val terms = question.searchTerms()
        if (terms.isEmpty()) return decline(question, arabic)

        val passages = buildList {
            add(location.description[language])
            add(location.historicalSummary[language])
            add(location.culturalImportance[language])
            addAll(sentences(location.story[language]))
            addAll(sentences(location.walkthrough[language]))
            if (location.landmarks[language].isNotEmpty()) {
                add(
                    if (arabic) {
                        "يمكنك تتبّع: ${location.landmarks[language].joinToString("، ")}."
                    } else {
                        "You can follow: ${location.landmarks[language].joinToString(", ")}."
                    },
                )
            }
            timeline.forEach { event ->
                add("${event.periodLabel[language]} - ${event.description[language]}")
            }
        }.filter { it.isNotBlank() }.distinct()

        val scored = passages
            .map { passage -> passage to passage.relevance(terms) }
            .filter { (_, score) -> score >= MINIMUM_RELEVANCE }
            .sortedByDescending { (_, score) -> score }

        if (scored.isEmpty()) return decline(question, arabic)

        val chosen = scored.take(3).map { it.first }
        return GuideAnswer(
            answer = chosen.joinToString(" "),
            answeredFromSource = true,
            excerpts = chosen,
            uncertaintyNotes = listOf(
                if (arabic) {
                    "أُجيب من السجل المراجع لهذا المكان على الجهاز، دون إضافة أي معلومة من خارجه."
                } else {
                    "Answered on the device from this location's reviewed record, with nothing added from outside it."
                },
            ),
            origin = GenerationOrigin.ON_DEVICE,
        )
    }

    private fun decline(question: String, arabic: Boolean) = GuideAnswer(
        answer = if (arabic) {
            "لا تغطي السجلات المراجعة هذا السؤال بصيغته الحالية، ولن أخمّن. جرّب السؤال عن التاريخ أو المعالم أو الأهمية الثقافية لهذا المكان."
        } else {
            "The reviewed records do not cover this question as asked, and I will not guess. Try asking about this place's history, its landmarks, or its cultural importance."
        },
        answeredFromSource = false,
        excerpts = emptyList(),
        uncertaintyNotes = listOf(
            if (arabic) {
                "لم يُجب الوضع دون اتصال عن: «${question.trim()}»."
            } else {
                "Offline: \"${question.trim()}\" was not answered because the record does not cover it."
            },
        ),
        origin = GenerationOrigin.ON_DEVICE,
    )

    private fun buildSection(vararg parts: Pair<String, String>): String = parts
        .filter { (_, body) -> body.isNotBlank() }
        .joinToString("\n\n") { (heading, body) -> "$heading\n$body" }

    companion object {
        /** At least this share of the question's terms must appear in a passage. */
        const val MINIMUM_RELEVANCE = 0.34

        /** Words too common to say anything about what is being asked. */
        private val STOP_WORDS = setOf(
            "what", "when", "where", "which", "who", "why", "how", "the", "and", "for", "are",
            "was", "were", "this", "that", "with", "from", "into", "about", "does", "did", "can",
            "tell", "there", "here", "have", "has", "you", "your", "its", "his", "her", "they",
            "في", "من", "على", "عن", "الى", "إلى", "هذا", "هذه", "ذلك", "التي", "الذي", "ما",
            "ماذا", "متى", "اين", "أين", "كيف", "لماذا", "هل", "هو", "هي", "و", "او", "أو",
            "كان", "كانت", "مع", "بين", "قد", "ثم", "كل",
        )

        internal fun String.searchTerms(): List<String> = normalise()
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length > 2 && it !in STOP_WORDS }
            .distinct()

        /** Share of the question's terms that appear in this passage. */
        internal fun String.relevance(terms: List<String>): Double {
            if (terms.isEmpty()) return 0.0
            val haystack = normalise()
            return terms.count { haystack.contains(it) }.toDouble() / terms.size
        }

        /**
         * Arabic is written with and without diacritics and with several alef
         * and ya forms, so a question typed one way must still match a record
         * written the other.
         */
        internal fun String.normalise(): String = lowercase()
            .replace(Regex("[\\u064B-\\u0652\\u0640]"), "")
            .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
            .replace('ى', 'ي').replace('ة', 'ه')

        internal fun sentences(text: String): List<String> = text
            .split(Regex("(?<=[.!?؟])\\s+"))
            .map(String::trim)
            .filter { it.isNotBlank() }
    }
}
