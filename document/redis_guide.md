# Hướng Dẫn Sử Dụng Redis Qua Command Line (CMD)

Tài liệu này hướng dẫn chi tiết cách khởi động Redis, kết nối CLI và các câu lệnh hữu ích nhất để bạn theo dõi, kiểm thử và gỡ lỗi (debug) danh sách token của dự án trực tiếp qua cửa sổ Command Prompt (CMD).

---

## 🏁 1. Cách Khởi Động Redis

### Bước 1: Mở Server (Duy trì dịch vụ)
Mở một cửa sổ CMD và gõ lệnh sau để khởi động Redis Server:
```bash
c:\redis\redis-server.exe
```
> [!IMPORTANT]
> Cửa sổ này phải được giữ mở trong suốt quá trình chạy dự án. Nếu tắt cửa sổ này, Spring Boot sẽ ném lỗi không thể kết nối tới Redis.

### Bước 2: Mở Client (Để nhập lệnh kiểm tra)
Mở thêm một cửa sổ CMD thứ hai và chạy lệnh sau để truy cập vào giao diện tương tác:
```bash
c:\redis\redis-cli.exe
```
Khi màn hình hiển thị `127.0.0.1:6379>` là bạn đã kết nối thành công và có thể bắt đầu gõ các lệnh dưới đây.

---

## 🔍 2. Các Lệnh Redis Cơ Bản Để Kiểm Tra Token Dự Án

Dưới đây là các câu lệnh trực quan được ánh xạ trực tiếp từ code Java của dự án:

### 2.1. Liệt kê tất cả các Key đang lưu trên Redis
Lệnh này giúp bạn xem nhanh có những token nào đang được lưu trữ:
```bash
KEYS *
```
*Kết quả mẫu:*
```text
1) "refresh_token:eyJhbGciOiJIUzI1Ni..."
2) "user_refresh_tokens:customer_test"
3) "blacklist:token:eyJhbGciOiJIUzI1Ni..."
```

### 2.2. Kiểm tra chi tiết thông tin Refresh Token (Kiểu dữ liệu String)
Do Refresh Token được lưu dưới dạng chuỗi JSON, bạn dùng lệnh `GET` để xem chi tiết:
```bash
GET refresh_token:<chuỗi_token_của_bạn>
```
*Kết quả mẫu:*
```json
"{\"token\":\"eyJhbG...\",\"username\":\"customer_test\",\"expiredAt\":\"2026-06-15T19:54:43\",\"revoked\":false}"
```

### 2.3. Xem danh sách token hoạt động của User (Kiểu dữ liệu Set)
Để xem tập hợp các token đang active của một người dùng, sử dụng lệnh `SMEMBERS` (Show Members):
```bash
SMEMBERS user_refresh_tokens:<tên_đăng_nhập>
```
*Ví dụ:*
```bash
SMEMBERS user_refresh_tokens:customer_test
```
*Kết quả mẫu:* (Trả về danh sách các chuỗi token đang hoạt động của user đó)
```text
1) "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpX..."
2) "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpX..."
```

### 2.4. Kiểm tra Access Token bị chặn (Blacklist)
Khi người dùng Logout, Access Token sẽ bị lưu vào danh sách đen. Sử dụng `GET` để kiểm tra:
```bash
GET blacklist:token:<chuỗi_access_token>
```
*Kết quả mẫu:* (Nếu trả về `"blacklisted"` nghĩa là token đã đăng xuất và bị chặn đứng ở Filter).
```text
"blacklisted"
```

---

## ⏳ 3. Quản Lý Thời Gian Sống (TTL) & Xóa Dữ Liệu

### 3.1. Kiểm tra thời gian còn lại của Token (TTL)
Muốn biết sau bao nhiêu giây nữa token đó sẽ tự động biến mất khỏi Redis:
```bash
TTL <tên_key>
```
*Ví dụ:*
```bash
TTL refresh_token:eyJhbGciOiJIUzI1Ni...
```
*Ý nghĩa kết quả:*
- Trả về một **số dương** (ví dụ: `86350`): Số giây còn lại trước khi key bị xóa.
- Trả về `-1`: Key không có thời hạn hết hạn (không bao giờ hết hạn).
- Trả về `-2`: Key không tồn tại (đã bị xóa hoặc hết hạn rồi).

### 3.2. Xóa thủ công một Key
Nếu muốn ép buộc thu hồi hoặc xóa bỏ ngay lập tức một token:
```bash
DEL <tên_key>
```
*Ví dụ:*
```bash
DEL refresh_token:eyJhbGciOiJIUzI1Ni...
```

### 3.3. Xóa sạch toàn bộ dữ liệu trên Redis (Reset hoàn toàn)
Khi bạn muốn xóa sạch mọi dữ liệu, session, blacklist trên Redis để test lại từ đầu:
```bash
FLUSHALL
```
> [!WARNING]
> Lệnh này sẽ xóa sạch 100% dữ liệu đang lưu trong bộ nhớ tạm của Redis. Hãy cẩn thận khi gõ lệnh này.

---

## 💡 4. Mẹo Nhỏ Khi Dùng CMD
- **Tự động gợi ý lệnh**: Khi gõ lệnh trên `redis-cli`, bạn có thể nhấn phím `TAB` để Redis tự động điền nốt tên lệnh hoặc tên key.
- **Thoát khỏi CLI**: Gõ lệnh `exit` hoặc `quit` (hoặc nhấn tổ hợp phím `Ctrl + C`) để thoát khỏi màn hình nhập lệnh `redis-cli`.
- **Dọn màn hình CLI**: Gõ lệnh `clear` để làm sạch màn hình CMD cho dễ nhìn.
