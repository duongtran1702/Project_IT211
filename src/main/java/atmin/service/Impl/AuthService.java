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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

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

    @Override
    public ResponseEntity<ApiResponse<Void>> register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists!");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists!");
        }

        Role roleUser = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() ->
                        new ResourceNotFoundException("ROLE_USER not found"));

        User newUser = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .fullName(request.getFullName())
                .roles(Set.of(roleUser))
                .build();
        userRepository.save(newUser);

        ApiResponse<Void> response = ApiResponse.success("User registered successfully!");
        return new ResponseEntity<>(response, HttpStatus.OK);
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

        RefreshToken newRefreshEntity = RefreshToken.builder()
                .token(newRefreshToken)
                .user(user)
                .expiredAt(LocalDateTime.now().plusNanos(jwtProperties.getRefreshExpiration() * 1_000_000L))
                .isRevoked(false)
                .build();
        refreshTokenRepository.save(newRefreshEntity);

        ApiResponse<AuthResponse> response = ApiResponse.success("Refresh successfully!",
                new AuthResponse(newAccessToken, newRefreshToken));

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
