package atmin.service;

import atmin.common.response.ApiResponse;
import atmin.controller.auth.dto.request.LoginRequest;
import atmin.controller.auth.dto.request.RegisterRequest;
import atmin.controller.auth.dto.response.AuthResponse;
import org.springframework.http.ResponseEntity;

public interface IAuthService {
    ResponseEntity<ApiResponse<Void>> register(RegisterRequest request);
    ResponseEntity<ApiResponse<AuthResponse>> login(LoginRequest request);
    ResponseEntity<ApiResponse<AuthResponse>> refreshToken(String refreshToken);
    ResponseEntity<ApiResponse<Void>> logout(String authHeader);
}