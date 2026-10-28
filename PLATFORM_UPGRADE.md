# Bảo mật, thống kê, bản dịch và tra cứu

## Trước khi chạy bản mới

1. Sao lưu DB bằng `mysqldump --single-transaction --routines --triggers --no-tablespaces`.
2. Kiểm tra DB đích, chạy `db/20261028_security_analytics_translations.sql` bằng tài khoản quản trị DB. Migration chỉ thêm cột/bảng, không tự chạy lúc khởi động.
3. Khởi động BE rồi FE. Các API mới cần bản BE này; không triển khai FE riêng trước khi BE và schema online đã được nâng cấp.

DB local `a509_db` đã được sao lưu và áp dụng migration ngày 28/10/2026 theo xác nhận của chủ dự án. Không áp dụng tự động lên DB online.

Compose local hiện chỉ publish `127.0.0.1:3306`. Cần đặt `MYSQL_ROOT_PASSWORD` qua biến môi trường và `MYSQL_DATA_VOLUME` bằng **tên volume MySQL hiện có**. Compose dùng external volume để tránh tạo nhầm DB trắng hoặc xóa volume khi dừng stack. Không chạy `down -v`, không tự thay tên volume. Bản ghi mật khẩu cố định đã được loại khỏi file Compose hiện tại; lịch sử Git cũ vẫn có thể chứa giá trị đó, nên mật khẩu ấy không được dùng cho môi trường online. DB online nên dùng user ứng dụng riêng với quyền tối thiểu thay vì root.

## Tính năng

- `/api/admin/overview?days=7|30|90`: ADMIN. Tổng tài khoản, tài khoản mới/hoạt động, nội dung đã xuất bản; lượt xem/khách/phiên và chuỗi ngày UTC; thiết bị, nguồn, quốc gia, khu vực, trang phổ biến.
- `/api/analytics/pageviews`: ghi sự kiện công khai. UUID sự kiện chống đếm lặp; mã khách và phiên được băm. Không lưu IP, UA nguyên bản, query string hay fragment. Tôn trọng DNT; loại trang quản trị/reset mật khẩu và bot nhận diện được. Số khách là ước tính trình duyệt, không phải số người tuyệt đối. Mã khách phía FE trong localStorage, phiên trong sessionStorage, ngắt sau 30 phút; opt-out bằng `A509_ANALYTICS_OPTOUT=1` trong localStorage.
- Giữ dữ liệu truy cập 90 ngày, dọn định kỳ khi ghi sự kiện. Không tạo dữ liệu lịch sử giả.
- `/api/admin/translations/{TYPE}` và `/{TYPE}/{id}`: ADMIN. Nhập tiếng Anh cho bài viết, quân trang, quốc gia, lực lượng/giai đoạn/danh mục, chú thích ảnh. Đọc công khai với `X-A509-Language: en`. Không dịch URL/slug; giữ nguồn tên gốc cho routing. Bản dịch cũ khi bản gốc thay đổi được đánh dấu cần rà lại và dùng nội dung gốc. Nội dung thiếu bản dịch hiển thị tiếng Việt với thông báo ở trang chi tiết.
- `/api/search?q=&type=&page=&size=`: tìm có/không dấu trong nội dung tiếng Việt/Anh; chỉ bài đã xuất bản, không xóa mềm. Có phân trang và thứ tự ưu tiên tiêu đề; không trả URL ảnh. Nội dung phục dựng liên kết tới đúng chuỗi quốc gia/lực lượng/giai đoạn. Hiện tra cứu quét các trường văn bản ở BE, phù hợp bộ sưu tập nhỏ; cần chỉ mục chuyên dụng khi số bài/nội dung lớn.
- FE: `/admin/dashboard`, `/admin/ban-dich`; phục dựng chọn bằng chữ. Dữ liệu ảnh cờ cũ được giữ trong DB.

## Cấu hình

```properties
APP_ANALYTICS_RETENTION_DAYS=90
APP_ANALYTICS_PROXY_SECRET=
APP_TRANSLATION_PROVIDER=manual
APP_TRANSLATION_API_KEY=
APP_DATA_INITIALIZER_ENABLED=false
```

