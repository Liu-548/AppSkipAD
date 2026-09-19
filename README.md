# SkipQC

App Android tự bấm nút **"Bỏ qua quảng cáo"** trên YouTube và các app xem phim bạn chọn. Dự án cá nhân, không thu thập dữ liệu, **không có quyền Internet** (CI kiểm tra mỗi lần build).

## Cài đặt
1. Tải `app-release.apk` ở mục **Releases** và cài.
2. Android 13+: vào *Thông tin ứng dụng SkipQC → ⋮ → Cho phép cài đặt bị hạn chế*.
3. *Cài đặt → Trợ năng → SkipQC* → bật.
4. Mở SkipQC, xử lý các dòng ✗ trong khối trạng thái (Pin, Thông báo, Tự khởi động).
5. Bấm **"Thêm nút vào thanh kéo xuống"**, sau đó bật/tắt ngay từ thanh kéo.

**Realme:** Quản lý pin ứng dụng → SkipQC → cho phép chạy nền + tự khởi chạy.
**Redmi:** Tự khởi động → bật SkipQC; Tiết kiệm pin → Không giới hạn; khóa app trong đa nhiệm.

## Cách dùng
Mặc định app **tắt** sau mỗi lần khởi động máy; bật bằng ô SkipQC trên thanh kéo xuống. Tùy chọn thêm: *Tự bật khi khởi động máy*, *Tự bật khi mở ứng dụng đã chọn*.

## Phát triển
Dự án vibe code: xem `CLAUDE.md` và `docs/`. Khóa ký APK trong `keystore/` được commit có chủ đích (dùng cá nhân) — chỉ cài APK từ repo này.

## License
MIT — xem `LICENSE`.
