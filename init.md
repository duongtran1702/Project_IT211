# Hướng Dẫn Khởi Tạo Dữ Liệu Mẫu (Database Initialization)

Tài liệu này cung cấp các câu lệnh SQL để khởi tạo dữ liệu mẫu cho hệ thống đặt sân cầu lông. Bản SQL này được tối ưu hóa cho cơ sở dữ liệu **MySQL** (`project_it211_me`).

> [!IMPORTANT]
> - Mật khẩu mẫu mặc định cho tất cả các tài khoản (`admin`, `manager_test`, `customer_test`) sau khi mã hóa BCrypt là: **`password123`**
> - Hãy chạy các câu lệnh SQL này theo đúng thứ tự để tránh lỗi ràng buộc khóa ngoại (Foreign Key Constraint).

---

## 📝 Script SQL Khởi Tạo Dữ Liệu

```sql
-- Sử dụng cơ sở dữ liệu của dự án
USE `project_it211_me`;

-- =========================================================================
-- 1. KHỞI TẠO BẢNG VAI TRÒ (roles)
-- =========================================================================
INSERT INTO `roles` (`id`, `role_name`, `description`) VALUES
(1, 'ROLE_ADMIN', 'Quản trị viên hệ thống có toàn quyền quản trị'),
(2, 'ROLE_MANAGER', 'Quản lý các cụm sân cầu lông và lịch đặt'),
(3, 'ROLE_CUSTOMER', 'Khách hàng đăng nhập hệ thống để đặt sân');

-- =========================================================================
-- 2. KHỞI TẠO TÀI KHOẢN NGƯỜI DÙNG (users)
-- BCrypt băm của 'password123' là: $2a$10$8.UnVuG9HHgffUDAlk8GP.3nSXZ4d4E24p.K/Wj5xU9zM2fIeOq2O
-- =========================================================================
INSERT INTO `users` (`id`, `username`, `password`, `full_name`, `email`, `phone_number`, `is_enabled`, `created_at`) VALUES
(1, 'admin', '$2a$10$8.UnVuG9HHgffUDAlk8GP.3nSXZ4d4E24p.K/Wj5xU9zM2fIeOq2O', 'System Administrator', 'admin@example.com', '0912345678', 1, NOW()),
(2, 'manager_test', '$2a$10$8.UnVuG9HHgffUDAlk8GP.3nSXZ4d4E24p.K/Wj5xU9zM2fIeOq2O', 'Nguyen Van Manager', 'manager@example.com', '0987654321', 1, NOW()),
(3, 'customer_test', '$2a$10$8.UnVuG9HHgffUDAlk8GP.3nSXZ4d4E24p.K/Wj5xU9zM2fIeOq2O', 'Tran Tri Customer', 'customer@example.com', '0901234567', 1, NOW());

-- =========================================================================
-- 3. GÁN VAI TRÒ CHO TÀI KHOẢN (user_role)
-- =========================================================================
INSERT INTO `user_role` (`user_id`, `role_id`) VALUES
(1, 1), -- admin gán ROLE_ADMIN
(2, 2), -- manager_test gán ROLE_MANAGER
(3, 3); -- customer_test gán ROLE_CUSTOMER

-- =========================================================================
-- 4. KHỞI TẠO CỤM SÂN CẦU LÔNG (badminton_clusters)
-- =========================================================================
INSERT INTO `badminton_clusters` (`id`, `name`, `address`, `hot_line`, `manager_id`, `is_deleted`, `created_at`) VALUES
(1, 'Sân Cầu Lông Bình Thạnh', '123 Lê Văn Việt, Quận 9, TP.HCM', '0281234567', 2, 0, NOW()),
(2, 'Sân Cầu Lông Thủ Đức', '456 Võ Văn Ngân, Thủ Đức, TP.HCM', '0287654321', 2, 0, NOW());

-- =========================================================================
-- 5. KHỞI TẠO CÁC SÂN CẦU LÔNG CHI TIẾT (courts)
-- =========================================================================
INSERT INTO `courts` (`id`, `court_name`, `type`, `image_url`, `is_available`, `cluster_id`, `is_deleted`, `created_at`) VALUES
(1, 'Sân Thảm A1', 'Sân Thảm PVC', 'https://res.cloudinary.com/demo/image/upload/court_a1.jpg', 1, 1, 0, NOW()),
(2, 'Sân Thảm A2', 'Sân Thảm PVC', 'https://res.cloudinary.com/demo/image/upload/court_a2.jpg', 1, 1, 0, NOW()),
(3, 'Sân Gỗ B1', 'Sân Gỗ Sồi', 'https://res.cloudinary.com/demo/image/upload/court_b1.jpg', 1, 2, 0, NOW()),
(4, 'Sân Thảm B2', 'Sân Thảm PVC', 'https://res.cloudinary.com/demo/image/upload/court_b2.jpg', 1, 2, 0, NOW());

-- =========================================================================
-- 6. KHỞI TẠO KHUNG GIỜ MẪU (time_slots)
-- =========================================================================
INSERT INTO `time_slots` (`id`, `start_time`, `end_time`, `price_factor`, `is_available`, `is_deleted`, `created_at`) VALUES
(1, '05:00:00', '07:00:00', 1.00, 1, 0, NOW()), -- Giờ thấp điểm hệ số 1.0
(2, '07:00:00', '09:00:00', 1.20, 1, 0, NOW()), -- Giờ thường hệ số 1.2
(3, '17:00:00', '19:00:00', 1.50, 1, 0, NOW()), -- Giờ cao điểm hệ số 1.5
(4, '19:00:00', '21:00:00', 1.50, 1, 0, NOW()); -- Giờ cao điểm hệ số 1.5

-- =========================================================================
-- 7. KHỞI TẠO CÁC LƯỢT ĐẶT LỊCH MẪU (bookings)
-- =========================================================================
INSERT INTO `bookings` (`id`, `booking_date`, `time_slot`, `total_price`, `status`, `user_id`, `court_id`, `is_deleted`, `created_at`) VALUES
(1, '2026-06-12', '07:00 - 09:00', 120000.00, 'PAID', 3, 1, 0, NOW()),
(2, '2026-06-12', '17:00 - 19:00', 150000.00, 'PENDING', 3, 3, 0, NOW());
```

---

## 💡 Hướng Dẫn Thực Thi

Bạn có thể áp dụng script SQL trên qua 2 cách phổ biến sau:

### Cách 1: Sử dụng Công Cụ Quản Trị CSDL (MySQL Workbench, DBeaver, Navicat...)
1. Kết nối với MySQL server cục bộ của bạn.
2. Mở một Tab SQL Query mới.
3. Sao chép toàn bộ nội dung script trên dán vào tab query.
4. Nhấn nút **Execute** (hoặc tổ hợp phím `Ctrl + Shift + Enter`) để thực thi.

### Cách 2: Thực thi qua Command Line Interface (CLI)
Mở cửa sổ Command Prompt hoặc PowerShell và chạy câu lệnh sau:
```bash
mysql -u root -p project_it211_me < init.sql
```
*(Thay thế `init.sql` bằng file chứa script SQL trên).*
