# SkipQC — Specification v1 (Android)

## 0. Scope
In: Android phones (test devices: Realme / realme UI, Redmi / HyperOS). Tap visible "Skip ad" buttons in watched apps.
Out (v1): Windows, TV, popup/redirect ads, ads without a skip button, network ad-blocking, Play Store, in-app rule editor, multiple screens.

## 1. Terms
- **Service enabled**: user turned SkipQC on in system Accessibility settings.
- **Service running**: `SkipService` is connected (can be enabled-but-killed on OEM ROMs).
- **Active**: our flag. When false, the service ignores every event immediately.
- **Watched apps**: package list the service is scoped to (R-04).

## 2. Skipping
- R-01 When a watched app shows a clickable skip button and Active is true, tap it within 500 ms of it becoming clickable.
- R-02 Matching order per event (implemented in pure `RuleEngine`):
  1. App rule `viewIds` via `findAccessibilityNodeInfosByViewId`.
  2. App rule `texts`, then `generic.texts`: candidates from `findAccessibilityNodeInfosByText`, keep only nodes whose trimmed `text` or `contentDescription` **equals** an entry (case-insensitive). Substring matches are rejected (avoids "Skip in 5", "Ads · 5").
  3. Node must be `isVisibleToUser && isEnabled`. Click the node, or its nearest clickable ancestor (max 3 levels up). If none clickable: `dispatchGesture` tap at bounds center.
- R-03 After a click: ignore that package for 1500 ms. Process at most one event per 100 ms per package (debounce).
- R-04 `serviceInfo.packageNames` = watched apps; update at runtime when the list changes. Empty list ⇒ service receives nothing.
- R-05 Watched apps default: `com.google.android.youtube` if installed. User may add any installed launchable app.

## 3. Modes (manual first; the two auto modes are opt-in, default OFF)
State in Prefs: `active: Boolean`, `offReason: MANUAL|AUTO`, `autoOnBoot: Boolean`, `autoWithApps: Boolean`, `watched: Set<String>`.
- R-10 **Manual (primary)**: QS tile or main-screen switch sets `active`; turning off sets `offReason=MANUAL`. Fresh install: `active=false`.
- R-11 **Auto on boot**: on BOOT_COMPLETED, `active = autoOnBoot`, `offReason = AUTO`. So by default the app is OFF after every reboot.
- R-12 **Auto with watched apps** (`autoWithApps=true`):
  - If `active=false` and `offReason=AUTO`: any event from a watched app ⇒ `active=true` (auto).
  - If `offReason=MANUAL`: re-activate only on `TYPE_WINDOW_STATE_CHANGED` from a watched app when ≥ 10 s passed since the last watched-app event (= user re-opened the app). Respects a manual OFF while the user stays in the app.
  - If activated automatically and no watched-app event for 10 min ⇒ `active=false`, `offReason=AUTO` (status icon disappears).
  - Enabling `autoWithApps` sets `offReason=AUTO`.
- R-13 `ModeLogic` is a pure function/state machine: `(state, input, nowMs) -> newState`. Inputs: `ManualSet(on)`, `Boot`, `WatchedEvent(isWindowStateChange)`, `IdleTick`. Unit tests cover every bullet of R-10..R-12.
- R-14 Every `active` change goes through one function (`Prefs.setActive`) which then: updates tile, updates notification, informs service. No other writer.

## 4. Quick Settings tile, status icon, logo
- R-20 **Status-bar icon**: while `active && service running`, show an ongoing, silent, low-importance notification (channel "Trạng thái") with small icon `ic_logo`, text "Đang tự bỏ qua quảng cáo", tap → MainActivity, action "Tắt" → `Prefs.setActive(false, MANUAL)`. Removed when inactive. Needs POST_NOTIFICATIONS (API 33+), asked once from MainActivity; if denied, app still works.
- R-21 **Tile** (`SkipTile`, TileService): icon `ic_logo`, label "SkipQC". State ACTIVE/INACTIVE mirrors `active`; subtitle (API 29+) "Đang bật" / "Đã tắt" / "Chưa cấp quyền". Tap:
  - service not enabled ⇒ open Accessibility settings and collapse panel (API 34+: `startActivityAndCollapse(PendingIntent)`; older: Intent overload);
  - else toggle via `Prefs.setActive(!active, MANUAL)`.
  Refresh with `TileService.requestListeningState` on every change.
- R-22 Main screen button "Thêm nút vào thanh kéo xuống" → `StatusBarManager.requestAddTileService` (API 33+). Hidden below API 33 (show hint text: kéo thanh thông báo → chỉnh sửa → kéo ô SkipQC vào).
- R-23 **Logo**: one vector `ic_logo.xml`, 24dp, single white path on transparent, a "skip-next" glyph (▶| shape). Reused for tile, notification, and launcher (adaptive icon: glyph centered on solid `#D32F2F` background). Keep it simple; owner reviews and may swap it later.

