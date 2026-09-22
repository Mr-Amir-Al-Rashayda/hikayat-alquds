package ps.hikayatalquds.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

/**
 * Lists are stored as a newline-joined string rather than JSON.
 *
 * Every list here is a list of human sentences - landmarks, quiz options,
 * uncertainty notes - none of which contain newlines, so the encoding is
 * lossless, greppable in a database inspector, and free of a JSON parse on
 * every row read.
 */
class ListConverter {
    @TypeConverter
    fun fromList(value: List<String>?): String = value?.joinToString(SEPARATOR).orEmpty()

    @TypeConverter
    fun toList(value: String?): List<String> =
        if (value.isNullOrEmpty()) emptyList() else value.split(SEPARATOR)

    private companion object {
        const val SEPARATOR = "\n"
    }
}

@Database(
    entities = [
        LocationEntity::class,
        StoryEntity::class,
        TimelineEventEntity::class,
        MediaEntity::class,
        QuizQuestionEntity::class,
        MemoryEntity::class,
        ArtisanEntity::class,
        GeneratedStoryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(ListConverter::class)
abstract class HikayatDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao
    abstract fun memoryDao(): MemoryDao
    abstract fun generatedStoryDao(): GeneratedStoryDao

    companion object {
        const val NAME = "hikayat-alquds.db"
    }
}
