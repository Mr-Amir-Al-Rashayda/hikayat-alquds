/**
 * Exports the reviewed Hikayat AlQuds content into the JSON files the Android
 * app bundles as offline assets.
 *
 * The web app, the API and the app must all show the same Jerusalem. Rather
 * than retyping the content into Kotlin, this reads the existing frontend data
 * modules - which are themselves generated from content/ai-ready/*.md,
 * content/media/media.json and content/quiz/quiz.json - and writes them out as
 * plain JSON.
 *
 * Run:  npm --prefix android/tools run export
 */
import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

import { locations, voiceOptions, artisanHighlights } from "../../frontend/src/locationsData";
import { MOCK_MEDIA, MOCK_QUIZ, MOCK_TIMELINE } from "../../frontend/src/mocks/generatedMockData";
import { MOCK_CONTRIBUTIONS, MOCK_LOCATIONS, MOCK_STORIES } from "../../frontend/src/mocks/mockApiData";
import { sourceCitations } from "../../frontend/src/sourceMetadata";
import { visitorWalkthrough } from "../../frontend/src/visitorWalkthroughs";
import { localizeQuiz, localizeTimeline } from "../../frontend/src/arabicContent";
import { localMedia } from "../../frontend/src/localMedia";

const here = dirname(fileURLToPath(import.meta.url));
const assets = join(here, "..", "app", "src", "main", "assets", "content");

/** Strips the web `/images/...` prefix; the app resolves assets by relative path. */
const asset = (url?: string) => url?.replace(/^\//, "") ?? undefined;

function write(name: string, value: unknown) {
  mkdirSync(assets, { recursive: true });
  const target = join(assets, name);
  writeFileSync(target, JSON.stringify(value, null, 0), "utf8");
  const size = JSON.stringify(value).length;
  console.log(`  ${name.padEnd(22)} ${String(size).padStart(8)} bytes`);
}

// --- locations ---------------------------------------------------------------
// One record per place, carrying both languages plus everything the detail
// screen needs that is not a separate collection.
const locationRecords = MOCK_LOCATIONS.map((api) => {
  const profile = locations.find((item) => item.id === api.id);
  if (!profile) throw new Error(`No profile for location "${api.id}"`);
  return {
    id: api.id,
    name: profile.name,
    arabicName: profile.arabicName,
    city: api.city,
    country: api.country,
    latitude: profile.lat,
    longitude: profile.lng,
    description: profile.summary,
    arabicDescription: profile.arabicSummary,
    culturalImportance: profile.culturalImportance,
    arabicCulturalImportance: profile.arabicCulturalImportance,
    historicalSummary: profile.historicalSummary,
    arabicHistoricalSummary: profile.arabicHistoricalSummary,
    landmarks: profile.landmarks,
    arabicLandmarks: profile.arabicLandmarks,
    storyTitle: profile.storyTitle,
    arabicStoryTitle: profile.arabicStoryTitle,
    story: profile.story,
    arabicStory: profile.arabicStory,
    walkthrough: visitorWalkthrough(api.id, false) ?? profile.story,
    arabicWalkthrough: visitorWalkthrough(api.id, true) ?? profile.arabicStory,
    coverImage: asset(profile.coverImage),
    coverImageCredit: profile.imageCredit,
    coverImageLicense: profile.imageLicense,
    coverImageSourceUrl: profile.imageSourceUrl,
    contentFile: api.contentFile,
    categories: profile.categories,
    interests: profile.interests,
    isPublished: true,
    citations: sourceCitations(api.contentFile, api.id),
  };
});
console.log("Exporting Hikayat AlQuds content ->", assets);
write("locations.json", locationRecords);

// --- stories -----------------------------------------------------------------
write(
  "stories.json",
  MOCK_STORIES.map((story) => {
    const profile = locations.find((item) => item.id === story.locationId);
    return {
      ...story,
      arabicTitle: profile?.arabicStoryTitle,
      arabicSummary: profile?.arabicSummary,
      arabicSimplifiedStory: profile?.arabicStory,
    };
  }),
);

// --- timeline / media / quiz -------------------------------------------------
// Both languages are emitted per row so the app never needs a lookup table.
write(
  "timeline.json",
  MOCK_TIMELINE.map((event) => {
    const arabic = localizeTimeline(event, true);
    return {
      ...event,
      arabicPeriodLabel: arabic.periodLabel,
      arabicDescription: arabic.description,
    };
  }),
);

// A photograph the repository holds is bundled and works with no network.
// One it does not hold keeps its Commons URL, and the app shows a credited
// placeholder rather than a broken frame when there is no connection.
let bundledCount = 0;
write(
  "media.json",
  MOCK_MEDIA.map((item) => {
    const local = asset(localMedia(item) ?? undefined);
    if (local) bundledCount += 1;
    return { ...item, assetPath: local, remoteUrl: item.url, remoteThumbUrl: item.thumbUrl };
  }),
);

write(
  "quiz.json",
  MOCK_QUIZ.map((question) => {
    const arabic = localizeQuiz(question, true);
    return {
      ...question,
      arabicQuestion: arabic.question,
      arabicOptions: arabic.options,
      arabicExplanation: arabic.explanation,
      arabicSourceNote: arabic.sourceNote,
    };
  }),
);

// --- memories / artisans / voices -------------------------------------------
write("memories.json", MOCK_CONTRIBUTIONS);
write(
  "artisans.json",
  artisanHighlights.map((item) => ({
    ...item,
    // Commons URLs need a network; the bundled copy is what ships in the APK.
    imageUrl: asset(item.fallbackImageUrl) ?? item.imageUrl,
    remoteImageUrl: item.imageUrl,
    fallbackImageUrl: asset(item.fallbackImageUrl),
  })),
);
write("voices.json", voiceOptions);

console.log(`\nDone. ${locationRecords.length} locations, ${MOCK_TIMELINE.length} timeline events, ` +
  `${MOCK_MEDIA.length} media, ${MOCK_QUIZ.length} questions, ${artisanHighlights.length} artisans.`);