## 5. Main screen (only screen; minimal)
- R-30 Top → bottom, one vertical scroll, framework widgets only:
  1. Status block (R-41): lines "Trợ năng", "Pin", "Thông báo", and on Realme/Redmi "Tự khởi động". Each line shows ✓/✗ and is tappable → opens the right settings screen.
  2. Switch "Bỏ qua quảng cáo" (= `active`, manual).
  3. Checkbox "Tự bật khi khởi động máy" (R-11).
  4. Checkbox "Tự bật khi mở ứng dụng đã chọn" (R-12).
  5. Section "Ứng dụng áp dụng": checkbox list of installed launchable apps (label + icon), apps with a rule first, then alphabetical.
  6. Button R-22.
  7. Debug builds only: button "Xuất cây giao diện (debug)" (R-80).
- R-31 All UI strings Vietnamese in `res/values/strings.xml`. Follows system dark mode.

## 6. OEM & health
- R-40 Brand detection by `Build.MANUFACTURER` (lowercase): `xiaomi|redmi|poco` ⇒ XIAOMI; `realme|oppo|oneplus` ⇒ OPLUS; else OTHER.
- R-41 Health checks shown in status block:
  - Accessibility: enabled (parse `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`) and running (`SkipService.isRunning`, volatile flag set in `onServiceConnected`, cleared in `onUnbind`/`onDestroy`). Enabled-but-not-running ⇒ "Đã bật nhưng không chạy — hãy tắt rồi bật lại trong Trợ năng".
  - Battery: `PowerManager.isIgnoringBatteryOptimizations`; tap ⇒ `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` with `package:` URI.
  - Notifications: `NotificationManager.areNotificationsEnabled()`.
  - Autostart (XIAOMI/OPLUS only): cannot be read ⇒ show "Kiểm tra thủ công" with tap shortcut (R-42).
- R-42 Autostart shortcuts, try in order, catch `ActivityNotFoundException`/`SecurityException`, final fallback `ACTION_APPLICATION_DETAILS_SETTINGS`. All components UNVERIFIED — confirm on device:
  - XIAOMI: `com.miui.securitycenter/com.miui.permcenter.autostart.AutoStartManagementActivity`
  - OPLUS: `com.coloros.safecenter/.startupapp.StartupAppListActivity`, `com.coloros.safecenter/.permission.startup.StartupAppListActivity`, `com.oplus.safecenter/.startupapp.StartupAppListActivity`
- R-43 API 33+ and service not enabled ⇒ show hint: "Nếu không bật được: Thông tin ứng dụng → ⋮ → Cho phép cài đặt bị hạn chế", with button to app details.

## 7. Rules
- R-50 `assets/rules.json`, loaded once at service connect. Schema:
```json
{ "schema": 1,
  "generic": { "texts": ["..."] },
  "apps": [ { "package": "pkg", "label": "Name", "verified": false,
              "viewIds": ["skip_ad_button"], "texts": ["Skip"] } ] }
```
  `viewIds` without `:` are expanded to `<package>:id/<value>`. Apps without an entry use `generic` only.
- R-51 Updating rules = edit JSON + rebuild. `verified:false` means IDs were not yet confirmed from a real dump (R-80).

## 8. Manifest & privacy
- R-60 Permissions — exactly these, nothing else: `RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. Services protected by `BIND_ACCESSIBILITY_SERVICE` and `BIND_QUICK_SETTINGS_TILE`.
- R-61 App listing via `<queries><intent>MAIN + LAUNCHER</intent></queries>`; never `QUERY_ALL_PACKAGES`.
- R-62 `accessibility_config.xml`: event types `typeWindowStateChanged|typeWindowContentChanged`; flags `flagReportViewIds`; `canRetrieveWindowContent=true`; `canPerformGestures=true`; `notificationTimeout=100`; `isAccessibilityTool=false`; Vietnamese description explaining the app only reads the chosen apps and has no internet access.
- R-63 `android:allowBackup="false"`.

## 9. Performance & robustness
- R-70 First line of `onAccessibilityEvent`: if not active (after ModeLogic) ⇒ return, before touching any node.
- R-71 Tree walks bounded: depth ≤ 30, ≤ 500 nodes. Null `rootInActiveWindow` ⇒ return.
- R-72 Idle check (R-12 10-min rule) uses one `Handler.postDelayed` every 60 s only while auto-activated; no polling otherwise.
- R-73 Release: `isMinifyEnabled=true`, `isShrinkResources=true`. Debug and release share `keystore/skipqc.keystore` so APKs update over each other.

## 10. Debug tooling
- R-80 Debug builds: button exports the current accessibility tree of the most recent watched-app window (class, viewId, text, contentDescription, clickable, enabled, bounds; depth-indented) to `getExternalFilesDir("dumps")/dump-<time>.txt`, then opens the share sheet. Service keeps the last root snapshot reference only in debug builds. Used to confirm rule IDs.

## 11. Later (do NOT build now)
Windows browser extension (content script clicking `.ytp-skip-ad-button`); Android TV build; tapping the skip button shown in YouTube's cast remote UI (Samsung TV workaround).
