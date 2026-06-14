package atmin.service;

import atmin.common.exception.DuplicateResourceException;
import atmin.controller.auth.dto.request.RegisterRequest;
import atmin.entity.Role;
import atmin.entity.User;
import atmin.repository.redis.TokenBlacklistRepository;
import atmin.repository.RoleRepository;
import atmin.repository.UserRepository;
import atmin.service.Impl.AuthService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho lớp AuthService.
 * <p>
 * Sử dụng @ExtendWith(MockitoExtension.class) để khởi tạo các Mock object tự động,
 * KHÔNG cần khởi động Spring Context hay kết nối database/Redis thực.
 * Tất cả dependency đều được giả lập (mock) để kiểm tra logic nghiệp vụ thuần túy.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // @Mock: Tạo đối tượng giả lập cho mỗi dependency mà AuthService cần inject.
    // Các đối tượng mock này sẽ trả về giá trị mặc định (null, false, 0, ...) trừ khi
    // ta cấu hình hành vi cụ thể bằng when(...).thenReturn(...).
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenBlacklistRepository tokenBlacklistRepository;

    // @InjectMocks: Tự động inject tất cả các @Mock ở trên vào AuthService,
    // tương đương với việc Spring inject dependency qua constructor.
    @InjectMocks
    private AuthService authService;

    // ==================== TEST 1 ====================
    /**
     * Kịch bản: Đăng ký tài khoản THÀNH CÔNG.
     * <p>
     * Điều kiện tiên quyết:
     * - Username "newuser" chưa tồn tại trong hệ thống.
     * - Email "new@email.com" chưa tồn tại.
     * - Số điện thoại "0123456789" chưa tồn tại.
     * - Role ROLE_CUSTOMER tồn tại trong database.
     * <p>
     * Kỳ vọng:
     * - Phương thức register() chạy mà KHÔNG ném ra bất kỳ exception nào.
     * - Phương thức userRepository.save() được gọi đúng 1 lần để lưu user mới.
     */
    @Test
    @DisplayName("TC1 - Đăng ký thành công khi thông tin hợp lệ và chưa tồn tại")
    void register_Success() {
        // ARRANGE (Chuẩn bị dữ liệu đầu vào và cấu hình hành vi mock)
        RegisterRequest request = new RegisterRequest(
                "newuser", "password123", "new@email.com", "New User", "0123456789");
        Role roleCustomer = Role.builder().id(1L).name("ROLE_CUSTOMER").build();

        // Giả lập: tất cả kiểm tra trùng lặp đều trả về false (chưa tồn tại)
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@email.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("0123456789")).thenReturn(false);
        // Giả lập: tìm thấy Role ROLE_CUSTOMER
        when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(Optional.of(roleCustomer));
        // Giả lập: mã hóa password
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");

        // ACT (Thực thi phương thức cần test)
        // assertDoesNotThrow: đảm bảo register() KHÔNG ném exception
        assertDoesNotThrow(() -> authService.register(request));

        // ASSERT (Kiểm tra kết quả)
        // verify: xác nhận rằng userRepository.save() đã được gọi đúng 1 lần
        verify(userRepository, times(1)).save(any(User.class));
    }

    // ==================== TEST 2 ====================
    /**
     * Kịch bản: Đăng ký THẤT BẠI vì username đã tồn tại.
     * <p>
     * Điều kiện tiên quyết:
     * - Username "existingUser" đã có trong cơ sở dữ liệu.
     * <p>
     * Kỳ vọng:
     * - Hệ thống ném ra DuplicateResourceException với thông báo "Username already exists!".
     * - Phương thức userRepository.save() KHÔNG BAO GIỜ được gọi (vì đã dừng sớm).
     */
    @Test
    @DisplayName("TC2 - Đăng ký thất bại khi username đã tồn tại (ném DuplicateResourceException)")
    void register_DuplicateUsername_ThrowsException() {
        // ARRANGE
        RegisterRequest request = new RegisterRequest(
                "existingUser", "password123", "new@email.com", "User", "0123456789");

        // Giả lập: username "existingUser" ĐÃ tồn tại
        when(userRepository.existsByUsername("existingUser")).thenReturn(true);

        // ACT & ASSERT
        // assertThrows: đảm bảo hệ thống ném đúng loại exception
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> authService.register(request)
        );

        // Kiểm tra nội dung thông báo lỗi
        assertEquals("Username already exists!", exception.getMessage());
        // Đảm bảo save() KHÔNG được gọi vì logic đã dừng sớm
        verify(userRepository, never()).save(any(User.class));
    }

    // ==================== TEST 3 ====================
    /**
     * Kịch bản: Đăng xuất THẤT BẠI vì header Authorization không hợp lệ.
     * <p>
     * Điều kiện tiên quyết:
     * - Header gửi lên là "InvalidHeader" (không có tiền tố "Bearer ").
     * <p>
     * Kỳ vọng:
     * - Hệ thống ném ra JwtException vì không thể trích xuất token từ header.
     * - Không có bất kỳ thao tác nào trên blacklist hay refresh token repository.
     */
    @Test
    @DisplayName("TC3 - Đăng xuất thất bại khi header Authorization không có tiền tố 'Bearer '")
    void logout_InvalidHeader_ThrowsException() {
        // ACT & ASSERT
        JwtException exception = assertThrows(
                JwtException.class,
                () -> authService.logout("InvalidHeader")
        );

        assertEquals("Full authentication is required to access this resource", exception.getMessage());
        // Đảm bảo không có thao tác nào trên blacklist (vì chưa qua được bước validate)
        verify(tokenBlacklistRepository, never()).blacklistToken(anyString(), anyLong());
    }
}
