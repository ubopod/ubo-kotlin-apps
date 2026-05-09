# ubo-kotlin-apps

Android phone (and forthcoming WearOS) applications built on top of
`../ubo-kotlin-grpc/`. Mirrors the layout and feature set of
`../ubo-swift-app/`.

## Layout

```
ubo-kotlin-apps/
├── settings.gradle.kts          # re-projects ../ubo-kotlin-grpc/{lib,protos}
├── build.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml
├── gradlew, gradlew.bat
├── gradle/wrapper/gradle-wrapper.{jar,properties}
└── phone-app/                   # Android phone app (Material 3 + Compose)
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/{values,drawable,mipmap-anydpi-v26}/
        └── kotlin/com/ubopod/uboapp/phone/
            ├── UboPhoneApplication.kt
            ├── MainActivity.kt
            ├── storage/Settings.kt        # DataStore preferences
            ├── viewmodel/DeviceViewModel.kt
            └── ui/
                ├── ContentScreen.kt        # top-level routing
                ├── onboarding/OnboardingScreen.kt
                ├── connection/ConnectionScreen.kt
                └── device/
                    ├── DeviceScreen.kt
                    ├── HomeViewRenderer.kt
                    └── MenuViewRenderer.kt
```

## Build / run

Same JDK + Android SDK requirements as `../ubo-kotlin-grpc/`.

```bash
# 1. Sync proto sources from the parent ubo_app repo (only once, or after
#    proto changes).
../ubo-kotlin-grpc/generate-protos.sh

# 2. Assemble a debug APK.
./gradlew :phone-app:assembleDebug

# 3. Install on a connected device or emulator.
./gradlew :phone-app:installDebug
adb shell am start -n com.ubopod.uboapp.phone/.MainActivity
```

To run against a Ubo gRPC server on your dev machine from the Android
emulator, enter the host `10.0.2.2` (the emulator's alias for the host
loopback) and the default port `50051`. From a real device on the same
LAN, use the device's IP address.

## Why this build re-projects `:lib` and `:protos`

`settings.gradle.kts` here uses Gradle's `project(...).projectDir = ...`
to include the sibling `:lib` and `:protos` modules from
`../ubo-kotlin-grpc/`. That means:

- The phone-app depends on `project(":lib")` directly — no Maven publish
  or composite-build dependency-substitution dance.
- Both Gradle settings (`ubo-kotlin-grpc/settings.gradle.kts` and this
  one) can coexist; you can build `:lib` standalone from
  `../ubo-kotlin-grpc/` for tests, or build the phone-app from here
  which transitively builds `:lib` + `:protos`.
- The two `gradle/libs.versions.toml` files share entries for
  Kotlin/AGP/protobuf/grpc/coroutines/JUnit/Truth so the included
  `:lib` build script resolves cleanly.

## What's in v1

The phone-app currently:

- Walks the user through a 4-page onboarding carousel.
- Persists the last-connected host/port via DataStore.
- Discovers Ubo devices via mDNS (`_uborpc._tcp.`) using
  `UboDiscovery.browse(context)` and lets the user tap a row to connect.
- Streams `current_view` and `status_bar` over gRPC and dispatches
  taps as `selectMenuItem(label = …)` via `UboClient`.
- Renders all seven `ViewData` subtypes:
  - **Home** — 2-column card grid with CPU/RAM/volume gauges.
  - **Menu** — vertical list with optional heading / subheading /
    pagination indicator.
  - **Notification** — large icon + title + content + extra-info card +
    action items + Dismiss button.
  - **Application** — placeholder + applicationId + extraData table +
    Back/Home shortcuts.
  - **Instruction** — title + body + optional spinner / countdown /
    progress text / footer.
  - **Prompt** — icon + title + body + one button per item (or Dismiss).
  - **Render** — six sub-kinds:
    - `qr_code` — ZXing-generated QR bitmap (`props["data"]`).
    - `qr_code_carousel` — `HorizontalPager` of QR codes (`props["payloads"]`).
    - `text_viewer` — scrollable, selectable monospace block.
    - `image_viewer` — `BitmapFactory`-decoded PNG/JPEG.
    - `status` — icon + title + status text.
    - `frame_stream` — placeholder (decoder lands once
      `UboConnection.subscribeToFrameStream` ships).
