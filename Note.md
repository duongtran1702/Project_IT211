### Ý nghĩa 4 hàm trạng thái tài khoản trong UserDetails (Spring Security)

Cả 4 hàm này đều là các **chốt chặn bảo mật (Security Checkpoints)** dùng để kiểm tra trạng thái hoạt động của tài khoản ngay tại thời điểm người dùng thực hiện đăng nhập.

1. **`isAccountNonExpired()`**
    * **Ý nghĩa:** Tài khoản còn hạn sử dụng hay không?
    * **Thực tế:** Thường áp dụng cho các mô hình kinh doanh bán tài khoản Premium, tài khoản dùng thử có thời hạn (ví dụ: gói học tập/xem phim 30 ngày). Khi hết hạn, hàm trả về `false` và hệ thống sẽ chặn đăng nhập công khai.

2. **`isAccountNonLocked()`**
    * **Ý nghĩa:** Tài khoản có đang an toàn (không bị khóa) hay không?
    * **Thực tế:** Dùng khi Người quản trị (Admin) thực hiện vô hiệu hóa (ban) tài khoản của người dùng vi phạm tiêu chuẩn, hoặc hệ thống tự động khóa tạm thời khi phát hiện có hành vi dò đoán mật khẩu (nhập sai quá 5 lần liên tiếp).

3. **`isCredentialsNonExpired()`**
    * **Ý nghĩa:** Mật khẩu (thông tin xác thực) còn trong hạn an toàn không?
    * **Thực tế:** Thường thấy ở môi trường doanh nghiệp hoặc ngân hàng đòi hỏi tính bảo mật cao, bắt buộc nhân viên phải thay đổi mật khẩu định kỳ sau mỗi 3 tháng. Nếu quá hạn, hệ thống sẽ điều hướng thẳng tới trang yêu cầu đổi mật khẩu mới.

4. **`isEnabled()`**
    * **Ý nghĩa:** Tài khoản đã được kích hoạt hoàn toàn để sẵn sàng sử dụng chưa?
    * **Thực tế:** Dùng trong luồng đăng ký thành viên mới. Khi vừa tạo tài khoản thành công, trạng thái sẽ là `false` (chờ kích hoạt). Người dùng bắt buộc phải click vào đường link xác thực được gửi qua Email hoặc nhập mã OTP thì trạng thái mới chuyển sang `true`.

---

### ⚠️ Lưu ý đặc biệt khi lập trình (Best Practice)
* Khi thiết kế cơ sở dữ liệu (Database) ban đầu, nếu bạn **chưa bổ sung các trường dữ liệu** tương ứng để quản lý các trạng thái này (như `is_locked`, `is_enabled`,...), bạn **bắt buộc phải cấu hình cố định `return true;`** cho cả 4 hàm.
* Nếu vô tình cấu hình nhầm bất kỳ hàm nào thành `return false;`, Spring Security sẽ mặc định hiểu rằng toàn bộ tài khoản trên hệ thống đã bị vô hiệu hóa/bị khóa và **ngăn chặn tất cả các lượt đăng nhập hợp lệ**.

---

### Vai trò của Annotation `@NullMarked`
* Thẻ `@NullMarked` đặt ở phạm vi hàm đóng vai trò như một bản cam kết kỹ thuật chặt chẽ với trình biên dịch Java.
* Nó khẳng định rằng các hàm kiểm tra trạng thái này bắt buộc phải trả về giá trị logic rõ ràng (`true` hoặc `false`), **tuyệt đối không được phép trả về giá trị trống (`null`)** trong mọi tình huống vận hành, giúp hệ thống phòng ngừa hoàn toàn các lỗi sập luồng (NullPointerException).

#### Check trùng phone
#### Dùng token cũ vẫn được

---

### 📌 UC-02: Quản trị danh mục và Xử lý dữ liệu nâng cao (Báo cáo doanh thu & Tổng hợp khung giờ) - CẦN LÀM SAU
- **Tác nhân**: `ROLE_ADMIN` hoặc `ROLE_MANAGER`.
- **Nghiệp vụ**:
  - GET danh sách tổng hợp các khung giờ đặt sân hoặc báo cáo doanh thu theo tháng.
  - Hỗ trợ các bộ lọc (ví dụ: `date=2026-06-08`, `status=CONFIRMED`).
- **Yêu cầu kỹ thuật bắt buộc**:
  - Tại tầng Service, tuyệt đối không dùng vòng lặp `for` hay `while` truyền thống.
  - Phải áp dụng Java Stream API: `.stream()`, `.filter()`, `.map()`, và `.collect(Collectors.toList())`.
  - Trả về danh sách DTO sạch (tránh liên kết đệ quy vô hạn và loại bỏ các trường thừa).
---
### 📌 Sao upload ảnh lại cho customer thay vì admin