Vị trí mặc định là `ZZ / Unknown`. Nếu dùng proxy có dữ liệu vị trí, proxy phải xóa header do khách gửi, tự cung cấp `X-A509-Country` (ISO 3166 alpha-2), `X-A509-Region` và `X-A509-Proxy-Secret` khớp secret riêng trong BE. Không đặt secret này ở FE. Không bật chế độ tin cậy header quốc gia trực tiếp từ trình duyệt. Không gọi dịch vụ GeoIP bên ngoài.

Dịch thủ công đã hoạt động. Hai biến cấu hình provider/key là điểm cấu hình dự phòng; chưa có adapter tự dịch. Không gửi nội dung tới nhà cung cấp dịch. Có bản dịch cho 100% nội dung thực tế còn phụ thuộc việc nhập và duyệt bản dịch trong admin.

## Sửa bảo mật

- Giới hạn yêu cầu theo IP cho đăng nhập/đăng ký/quên/đặt lại mật khẩu/liên hệ/tra cứu/thống kê; trả 429 với Retry-After. Bản hiện tại dùng bộ nhớ một BE, cần giới hạn ở gateway/Redis khi chạy nhiều replica. Chỉ tin IP từ reverse proxy đã cấu hình và có quy tắc xóa header giả.
- Login trả thông báo chung 401. Đăng ký kiểm tra dữ liệu và BCrypt tối đa 72 byte; không nâng quyền qua payload.
- Quên mật khẩu phản hồi chung, gửi qua hàng đợi có giới hạn. Reset token ngẫu nhiên 256 bit, chỉ lưu SHA-256, hết hạn 15 phút, khóa hàng khi sử dụng. Đổi mật khẩu hoặc khóa/mở tài khoản tăng phiên bản phiên đăng nhập; JWT cũ không thể dùng lại.
- Upload kiểm tra kích thước 5MB, MIME và chữ ký JPEG/PNG/GIF/WebP trước Cloudinary; dùng HTTPS/resource_type=image. SVG không được nhận. Ảnh avatar hiện tại không bị xóa trước khi upload mới thành công.
- Ẩn chi tiết lỗi hệ thống, bật kiểm tra hostname và STARTTLS bắt buộc cho SMTP. Tắt initializer sửa nội dung lúc khởi động theo mặc định.
- Spring Boot 4.0.9, Cloudinary HTTP5, Apache HTTP Client/Core đã vá. Audit dùng `npm audit` và OSV theo tọa độ Maven, chỉ kiểm tra các cảnh báo có trong cơ sở dữ liệu tại thời điểm quét; không phải chứng nhận hết mọi lỗ hổng.

Theo [OWASP Authentication](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html), [Forgot Password](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html), [File Upload](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html) và [Spring security advisories](https://spring.io/security/).

## Mật khẩu cũ

`tools/HashLegacyPasswords.java` là công cụ thủ công, không chạy trong ứng dụng. Mặc định chỉ đếm; chỉ ghi khi có `--apply <fresh-backup.sql>`, xác minh BCrypt khớp mật khẩu cũ trong bộ nhớ và dùng transaction. Không in mật khẩu, không thay văn bản mật khẩu hoặc token_version. Chạy với classpath gồm mysql-connector-j, spring-security-crypto và commons-logging. Chỉ chấp nhận DB localhost/a509_db để tránh ghi nhầm DB online.

Theo xác nhận riêng, DB local đã chuyển 1 bản ghi cũ sang BCrypt; kiểm tra lại còn 0 bản ghi legacy. Cơ chế tương thích cũ còn trong mã để việc triển khai không tự khóa tài khoản ở DB khác; đăng ký/đặt lại mật khẩu luôn dùng BCrypt. Nếu DB hoặc backup cũ từng bị lộ, cần đổi mật khẩu liên quan; băm lại không thu hồi bản mật khẩu đã bị sao chép.

Ảnh nhạy cảm hiện được ẩn URL với khách theo lựa chọn trước đó. URL Cloudinary đã được thành viên xem vẫn có thể được chia sẻ; muốn kiểm soát cả truy cập URL gốc cần kho private hoặc signed delivery.
