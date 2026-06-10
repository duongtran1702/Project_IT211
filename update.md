# Nhật Ký Cập Nhật Hệ Thống (Update Log)

Tài liệu này ghi nhận các thực thể JPA mới, ngoại lệ tùy chỉnh liên quan đến Cloudinary, và tính năng Quản lý người dùng vừa được tích hợp vào dự án dựa trên sơ đồ ERD và tài liệu đặc tả yêu cầu hệ thống (SRS).

---

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
