package atmin.service;

import atmin.controller.auth.dto.request.LoginRequest;
import atmin.controller.auth.dto.request.RegisterRequest;
import atmin.controller.auth.dto.response.AuthResponse;
import atmin.controller.auth.dto.request.ChangePasswordRequest;
import atmin.controller.auth.dto.request.ForgotPasswordRequest;
import atmin.controller.auth.dto.request.ResetPasswordRequest;

public interface IAuthService {
    void register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(String refreshToken);
    void logout(String authHeader);
    void changePassword(String username, ChangePasswordRequest request);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}