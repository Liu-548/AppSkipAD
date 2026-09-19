# SkipQC — agent guide

Android app that auto-taps "Skip ad" buttons in YouTube and user-picked video apps, using an AccessibilityService. Personal use, shared as APK via GitHub. **Vibe-coded: the owner reviews results only and does not read code.**

Language: code, comments, docs, commits → English. Everything the owner sees (UI strings, your chat replies, docs/REVIEW.md, README.md) → Vietnamese.

## Read order
1. This file (always loaded).
2. `docs/SPEC.md` — requirements with IDs (R-xx). Source of truth. Read only the sections your task needs.
3. `docs/TASKS.md` — do the **first unchecked task only**, then stop and report.
Skip `docs/REVIEW.md` and `README.md` unless the task says to edit them.

## Hard rules (never break; if a task seems to require breaking one, stop and ask)
- H1 No network permission of any kind (INTERNET, ACCESS_NETWORK_STATE…). CI fails the build if one appears.
- H2 Runtime dependencies: none. Android framework + Kotlin stdlib only. Test-only deps allowed: junit, org.json (already in catalog).
- H3 No AndroidX, no Compose. Plain framework Views, `android:Theme.DeviceDefault.DayNight`.
- H4 Never persist or log on-screen text in release builds. The tree dump (R-80) exists in debug builds only (`BuildConfig.DEBUG`).
- H5 Accessibility service must always have an explicit `packageNames` list (watched apps). Never null = all apps.
- H6 Do not add features, screens, settings or permissions not in SPEC. Ambiguity → ask owner one short Vietnamese question.
- H7 Keep `minSdk 26`, `compileSdk/targetSdk 36`, AGP/Gradle versions in `gradle/libs.versions.toml` + wrapper. Bump only if the build is broken, and log why.

## Commands
- Build: `gradlew.bat assembleDebug` (Windows) / `./gradlew assembleDebug`
- Unit tests: `gradlew testDebugUnitTest` — JVM tests for pure logic only (`RuleEngine`, `ModeLogic`)
- Lint: `gradlew lintDebug`
- APK: `app/build/outputs/apk/debug/app-debug.apk`
- Toolchain: JDK 17+ and Android SDK (`ANDROID_HOME` or `local.properties` → `sdk.dir`). If missing, tell the owner exactly what to install; do not install silently. CI (`.github/workflows/build.yml`) is the fallback builder.

## Code map (target state; create files as tasks require)
```
app/src/main/kotlin/com/skipqc/
  Prefs.kt          all persisted state + change notifications (single source of truth)
  ModeLogic.kt      pure state machine for active/auto modes (R-10..R-14), unit-tested
  RuleEngine.kt     pure matching over a NodeView interface (R-02), unit-tested
  Rules.kt          load/parse assets/rules.json (R-50)
  SkipService.kt    AccessibilityService: events → ModeLogic → RuleEngine → click
  SkipTile.kt       Quick Settings tile (R-21)
  StatusNotifier.kt status-bar icon notification (R-20)
  BootReceiver.kt   BOOT_COMPLETED → ModeLogic.onBoot (R-11)
  MainActivity.kt   the only screen (R-30)
  Oem.kt            Realme/Redmi settings shortcuts + health checks (R-40..R-42)
  DebugDump.kt      debug-only tree dump (R-80)
app/src/main/assets/rules.json
app/src/main/res/xml/accessibility_config.xml
app/src/main/res/drawable/ic_logo.xml   one monochrome glyph used everywhere (R-23)
```
Files < 200 lines, one main class per file. Pure logic must not import `android.*` (so JVM tests run).

## Per-task loop
1. Read the task + referenced R-IDs. 2. Implement the minimum. 3. Build + unit tests green. 4. Tick the box in `docs/TASKS.md`, add one line to its Log. 5. Reply to owner in Vietnamese, ≤ 6 lines: what changed + how to test on the phone (point to the matching section of `docs/REVIEW.md`).
