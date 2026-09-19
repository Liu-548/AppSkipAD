# Checklist duyệt (dành cho chủ dự án)

Không cần đọc code. Tải APK từ GitHub Actions (tab Actions → lần chạy mới nhất → Artifacts) hoặc từ Releases, cài lên máy rồi kiểm tra theo mục tương ứng với mốc AI vừa làm.

## Luôn kiểm tra (mọi mốc)
- [ ] GitHub Actions màu xanh (bước "Permission check" cũng xanh = app không có quyền Internet).
- [ ] Cài đè lên bản cũ được, không báo "Ứng dụng chưa được cài đặt".

## M0 — Build
- [ ] Actions xanh, có file APK trong Artifacts.

## M1 — Dịch vụ + xuất cây giao diện
- [ ] Bật được SkipQC trong Cài đặt → Trợ năng (nếu bị chặn: Thông tin ứng dụng → ⋮ → Cho phép cài đặt bị hạn chế).
- [ ] Mở YouTube, đợi quảng cáo có nút "Bỏ qua", mở SkipQC bấm "Xuất cây giao diện", gửi file cho AI.

## M2 — Bỏ qua quảng cáo
- [ ] Quảng cáo YouTube có nút Bỏ qua → tự bấm, không cần chạm.
- [ ] Không bấm nhầm thứ khác trong YouTube (lướt feed, mở menu, xem Shorts vài phút).

## M3 — Chế độ
- [ ] Khởi động lại máy → app ở trạng thái TẮT (luôn luôn; phần tự bật khi khởi động đã bỏ).
- [ ] Bật "Tự bật khi mở ứng dụng đã chọn", tắt công tắc chính → mở YouTube → tự bật.
- [ ] Đang trong YouTube tắt bằng tay → vẫn tắt cho tới khi thoát ra và mở lại YouTube.
- [ ] Chơi game (không có trong danh sách) → máy không nóng/lag hơn bình thường.

## M4 — Nút thanh kéo xuống, icon, logo
- [ ] Thêm được ô SkipQC vào thanh kéo xuống; bấm ô → bật/tắt, ô sáng/tối đúng trạng thái.
- [ ] Khi bật: có icon SkipQC trên thanh trạng thái. Khi tắt: icon biến mất.
- [ ] Logo đơn giản, nhìn rõ ở cả ô thanh kéo, thanh trạng thái và màn hình chính. (Muốn đổi thì mô tả cho AI.)

## M5 — Màn hình chính
- [ ] Chỉ 1 màn hình, đủ: trạng thái quyền, công tắc, 2 ô tự bật, danh sách app, nút thêm vào thanh kéo.
- [ ] Bấm từng dòng trạng thái (Trợ năng / Pin / Thông báo / Tự khởi động) → mở đúng trang cài đặt trên Realme và Redmi.
- [ ] Chế độ tối hiển thị ổn.

## M6 — Phát hành
- [ ] Có bản Release v0.1.0 kèm APK trên GitHub.
- [ ] Làm theo README trên một máy mới (của bạn bè) → chạy được.
