package ps.hikayatalquds.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import ps.hikayatalquds.BuildConfig
import ps.hikayatalquds.data.local.ContentDao
import ps.hikayatalquds.data.local.GeneratedStoryDao
import ps.hikayatalquds.data.local.HikayatDatabase
import ps.hikayatalquds.data.local.MemoryDao
import ps.hikayatalquds.data.remote.BaseUrlInterceptor
import ps.hikayatalquds.data.remote.HikayatApi
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {
    @Provides
    @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun defaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    /**
     * A scope that outlives any screen, for work the app owes regardless of
     * what the reader is looking at - seeding the archive, flushing a queued
     * contribution. SupervisorJob so one failure does not cancel the rest.
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun providesApplicationScope(@DefaultDispatcher dispatcher: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatcher)
}

@Module
@InstallIn(SingletonComponent::class)
object SerializationModule {
    @Provides
    @Singleton
    fun providesJson(): Json = Json {
        // The app must keep working when a deployment adds a field it has never
        // heard of, so unknown keys are skipped rather than fatal.
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = true
        encodeDefaults = true
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun providesDatabase(@ApplicationContext context: Context): HikayatDatabase =
        Room.databaseBuilder(context, HikayatDatabase::class.java, HikayatDatabase.NAME)
            // Version 1 has no history to migrate from. When version 2 arrives
            // it gets a written migration; the archive is reseedable from the
            // bundled assets either way, so no reader ever loses content.
            .build()

    @Provides fun contentDao(database: HikayatDatabase): ContentDao = database.contentDao()

    @Provides fun memoryDao(database: HikayatDatabase): MemoryDao = database.memoryDao()

    @Provides
    fun generatedStoryDao(database: HikayatDatabase): GeneratedStoryDao =
        database.generatedStoryDao()
}

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {
    @Provides
    @Singleton
    fun providesDataStore(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope) {
        context.preferencesDataStoreFile("hikayat-alquds")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Retrofit needs a base URL when it is built, but the reader can change the
     * API address in Settings at any time. This placeholder is never contacted:
     * [BaseUrlInterceptor] rewrites every request against the configured
     * address, and refuses the request outright when there is none.
     */
    private const val PLACEHOLDER_BASE_URL = "http://localhost/"

    @Provides
    @Singleton
    fun providesOkHttp(baseUrl: BaseUrlInterceptor): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(baseUrl)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC },
                )
            }
        }
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)   // story generation is slower than a read
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Provides
    @Singleton
    fun providesRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(PLACEHOLDER_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun providesApi(retrofit: Retrofit): HikayatApi = retrofit.create(HikayatApi::class.java)
}
