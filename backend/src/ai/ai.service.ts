import { HttpService } from '@nestjs/axios';
import {
  Injectable,
  Logger,
  ServiceUnavailableException,
} from '@nestjs/common';
import { firstValueFrom } from 'rxjs';
import { AskGuideDto } from './dto/ask-guide.dto';
import { GenerateStoryDto } from './dto/generate-story.dto';
import {
  AiGuideResponse,
  AiStoryResponse,
  GeneratedStory,
  GuideAnswer,
} from './interfaces/generated-story.interface';
import { Location } from '../database/entities/location.entity';
import { LocationsService } from '../locations/locations.service';
import { StoriesService } from '../stories/stories.service';

/**
 * AiService
 *
 * Client for the AI module in ai/ (see ai/api-contract.md). Responsibilities:
 *
 *  1. resolve the location so the response carries real provenance;
 *  2. call the AI service over HTTP;
 *  3. fall back to a local simplifier if that service is unreachable, so the
 *     user still gets a narrative;
 *  4. store the result as a story with status `pending_review`.
 *
 * Generated stories are never published automatically. They land in the review
 * queue, which is the same rule the content workflow applies to everything else.
 */
@Injectable()
export class AiService {
  private readonly logger = new Logger(AiService.name);

  constructor(
    private readonly httpService: HttpService,
    private readonly locationsService: LocationsService,
    private readonly storiesService: StoriesService,
  ) {}

  async generateStory(dto: GenerateStoryDto): Promise<GeneratedStory> {
    const location = dto.locationId
      ? await this.locationsService.findOne(dto.locationId)
      : null;

    const audience = dto.targetAudience ?? 'general';
    const tone = dto.tone ?? 'storytelling';
    const language = dto.language ?? 'en';

    let story: GeneratedStory;
    try {
      const { data } = await firstValueFrom(
        this.httpService.post<AiStoryResponse>('/generate-story', {
          location_id: dto.locationId,
          location_name: location?.name,
          historical_text: dto.historicalText,
          target_audience: audience,
          language,
          tone,
          max_words: dto.maxWords,
          is_reviewed: true,
        }),
      );

      if (data.status === 'error' || !data.narrative) {
        throw new Error(
          data.error?.message ?? 'AI service returned no narrative',
        );
      }
      story = this.toGeneratedStory(data);
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      this.logger.warn(
        `AI service unavailable (${message}) - using the local fallback simplifier.`,
      );
      if (!location) {
        // Without a known location there is nothing local to fall back to.
        throw new ServiceUnavailableException(
          'The AI service is unavailable and no location was given to fall back on.',
        );
      }
      story = this.localFallback(location, audience, language, tone);
    }

    if (dto.persist !== false && location) {
      story.storyId = await this.persist(story, location);
    }
    return story;
  }

  async askGuide(dto: AskGuideDto): Promise<GuideAnswer> {
    try {
      const { data } = await firstValueFrom(
        this.httpService.post<AiGuideResponse>('/ask-guide', {
          question: dto.question,
          location_id: dto.locationId,
          language: dto.language ?? 'en',
          // Without this a follow-up like "and when?" has no keywords of its
          // own and the guide correctly, but uselessly, refuses to answer.
          history: dto.history ?? [],
        }),
      );
      return {
        answer: data.answer,
        answeredFromSource: data.answered_from_source,
        excerpts: data.excerpts ?? [],
        uncertaintyNotes: data.uncertainty_notes ?? [],
        generatedBy: data.model?.provider ?? 'unknown',
      };
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      this.logger.warn(`AI service unavailable for ask-guide (${message}).`);
      return {
        answer:
          'The heritage guide is temporarily unavailable. The reviewed content for this location is still readable on the location page.',
        answeredFromSource: false,
        excerpts: [],
        uncertaintyNotes: ['The AI service could not be reached.'],
        generatedBy: 'backend-fallback',
      };
    }
  }

  private toGeneratedStory(data: AiStoryResponse): GeneratedStory {
    return {
      title: data.title,
      summary: data.summary,
      narrative: data.narrative,
      targetAudience: data.target_audience,
      language: data.language,
      tone: data.tone,
      wordCount: data.word_count,
      uncertaintyNotes: data.uncertainty_notes ?? [],
      warnings: data.warnings ?? [],
      source: {
        locationId: data.source?.location_id,
        locationName: data.source?.location_name,
        contentFile: data.source?.content_file,
        summaryFile: data.source?.summary_file,
      },
      generatedBy: data.model?.provider ?? 'unknown',
    };
  }

  /**
   * Last-resort narrative built from the seeded location description alone.
   *
   * It adds no facts - it only reuses the description already stored for the
   * location - and it says plainly in the uncertainty notes that the AI service
   * was not reached, so nobody mistakes it for a generated story.
   */
  private localFallback(
    location: Location,
    audience: string,
    language: string,
    tone: string,
  ): GeneratedStory {
    const description = location.description ?? '';
    const narrative = [
      `${location.name}${location.arabicName ? ` (${location.arabicName})` : ''}`,
      '',
      description,
      '',
      'This fallback uses the stored, reviewed Palestinian heritage record for this location; the bibliography is shown in the source panel below.',
    ].join('\n');

    return {
      title: location.name,
      summary: description.split('. ')[0] ?? location.name,
      narrative,
      targetAudience: audience,
      language,
      tone,
      wordCount: narrative.split(/\s+/).filter(Boolean).length,
      uncertaintyNotes: [
        'The AI service could not be reached, so this text is the stored location description rather than a generated narrative.',
      ],
      warnings: [],
      source: {
        locationId: location.id,
        locationName: location.name,
        contentFile: location.contentFile,
        summaryFile: location.aiSummaryFile,
      },
      generatedBy: 'backend-fallback',
    };
  }

  private async persist(
    story: GeneratedStory,
    location: Location,
  ): Promise<string | undefined> {
    try {
      const saved = await this.storiesService.create({
        locationId: location.id,
        authorId: null,
        title: story.title,
        summary: story.summary,
        originalContent: story.source.summaryFile ?? location.aiSummaryFile,
        simplifiedStory: story.narrative,
        audience: story.targetAudience,
        language: story.language,
        tone: story.tone,
        source: location.contentFile,
        isAiGenerated: true,
        uncertaintyNotes: story.uncertaintyNotes,
        // Generated stories go to the review queue, never straight to published.
        status: 'pending_review',
      });
      return saved.id;
    } catch (error) {
      // Storing is a bonus; failing to store must not fail the request.
      const message = error instanceof Error ? error.message : String(error);
      this.logger.warn(`Could not store the generated story: ${message}`);
      return undefined;
    }
  }
}
