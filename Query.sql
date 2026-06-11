-- Sử dụng cơ sở dữ liệu của dự án
# drop database project_it211_me;
USE `project_it211_me`;

-- =========================================================================
-- 1. KHỞI TẠO BẢNG VAI TRÒ (roles)
-- =========================================================================
INSERT INTO `roles` (`id`, `role_name`, `description`)
VALUES (1, 'ROLE_ADMIN', 'Quản trị viên hệ thống có toàn quyền quản trị'),
       (2, 'ROLE_MANAGER', 'Quản lý các cụm sân cầu lông và lịch đặt'),
       (3, 'ROLE_CUSTOMER', 'Khách hàng đăng nhập hệ thống để đặt sân');

-- =========================================================================
-- 2. KHỞI TẠO TÀI KHOẢN NGƯỜI DÙNG (users)
-- =========================================================================
INSERT INTO `users` (`id`, `username`, `password`, `full_name`, `email`, `phone_number`, `is_enabled`, `created_at`, `updated_at`, `is_deleted`)
VALUES (1, 'admin', '$2a$10$8.UnVuG9HHgffUDAlk8GP.3nSXZ4d4E24p.K/Wj5xU9zM2fIeOq2O', 'System Administrator',
        'admin@example.com', '0912345678', true, NOW(), NULL, false),
       (2, 'manager_test', '$2a$10$8.UnVuG9HHgffUDAlk8GP.3nSXZ4d4E24p.K/Wj5xU9zM2fIeOq2O', 'Nguyen Van Manager',
        'manager@example.com', '0987654321', true, NOW(), NULL, false),
       (3, 'customer_test', '$2a$10$8.UnVuG9HHgffUDAlk8GP.3nSXZ4d4E24p.K/Wj5xU9zM2fIeOq2O', 'Tran Tri Customer',
        'customer@example.com', '0901234567', true, NOW(), NULL, false);

-- =========================================================================
-- 3. GÁN VAI TRÒ CHO TÀI KHOẢN (user_role)
-- =========================================================================
INSERT INTO `user_role` (`user_id`, `role_id`)
VALUES (1, 1), -- admin gán ROLE_ADMIN
       (2, 2), -- manager_test gán ROLE_MANAGER
       (3, 3); -- customer_test gán ROLE_CUSTOMER

-- =========================================================================
-- 4. KHỞI TẠO CỤM SÂN CẦU LÔNG (badminton_clusters)
-- =========================================================================
INSERT INTO `badminton_clusters` (`id`, `name`, `address`, `hot_line`, `manager_id`, `created_at`, `updated_at`, `is_deleted`)
VALUES (1, 'Sân Cầu Lông Bình Thạnh', '123 Lê Văn Việt, Quận 9, TP.HCM', '0281234567', 2, NOW(), NULL, false),
       (2, 'Sân Cầu Lông Thủ Đức', '456 Võ Văn Ngân, Thủ Đức, TP.HCM', '0287654321', 2, NOW(), NULL, false);

-- =========================================================================
-- 5. KHỞI TẠO CÁC SÂN CẦU LÔNG CHI TIẾT (courts)
-- =========================================================================
INSERT INTO `courts` (`id`, `court_name`, `type`, `image_url`, `is_available`, `cluster_id`, `created_at`, `updated_at`, `is_deleted`)
VALUES (1, 'Sân Thảm A1', 'Sân Thảm PVC', 'https://res.cloudinary.com/demo/image/upload/court_a1.jpg', true, 1, NOW(), NULL, false),
       (2, 'Sân Thảm A2', 'Sân Thảm PVC', 'https://res.cloudinary.com/demo/image/upload/court_a2.jpg', true, 1, NOW(), NULL, false),
       (3, 'Sân Gỗ B1', 'Sân Gỗ Sồi', 'https://res.cloudinary.com/demo/image/upload/court_b1.jpg', true, 2, NOW(), NULL, false),
       (4, 'Sân Thảm B2', 'Sân Thảm PVC', 'https://res.cloudinary.com/demo/image/upload/court_b2.jpg', true, 2, NOW(), NULL, false);

-- =========================================================================
-- 6. KHỔI TẠO KHUNG GIỜ MẪU (time_slots)
-- =========================================================================
INSERT INTO `time_slots` (`id`, `start_time`, `end_time`, `price_factor`, `is_available`, `created_at`, `updated_at`, `is_deleted`)
VALUES (1, '05:00:00', '07:00:00', 1.00, true, NOW(), NULL, false), -- Giờ thấp điểm hệ số 1.0
       (2, '07:00:00', '09:00:00', 1.20, true, NOW(), NULL, false), -- Giờ thường hệ số 1.2
       (3, '17:00:00', '19:00:00', 1.50, true, NOW(), NULL, false), -- Giờ cao điểm hệ số 1.5
       (4, '19:00:00', '21:00:00', 1.50, true, NOW(), NULL, false); -- Giờ cao điểm hệ số 1.5

-- =========================================================================
-- 7. KHỞI TẠO CÁC LƯỢT ĐẶT LỊCH MẪU (bookings)
-- =========================================================================
INSERT INTO `bookings` (`id`, `booking_date`, `time_slot`, `total_price`, `status`, `user_id`, `court_id`, `created_at`, `updated_at`, `is_deleted`)
VALUES (1, '2026-06-12', '07:00 - 09:00', 120000.00, 'PAID', 3, 1, NOW(), NULL, false),
       (2, '2026-06-12', '17:00 - 19:00', 150000.00, 'PENDING', 3, 3, NOW(), NULL, false);