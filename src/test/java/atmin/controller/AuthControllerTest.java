package atmin.controller;

import atmin.common.exception.GlobalExceptionHandler;
import atmin.controller.auth.AuthController;
import atmin.controller.auth.dto.response.AuthResponse;
import atmin.service.IAuthService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import atmin.infrastructure.security.jwt.JwtAuthenticationFilter;

/**
 * Unit Test cho lớp AuthController.
 * <p>
 * Sử dụng @WebMvcTest để chỉ khởi tạo lớp Controller được chỉ định (AuthController),
 * KHÔNG khởi tạo toàn bộ Spring Context (nhanh hơn @SpringBootTest).
 * <p>
 * excludeAutoConfiguration = SecurityAutoConfiguration.class:
 *   Tắt Spring Security tự động để test controller mà không cần xác thực JWT.
 *   Mục đích: tập trung kiểm tra logic xử lý request/response của controller,
 *   không kiểm tra cơ chế bảo mật (đó là việc của integration test).
 *
 * @AutoConfigureMockMvc(addFilters = false):
 *   Tắt tất cả các Security Filter (bao gồm JwtAuthenticationFilter).
 *
 * @MockitoBean: Giả lập IAuthService - dependency duy nhất của AuthController.
 *   Controller test KHÔNG kiểm tra logic nghiệp vụ (đã có Service test),
 *   mà chỉ kiểm tra: HTTP method, URL mapping, status code, cấu trúc JSON response.
 */
@WebMvcTest(
        controllers = {AuthController.class, GlobalExceptionHandler.class},
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    // MockMvc: công cụ giả lập HTTP request mà không cần khởi động server thực.
    // Cho phép gọi API và kiểm tra response (status, body, header) trong test.
    @Autowired
    private MockMvc mockMvc;

    // ObjectMapper: chuyển đổi Java object <-> JSON string.
    // Dùng để tạo request body dạng JSON từ Map/Object.
    @Autowired
    private ObjectMapper objectMapper;

    // Mock service: tất cả logic nghiệp vụ đều được giả lập.
    // Controller chỉ là lớp "cầu nối" giữa HTTP request và Service.
    @MockitoBean
    private IAuthService authService;

    // ==================== TEST 6 ====================
    /**
     * Kịch bản: Gọi API đăng ký với dữ liệu hợp lệ, trả về HTTP 201 Created.
     * <p>
     * Mô tả luồng:
     * 1. Client gửi POST /api/v1/auth/register với body JSON hợp lệ.
     * 2. Spring Validation kiểm tra @Valid → PASS (tất cả trường đều hợp lệ).
     * 3. Controller gọi authService.register(request) → mock doNothing (thành công).
     * 4. Controller trả về HTTP 201 + ApiResponse { success: true, message: "..." }.
     * <p>
     * Kiểm tra:
     * - HTTP status = 201 (Created)
     * - JSON field "success" = true
     * - JSON field "message" = "User registered successfully!"
     */
    @Test
    @DisplayName("TC6 - POST /register với dữ liệu hợp lệ trả về 201 Created")
    void register_Success_Returns201() throws Exception {
        // ARRANGE: Giả lập service register() không ném exception
        doNothing().when(authService).register(any());

        // Tạo request body JSON hợp lệ (đáp ứng tất cả @NotBlank, @Size, @Email, @Pattern)
        Map<String, String> requestBody = Map.of(
                "username", "newuser",
                "password", "password123",
                "email", "new@email.com",
                "fullName", "New User",
                "phoneNumber", "0123456789"
        );

        // ACT & ASSERT: Gửi POST request và kiểm tra response
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)        // Header: Content-Type = application/json
                        .content(objectMapper.writeValueAsString(requestBody)))  // Body: JSON string
                .andExpect(status().isCreated())                         // HTTP 201
                .andExpect(jsonPath("$.success").value(true))            // JSON: success = true
                .andExpect(jsonPath("$.message").value("User registered successfully!"));
    }

    // ==================== TEST 7 ====================
    /**
     * Kịch bản: Gọi API đăng ký với body THIẾU trường bắt buộc, trả về HTTP 400.
     * <p>
     * Mô tả luồng:
     * 1. Client gửi POST /api/v1/auth/register với body JSON rỗng ({}).
     * 2. Spring Validation kiểm tra @Valid → FAIL (username, password, email đều blank).
     * 3. GlobalExceptionHandler bắt MethodArgumentNotValidException → trả 400.
     * 4. authService.register() KHÔNG BAO GIỜ được gọi (validation chặn trước).
     * <p>
     * Kiểm tra:
     * - HTTP status = 400 (Bad Request)
     * - JSON chứa thông tin lỗi validation
     * <p>
     * Ý nghĩa: Đảm bảo lớp Controller + Validation chặn đầu vào sai TRƯỚC KHI
     *           logic nghiệp vụ trong Service được thực thi.
     */
    @Test
    @DisplayName("TC7 - POST /register với body rỗng trả về 400 Bad Request (validation)")
    void register_InvalidBody_Returns400() throws Exception {
        // ACT & ASSERT: Gửi body rỗng
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))     // Body rỗng: tất cả @NotBlank sẽ fail
                .andExpect(status().isBadRequest());    // HTTP 400
    }

    // ==================== TEST 8 ====================
    /**
     * Kịch bản: Gọi API đăng nhập thành công, trả về HTTP 200 + token.
     * <p>
     * Mô tả luồng:
     * 1. Client gửi POST /api/v1/auth/login với username và password.
     * 2. Controller gọi authService.login(request).
     * 3. Service trả về AuthResponse chứa accessToken và refreshToken.
     * 4. Controller bọc trong ApiResponse và trả về HTTP 200.
     * <p>
     * Kiểm tra:
     * - HTTP status = 200 (OK)
     * - JSON chứa data.accessToken và data.refreshToken đúng giá trị mock.
     */
    @Test
    @DisplayName("TC8 - POST /login thành công trả về 200 OK kèm access + refresh token")
    void login_Success_Returns200() throws Exception {
        // ARRANGE: Giả lập service trả về token
        AuthResponse mockResponse = new AuthResponse("mock_access_token", "mock_refresh_token");
        when(authService.login(any())).thenReturn(mockResponse);

        Map<String, String> requestBody = Map.of(
                "username", "testuser",
                "password", "password123"
        );

        // ACT & ASSERT
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())                                              // HTTP 200
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("mock_access_token"))     // Token đúng
                .andExpect(jsonPath("$.data.refreshToken").value("mock_refresh_token"));
    }
}
