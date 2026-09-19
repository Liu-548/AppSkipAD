# SkipQC

App Android tự bấm nút **"Bỏ qua quảng cáo"** trên YouTube và các app xem phim bạn chọn. Dự án cá nhân, không thu thập dữ liệu, **không có quyền Internet** (CI kiểm tra mỗi lần build).

## Tải app
Link tải thẳng bản mới nhất (mở bằng trình duyệt trên điện thoại):
**https://github.com/Liu-548/AppSkipAD/releases/latest/download/app-release.apk**

Hoặc vào https://github.com/Liu-548/AppSkipAD → mục **Releases** bên phải → bản trên cùng → mục **Assets** → bấm `app-release.apk`.

## Cài đặt
1. Mở file `app-release.apk` vừa tải (Chrome hỏi "Vẫn tải xuống?" → chọn có; máy hỏi "Cho phép cài từ nguồn này?" → bật).
2. Android 13+: vào *Thông tin ứng dụng SkipQC → ⋮ → Cho phép cài đặt bị hạn chế*.
3. *Cài đặt → Trợ năng → SkipQC* → bật.
4. Mở SkipQC, cho phép gửi thông báo (để có icon trạng thái), rồi xử lý các dòng ✗ trong khối trạng thái (Pin, Tự khởi động).
5. Chọn ứng dụng áp dụng ở mục **Ứng dụng áp dụng** (YouTube nằm đầu danh sách, mặc định đã chọn).
6. Bấm **"Thêm nút vào thanh kéo xuống"**. Nếu máy không hiện hộp thoại: kéo thanh thông báo xuống → biểu tượng chỉnh sửa → kéo ô **SkipQC** vào.

**Realme:** Quản lý pin ứng dụng → SkipQC → cho phép chạy nền. Máy Realme không gửi tín hiệu khởi động cho app cài ngoài, nên nếu muốn dịch vụ sống lại sau khi khởi động máy thì bật thêm *Tự khởi động* cho SkipQC.
**Redmi:** Tự khởi động → bật SkipQC; Tiết kiệm pin → Không giới hạn; khóa app trong đa nhiệm.

## Cách dùng
Bật/tắt bằng ô **SkipQC** trên thanh kéo xuống, bằng công tắc trong app, hoặc nút **Tắt** trên thông báo. Khởi động lại máy thì app luôn ở trạng thái **tắt**.

Có một tuỳ chọn: **Tự bật khi mở ứng dụng đã chọn** — mở YouTube là app tự bật, rời YouTube 10 phút là tự tắt. Tắt tay trong lúc đang xem thì app tôn trọng, chỉ bật lại khi bạn thoát hẳn rồi mở lại.

## Phát triển
Dự án vibe code: xem `CLAUDE.md` và `docs/`. Khóa ký APK không nằm trong repo (giữ ở máy chủ dự án + GitHub Secrets) — chỉ cài APK lấy từ repo này.

## License
MIT — xem `LICENSE`.
