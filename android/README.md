# Hikayat AlQuds · Android

The Jerusalem archive as a native app. Kotlin, Jetpack Compose, Arabic-first,
and readable with the radio switched off.

The web app and this app are the same product built from the same reviewed
content: the eight quarters, gates and neighbourhoods, their stories,
timelines, photographs, quizzes and contributed memories. Nothing was retyped
for Android — the bundled data is generated from the same sources the website
and the API read.

---

## Quick start

```bash
cd android
./gradlew installDebug        # a connected device or emulator
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
```

There is nothing else to set up. No API key, no backend, no database server:
the whole reviewed archive ships inside the APK. If you want live data too,
see [Connecting to the API](#connecting-to-the-api).

Requires JDK 17 and Android SDK 36. `local.properties` needs one line:

```properties
sdk.dir=/path/to/Android/Sdk
```

---

## What it does

| | |
|---|---|
| **Read** | Eight Jerusalem places, each with a reviewed story, a walkthrough, a historical timeline, a photograph gallery, local artisans, a quiz and the memories people contributed |
| **Retell** | Any place retold for a student, a visitor, a child, a historian, or in two minutes — always from the reviewed record, never from general knowledge |
| **Ask** | A heritage guide that answers from reviewed content and says when the record does not cover a question |
| **Listen** | Narration through the device's speech engine, with Arabic voices ranked best-first, sentence highlighting, and a real pause |
| **Walk** | A tour planner: language, time available and interests in, a numbered walking route out, with walking times, distances, compass bearings and live directions per leg |
| **Find** | A drawn, to-scale map of the Old City walls, the four named gates and the eight places, with a scale bar, pinch-zoom, category filters and your route on it |
| **Connect** | The archive as a web of shared themes and attached memories |
| **Contribute** | A memory submitted for editorial review, with a reference code to check on it later — queued on the device if there is no connection, never faked |
| **Keep** | "My Jerusalem Story": a keepsake assembled from your own progress, shareable, and savable to wherever you choose through the system file picker |

Plus: full Arabic/English interface with correct RTL, light and dark themes,
search across the whole archive, bookmarks, a daily story notification, and
deep links (`hikayat://locations/silwan`).

---

## How it is built

```
android/
├── app/src/main/
│   ├── assets/content/         generated JSON: the reviewed archive
│   ├── assets/images/          34 bundled photographs
│   ├── res/font/               Thmanyah faces, converted from the web .woff2
│   └── java/ps/hikayatalquds/
│       ├── core/designsystem/  brand palette, typography, shared components
│       ├── core/ui/            the in-app language and RTL plumbing
│       ├── data/
│       │   ├── asset/          reads the bundled JSON
│       │   ├── local/          Room: entities, DAOs, the database
│       │   ├── remote/         Retrofit client for the optional API
│       │   ├── mapper/         JSON ↔ Room ↔ domain
│       │   ├── preferences/    DataStore: settings and the private journey
│       │   ├── repository/     the app's single source of truth
│       │   └── sync/           WorkManager: refresh, and the daily story
│       ├── domain/
│       │   ├── model/          immutable content models, bilingual by type
│       │   ├── narrative/      the on-device retelling and the grounded guide
│       │   └── tour/           the walking-route planner
│       ├── narration/          text-to-speech with sentence tracking
│       └── ui/                 one package per screen, ViewModel + Compose
└── tools/                      asset generation and a build guard
```

**Room is the single source of truth.** Both inputs — the JSON bundled in the
APK and, when one is configured, the API — are written into it, and every
screen reads from it. That is what makes the app behave identically on a plane
and on a good connection, and why no screen has to ask "am I online?" to know
what to draw.

**Bilingual by type, not by resource qualifier.** Reviewed content carries its
Arabic and its English together (`LocalizedText`), because they are one
reviewed record rather than a translation pair. Only the app's own chrome lives
in `strings.xml` / `values-ar/strings.xml`.

**Single module, strictly layered.** `domain` knows nothing about Android,
`data` knows nothing about Compose, `ui` talks to repositories through
ViewModels. One app of this size does not earn the build complexity of ten
Gradle modules; the layering is what keeps it honest.

### Versions

Pinned to the last AGP 8 line on purpose. AGP 9 turns on its new DSL and
built-in Kotlin by default, which the Hilt and KSP plugins this app relies on
do not yet fully support — moving up should be a deliberate upgrade, not a
drift. Everything is in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

| | |
|---|---|
| Kotlin | 2.2.21 |
| AGP / Gradle | 8.13.2 / 8.14.3 |
| compileSdk / targetSdk / minSdk | 36 / 36 / 24 |
| Compose BOM | 2025.12.01 |
| Hilt · Room · KSP | 2.57.2 · 2.8.2 · 2.2.21-2.0.5 |

---

## Where the data comes from

The bundled archive is **generated**, never hand-maintained:

```bash
# JSON: locations, stories, timeline, media, quiz, memories, artisans
npx --yes esbuild tools/export-assets.ts --bundle --platform=node \
  --format=esm --packages=external --outfile=tools/.export.mjs
node tools/.export.mjs

# Photographs, fonts and the launcher icon
python tools/prepare_media.py
```

`export-assets.ts` imports the web app's own data modules — which are
themselves generated from `content/ai-ready/*.md`, `content/media/media.json`
and `content/quiz/quiz.json` — so a correction to the reviewed content reaches
the website, the API and this app from one place. CI fails if the committed
assets have drifted.

`prepare_media.py` re-encodes the 43 archive photographs for a phone screen
(43.8 MB → 11.4 MB), converts the Thmanyah faces from `.woff2` to TrueType,
and builds the adaptive launcher icon from the official logo.

### What ships offline, and what does not

| | |
|---|---|
| Bundled in the APK | 8 places, 48 timeline entries, 40 quiz questions, 16 artisans, 64 photograph records, **34 of the photographs themselves** |
| Needs a connection | The other 30 photographs (hosted on Wikimedia Commons), newly contributed memories, server-side story generation |

The app says which of these it is showing, every time, in a banner on the home
screen. "Bundled" is not a degraded mode — it is the complete reviewed archive
— but "as shipped" and "live" are different claims and only one is true at a
time.

---

## Connecting to the API

Optional. **Settings → Data and connection → API address.**

```
http://10.0.2.2:3000/api/v1     # Android emulator → your computer's localhost
http://192.168.1.20:3000/api/v1 # a phone on the same network
```

On the emulator, `10.0.2.2` is the host machine. Start the backend from the
repository root as described in the [project README](../README.md), press
**Test connection**, and the banner switches to live.

Reads that fail fall back to the bundled archive. Writes never fall back: a
contribution that cannot be sent is stored on the device and the contributor is
told exactly that. A fake receipt would be worse than an error.

---

## Testing

```bash
./gradlew testDebugUnitTest         # 79 JVM tests, including Robolectric
./gradlew connectedDebugAndroidTest # 14 tests driving the real app on a device
./gradlew lintDebug
python tools/check_nested_comments.py
```

| Suite | What it actually proves |
|---|---|
| `ItineraryPlannerTest` | Routes have the right number of stops, no repeats, no backtracking, plausible walking legs, and the same interest ordering as the web planner |
| `OnDeviceNarratorTest` | **No retelling introduces a sentence or a year that is not in the reviewed record**, and a question the record does not answer comes back as a refusal |
| `BundledArchiveTest` | The shipped assets parse, load through the real Room schema with both languages intact, every quiz answer cites a source, every photograph carries a credit, every declared offline photograph is really in the APK, and a pending memory can never leak into a readable list |
| `NarrationSegmentationTest` | Sentence splitting handles Arabic punctuation, loses no words, and never breaks mid-word |
| `ConstellationGraphTest` | Every line on the connections graph is a real relationship; nothing is drawn for visual balance |
| `MapGeometryTest` | The map is to scale - a north-south kilometre draws as long as an east-west one - the walled city keeps its real proportions, north is up, every gate sits *on* the wall, and the circuit is the size of the real one |
| `ArchiveJourneyTest` | The whole journey on a device, in Arabic: the eight places load with no network, a place opens, the timeline has dated entries, the quiz cites its source, the planner builds a route |

`app/src/test/resources/robolectric.properties` pins the emulated framework to
API 35: Robolectric's API 36 sandbox needs Java 21, and nothing under test is
API-36-specific.

`ScreenshotTest` writes a PNG of a screen by drawing the view hierarchy into
a software `Canvas`, because `screencap` and `captureToImage` both read the
window surface, which a GPU-less emulator returns as solid black. Two of its
captures are committed under [`store/screenshots/`](store/screenshots).

The instrumented tests target surfaces by `testTag` rather than by Arabic
prose, so a copy change does not break them; the text assertions that remain
are ones where the wording *is* the promise being tested. Each test also clears
the app's data first, because the app deliberately persists a journey and a
saved route that would otherwise leak into the next test.

---

## Release build

```bash
./gradlew assembleRelease      # R8 + resource shrinking: 37 MB debug -> 17 MB
./gradlew bundleRelease        # an .aab for Play
```

Signing is read from `keystore.properties` (never committed) or from the
environment, so CI can sign without a file on disk:

```properties
storeFile=../release.keystore
storePassword=...
keyAlias=hikayat
keyPassword=...
```

```bash
HIKAYAT_KEYSTORE=... HIKAYAT_KEYSTORE_PASSWORD=... \
HIKAYAT_KEY_ALIAS=... HIKAYAT_KEY_PASSWORD=... ./gradlew bundleRelease
```

With neither present the release variant falls back to the debug key, so it can
still be assembled and installed — nothing ships unsigned by accident.

The R8 rules in [`app/proguard-rules.pro`](app/proguard-rules.pro) exist mostly
to protect the data layer: `kotlinx.serialization` resolves serializers
reflectively, and a release build that renamed the archive's fields would
silently empty the app of Jerusalem. CI builds the release variant for exactly
that reason.

---

## Privacy

There are no accounts, and nothing about a reader leaves the device.

Which places you opened, which quizzes you finished, which narrations you heard
and which memories you revealed are kept in DataStore on the phone. Location is
read only after an explicit tap on "check my location", compared against
coordinates the app already has, and never sent anywhere. **Settings → Your
data → Forget my journey** erases all of it.

Permissions, and why each one is there:

| | |
|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE` | Only to reach an optional API and the photographs that are not bundled |
| `ACCESS_COARSE_LOCATION` | Only after tapping "check my location" |
| `POST_NOTIFICATIONS` | Only if the daily story is turned on |

None of them are needed to read the archive.

---

## The rule this app is built around

The generator never invents a historical fact. It simplifies, rearranges and
summarises reviewed content, and nothing else.

That is enforced rather than asserted:

- The on-device narrator only ever emits sentences from the stored reviewed
  record for that place. Its framing sentences are about *reading* the place —
  where to look, what to keep separate — never about what happened there.
- The guide retrieves from the record and declines when the record does not
  cover a question. A refusal is a correct answer here, and it is labelled
  **not in the record** rather than dressed up as one.
- Every generated text states which generator wrote it and what it refused to
  claim, on screen, not in a help page.
- Timeline entries resting on oral or religious tradition are drawn differently
  and labelled as tradition.
- Every quiz answer cites the section of reviewed content it comes from.
- Every photograph carries its photographer and licence everywhere it appears,
  including full-screen. A then-and-now comparison is only offered where the
  archive holds two photographs of the *same subject*.
- No contributed memory is ever displayed before an editor approves it — the
  repository filters by status, not the UI.

---

## Team

اوس حماد · يزيد الحداد · امير الرشايدة · اسماء عبداللطيف · عبير شبانة

HIKAYAT ALQUDS · حكاية القدس · Jerusalem Hackathon, Q GUIDE track