- Renders inline BBCode markup (`[b]`, `[i]`, `[u]`, `[color=#hex]`)
  via `markupAnnotated`.
- Renders icons via `IconView`, which routes Nerd-Font Unicode
  Private-Use codepoints through the bundled `arimo_nerd.ttf` and falls
  back to Material Icons via `SymbolMapper` for semantic keys.
- When connected, presents a `Scaffold` with a `NavigationBar` that
  swaps between **Dashboard** (CPU / RAM gauges, temperature, clock,
  quick-action grid for mute / blank-display / LEDs / reboot / power)
  and **Device** (the seven-way `ViewData` dispatch).
- Shows a Material 3 `ModalBottomSheet` (`InputFormSheet`) whenever
  `client.activeInputs` is non-empty. Each `InputFieldType` maps to a
  native control: `text` / `password` / `long` / `number` /
  `checkbox` / `select` / `color` / `date` / `time` / `file`. Submit
  dispatches `client.provideInput`; cancel (or swipe-down) dispatches
  `client.cancelInput`.
- Hardware services owned by `DeviceViewModel`:
  - `CameraService` (CameraX, ImageAnalysis-only) — captures camera
    frames at 640 × 480, encodes YUV → NV21 → JPEG, dispatches each as
    a `CameraReportImageEvent` via the new `client.sendCameraFrame(...)`
    helper. Manual `start()` / `stop()` for now; auto-trigger from
    `client.isCameraViewfinderActive` lights up once the
    SubscribeEvent decoder lands.
  - `MicCaptureService` (AudioRecord) — 16 kHz mono PCM16 capture in a
    coroutine loop, streamed via `client.reportAudioSample`. Wired to
    a Dashboard "Push to talk" button with runtime `RECORD_AUDIO`
    permission flow.
  - `AudioPlaybackService` (AudioTrack) — `play(sample, volume)` API
    ready; auto-subscribe to `client.playbackEvents()` lands with the
    SubscribeEvent decoder.

## `wear-app` (WearOS)

The `:wear-app` module ships a slim WearOS application against the same
`UboClient` from `:lib`. Layout:

```
wear-app/src/main/
├── AndroidManifest.xml          # uses-feature watch, uses-library wearable
├── res/{values,drawable,mipmap-anydpi-v26}
└── kotlin/com/ubopod/uboapp/wear/
    ├── UboWearApplication.kt
    ├── MainActivity.kt           # UboWearTheme + WatchContentScreen
    ├── viewmodel/DeviceViewModel.kt   # slim — no camera/mic/audio
    ├── storage/Settings.kt            # DataStore (separate file from phone)
    └── ui/
        ├── WatchContentScreen.kt
        ├── connection/WatchConnectionScreen.kt
        └── device/
            ├── WatchDeviceScreen.kt
            ├── WatchHomeRenderer.kt
            ├── WatchMenuRenderer.kt
            └── WatchExtraRenderers.kt   # notification/application/instruction/prompt/render
```

What v1 of the watch app does:

- Routes between connection / connecting-spinner / connected based on
  `connectionState`; no onboarding (the watch goes straight to the
  connection screen).
- Manual host/port entry via `BasicTextField`s; auto-attempts the saved
  host on first launch.
- Renders all seven `ViewData` subtypes in compact form using Wear
  Compose Material 2 (`ScalingLazyColumn`, `Chip`, `Scaffold` +
  `PositionIndicator`). Items truncate to ~14 chars to fit a watch
  chip; pagination indicator shown when applicable.
- Render-view sub-kinds collapse to text on the watch: QR codes /
  images / frame-streams just show "open the phone app" with the
  payload preview.
- Reuses the same `:lib` gRPC client — `currentView` /
  `connectionState` / `statusBar` / `systemStats` /
  `activeInputs` flow into the watch ViewModel without changes.

What's intentionally absent:

- Camera, microphone, audio playback (no hardware on most watches; the
  Swift `ubo Watch App` makes the same call).
