# Tasks — do the first unchecked item only, then report

Each task ends with: build green + unit tests green + owner check in `docs/REVIEW.md` (same M-number).

## M0 Build baseline
- [x] M0.1 Verify scaffold builds as-is (`assembleDebug`, `lintDebug`). Versions were set from AGP 9.3 release notes (AGP 9.3.3, Gradle 9.5.0, JDK 17, built-in Kotlin) but never compiled. Fix only what breaks; log changes. Ensure `gradlew` has the exec bit in git (`git update-index --chmod=+x gradlew`).
- [x] M0.2 Push; confirm GitHub Actions is green and the permission check step passes.

## M1 Service + debug dump (goal: learn the real skip-button IDs)
- [x] M1.1 `SkipService` + `accessibility_config.xml` + manifest entries (R-04, R-60..R-63). Watched list hardcoded to YouTube for now. No clicking yet.
- [x] M1.2 `DebugDump.kt` + temporary debug button in MainActivity (R-80).
- [x] M1.3 STOP: ask owner to install, play a YouTube ad, press dump when "Skip"/"Bỏ qua" is visible, and send the file. Update `rules.json` from the dump, set `verified: true`.

## M2 Skipping
- [x] M2.1 `Rules.kt` + `RuleEngine.kt` over a `NodeView` interface (R-02, R-50). Unit tests: viewId hit, exact text hit, substring rejected, disabled/invisible rejected, clickable-ancestor walk.
- [x] M2.2 Wire into `SkipService` with cooldown/debounce (R-01, R-03, R-70, R-71). Active hardcoded true for this task.

## M3 Modes
- [x] M3.1 `Prefs.kt` + `ModeLogic.kt` (R-10..R-14) with unit tests for every R-12 bullet.
- [x] M3.2 `BootReceiver` (R-11); service uses ModeLogic + idle handler (R-72); watched list from Prefs, live `packageNames` update (R-04, R-05).

## M4 Tile, status icon, logo
- [x] M4.1 `ic_logo.xml` + adaptive launcher icon (R-23).
- [x] M4.2 `SkipTile` (R-21) and `StatusNotifier` (R-20), both driven by `Prefs.setActive`.

## M5 Main screen
- [x] M5.1 `Oem.kt` health checks + shortcuts (R-40..R-43).
- [x] M5.2 Final `MainActivity` layout (R-30, R-31, R-22). Remove the temporary M1 button (debug dump stays, debug-only).

## M6 Release
- [x] M6.1 Release build config check (R-73); README install section verified against the real UI.
- [x] M6.2 Tag `v0.1.0`; CI attaches release APK to the GitHub Release.

