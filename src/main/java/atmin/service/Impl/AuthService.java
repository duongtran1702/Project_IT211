package atmin.service.Impl;

import atmin.common.exception.DuplicateResourceException;
import atmin.common.exception.ResourceNotFoundException;
import atmin.common.response.ApiResponse;
import atmin.controller.auth.dto.request.LoginRequest;
import atmin.controller.auth.dto.request.RegisterRequest;
import atmin.controller.auth.dto.response.AuthResponse;
import atmin.entity.RefreshToken;
import atmin.entity.Role;
import atmin.entity.User;
import atmin.entity.TokenBlacklist;
import atmin.repository.TokenBlacklistRepository;
import atmin.controller.auth.dto.request.ChangePasswordRequest;
import atmin.controller.auth.dto.request.ForgotPasswordRequest;
import atmin.controller.auth.dto.request.ResetPasswordRequest;
import atmin.service.IEmailService;
import java.util.UUID;
import java.time.ZoneId;
import java.util.Date;
import atmin.infrastructure.security.jwt.JwtProperties;
import atmin.infrastructure.security.jwt.JwtProvider;
import atmin.repository.RefreshTokenRepository;
import atmin.repository.RoleRepository;
import atmin.repository.UserRepository;
import atmin.service.IAuthService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements IAuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final IEmailService emailService;

    @Override
    public ResponseEntity<ApiResponse<Void>> register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists!");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists!");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new DuplicateResourceException("Phone number already exists!");
        }

        Role roleUser = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() ->
                        new ResourceNotFoundException("ROLE_CUSTOMER not found"));

        User newUser = request.toEntity(passwordEncoder.encode(request.getPassword()), roleUser);
        userRepository.save(newUser);

        ApiResponse<Void> response = ApiResponse.success("User registered successfully!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<ApiResponse<AuthResponse>> login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        log.info("{} logged in successfully", request.getUsername());

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));

        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        List<RefreshToken> activeTokens = refreshTokenRepository.findAllActiveByUser(user);
        if (activeTokens != null && !activeTokens.isEmpty()) {
            activeTokens.forEach(t -> t.setRevoked(true));
            refreshTokenRepository.saveAll(activeTokens);
        }

        LocalDateTime expiryDateTime = LocalDateTime.now()
                .plus(jwtProperties.getRefreshExpiration(), ChronoUnit.MILLIS);

        RefreshToken refreshEntity = RefreshToken.builder()
                .token(refreshToken)
                .user(user)
                .expiredAt(expiryDateTime)
                .isRevoked(false)
                .build();
        refreshTokenRepository.save(refreshEntity);

        ApiResponse<AuthResponse> response = ApiResponse.success("Login successfully!",
                new AuthResponse(accessToken, refreshToken));

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(String refreshToken) {

        jwtProvider.validateRefreshToken(refreshToken);

        RefreshToken tokenEntity = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));

        if (tokenEntity.isRevoked()) {
            List<RefreshToken> activeTokens = refreshTokenRepository.findAllActiveByUser(tokenEntity.getUser());
            if (activeTokens != null && !activeTokens.isEmpty()) {
                activeTokens.forEach(t -> t.setRevoked(true));
                refreshTokenRepository.saveAll(activeTokens);
            }
            throw new JwtException("Refresh token has been revoked due to potential reuse attack");
        }

        User user = tokenEntity.getUser();
        String newAccessToken = jwtProvider.generateAccessToken(user);
        String newRefreshToken = jwtProvider.generateRefreshToken(user);

        // Thu hồi token cũ và lưu token mới
        tokenEntity.setRevoked(true);
        refreshTokenRepository.save(tokenEntity);

        // Tránh bị tràn
        LocalDateTime expiryDateTime = LocalDateTime.now()
                .plus(jwtProperties.getRefreshExpiration(), ChronoUnit.MILLIS);

        RefreshToken newRefreshEntity = RefreshToken.builder()
                .token(newRefreshToken)
                .user(user)
                .expiredAt(expiryDateTime)
                .isRevoked(false)
                .build();
        refreshTokenRepository.save(newRefreshEntity);

        ApiResponse<AuthResponse> response = ApiResponse.success("Refresh successfully!",
                new AuthResponse(newAccessToken, newRefreshToken));

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Void>> logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new JwtException("Full authentication is required to access this resource");
        }

        String token = authHeader.substring(7);
        jwtProvider.validateAccessToken(token);

        String username = jwtProvider.getUsernameFromToken(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for this token: " + username));

        Date expirationDate = jwtProvider.getExpirationDateFromToken(token);
        LocalDateTime expiryTime = expirationDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        TokenBlacklist blacklist = TokenBlacklist.builder()
                .token(token)
                .expiryTime(expiryTime)
                .user(user)
                .build();
        tokenBlacklistRepository.save(blacklist);

        return new ResponseEntity<>(ApiResponse.success("Logout successfully!"), HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Void>> changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Old password does not match!");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match!");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Thu hồi tất cả Refresh Token cũ của User để bắt đăng nhập lại
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllActiveByUser(user);
        if (activeTokens != null && !activeTokens.isEmpty()) {
            activeTokens.forEach(t -> t.setRevoked(true));
            refreshTokenRepository.saveAll(activeTokens);
        }

        return new ResponseEntity<>(ApiResponse.success("Password changed successfully!"), HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Void>> forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Email does not exist!"));

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        emailService.sendResetPasswordEmail(user.getEmail(), token);

        ApiResponse<Void> response = ApiResponse.success("Password reset email sent successfully. Please check your inbox.");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<Void>> resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token!"));

        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invalid or expired reset token!");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match!");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        // Thu hồi tất cả Refresh Token cũ
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllActiveByUser(user);
        if (activeTokens != null && !activeTokens.isEmpty()) {
            activeTokens.forEach(t -> t.setRevoked(true));
            refreshTokenRepository.saveAll(activeTokens);
        }

        return new ResponseEntity<>(ApiResponse.success("Password reset successfully!"), HttpStatus.OK);
    }
}
