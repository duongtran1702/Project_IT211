# Nhật ký cập nhật dự án (Chronological Update Log)

Tài liệu này ghi lại toàn bộ các thay đổi và cập nhật đã thực hiện trên dự án theo dòng thời gian từ lúc bắt đầu nâng cấp hệ thống.

---

## 📅 [14-06-2026] - Buổi Chiều & Tối

### 1. Di chuyển lưu trữ Token sang Redis (FR-13)
*   **Mục tiêu:** Tăng tốc độ xác thực và tối ưu hóa hệ thống bằng cách chuyển lưu trữ Refresh Token và Danh sách đen Token (Blacklist) từ cơ sở dữ liệu MySQL sang bộ nhớ tạm Redis.
*   **Chi tiết thay đổi:**
    *   Thêm thư viện `spring-boot-starter-data-redis` vào [build.gradle](file:///d:/IT211/Me/build.gradle).
    *   Cấu hình kết nối Redis tại [application.properties](file:///d:/IT211/Me/src/main/resources/application.properties).
    *   Tạo lớp cấu hình [RedisConfig.java](file:///d:/IT211/Me/src/main/java/atmin/infrastructure/config/RedisConfig.java) để đăng ký bean `StringRedisTemplate`.
    *   Tạo lớp DTO [RefreshTokenRedis.java](file:///d:/IT211/Me/src/main/java/atmin/repository/redis/dto/RefreshTokenRedis.java) và chuyển đổi [TokenBlacklistRepository.java](file:///d:/IT211/Me/src/main/java/atmin/repository/redis/TokenBlacklistRepository.java), [RefreshTokenRepository.java](file:///d:/IT211/Me/src/main/java/atmin/repository/redis/RefreshTokenRepository.java) sang sử dụng Redis với cơ chế tự động hết hạn (TTL).
    *   Cập nhật logic `login`, `logout`, `refreshToken` trong [AuthService.java](file:///d:/IT211/Me/src/main/java/atmin/service/Impl/AuthService.java).

---

### 2. Triển khai Ghi log thời gian thực hiện (FR-11)
*   **Mục tiêu:** Đo lường hiệu năng của toàn bộ các API và Services trong hệ thống.
*   **Chi tiết thay đổi:**
    *   Tạo khía cạnh [LoggingAspect.java](file:///d:/IT211/Me/src/main/java/atmin/common/aspect/LoggingAspect.java) sử dụng Spring AOP.
    *   Cấu hình `@Around` pointcut bao phủ toàn bộ Controller (`atmin.controller..*`) và Service (`atmin.service.Impl..*`).
    *   Ghi nhận thời gian bắt đầu, kết thúc của method và tính toán độ trễ dưới định dạng: `[PERFORMANCE] Class -> Method() executed in X ms`.
    *   Tạo tài liệu chi tiết đặc tả thiết kế tại [fr_11.md](file:///d:/IT211/Me/document/fr_11.md).

---

### 3. Xây dựng bộ 10 Unit Tests tiêu chuẩn (FR-12)
*   **Mục tiêu:** Đảm bảo chất lượng mã nguồn và độ ổn định của các chức năng lõi.
*   **Chi tiết thay đổi:**
    *   Tạo các lớp kiểm thử dịch vụ: [AuthServiceTest.java](file:///d:/IT211/Me/src/test/java/atmin/service/AuthServiceTest.java), [AdminServiceTest.java](file:///d:/IT211/Me/src/test/java/atmin/service/AdminServiceTest.java).
    *   Tạo các lớp kiểm thử API: [AuthControllerTest.java](file:///d:/IT211/Me/src/test/java/atmin/controller/AuthControllerTest.java), [AdminControllerTest.java](file:///d:/IT211/Me/src/test/java/atmin/controller/AdminControllerTest.java) sử dụng `@WebMvcTest` + MockMvc.
    *   Xử lý lỗi biên dịch Spring Boot 4.x và lỗi nạp chồng context bảo mật bằng cách loại trừ `JwtAuthenticationFilter` ra khỏi MockMvc test context.
    *   Tạo tài liệu đặc tả kết quả kiểm thử tại [fr_12.md](file:///d:/IT211/Me/document/fr_12.md).

---

### 4. Cải tiến SRS Compliance (Tuân thủ đặc tả)
*   **Mục tiêu:** Đồng bộ mã nguồn theo đúng các chuẩn mực được giảng viên quy định trong SRS.
*   **Chi tiết thay đổi:**
    *   Cập nhật [AdminService.java](file:///d:/IT211/Me/src/main/java/atmin/service/Impl/AdminService.java) chuyển sang dùng Java 8 Stream API (`.stream()`, `.filter()`, `.map()`, `.collect()`) để ánh xạ từ Entity sang DTO đúng theo yêu cầu (UC-02).
    *   Loại bỏ kiểm tra trùng lặp ngày đặt sân dư thừa trong [BookingService.java](file:///d:/IT211/Me/src/main/java/atmin/service/Impl/BookingService.java) vì đã được xử lý tự động bởi `@FutureOrPresent` tại DTO Validation.

---

### 5. Khắc phục lỗi trùng lặp logs & Bỏ qua logs trên Git
*   **Mục tiêu:** Giúp logs hiển thị gọn gàng, sạch sẽ, không bị in lặp 2 lần trên console/log file và bảo vệ kho lưu trữ mã nguồn.
*   **Chi tiết thay đổi:**
    *   Chỉnh sửa [logback-spring.xml](file:///d:/IT211/Me/src/main/resources/logback-spring.xml) chuyển thuộc tính `additivity` của logger gói `atmin` thành `false` và cấu hình độc lập ghi ra cả `CONSOLE` và `FILE` appender.
    *   Thêm đường dẫn `logs/` vào file [.gitignore](file:///d:/IT211/Me/.gitignore) để tránh việc commit các file log tự sinh lên Git.

---

### 6. Nâng cấp hệ thống Log chuẩn Production với MDC
*   **Mục tiêu:** Định dạng log có cấu trúc (Structured Logging) giúp truy vết luồng xử lý và phân biệt các request chạy song song dễ dàng.
*   **Chi tiết thay đổi:**
    *   Tạo mới [MdcLoggingFilter.java](file:///d:/IT211/Me/src/main/java/atmin/infrastructure/security/MdcLoggingFilter.java) để tự động khởi tạo UUID ngắn (8 ký tự) làm `traceId`, đồng thời theo dõi các thông tin HTTP Context (`method`, `uri`, `username`).
    *   Đăng ký `MdcLoggingFilter` vào bộ lọc bảo mật [SecurityConfig.java](file:///d:/IT211/Me/src/main/java/atmin/infrastructure/security/SecurityConfig.java) trước `JwtAuthenticationFilter`.
    *   Tích hợp việc tự động cập nhật `username` từ JWT Token vào MDC bên trong [JwtAuthenticationFilter.java](file:///d:/IT211/Me/src/main/java/atmin/infrastructure/security/jwt/JwtAuthenticationFilter.java).
    *   Cập nhật lại Pattern in log trong [logback-spring.xml](file:///d:/IT211/Me/src/main/resources/logback-spring.xml), tự động gắn `[Trace: traceId] [User: username]` vào trước tất cả các log nghiệp vụ (tự động hiển thị `SYSTEM` khi chạy nền hoặc khởi động). Đồng thời cấu hình giới hạn in Stack Trace trên Console tối đa 10 dòng (`%ex{10}`) và **chặn hoàn toàn việc ghi Stack Trace vào file log** (`%nopex`) để giữ cho file log sạch sẽ, nhẹ nhàng và chỉ chứa các thông tin log nghiệp vụ/sự thay đổi thực sự ảnh hưởng đến hệ thống.

---

### 7. Khắc phục lỗi cấu hình thuộc tính Serialization Mode của Page
*   **Mục tiêu:** Sửa lỗi cảnh báo của IDE và ứng dụng `Cannot resolve configuration property 'spring.data.web.page.serialization-mode'`.
*   **Chi tiết thay đổi:**
    *   Thay đổi thuộc tính cấu hình không chính xác `spring.data.web.page.serialization-mode=via_dto` thành `spring.data.web.pageable.serialization-mode=via-dto` trong [application.properties](file:///d:/IT211/Me/src/main/resources/application.properties) để tương thích chính xác với Spring Boot 3.4+ / 4.x.

---

### 8. Đặt tên hiển thị cho người gửi email (Email Sender Display Name)
*   **Mục tiêu:** Hiển thị tên đại diện/tên thương hiệu khi gửi email thay vì chỉ hiển thị địa chỉ email thô (như `wer.company.edu`).
*   **Chi tiết thay đổi:**
    *   Thêm thuộc tính cấu hình `spring.mail.from-name=Sân Cầu Lông Atmin` trong [application.properties](file:///d:/IT211/Me/src/main/resources/application.properties).
    *   Cập nhật [EmailService.java](file:///d:/IT211/Me/src/main/java/atmin/service/Impl/EmailService.java) để inject thuộc tính email người gửi (`spring.mail.username`) và tên đại diện người gửi (`spring.mail.from-name`).
    *   Sử dụng phương thức `helper.setFrom(fromEmail, fromName)` trong `MimeMessageHelper` để định hình lại email gửi đi với tên hiển thị trực quan và thân thiện.

