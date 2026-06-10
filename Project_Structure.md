# 📂 KIẾN TRÚC DỰ ÁN: HYBRID LAYERED WITH CONTROLLER-FEATURE DTO

Hệ thống được tổ chức theo mô hình **Kiến trúc Lai (Hybrid)** tối ưu: Luồng nghiệp vụ cốt lõi tuân thủ nghiêm ngặt mô hình phân tầng tuyến tính (`Controller` ➔ `Service` ➔ `Repository`) theo đúng yêu cầu đặc tả của đề tài. Tuy nhiên, để tối đa hóa khả năng bảo trì và tính đóng gói, các đối tượng vận chuyển dữ liệu (DTO) được đưa về quản lý cục bộ (Local DTO) theo từng cụm tính năng bên trong tầng Controller.

---

## 📈 Sơ Đồ Cấu Trúc Thư Mục Tổng Thể

```text
atmin/ (hoặc com.rikkei/)
│
├── config/                          # 1. CẤU HÌNH KHỞI TẠO HỆ THỐNG
│   ├── AppConfig.java               # Cấu hình khởi tạo Bean dùng chung (ModelMapper, ObjectMappers,...)
│   └── DataSourceConfig.java        # Cấu hình Hibernate, SessionFactory & Quản lý Transaction
│
├── infrastructure/                  # 2. CỤM TÍNH NĂNG HẠ TẦNG ĐỘC LẬP
│   │
│   ├── security/                    # 🔐 BỘ LÕI BẢO MẬT KHÔNG TRẠNG THÁI (STATELESS)
│   │   ├── WebSecurityConfig.java   # Cấu hình bộ lọc SecurityFilterChain & Ma trận phân quyền URL
│   │   ├── CustomUserDetailsService.java # Tải dữ liệu định danh người dùng từ Database phục vụ Security
│   │   └── jwt/
│   │       ├── JwtAuthFilter.java   # Filter đánh chặn Request check Token hợp lệ & đối chiếu Blacklist
│   │       ├── JwtProperties.java   # Class nạp Key cấu hình bằng Lombok từ application.properties
│   │       └── JwtProvider.java     # Thư viện đúc mã hóa/giải mã và trích xuất Claims từ Token
│   │
│   └── upload/                      # ☁️ HẠ TẦNG LƯU TRỮ ĐÁM MÂY (INTEGRATION SDK)
│       ├── CloudinaryService.java   # Interface định nghĩa cổng truyền nhận dữ liệu tệp tin
│       └── CloudinaryServiceImpl.java # Triển khai SDK Cloudinary truyền tải luồng dữ liệu (File Stream)
│
├── controller/                      # 3. TẦNG GIAO TIẾP RESTful API (GOM CỤM FEATURE + LOCAL DTO)
│   │
│   ├── auth/                        # 🔑 Feature: Xác thực hệ thống (FR-01, FR-02, FR-03)
│   │   ├── AuthController.java
│   │   └── dto/                     # DTO đóng gói riêng cho luồng Auth
│   │       ├── LoginRequest.java
│   │       ├── RegisterRequest.java
│   │       └── JwtResponse.java
│   │
│   ├── user/                        # 👤 Feature: Quản lý thành viên (FR-05, FR-10)
│   │   ├── UserController.java
│   │   └── dto/                     # DTO đóng gói riêng cho luồng User
│   │       ├── UserUpdateRequest.java
│   │       └── UserProfileResponse.java
│   │
│   ├── court/                       # 🏸 Feature: Quản lý Sân cầu lông (FR-09, UC-05)
│   │   ├── CourtController.java
│   │   └── dto/                     # DTO đóng gói riêng cho luồng Sân
│   │       ├── CourtCreateRequest.java
│   │       └── CourtDetailResponse.java
│   │
│   └── booking/                     # 📅 Feature: Đặt lịch & Phê duyệt (FR-06, FR-07, FR-08)
│       ├── BookingController.java
│       └── dto/                     # DTO đóng gói riêng cho luồng Đặt lịch
│           ├── BookingRequest.java
│           └── BookingHistoryResponse.java
│
├── service/                         # 4. TẦNG XỬ LÝ NGHIỆP VỤ (LOGIC LÕI)
│   ├── impl/                        # Hiện thực hóa chi tiết logic nghiệp vụ hệ thống
│   │   ├── AuthServiceImpl.java     # Logic trích xuất cấu trúc token, tính toán thời gian đưa vào Blacklist
│   │   ├── UserServiceImpl.java     # Logic xử lý tài khoản, áp dụng mã hóa một chiều qua BCrypt
│   │   ├── CourtServiceImpl.java    # Nghiệp vụ sân bãi - Inject CloudinaryService từ hạ tầng để lấy URL ảnh
│   │   └── BookingServiceImpl.java  # [UC-02] Tính toán giá động, kiểm toán trùng lịch qua Java Stream API
│   │
│   ├── AuthService.java             # Hệ thống các Interface nghiệp vụ độc lập
│   ├── UserService.java
│   ├── CourtService.java
│   └── BookingService.java
│
├── repository/                      # 5. TẦNG TRUY VẤN CƠ SỞ DỮ LIỆU (SPRING DATA JPA)
│   ├── UserRepository.java          # Thao tác dữ liệu bảng tài khoản người dùng
│   ├── RoleRepository.java          # Thao tác dữ liệu bảng vai trò (ADMIN, MANAGER, CUSTOMER)
│   ├── CourtClusterRepository.java  # Thao tác cụm sân cầu lông
│   ├── CourtRepository.java         # Thao tác thông tin chi tiết từng sân
│   ├── TimeSlotRepository.java      # Quản lý khung giờ và price_factor phục vụ giá động
│   ├── BookingRepository.java       # Kiểm soát vòng đời đơn đặt lịch (PENDING, CONFIRMED, CANCELLED)
│   └── TokenBlacklistRepository.java # Truy vấn danh sách đen Token phục vụ luồng bảo mật chặn Logout
│
├── entity/                          # 6. TẦNG THỰC THỂ MAPPING DATABASE (HIBERNATE ORM)
│   ├── BaseEntity.java              # Lớp cha trừu tượng [@MappedSuperclass] chứa Audit Trail & Soft Delete
│   ├── User.java                    # Thực thể Người dùng [Implements UserDetails] chứa 4 chốt chặn bảo mật
│   ├── Role.java                    # Thực thể lưu trữ vai trò người dùng
│   ├── CourtCluster.java            # Thực thể cụm sân cầu lông
│   ├── Court.java                   # Thực thể sân (Gồm trường image_url nhận link tĩnh từ đám mây)
│   ├── TimeSlot.java                # Thực thể lưu khung giờ vận hành của chuỗi sân
│   ├── Booking.java                 # Thực thể Đặt lịch (Theo dõi biến động State: PENDING -> CONFIRMED)
│   └── TokenBlacklist.java          # Thực thể lưu chuỗi Token hết hiệu lực sau khi Logout (UC-03)
│
└── common/                          # 7. THÀNH PHẦN HỖ TRỢ TOÀN CỤC (AOP & EXCEPTION)
    ├── aspect/
    │   └── LoggingAspect.java       # [UC-04] Tự động bắt sự kiện đặt sân qua AOP để ghi nhật ký tự động
    ├── exception/
    │   └── GlobalExceptionHandler.java # [@RestControllerAdvice] Bắt exception tập trung, trả mã lỗi sạch 400, 503
    └── response/
        ├── ApiResponse.java         # Định dạng mẫu JSON thành công chuẩn { success, message, data }
        └── ApiErrorResponse.java    # Định dạng mẫu JSON báo lỗi chuẩn { timestamp, status, error, message, path }