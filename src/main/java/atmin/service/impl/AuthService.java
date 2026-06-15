package atmin.service.impl;

import atmin.common.exception.DuplicateResourceException;
import atmin.common.exception.ResourceNotFoundException;
import atmin.controller.auth.dto.request.LoginRequest;
import atmin.controller.auth.dto.request.RegisterRequest;
import atmin.controller.auth.dto.response.AuthResponse;
import atmin.entity.Role;
import atmin.entity.User;
import atmin.repository.redis.TokenBlacklistRepository;
import atmin.repository.redis.dto.RefreshTokenRedis;
import atmin.controller.auth.dto.request.ChangePasswordRequest;
import atmin.controller.auth.dto.request.ForgotPasswordRequest;
import atmin.controller.auth.dto.request.ResetPasswordRequest;
import atmin.service.IEmailService;
import java.util.UUID;
import java.util.Date;
import atmin.infrastructure.security.jwt.JwtProperties;
import atmin.infrastructure.security.jwt.JwtProvider;
import atmin.repository.redis.RefreshTokenRepository;
import atmin.repository.RoleRepository;
import atmin.repository.UserRepository;
import atmin.service.IAuthService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    public void register(RegisterRequest request) {
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
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        log.info("{} logged in successfully", request.getUsername());

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));

        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        // REDIS: Tìm và thu hồi (revoked = true) toàn bộ Refresh Token cũ của user này để ép đăng xuất các phiên cũ
        List<RefreshTokenRedis> activeTokens = refreshTokenRepository.findAllActiveByUser(user);
        if (activeTokens != null && !activeTokens.isEmpty()) {
            activeTokens.forEach(t -> t.setRevoked(true));
            refreshTokenRepository.saveAll(activeTokens);
        }

        long expiryDateTime = System.currentTimeMillis() + jwtProperties.getRefreshExpiration();

        // REDIS: Tạo mới và lưu Refresh Token vào Redis dưới dạng DTO kèm thời gian hết hạn TTL
        RefreshTokenRedis refreshEntity = RefreshTokenRedis
                .builder()
                .token(refreshToken)
                .username(user.getUsername())
                .expiredAt(expiryDateTime)
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshEntity);

        return new AuthResponse(accessToken, refreshToken);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        jwtProvider.validateRefreshToken(refreshToken);

        // REDIS: Tìm kiếm thông tin Refresh Token trên Redis
        RefreshTokenRedis tokenEntity = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));

        // REDIS: Phát hiện tấn công tái sử dụng (Reuse Attack)
        // Nếu Refresh Token này đã bị thu hồi trước đó (revoked = true), ta sẽ thu hồi toàn bộ token khác của user này
        if (tokenEntity.isRevoked()) {
            List<RefreshTokenRedis> activeTokens = refreshTokenRepository.findAllActiveByUsername(tokenEntity.getUsername());
            if (activeTokens != null && !activeTokens.isEmpty()) {
                activeTokens.forEach(t -> t.setRevoked(true));
                refreshTokenRepository.saveAll(activeTokens);
            }
            throw new JwtException("Refresh token has been revoked due to potential reuse attack");
        }

        User user = userRepository.findByUsername(tokenEntity.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String newAccessToken = jwtProvider.generateAccessToken(user);
        String newRefreshToken = jwtProvider.generateRefreshToken(user);

        // REDIS: Thu hồi token cũ (revoked = true) và cập nhật lại lên Redis
        tokenEntity.setRevoked(true);
        refreshTokenRepository.save(tokenEntity);

        long expiryDateTime = System.currentTimeMillis() + jwtProperties.getRefreshExpiration();

        // REDIS: Tạo mới và lưu Refresh Token mới vào Redis
        RefreshTokenRedis newRefreshEntity = RefreshTokenRedis.builder()
                .token(newRefreshToken)
                .username(user.getUsername())
                .expiredAt(expiryDateTime)
                .revoked(false)
                .build();
        refreshTokenRepository.save(newRefreshEntity);

        return new AuthResponse(newAccessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new JwtException("Full authentication is required to access this resource");
        }

        String token = authHeader.substring(7);
        jwtProvider.validateAccessToken(token);

        // REDIS: Lấy tên người dùng từ Access Token
        String username = jwtProvider.getUsernameFromToken(token);

        // REDIS: Thu hồi tất cả Refresh Token của người dùng này khi đăng xuất
        List<RefreshTokenRedis> activeTokens = refreshTokenRepository.findAllActiveByUsername(username);
        if (activeTokens != null && !activeTokens.isEmpty()) {
            activeTokens.forEach(t -> t.setRevoked(true));
            refreshTokenRepository.saveAll(activeTokens);
        }

        // REDIS: Lấy thời gian hết hạn còn lại của Access Token và đưa token đó vào blacklist trên Redis
        Date expirationDate = jwtProvider.getExpirationDateFromToken(token);
        long expiryDurationMs = expirationDate.getTime() - System.currentTimeMillis();
        tokenBlacklistRepository.blacklistToken(token, expiryDurationMs);
    }

    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Old password does not match!");
        }

        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new IllegalArgumentException("New password must be different from the old password!");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match!");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        // REDIS: Thu hồi toàn bộ Refresh Token của User này trên Redis để bắt họ đăng nhập lại trên mọi thiết bị
        List<RefreshTokenRedis> activeTokens = refreshTokenRepository.findAllActiveByUser(user);
        if (activeTokens != null && !activeTokens.isEmpty()) {
            activeTokens.forEach(t -> t.setRevoked(true));
            refreshTokenRepository.saveAll(activeTokens);
        }
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Email does not exist!"));

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        emailService.sendResetPasswordEmail(user.getEmail(), token);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
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
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        // REDIS: Thu hồi toàn bộ Refresh Token cũ của User trên Redis sau khi reset mật khẩu thành công
        List<RefreshTokenRedis> activeTokens = refreshTokenRepository.findAllActiveByUser(user);
        if (activeTokens != null && !activeTokens.isEmpty()) {
            activeTokens.forEach(t -> t.setRevoked(true));
            refreshTokenRepository.saveAll(activeTokens);
        }
    }
}
