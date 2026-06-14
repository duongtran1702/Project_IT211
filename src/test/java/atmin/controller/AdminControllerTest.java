package atmin.controller;

import atmin.common.exception.GlobalExceptionHandler;
import atmin.common.exception.ResourceNotFoundException;
import atmin.controller.admin.AdminController;
import atmin.controller.admin.dto.response.UserResponse;
import atmin.service.IAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import atmin.infrastructure.security.jwt.JwtAuthenticationFilter;

/**
 * Unit Test cho lớp AdminController.
 *
 * @WebMvcTest chỉ khởi tạo AdminController + GlobalExceptionHandler.
 * Security bị tắt hoàn toàn để test mà không cần mock JWT/Authentication.
 * <p>
 * Mục tiêu test:
 * - Kiểm tra URL mapping (@GetMapping, @PathVariable) hoạt động đúng.
 * - Kiểm tra HTTP status code trả về chính xác (200, 404).
 * - Kiểm tra cấu trúc JSON response đúng format ApiResponse.
 * - Kiểm tra GlobalExceptionHandler xử lý exception đúng cách.
 */
@WebMvcTest(
        controllers = {AdminController.class, GlobalExceptionHandler.class},
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Mock service: giả lập toàn bộ logic nghiệp vụ admin
    @MockitoBean
    private IAdminService adminService;

    // ==================== TEST 9 ====================
    /**
     * Kịch bản: Gọi API lấy thông tin user theo ID THÀNH CÔNG.
     * <p>
     * Mô tả luồng:
     * 1. Client gửi GET /api/v1/admin/users/1.
     * 2. Spring MVC parse @PathVariable id = 1.
     * 3. Controller gọi adminService.getUserById(1L).
     * 4. Service trả về UserResponse (mock).
     * 5. Controller bọc trong ApiResponse và trả về HTTP 200.
     * <p>
     * Kiểm tra:
     * - HTTP status = 200 (OK)
     * - JSON body chứa đúng thông tin user: username, email, fullName
     * - JSON field "success" = true
     */
    @Test
    @DisplayName("TC9 - GET /admin/users/1 trả về 200 OK với thông tin user đúng")
    void getUserById_Success_Returns200() throws Exception {
        // ARRANGE: Tạo UserResponse mock
        UserResponse mockUser = UserResponse.builder()
                .id(1L)
                .username("admin")
                .fullName("Admin User")
                .email("admin@email.com")
                .phoneNumber("0987654321")
                .roles(Set.of("ROLE_ADMIN"))
                .build();

        when(adminService.getUserById(1L)).thenReturn(mockUser);

        // ACT & ASSERT
        mockMvc.perform(get("/api/v1/admin/users/1"))
                .andExpect(status().isOk())                                     // HTTP 200
                .andExpect(jsonPath("$.success").value(true))                    // success = true
                .andExpect(jsonPath("$.data.username").value("admin"))           // username đúng
                .andExpect(jsonPath("$.data.email").value("admin@email.com"))    // email đúng
                .andExpect(jsonPath("$.data.fullName").value("Admin User"));     // fullName đúng
    }

    // ==================== TEST 10 ====================
    /**
     * Kịch bản: Gọi API lấy user theo ID nhưng user KHÔNG TỒN TẠI → HTTP 404.
     * <p>
     * Mô tả luồng:
     * 1. Client gửi GET /api/v1/admin/users/999.
     * 2. Controller gọi adminService.getUserById(999L).
     * 3. Service ném ResourceNotFoundException("User not found with id: 999").
     * 4. GlobalExceptionHandler bắt exception và trả về HTTP 404.
     * <p>
     * Kiểm tra:
     * - HTTP status = 404 (Not Found)
     * - JSON chứa thông báo lỗi "User not found with id: 999"
     * <p>
     * Ý nghĩa: Test này kiểm tra sự phối hợp giữa Controller, Service (mock),
     *           và GlobalExceptionHandler. Đảm bảo exception được xử lý đúng cách
     *           và client nhận được thông báo lỗi có ý nghĩa, không phải stacktrace.
     */
    @Test
    @DisplayName("TC10 - GET /admin/users/999 trả về 404 Not Found khi user không tồn tại")
    void getUserById_NotFound_Returns404() throws Exception {
        // ARRANGE: Giả lập service ném ResourceNotFoundException
        when(adminService.getUserById(999L))
                .thenThrow(new ResourceNotFoundException("User not found with id: 999"));

        // ACT & ASSERT
        mockMvc.perform(get("/api/v1/admin/users/999"))
                .andExpect(status().isNotFound())                                           // HTTP 404
                .andExpect(jsonPath("$.message").value("User not found with id: 999"));      // Thông báo lỗi
    }
}
