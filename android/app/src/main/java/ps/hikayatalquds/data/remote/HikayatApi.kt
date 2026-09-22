package ps.hikayatalquds.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** The `/api/v1` surface of the Hikayat AlQuds backend. */
interface HikayatApi {

    @GET("health")
    suspend fun health(): HealthDto

    @GET("locations")
    suspend fun locations(): List<LocationDto>

    @GET("locations/{id}")
    suspend fun location(@Path("id") id: String): LocationDto

    @GET("locations/{id}/media")
    suspend fun media(@Path("id") id: String): List<MediaDto>

    @GET("locations/{id}/before-after")
    suspend fun beforeAfter(@Path("id") id: String): BeforeAfterDto

    @GET("locations/{id}/timeline")
    suspend fun timeline(@Path("id") id: String): List<TimelineEventDto>

    @GET("locations/{id}/quiz")
    suspend fun quiz(@Path("id") id: String): List<QuizQuestionDto>

    @GET("locations/{id}/related")
    suspend fun related(@Path("id") id: String): List<LocationDto>

    @GET("stories/{locationId}")
    suspend fun stories(@Path("locationId") locationId: String): List<StoryDto>

    @GET("stories/featured")
    suspend fun featuredStory(): FeaturedStoryDto

    @GET("stories/random")
    suspend fun randomStory(@Query("exclude") exclude: String? = null): FeaturedStoryDto

    @GET("contributions")
    suspend fun contributions(@Query("locationId") locationId: String? = null): List<ContributionDto>

    @GET("contributions/status/{code}")
    suspend fun contributionStatus(@Path("code") code: String): ContributionStatusDto

    @POST("contributions")
    suspend fun submitContribution(@Body body: NewContributionDto): ContributionReceiptDto

    @POST("ai/generate-story")
    suspend fun generateStory(@Body body: GenerateStoryRequest): GeneratedStoryDto

    @POST("ai/ask-guide")
    suspend fun askGuide(@Body body: AskGuideRequest): GuideAnswerDto
}
