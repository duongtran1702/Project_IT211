# Nhật Ký Cập Nhật Hệ Thống (Update Log)

## 📌 [Cập nhật ngày 12/06/2026] - Tối ưu hóa truy vấn, dọn dẹp token và tính năng Đăng xuất (Blacklisting)

Hệ thống đã được cập nhật tối ưu hóa các truy vấn liên quan đến Booking, sửa đổi cơ chế scheduler dọn dẹp token, tích hợp file cấu hình môi trường, triển khai nghiệp vụ Đăng xuất (Blacklisting) và tính năng Phê duyệt / Từ chối lịch (FR-08):

### 1. Áp dụng Constructor Projection trong BookingRepository
*   **File chỉnh sửa:** [BookingRepository.java](file:///d:/IT211/Me/src/main/java/atmin/repository/BookingRepository.java)
*   **Chi tiết:** Thay đổi câu truy vấn `findBookingsByUser` và bổ sung `findResponseById` để sử dụng cấu trúc `SELECT new atmin.controller.booking.dto.response.BookingResponse(...)`. Giải pháp này giúp chỉ lấy chính xác các cột cần thiết từ Database (`b.id`, `b.bookingDate`, `b.timeSlot`, `b.totalPrice`, `b.status`, `c.courtName`, `cl.name`, `u.fullName`) và tự động khởi tạo trực tiếp DTO `BookingResponse`. Nhờ đó, tránh tải toàn bộ thực thể cồng kềnh như `User` (với eager roles) hay `Court`, `BadmintonCluster`.

### 2. Loại bỏ bước mapping thủ công trong BookingService
*   **File chỉnh sửa:** [BookingService.java](file:///d:/IT211/Me/src/main/java/atmin/service/Impl/BookingService.java)
*   **Chi tiết:**
    *   Trong phương thức `getBookingHistory`, trả về trực tiếp `Page<BookingResponse>` nhận được từ repository thay vì map thủ công trong bộ nhớ (giúp triệt tiêu hoàn toàn lỗi N+1 SQL queries khi tải danh sách lịch sử booking).
    *   Trong phương thức `createBooking`, sau khi lưu booking mới thành công, tiến hành truy vấn lại bằng phương thức chiếu `findResponseById` để trả về phản hồi tối ưu nhất cho Client.

### 3. Cấu hình tự động dọn dẹp Refresh Token hết hạn vào 12h đêm hàng ngày
*   **File chỉnh sửa:** [RefreshTokenCleanupScheduler.java](file:///d:/IT211/Me/src/main/java/atmin/infrastructure/scheduler/RefreshTokenCleanupScheduler.java) & [Application.java](file:///d:/IT211/Me/src/main/java/atmin/Application.java)
*   **Chi tiết:**
    *   Cập nhật biểu thức cron trong `@Scheduled` của `RefreshTokenCleanupScheduler` từ `"0 0 2 * * ?"` (2h sáng) sang `"0 0 0 * * ?"` để hệ thống tự động quét và xóa sạch các token hết hạn vào lúc 12h đêm (00:00) hàng ngày.
    *   Bổ sung annotation `@EnableScheduling` trong `Application.java` để kích hoạt tính năng thực thi tác vụ định kỳ của Spring Boot.

### 4. Chuyển cấu hình JWT Secret Key sang file `.env`
*   **File chỉnh sửa:** [Application.java](file:///d:/IT211/Me/src/main/java/atmin/Application.java), [application.properties](file:///d:/IT211/Me/src/main/resources/application.properties), [.env](file:///d:/IT211/Me/.env)
*   **Chi tiết:**
    *   Xây dựng phương thức `loadDotenv()` trong `Application.java` để đọc và phân tích file `.env` tại thời điểm khởi chạy, nạp động các biến môi trường thành System Properties.
    *   Chuyển giá trị của `jwt.secret-key` từ `application.properties` sang biến `JWT_SECRET_KEY` trong file `.env`.
    *   Thay thế bằng placeholder `jwt.secret-key=${JWT_SECRET_KEY}` trong `application.properties` để Spring Boot tự động nhận diện giá trị tương ứng.

### 5. Triển khai chức năng Đăng xuất & Thu hồi Token - Blacklisting (UC-03 / FR-03)
*   **Các file chỉnh sửa/thêm mới:** [TokenBlacklistRepository.java](file:///d:/IT211/Me/src/main/java/atmin/repository/TokenBlacklistRepository.java) [NEW], [JwtProvider.java](file:///d:/IT211/Me/src/main/java/atmin/infrastructure/security/jwt/JwtProvider.java), [IAuthService.java](file:///d:/IT211/Me/src/main/java/atmin/service/IAuthService.java), [AuthService.java](file:///d:/IT211/Me/src/main/java/atmin/service/Impl/AuthService.java), [AuthController.java](file:///d:/IT211/Me/src/main/java/atmin/controller/auth/AuthController.java), [JwtAuthenticationFilter.java](file:///d:/IT211/Me/src/main/java/atmin/infrastructure/security/jwt/JwtAuthenticationFilter.java)
*   **Chi tiết:**
    *   Xây dựng kho lưu trữ `TokenBlacklistRepository` tương tác với bảng `token_blacklists`.
    *   Hỗ trợ trích xuất thời gian hết hạn (`Expiration`) trong `JwtProvider`.
    *   Triển khai dịch vụ `logout` trong `AuthService` thực hiện giải mã token, kiểm tra tính hợp lệ, tính toán thời điểm hết hạn còn lại và lưu vào danh sách đen `token_blacklists`.
    *   Thêm endpoint POST `/api/v1/auth/logout` trong `AuthController` đính kèm header `Authorization`.
    *   Tích hợp kiểm tra danh sách đen trong `JwtAuthenticationFilter`. Nếu một token đã đăng xuất được gửi lên, filter lập tức chặn lại và trả về lỗi `403 Forbidden` cùng JSON phản hồi lỗi chuẩn.
    *   Cấu hình `JwtAuthenticationFilter` tự động chặn và trả về mã lỗi `401 Unauthorized` chứa thông điệp cụ thể của ngoại lệ `JwtException` (ví dụ: `"Token has expired"` khi hết hạn) thay vì bỏ qua để Spring Security ném ra thông báo mặc định `"Full authentication is required..."`. Cách này giúp Client/Frontend nhận diện chính xác lỗi hết hạn để kích hoạt luồng Refresh Token.

### 6. Triển khai tính năng Phê duyệt / Từ chối lịch đặt sân (UC-08 / FR-08)
*   **Các file chỉnh sửa/thêm mới:** [BookingManagerController.java](file:///d:/IT211/Me/src/main/java/atmin/controller/booking/BookingManagerController.java) [NEW], [BookingStatusUpdateRequest.java](file:///d:/IT211/Me/src/main/java/atmin/controller/booking/dto/request/BookingStatusUpdateRequest.java) [NEW], [IBookingService.java](file:///d:/IT211/Me/src/main/java/atmin/service/IBookingService.java), [BookingService.java](file:///d:/IT211/Me/src/main/java/atmin/service/Impl/BookingService.java), [BookingRepository.java](file:///d:/IT211/Me/src/main/java/atmin/repository/BookingRepository.java), [SecurityConfig.java](file:///d:/IT211/Me/src/main/java/atmin/infrastructure/security/SecurityConfig.java)
*   **Chi tiết:**
    *   Xây dựng DTO `BookingStatusUpdateRequest` với các ràng buộc kiểm tra hợp lệ (`@NotBlank`, `@Pattern` chỉ chấp nhận `CONFIRMED` hoặc `REJECTED`).
    *   Tạo controller `BookingManagerController` với các API dành riêng cho Manager/Admin tại `/api/v1/manager/bookings`:
        *   `GET` để truy vấn danh sách đặt sân có hỗ trợ phân trang và lọc theo trạng thái (`status`).
        *   `PUT /{id}` để cập nhật trạng thái đơn đặt lịch.
    *   Thực hiện triển khai các phương thức nghiệp vụ trong `BookingService`:
        *   `getBookings`: Lấy danh sách booking từ repo theo trạng thái.
        *   `updateBookingStatus`: Kiểm tra booking có tồn tại không, nếu trạng thái hiện tại khác `PENDING` thì ném lỗi `DuplicateResourceException` (409 Conflict), ngược lại cập nhật và lưu trạng thái mới.
    *   Khai báo phương thức truy vấn `findBookingsByStatus` sử dụng Constructor Projection trong `BookingRepository`.
    *   Cấu hình phân quyền trong `SecurityConfig` cho phép các vai trò `ROLE_MANAGER` và `ROLE_ADMIN` được quyền truy cập vào đường dẫn `/api/v1/manager/bookings/**`.

---

## 📌 [Cập nhật ngày 11/06/2026] - Sửa lỗi bảo mật, xác thực & phân quyền (FR-04 & FR-05)

Hệ thống đã được cập nhật sửa đổi các lỗi bảo mật và tối ưu hóa luồng xử lý ngoại lệ liên quan đến cơ chế đăng nhập (FR-04) và quản lý phân quyền token (FR-05) theo đúng đặc tả SRS:

### 1. Đồng nhất lỗi đăng nhập sai tài khoản/mật khẩu
*   **File chỉnh sửa:** [GlobalExceptionHandler.java](file:///d:/IT211/Me/src/main/java/atmin/common/exception/GlobalExceptionHandler.java)
*   **Nguyên nhân lỗi cũ:** Khi xác thực thông tin đăng nhập thất bại, `AuthenticationManager` ném ra `BadCredentialsException` (kế thừa từ `AuthenticationException`). Do thiếu bộ xử lý cụ thể, lỗi này bị đẩy vào bộ bắt lỗi chung `RuntimeException` dẫn tới phản hồi mã lỗi `500 Internal Server Error` không chuẩn xác.
*   **Giải pháp xử lý:** Thêm `@ExceptionHandler(AuthenticationException.class)` nhằm bắt trực tiếp các lỗi liên quan đến thông tin xác thực và trả về JSON chuẩn mã `401 Unauthorized` cùng nội dung mô tả lỗi cụ thể của Spring Security (ví dụ: `"Bad credentials"`).

### 2. Chuẩn hóa luồng xác thực Token khi gặp lỗi Token không hợp lệ
*   **File chỉnh sửa:** [JwtAuthenticationFilter.java](file:///d:/IT211/Me/src/main/java/atmin/infrastructure/security/jwt/JwtAuthenticationFilter.java)
*   **Nguyên nhân lỗi cũ:** Khi nhận được token sai cấu trúc hoặc chữ ký không khớp (`JwtException`), bộ lọc tự động ngắt chuỗi Filter và ghi phản hồi lỗi trực tiếp chứa thông điệp kỹ thuật (ví dụ: `"Signature or structure not valid"`). Điều này không đúng với mong muốn hệ thống là trả về thông điệp bảo mật đồng nhất cho các request không hợp lệ.
*   **Giải pháp xử lý:** Sửa luồng xử lý trong khối `catch (JwtException e)` chỉ ghi nhận log cảnh báo và cho phép luồng tiếp tục di chuyển qua các Filter tiếp theo mà không thiết lập thông tin xác thực (`SecurityContext` để trống). Spring Security sẽ nhận diện đây là một request chưa được xác thực và chuyển giao việc phản hồi cho `AuthenticationEntryPoint` xử lý đồng nhất.

### 3. Cấu hình Custom Entry Point và Access Denied Handler
*   **File chỉnh sửa:** [SecurityConfig.java](file:///d:/IT211/Me/src/main/java/atmin/infrastructure/security/SecurityConfig.java)
*   **Nguyên nhân lỗi cũ:** Chưa khai báo các bộ xử lý phản hồi lỗi bảo mật tùy chỉnh cho các trường hợp không có quyền truy cập hoặc chưa đăng nhập, dẫn đến việc Spring Security trả về phản hồi mặc định (hoặc phản hồi rỗng).
*   **Giải pháp xử lý:** Tiêm `ObjectMapper` và xây dựng cấu hình tùy chỉnh:
    *   **Custom AuthenticationEntryPoint:** Trả về JSON lỗi `401 Unauthorized` có message `"Full authentication is required to access this resource"` cho mọi trường hợp chưa được xác thực (không đính token hoặc token lỗi).
    *   **Custom AccessDeniedHandler:** Trả về JSON lỗi `403 Forbidden` có message `"Access Denied"` khi người dùng có token hợp lệ nhưng vai trò không đủ quyền truy cập (ví dụ: Customer cố ý gọi API của Admin).
    *   Đăng ký trực tiếp trong cấu hình `.exceptionHandling()` của `SecurityFilterChain`.

### 4. Điều chỉnh chỉ số phân trang (Pagination Index) thành 1-based cho phía Client
*   **File chỉnh sửa:** [AdminController.java](file:///d:/IT211/Me/src/main/java/atmin/controller/admin/AdminController.java)
*   **Nguyên nhân lỗi cũ:** Spring Data JPA mặc định sử dụng phân trang bắt đầu từ `0` (0-indexed). Khi Client truyền tham số `page=1` để lấy trang đầu tiên, hệ thống hiểu là lấy trang thứ hai (trang này bị trống dữ liệu nếu tổng số bản ghi nhỏ hơn hoặc bằng kích thước trang), tạo cảm giác như hệ thống không lấy được dữ liệu trong DB.
*   **Giải pháp xử lý:** Thay đổi default value của tham số `page` thành `"1"` và thực hiện trừ đi 1 đơn vị (`page - 1`) trước khi khởi tạo `Pageable` để ánh xạ chính xác về chỉ số 0-indexed của JPA.

### 5. Xử lý lỗi sai định dạng tham số (Parameter Type Mismatch) thành 400 thay vì 500
*   **File chỉnh sửa:** [GlobalExceptionHandler.java](file:///d:/IT211/Me/src/main/java/atmin/common/exception/GlobalExceptionHandler.java)
*   **Nguyên nhân lỗi cũ:** Khi người dùng truyền sai kiểu dữ liệu của tham số (ví dụ: `size=abc` hoặc `id=abc`), Spring Boot ném ngoại lệ `MethodArgumentTypeMismatchException`. Ngoại lệ này chưa được xử lý cục bộ nên bị bắt bởi handler `RuntimeException` dẫn tới mã lỗi 500.
*   **Giải pháp xử lý:** Khai báo phương thức xử lý lỗi `@ExceptionHandler(MethodArgumentTypeMismatchException.class)` để bắt lỗi chuyển đổi kiểu dữ liệu và trả về mã lỗi `400 Bad Request` cùng thông điệp rõ ràng: `"Failed to convert value of type 'java.lang.String' to required type 'int' for parameter 'page' / 'size'"`.

### 6. Cho phép chuyển tiếp tới Endpoint `/error` (Lỗi 401 khi tạo/cập nhật người dùng)
*   **File chỉnh sửa:** [SecurityConfig.java](file:///d:/IT211/Me/src/main/java/atmin/infrastructure/security/SecurityConfig.java)
*   **Nguyên nhân lỗi cũ:** Khi có bất kỳ lỗi không mong muốn hoặc lỗi kiểm tra dữ liệu xảy ra trong Controller, Spring Boot chuyển tiếp (forward) yêu cầu sang `/error`. Vì `/error` không được cấu hình cho phép truy cập công khai trong Spring Security, request này bị chặn và trả về lỗi `401 Unauthorized` kèm message `"Full authentication is required..."` che khuất lỗi thực tế.
*   **Giải pháp xử lý:** Bổ sung đường dẫn `"/error"` vào danh sách `.permitAll()` trong cấu hình bảo mật `SecurityFilterChain`, giúp các lỗi thực tế (như kiểm tra hợp lệ hoặc lỗi DB) được trả ra bình thường cho Client kiểm tra.

Tài liệu này ghi nhận các thực thể JPA mới, ngoại lệ tùy chỉnh liên quan đến Cloudinary, và tính năng Quản lý người dùng vừa được tích hợp vào dự án dựa trên sơ đồ ERD và tài liệu đặc tả yêu cầu hệ thống (SRS).

## 📂 Các File Đã Thêm Mới & Chỉnh Sửa

### 1. Thực Thể JPA Mới & Cập Nhật (`atmin.entity`)

- **[BaseEntity.java](file:///d:/IT211/Me/src/main/java/atmin/entity/BaseEntity.java)**: Lớp cha trừu tượng (`@MappedSuperclass`) cung cấp các trường: `createdAt`, `updatedAt`, và `isDeleted`.
- **[User.java](file:///d:/IT211/Me/src/main/java/atmin/entity/User.java) [Cập nhật]**: Bổ sung hai trường `phone_number` và `is_enabled` để tương thích với sơ đồ ERD, đồng thời sửa hàm `isEnabled()` kiểm tra động trạng thái kích hoạt tài khoản.
- **[BadmintonCluster.java](file:///d:/IT211/Me/src/main/java/atmin/entity/BadmintonCluster.java)**: Đại diện cho cụm sân cầu lông (Bảng `badminton_clusters`).
- **[Court.java](file:///d:/IT211/Me/src/main/java/atmin/entity/Court.java)**: Đại diện cho sân cầu lông chi tiết (Bảng `courts`).
- **[Booking.java](file:///d:/IT211/Me/src/main/java/atmin/entity/Booking.java)**: Đại diện cho giao dịch đặt lịch sân (Bảng `bookings`).
- **[TimeSlot.java](file:///d:/IT211/Me/src/main/java/atmin/entity/TimeSlot.java)**: Đại diện cho các khung giờ vận hành để tính toán giá động (Bảng `time_slots`).
- **[TokenBlacklist.java](file:///d:/IT211/Me/src/main/java/atmin/entity/TokenBlacklist.java)**: Lưu trữ các Token đã bị vô hiệu hóa sau khi người dùng nhấn Logout (Bảng `token_blacklists`).

---

### 2. Tầng Quản Lý Người Dùng Mới (`FR-05`)

Chức năng này giúp Admin (`ROLE_ADMIN`) thực hiện các nghiệp vụ CRUD, Tìm kiếm và Phân trang danh sách người dùng.

- **DTOs (`atmin.controller.user.dto`)**:
    - **[UserCreateRequest.java](file:///d:/IT211/Me/src/main/java/atmin/controller/user/dto/UserCreateRequest.java)**: Đóng gói và validate dữ liệu tạo mới người dùng.
    - **[UserUpdateRequest.java](file:///d:/IT211/Me/src/main/java/atmin/controller/user/dto/UserUpdateRequest.java)**: Đóng gói và validate dữ liệu cập nhật người dùng.
    - **[UserResponse.java](file:///d:/IT211/Me/src/main/java/atmin/controller/user/dto/UserResponse.java)**: Định dạng dữ liệu phản hồi an toàn cho Client (giấu mật khẩu, chuyển vai trò thành Set tên).
- **Repository (`atmin.repository`)**:
    - **[UserRepository.java](file:///d:/IT211/Me/src/main/java/atmin/repository/UserRepository.java) [Cập nhật]**: Thêm phương thức `@Query("searchUsers")` hỗ trợ phân trang lọc theo từ khóa.
- **Service (`atmin.service` & `atmin.service.Impl`)**:
    - **[IUserService.java](file:///d:/IT211/Me/src/main/java/atmin/service/IUserService.java)**: Giao diện nghiệp vụ quản lý người dùng.
    - **[UserServiceImpl.java](file:///d:/IT211/Me/src/main/java/atmin/service/Impl/UserServiceImpl.java)**: Triển khai các phương thức nghiệp vụ chi tiết, áp dụng Java Stream API để ánh xạ DTO (UC-02) và BCrypt mã hóa mật khẩu.
- **Controller & Security (`atmin.controller.user` & `atmin.infrastructure.security`)**:
    - **[UserController.java](file:///d:/IT211/Me/src/main/java/atmin/controller/user/UserController.java)**: Cung cấp API RESTful CRUD dưới đường dẫn `/api/v1/admin/users`.
    - **[SecurityConfig.java](file:///d:/IT211/Me/src/main/java/atmin/infrastructure/security/SecurityConfig.java) [Cập nhật]**: Ánh xạ phân quyền mới, bảo vệ `/api/v1/admin/**` chỉ cho phép vai trò `ADMIN` truy cập.

---

### 3. Ngoại Lệ & Xử Lý Lỗi Tải Ảnh (`atmin.common.exception`)

- **[CloudStorageException.java](file:///d:/IT211/Me/src/main/java/atmin/common/exception/CloudStorageException.java)**: Ngoại lệ ném ra khi dịch vụ Cloudinary gặp sự cố.
- **[GlobalExceptionHandler.java](file:///d:/IT211/Me/src/main/java/atmin/common/exception/GlobalExceptionHandler.java) [Cập nhật]**: Đã thêm bộ đánh chặn và trả về HTTP Status `503 Service Unavailable` cùng cấu trúc `ApiErrorResponse` chuẩn hóa.

---

## 📊 Chi Tiết Các Bảng Trong Cơ Sở Dữ Liệu (Database Schema Details)

Dưới đây là giải thích chi tiết mục đích sử dụng và các trường dữ liệu chính của từng bảng trong hệ thống, kết hợp giữa đặc tả SRS và lý thuyết thiết kế cơ sở dữ liệu thực tế:

### 1. Bảng `users` (Quản lý Người dùng)

- **Mục đích:** Lưu trữ thông tin tài khoản của toàn bộ hệ thống (Customer, Manager, Admin). Tích hợp trực tiếp với Spring Security phục vụ xác thực (Authentication).
- **Các trường chính:**
    - `id` (BIGINT, PK, Auto Increment): Mã định danh duy nhất của người dùng.
    - `username` (VARCHAR(50), Unique): Tên đăng nhập. Được đánh chỉ mục để tối ưu tốc độ kiểm tra lúc đăng nhập.
    - `password` (VARCHAR(100)): Mật khẩu đã được mã hóa bằng thuật toán BCrypt cường độ mạnh (không lưu dạng Plain Text).
    - `full_name` (VARCHAR(100)): Họ tên đầy đủ phục vụ ghi nhận hóa đơn.
    - `email` (VARCHAR(100), Unique): Email dùng cho các tính năng liên lạc, quên mật khẩu.
    - `phone_number` (VARCHAR(20)): Số điện thoại liên lạc.
    - `is_enabled` (BOOLEAN): Trạng thái tài khoản (Kích hoạt/Khóa).
- **Mối quan hệ:** Liên kết nhiều-nhiều (`N-N`) với bảng `roles` qua bảng trung gian `user_role`.

### 2. Bảng `roles` (Vai trò & Phân quyền)

- **Mục đích:** Lưu trữ các chức danh quyền hạn trong hệ thống để thực hiện phân quyền (Authorization) tại tầng Security.
- **Các trường chính:**
    - `id` (BIGINT, PK): Mã vai trò.
    - `role_name` (VARCHAR(10)): Tên vai trò theo chuẩn Spring Security (`ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_CUSTOMER`).
    - `description` (VARCHAR(255)): Mô tả quyền hạn vai trò.

### 3. Bảng `refresh_tokens` (Cơ chế Xoay vòng Token)

- **Mục đích:** Quản lý vòng đời Refresh Token (FR-02) nhằm cấp lại Access Token mới mà không yêu cầu người dùng nhập lại thông tin xác thực.
- **Các trường chính:**
    - `token` (VARCHAR(255), Unique): Chuỗi token ngẫu nhiên.
    - `expired_at` (DATETIME): Thời hạn sử dụng của Refresh Token.
    - `is_revoked` (BOOLEAN): Cờ đánh dấu đã thu hồi (dùng để phát hiện và phòng chống Reuse Attack).
    - `user_id` (BIGINT, FK): Liên kết đến người dùng sở hữu token.

### 4. Bảng `token_blacklists` (Danh sách đen Token)

- **Mục đích:** Hỗ trợ tính năng Đăng xuất (UC-03). Khi người dùng nhấn đăng xuất, Access Token hiện tại (kể cả khi còn hạn) sẽ bị lưu vào bảng này. Bộ lọc bảo mật sẽ chặn lập tức mọi Request mang token nằm trong Blacklist (trả về lỗi 403 Forbidden).
- **Các trường chính:**
    - `token` (TEXT): Lưu chuỗi Access Token bị thu hồi.
    - `expiry_time` (DATETIME): Thời gian hết hạn gốc của token. Phục vụ việc tự động dọn dẹp các bản ghi quá hạn để giải phóng bộ nhớ.
    - `user_id` (BIGINT, FK): Liên kết đến người dùng đã đăng xuất.

### 5. Bảng `badminton_clusters` (Cụm Sân Cầu Lông)

- **Mục đích:** Quản lý thông tin chuỗi cơ sở, các cụm chi nhánh sân cầu lông của hệ thống.
- **Các trường chính:**
    - `name` (VARCHAR(100)): Tên cụm sân chi nhánh.
    - `address` (VARCHAR(255)): Địa chỉ vật lý của cụm sân.
    - `hot_line` (VARCHAR(20)): Số điện thoại đường dây nóng hỗ trợ khách hàng.
    - `manager_id` (BIGINT, FK): Người quản lý chi nhánh (`User` có quyền `ROLE_MANAGER`).

### 6. Bảng `courts` (Sân Cầu Lông Chi Tiết)

- **Mục đích:** Lưu trữ các sân nhỏ cụ thể bên trong mỗi cụm sân.
- **Các trường chính:**
    - `court_name` (VARCHAR(50)): Số sân hoặc tên sân (Ví dụ: Sân số 1, Sân số 2).
    - `type` (VARCHAR(50)): Loại mặt sân (Thảm cao su, sàn gỗ...).
    - `image_url` (VARCHAR(255)): URL hình ảnh sân tải lên đám mây Cloudinary (FR-09, UC-05).
    - `is_available` (BOOLEAN): Trạng thái sân có thể sử dụng (đóng/mở cửa để sửa chữa hoặc cho thuê).
    - `cluster_id` (BIGINT, FK): Liên kết đến cụm sân trực thuộc.

### 7. Bảng `bookings` (Thông Tin Đặt Sân)

- **Mục đích:** Lưu trữ nghiệp vụ cốt lõi, nhật ký giao dịch thuê sân của khách hàng (FR-06, FR-07, FR-08).
- **Các trường chính:**
    - `booking_date` (DATE): Ngày khách chơi cầu lông.
    - `time_slot` (VARCHAR(50)): Khung giờ đã chọn đặt.
    - `total_price` (DECIMAL(10,2)): Tổng số tiền cần thanh toán cho ca chơi.
    - `status` (VARCHAR(20)): Trạng thái đơn đặt (`PENDING` - Chờ duyệt, `CONFIRMED` - Đã duyệt chơi, `CANCELLED` - Đã hủy).
    - `user_id` (BIGINT, FK): Trỏ đến khách hàng đặt sân.
    - `court_id` (BIGINT, FK): Trỏ đến sân được đặt.

### 8. Bảng `time_slots` (Quản Lý Khung Giờ & Giá Động)

- **Mục đích:** Định nghĩa các ca chơi cố định và hỗ trợ quản lý giá động qua hệ số nhân. Giờ cao điểm (ví dụ: ca tối) sẽ có hệ số giá nhân cao hơn giờ thấp điểm.
- **Các trường chính:**
    - `start_time` (TIME): Giờ bắt đầu ca chơi.
    - `end_time` (TIME): Giờ kết thúc ca chơi.
    - `price_factor` (DECIMAL(5,2)): Hệ số giá nhân (Ví dụ: Giờ vàng = 1.5, Giờ thường = 1.0).
    - `is_available` (BOOLEAN): Trạng thái mở/đóng nhận đặt của khung giờ này.

---

## 🛠️ Kết Quả Kiểm Tra Hệ Thống

Dự án đã được biên dịch thành công thông qua Gradle wrapper:

```bash
.\gradlew compileJava
```

**Kết quả:**

```text
> Task :compileJava
BUILD SUCCESSFUL in 10s
```

Hệ thống hoàn toàn sạch lỗi cú pháp, sẵn sàng phục vụ việc phát triển tiếp các tầng Service và Controller tương ứng cho các nghiệp vụ đặt sân và quản lý hình ảnh.