## Log
<!-- one line per finished task: date · task · note -->
- 2026-09-19 · M0.1 · Build xanh voi AGP 9.3.3 / Gradle 9.5.0 / JDK 25; khong doi version. Lint: them `AppTheme` (`values/` = Theme.DeviceDefault, `values-v29/` = .DayNight) vi DayNight chi co tu API 29 > minSdk 26. `gradlew` da co exec bit.
- 2026-09-19 · M0.2 · Day len https://github.com/Liu-548/AppSkipAD ; Actions xanh (run 35419166345), buoc Permission check pass, co artifact SkipQC-apk.
- 2026-09-19 · Khoa ky · Da xoa keystore khoi repo, tao khoa moi (mat khau ngau nhien trong `local.properties`), CI lay tu secrets KEYSTORE_B64/KEYSTORE_PASSWORD; APK CI va APK may deu co van tay 62256be0...
- 2026-09-19 · M1.1 · SkipService (chi ket noi, chua bam), accessibility_config.xml (R-62), manifest du 3 quyen R-60 + queries R-61; APK chi co dung 3 quyen do.
- 2026-09-19 · M1.2 · DebugDump (do sau <=30, <=500 node theo R-71) + nut debug tam trong MainActivity; chia se file qua DumpProvider nam trong source set `debug` (khong dung AndroidX, ban release khong co provider lan quyen nao them).
- 2026-09-19 · M1.3 · Dump that tren Realme RMX3370 (Android 13) khi quang cao YouTube dang chay: `skip_ad_button` (FrameLayout, clickable=true) > `skip_ad_button_container` (desc "Bo qua quang cao") > `skip_ad_button_text` (text "Bo qua"). rules.json dung san, chi set verified=true.
- 2026-09-19 · Sua loi M1.2 · Giu tham chieu node de dump sau la vo dung (roi app la node chet, khong con node con). Nay service render cay ra text ngay luc co su kien (debug, toi da 1 lan/giay) va chi nhan root dung goi cua app duoc theo doi.
- 2026-09-19 · M2.1 · Rules.kt + RuleEngine.kt tren interface NodeView/NodeFinder (khong import android.*); 10 unit test xanh: trung viewId, trung text/desc chinh xac, loai "Skip in 5", loai node an/disabled, di len toi da 3 cap tim node bam duoc.
- 2026-09-19 · M2.2 · Noi RuleEngine vao service: debounce 100ms + cooldown 1500ms moi goi, bam node hoac tap giua vung neu khong bam duoc. Thu that tren RMX3370: quang cao YouTube bi bo qua tu dong. Luu y khi do: `uiautomator dump` lam treo tam thoi moi dich vu tro nang khac nen dung no de kiem tra se ra ket qua sai.
- 2026-09-19 · M3.1 · ModeLogic thuan (ModeState + 4 input, R-10..R-12) va Prefs la noi duy nhat ghi trang thai (R-14). 12 unit test phu tung gach dau dong R-12; tong 22 test xanh. Chua noi vao service/boot — do la M3.2.
- 2026-09-19 · M3.2 · BootReceiver + service chay qua ModeLogic (R-70 thoat truoc khi cham node), danh sach app lay tu Prefs va cap nhat packageNames ngay khi doi (R-04/R-05), idle tick 60s chi khi dang tu bat (R-72). Prefs khong ghi dia cho lastWatchedEventAt (moi su kien deu doi). Chua thu tren may: cap da rut, se kiem tra lai khi cam vao.
- 2026-09-19 · M4.1 · ic_logo.xml (glyph skip-next) + icon launcher thich ung dung lai chinh no qua <inset>, nen ca app chi co mot hinh. Da xem tren may that: nen do #D32F2F, glyph trang, ro.
- 2026-09-19 · M4.2 · SkipTile (o thanh keo xuong) + StatusNotifier (icon trang thai, kem nut "Tat"), ca hai deu doc/ghi qua Prefs (R-14). Lint bat loi dung startActivityAndCollapse(Intent) — phai giu vi ban PendingIntent chi co tu API 34, da suppress dung dong do. Chua bam thu tren may: cap rut giua chung.
- 2026-09-19 · M5.1 · Oem.kt: nhan dien hang may (R-40), 4 dong tinh trang Tro nang/Pin/Thong bao/Tu khoi dong (R-41), loi tat sang trang cai dat kem duong lui neu may khong co (R-42), goi y "cai dat bi han che" cho Android 13+ (R-43). Chua co man hinh hien thi — do la M5.2.
- 2026-09-19 · Bo tinh nang tu bat khi khoi dong may (chu du an yeu cau). Da go BootReceiver, quyen RECEIVE_BOOT_COMPLETED va o tick tuong ung; SPEC chuyen muc nay xuong phan "Later". Van giu: khoi dong lai may thi app luon TAT — service tu so sanh thoi diem boot, da thu that tren RMX3370 (truoc reboot bat, sau reboot tat) va khong can quyen nao.
- 2026-09-19 · M5.2 · Man hinh chinh that (R-30/R-31/R-22): 4 dong tinh trang bam duoc, cong tac, o tick tu bat, danh sach app co icon (app co luat len dau), nut them o vao thanh keo xuong, xin quyen thong bao. Checkbox app chi phan ung khi nguoi dung cham (setOnCheckedChangeListener no ca khi code set -> co the xoa trang danh sach).
- 2026-09-19 · Thu tren RMX3370 · Che do toi OK; tick "Tu bat khi mo ung dung da chon" -> mo YouTube thi app tu bat (active+autoActivated) va bo qua quang cao; thong bao trang thai hien dung (im lang, ghim, co nut Tat).
- 2026-09-19 · M6.1 · Ban release: 50 KB, dung 2 quyen, khong co provider debug, ky cung khoa voi ban debug nen cai de duoc (da thu tren may). README viet lai theo man hinh that (bo muc tu bat khi khoi dong, them buoc chon app va cach keo o vao thanh keo xuong).
- 2026-09-19 · Canh bao khi test · Lenh `adb shell monkey ... 1` con ban kem MOT su kien cham ngau nhien — chinh no bam vao banner quang cao trong YouTube va mo trang sym.com.vn, khong phai app bam nham. Dung `am start -n <pkg>/<activity>` de mo app khi test. Da do lai co log: app chi bam dung node skip_ad_button (clickable, visible, dung toa do).
- 2026-09-19 · M6.2 · Da gan the v0.1.0, CI tu tao Release kem app-release.apk (50457 byte). Tai file tu Releases ve kiem lai: dung 2 quyen, dung chu ky 62256be0..., cai de len may that chay binh thuong. Toan bo TASKS da xong.
- 2026-09-19 · Kiem tra an toan (REVIEW muc M2) · Bat app roi luot feed YouTube ~20 lan va xem Shorts ~10 video tren RMX3370: khong co app nao khac bi mo, khong crash, khong bam nham gi.
- 2026-09-19 · Giao dien (chu du an yeu cau) · Mot bang mau toi duy nhat, bo thanh tieu de he thong, bo values-v29 (khong con theo che do sang/toi cua may). Dong trang thai co dau mau (xanh/do/vang), icon app ep ve 32dp, hai nut chuyen len tren danh sach app vi danh sach dai vo tan. SPEC R-30/R-31 sua theo.
- 2026-09-19 · Chong doi ID (chu du an hoi) · Them buoc du phong trong RuleEngine: neu ca viewId lan text deu truot thi quet cay (gioi han 30 cap/500 node, toi da 2 giay mot lan) tim node hien co viewId chua "skip_ad" -> chiu duoc ten id bi doi (skip_ad_button_v2...). Chi doi chieu theo ID, khong noi long doi chieu chu, vi mot video ten "Skip the line" cung bam duoc. 23 test xanh.
