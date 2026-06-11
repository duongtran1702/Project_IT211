package atmin.controller.auth;

import atmin.common.response.ApiResponse;
import atmin.controller.auth.dto.request.LoginRequest;
import atmin.controller.auth.dto.request.RefreshTokenRequest;
import atmin.controller.auth.dto.request.RegisterRequest;
import atmin.controller.auth.dto.response.AuthResponse;
import atmin.service.IAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final IAuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refreshToken(request.getRefreshToken());
    }
}
