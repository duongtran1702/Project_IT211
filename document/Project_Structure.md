# 📂 KIẾN TRÚC DỰ ÁN: HYBRID LAYERED WITH CONTROLLER-FEATURE DTO

Hệ thống được tổ chức theo mô hình **Kiến trúc Lai (Hybrid)** tối ưu: Luồng nghiệp vụ cốt lõi tuân thủ nghiêm ngặt mô hình phân tầng tuyến tính (`Controller` ➔ `Service` ➔ `Repository`) theo đúng yêu cầu đặc tả của đề tài. Tuy nhiên, để tối đa hóa khả năng bảo trì và tính đóng gói, các đối tượng vận chuyển dữ liệu (DTO) được đưa về quản lý cục bộ (Local DTO) theo từng cụm tính năng bên trong tầng Controller.

---

## 📈 Sơ Đồ Cấu Trúc Thư Mục Thực Tế

```text
src/main/java/atmin/
│
├── infrastructure/                  # 1. CỤM TÍNH NĂNG HẠ TẦNG ĐỘC LẬP
│   │
│   ├── security/                    # 🔐 BỘ LÕI BẢO MẬT KHÔNG TRẠNG THÁI (STATELESS)
│   │   ├── SecurityConfig.java      # Cấu hình bộ lọc SecurityFilterChain & Ma trận phân quyền URL
│   │   ├── SecurityExceptionConfig.java # Cấu hình xử lý lỗi bảo mật: AuthenticationEntryPoint (401) & AccessDeniedHandler (403)
│   │   ├── principal/
│   │   │   └── UserDetailServiceCustom.java # Tải dữ liệu định danh người dùng từ Database phục vụ Security
│   │   └── jwt/
│   │       ├── JwtAuthenticationFilter.java # Filter đánh chặn Request check Token hợp lệ & đối chiếu Blacklist
│   │       ├── JwtProperties.java   # Class nạp Key cấu hình bằng Lombok từ application.properties
│   │       └── JwtProvider.java     # Thư viện đúc mã hóa/giải mã và trích xuất Claims từ Token
│   │
│   ├── upload/                      # ☁️ HẠ TẦNG LƯU TRỮ ĐÁM MÂY (INTEGRATION SDK CLOUDINARY)
│   │   ├── UploadService.java       # Tầng Service xử lý upload tệp tin lên Cloudinary có validate định dạng/dung lượng
│   │   ├── CloudinaryProperties.java # Class nạp Key cấu hình Cloudinary từ application.properties
│   │   └── CloudinaryConfig.java    # Khởi tạo bean Cloudinary kết nối Cloud Storage
│   │
│   └── scheduler/                   # ⏰ TÁC VỤ CHẠY ĐỊNH KỲ TỰ ĐỘNG
│       └── CleanupScheduler.java    # Quét dọn tự động Refresh Token hết hạn định kỳ lúc 00:00 hàng ngày
│
├── controller/                      # 2. TẦNG GIAO TIẾP RESTful API (GOM CỤM FEATURE + LOCAL DTO)
│   │
│   ├── auth/                        # 🔑 Feature: Xác thực hệ thống (FR-01, FR-02, FR-03, FR-10)
│   │   ├── AuthController.java      # Các API Đăng ký, Đăng nhập, Làm mới Token, Đăng xuất, Đổi/Quên mật khẩu
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── LoginRequest.java
│   │       │   ├── RegisterRequest.java
│   │       │   ├── RefreshTokenRequest.java
│   │       │   ├── ChangePasswordRequest.java
│   │       │   ├── ForgotPasswordRequest.java
│   │       │   └── ResetPasswordRequest.java
│   │       └── response/
│   │           └── AuthResponse.java
│   │
│   ├── admin/                       # 👤 Feature: Quản lý thành viên dành cho Admin (FR-05)
│   │   ├── AdminController.java     # CRUD, Tìm kiếm, Phân trang danh sách tài khoản
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── UserCreateRequest.java
│   │       │   └── UserUpdateRequest.java
│   │       └── response/
│   │           └── UserResponse.java
│   │
│   ├── booking/                     # 📅 Feature: Đặt lịch & Phê duyệt đơn (FR-06, FR-07, FR-08)
│   │   ├── BookingController.java   # API dành cho Customer: Đặt lịch chơi, xem Lịch sử đặt sân cá nhân
│   │   ├── BookingManagerController.java # API dành cho Manager/Admin: Xem danh sách đặt sân, Phê duyệt/Từ chối lịch
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── BookingRequest.java
│   │       │   └── BookingStatusUpdateRequest.java
│   │       └── response/
│   │           └── BookingResponse.java
│   │
│   └── file/                        # 🖼️ Feature: Tải lên hình ảnh hệ thống (FR-09)
│       └── UploadController.java    # API dành cho Manager: Upload ảnh chung, Cập nhật ảnh đại diện sân cầu lông
│
├── service/                         # 3. TẦNG XỬ LÝ NGHIỆP VỤ (LOGIC LÕI)
│   │
│   ├── Impl/                        # Hiện thực hóa chi tiết logic nghiệp vụ hệ thống (Chữ I viết hoa)
│   │   ├── AdminService.java        # Xử lý CRUD người dùng, mã hóa BCrypt khi tạo tài khoản
│   │   ├── AuthService.java         # Xử lý xác thực, cấp phát/xoay vòng token, đưa token vào blacklist khi logout
│   │   ├── BookingService.java      # Xử lý tạo đặt sân, chống trùng lịch, tính toán giá động, cập nhật trạng thái
│   │   └── EmailService.java        # Dịch vụ gửi email JavaMailSender chứa mã thông báo đặt lại mật khẩu
│   │
│   ├── IAdminService.java           # Hệ thống các Interface nghiệp vụ độc lập
│   ├── IAuthService.java
│   ├── IBookingService.java
│   └── IEmailService.java
│
├── repository/                      # 4. TẦNG TRUY VẤN CƠ SỞ DỮ LIỆU (SPRING DATA JPA)
│   ├── UserRepository.java          # Thao tác dữ liệu bảng users
│   ├── RoleRepository.java          # Thao tác dữ liệu bảng roles (ADMIN, MANAGER, CUSTOMER)
│   ├── RefreshTokenRepository.java  # Quản lý thời hạn và trạng thái thu hồi của Refresh Token
│   ├── TokenBlacklistRepository.java # Truy vấn danh sách đen Access Token đã bị vô hiệu hóa
│   ├── CourtRepository.java         # Thao tác thông tin chi tiết từng sân cầu lông
│   ├── TimeSlotRepository.java      # Quản lý khung giờ hoạt động và price_factor
│   └── BookingRepository.java       # Theo dõi và quản lý vòng đời đơn đặt lịch
│
├── entity/                          # 5. TẦNG THỰC THỂ MAPPING DATABASE (JPA / HIBERNATE ORM)
│   ├── BaseEntity.java              # Lớp cha trừu tượng [@MappedSuperclass] chứa Audit Trail & Soft Delete
│   ├── User.java                    # Thực thể Người dùng [Implements UserDetails] tích hợp Security
│   ├── Role.java                    # Thực thể vai trò (Role)
│   ├── RefreshToken.java            # Thực thể lưu vết Refresh Token để xoay vòng token bảo mật
│   ├── TokenBlacklist.java          # Thực thể lưu Access Token bị vô hiệu hóa sau khi đăng xuất
│   ├── BadmintonCluster.java        # Thực thể cụm sân cầu lông (Chi nhánh)
│   ├── Court.java                   # Thực thể sân cầu lông (Chứa URL hình ảnh lưu trên Cloudinary)
│   ├── TimeSlot.java                # Thực thể lưu khung giờ vận hành và hệ số giá
│   └── Booking.java                 # Thực thể đơn đặt sân của khách hàng
│
└── common/                          # 6. THÀNH PHẦN HỖ TRỢ TOÀN CỤC (AOP & EXCEPTION)
    ├── aspect/
    │   └── LoggingAspect.java       # Ghi nhận log hoạt động hệ thống tự động qua Aspect Oriented Programming
    ├── exception/
    │   ├── GlobalExceptionHandler.java # [@RestControllerAdvice] Bắt exception tập trung, trả mã lỗi sạch 400, 403, 503
    │   ├── ResourceNotFoundException.java # Ngoại lệ khi không tìm thấy tài nguyên (User, Booking, Court...)
    │   ├── DuplicateResourceException.java # Ngoại lệ khi dữ liệu bị xung đột hoặc trùng lặp (Username, Email...)
    │   └── CloudStorageException.java # Ngoại lệ khi kết nối hoặc truyền dữ liệu Cloudinary thất bại
    └── response/
        ├── ApiResponse.java         # Định dạng JSON phản hồi thành công chuẩn { success, message, data }
        └── ApiErrorResponse.java    # Định dạng JSON phản hồi lỗi chuẩn { timestamp, status, error, message, path }
```