package atmin.service;

import atmin.common.response.ApiResponse;
import atmin.controller.auth.dto.request.LoginRequest;
import atmin.controller.auth.dto.request.RegisterRequest;
import atmin.controller.auth.dto.response.AuthResponse;
import atmin.controller.auth.dto.request.ChangePasswordRequest;
import atmin.controller.auth.dto.request.ForgotPasswordRequest;
import atmin.controller.auth.dto.request.ResetPasswordRequest;
import org.springframework.http.ResponseEntity;

public interface IAuthService {
    ResponseEntity<ApiResponse<Void>> register(RegisterRequest request);
    ResponseEntity<ApiResponse<AuthResponse>> login(LoginRequest request);
    ResponseEntity<ApiResponse<AuthResponse>> refreshToken(String refreshToken);
    ResponseEntity<ApiResponse<Void>> logout(String authHeader);
    ResponseEntity<ApiResponse<Void>> changePassword(String username, ChangePasswordRequest request);
    ResponseEntity<ApiResponse<Void>> forgotPassword(ForgotPasswordRequest request);
    ResponseEntity<ApiResponse<Void>> resetPassword(ResetPasswordRequest request);
}