- mDNS discovery — manual entry only on a 1.4″ screen.
- BBCode markup parsing and Nerd-Font icon rendering — kept to plain
  text + Material Icons on the watch.

## `widget-app` (home-screen widget)

The `:widget-app` Android library hosts a Jetpack Glance App Widget.
The phone-app depends on `:widget-app` and registers the receiver in
its own manifest:

```
widget-app/src/main/
├── AndroidManifest.xml
├── res/xml/ubo_widget_info.xml          # appwidget-provider metadata
└── kotlin/com/ubopod/uboapp/widget/
    ├── SharedSystemStats.kt              # phone-app → widget snapshot
    ├── WidgetDataStore.kt                # process-safe DataStore
    ├── UboGlanceAppWidget.kt             # Glance composable
    ├── UboWidgetReceiver.kt              # GlanceAppWidgetReceiver subclass
    └── UboWidgetRefreshWorker.kt         # WorkManager periodic refresh
```

Behaviour:

- The phone-app's `DeviceViewModel` collects `client.systemStats` and
  pushes a [SharedSystemStats] into [WidgetDataStore] every ≥ 5 s
  (matches the Swift port's `lastWidgetUpdate` throttle), then calls
  `UboWidgetRefreshWorker.refreshNow(context)` to nudge the launcher.
- `UboWidgetRefreshWorker.enqueue` registers a 30-min periodic worker
  so the widget keeps refreshing even when the phone-app process is
  gone (Android may throttle this).
- The Glance composable shows: a status dot (green = connected, red =
  disconnected) + device host + `Connected` / `Disconnected` label,
  CPU / RAM percent tiles, and an optional temperature chip. When the
  snapshot is ≥ 5 minutes old a `stale — phone may be offline`
  caption appears.
- Single responsive size for now (3 × 2 cells, resizable). Adding a
  small/medium/large split or accessory variants is mechanical.

## Live data flow (after step 7.5 decoder pass)

`UboConnection` now exposes four new server-side subscriptions that all
run through the [ReconnectPolicy] retry loop:

- `subscribeToSystemStats(): Flow<SystemStats>` — selectors
  `["state.system", "state.sensors"]`, merged across frames into a
  unified [SystemStats]. Feeds `client.systemStats`, the Dashboard
  gauges, and the home-screen widget.
- `subscribeToActiveInputs(): Flow<List<WebUIInputDescription>>` —
  selector `["state.web_ui"]`. Re-emits the current list whenever an
  input demand is added or resolved. Feeds `client.activeInputs`,
  which `ContentScreen` watches to surface the
  [InputFormSheet][ui/inputs/InputFormSheet.kt] modal bottom sheet.
- `subscribeToCameraEvents(): Flow<CameraEvent>` — `SubscribeEvent`
  filtering on `CameraStartViewfinderEvent` /
  `CameraStopViewfinderEvent`. Feeds
  `client.isCameraViewfinderActive` / `cameraPattern`; the phone-app's
  `DeviceViewModel` watches that flow and starts / stops the local
  CameraX pipeline automatically.
- `playbackEvents(): Flow<PlaybackEvent>` — `SubscribeEvent` filtering
  on `AudioPlayAudioSampleEvent` / `AudioPlayAudioSequenceEvent` /
  `AudioStopPlaybackEvent`. The phone-app's `DeviceViewModel`
  collects this and routes each sample through `AudioPlaybackService`
  for AudioTrack playback.

`UboClient.startStatsSubscription` / `startInputsSubscription` /
`startCameraSubscription` are no longer scaffolds — they spawn jobs in
the client's coroutine scope wrapped in `runWithRetry`.

## Roadmap

- Connection-readiness probe inside `UboConnection.connect` matching
  Swift's 10×500 ms readiness check (today the OkHttp transport
  lazy-connects on the first RPC).
- File picker integration in `InputFormSheet`
  (`rememberLauncherForActivityResult` +
  `ActivityResultContracts.GetContent`).
- Action-builder coverage for the long-tail `Ubo.Action` variants
  (camera, docker, file system, IR, settings) — the Swift port has
  the same gaps.
