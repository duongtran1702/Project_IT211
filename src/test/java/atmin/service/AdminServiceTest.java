package atmin.service;

import atmin.common.exception.ResourceNotFoundException;
import atmin.controller.admin.dto.response.UserResponse;
import atmin.entity.Role;
import atmin.entity.User;
import atmin.repository.UserRepository;
import atmin.service.impl.AdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho lớp AdminService.
 * <p>
 * Kiểm tra logic nghiệp vụ quản lý người dùng của Admin:
 * - Tìm kiếm user theo ID (thành công và thất bại).
 * <p>
 * Kỹ thuật sử dụng:
 * - @Mock: Giả lập UserRepository, RoleRepository, PasswordEncoder.
 * - @InjectMocks: Tự động inject các mock vào AdminService.
 * - when().thenReturn(): Cấu hình giá trị trả về cho mock.
 * - assertThrows(): Kiểm tra exception được ném ra.
 */
import atmin.controller.admin.dto.request.UserUpdateRequest;
import atmin.repository.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminService adminService;

    /**
     * Tạo một đối tượng User mẫu dùng chung cho các test case.
     * Phương thức helper này giúp tránh lặp code khi tạo dữ liệu test.
     */
    private User createSampleUser() {
        Role role = Role.builder().id(1L).name("ROLE_CUSTOMER").build();
        return User.builder()
                .id(1L)
                .username("testuser")
                .fullName("Test User")
                .email("test@email.com")
                .phoneNumber("0123456789")
                .roles(Set.of(role))
                .build();
    }

    // ==================== TEST 4 ====================
    /**
     * Kịch bản: Lấy thông tin user theo ID THÀNH CÔNG.
     * <p>
     * Điều kiện tiên quyết:
     * - User với ID = 1 tồn tại trong database.
     * <p>
     * Kỳ vọng:
     * - Trả về đối tượng UserResponse có đầy đủ thông tin (username, email, roles...).
     * - Dữ liệu trả về khớp chính xác với dữ liệu trong database.
     */
    @Test
    @DisplayName("TC4 - Lấy user theo ID thành công khi user tồn tại")
    void getUserById_Success() {
        // ARRANGE
        User user = createSampleUser();
        // Giả lập: findById(1L) trả về user mẫu
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // ACT
        UserResponse response = adminService.getUserById(1L);

        // ASSERT
        // Kiểm tra dữ liệu trả về khớp với dữ liệu mẫu
        assertNotNull(response, "Response không được null");
        assertEquals("testuser", response.getUsername());
        assertEquals("test@email.com", response.getEmail());
        assertEquals("Test User", response.getFullName());
        // Kiểm tra role được ánh xạ đúng từ entity sang DTO
        assertTrue(response.getRoles().contains("ROLE_CUSTOMER"));

        // Xác nhận findById() đã được gọi đúng 1 lần với đúng tham số
        verify(userRepository, times(1)).findById(1L);
    }

    // ==================== TEST 5 ====================
    /**
     * Kịch bản: Lấy thông tin user theo ID THẤT BẠI vì user không tồn tại.
     * <p>
     * Điều kiện tiên quyết:
     * - Không có user nào với ID = 999 trong database.
     * <p>
     * Kỳ vọng:
     * - Hệ thống ném ra ResourceNotFoundException với thông báo chứa ID.
     * - Đây là hành vi bảo vệ quan trọng: đảm bảo hệ thống không trả về null
     *   mà luôn báo lỗi rõ ràng khi tài nguyên không tồn tại.
     */
    @Test
    @DisplayName("TC5 - Lấy user theo ID thất bại khi user không tồn tại (ném ResourceNotFoundException)")
    void getUserById_NotFound_ThrowsException() {
        // ARRANGE
        // Giả lập: findById(999L) trả về Optional.empty() (không tìm thấy)
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> adminService.getUserById(999L)
        );

        // Kiểm tra thông báo lỗi chứa ID để dễ debug
        assertEquals("User not found with id: 999", exception.getMessage());
    }

    // ==================== TEST 6 ====================
    /**
     * Kịch bản: Cập nhật thông tin user thành công khi isEnabled là null.
     * <p>
     * Kỳ vọng:
     * - Trạng thái enabled của user không bị thay đổi (giữ nguyên giá trị cũ).
     * - Không xảy ra NullPointerException khi unboxing.
     */
    @Test
    @DisplayName("TC6 - Cập nhật thông tin user thành công khi isEnabled là null")
    void updateUser_NullIsEnabled_Success() {
        // ARRANGE
        User user = createSampleUser();
        user.setEnabled(true);

        UserUpdateRequest request = UserUpdateRequest.builder()
                .fullName("Updated Name")
                .email("updated@email.com")
                .phoneNumber("0123456789")
                .isEnabled(null)
                .roleIds(null)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // ACT
        UserResponse response = adminService.updateUser(1L, request);

        // ASSERT
        assertNotNull(response);
        assertEquals("Updated Name", response.getFullName());
        assertEquals("updated@email.com", response.getEmail());
        assertTrue(user.isEnabled()); // Remains true, not changed to false or crashed with NPE

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(user);
    }
}
