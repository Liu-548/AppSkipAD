# Tasks — do the first unchecked item only, then report

Each task ends with: build green + unit tests green + owner check in `docs/REVIEW.md` (same M-number).

## M0 Build baseline
- [ ] M0.1 Verify scaffold builds as-is (`assembleDebug`, `lintDebug`). Versions were set from AGP 9.3 release notes (AGP 9.3.3, Gradle 9.5.0, JDK 17, built-in Kotlin) but never compiled. Fix only what breaks; log changes. Ensure `gradlew` has the exec bit in git (`git update-index --chmod=+x gradlew`).
- [ ] M0.2 Push; confirm GitHub Actions is green and the permission check step passes.

## M1 Service + debug dump (goal: learn the real skip-button IDs)
- [ ] M1.1 `SkipService` + `accessibility_config.xml` + manifest entries (R-04, R-60..R-63). Watched list hardcoded to YouTube for now. No clicking yet.
- [ ] M1.2 `DebugDump.kt` + temporary debug button in MainActivity (R-80).
- [ ] M1.3 STOP: ask owner to install, play a YouTube ad, press dump when "Skip"/"Bỏ qua" is visible, and send the file. Update `rules.json` from the dump, set `verified: true`.

## M2 Skipping
- [ ] M2.1 `Rules.kt` + `RuleEngine.kt` over a `NodeView` interface (R-02, R-50). Unit tests: viewId hit, exact text hit, substring rejected, disabled/invisible rejected, clickable-ancestor walk.
- [ ] M2.2 Wire into `SkipService` with cooldown/debounce (R-01, R-03, R-70, R-71). Active hardcoded true for this task.

## M3 Modes
- [ ] M3.1 `Prefs.kt` + `ModeLogic.kt` (R-10..R-14) with unit tests for every R-12 bullet.
- [ ] M3.2 `BootReceiver` (R-11); service uses ModeLogic + idle handler (R-72); watched list from Prefs, live `packageNames` update (R-04, R-05).

## M4 Tile, status icon, logo
- [ ] M4.1 `ic_logo.xml` + adaptive launcher icon (R-23).
- [ ] M4.2 `SkipTile` (R-21) and `StatusNotifier` (R-20), both driven by `Prefs.setActive`.

## M5 Main screen
- [ ] M5.1 `Oem.kt` health checks + shortcuts (R-40..R-43).
- [ ] M5.2 Final `MainActivity` layout (R-30, R-31, R-22). Remove the temporary M1 button (debug dump stays, debug-only).

## M6 Release
- [ ] M6.1 Release build config check (R-73); README install section verified against the real UI.
- [ ] M6.2 Tag `v0.1.0`; CI attaches release APK to the GitHub Release.

## Log
<!-- one line per finished task: date · task · note -->
