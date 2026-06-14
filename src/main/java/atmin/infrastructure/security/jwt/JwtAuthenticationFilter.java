package atmin.infrastructure.security.jwt;

import atmin.common.response.ApiErrorResponse;
import atmin.repository.UserRepository;
import atmin.repository.redis.TokenBlacklistRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws IOException, ServletException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (tokenBlacklistRepository.existsByToken(token)) {
                ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.FORBIDDEN.value())
                        .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                        .message("Access Denied: Token is blacklisted")
                        .path(request.getRequestURI())
                        .build();

                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                return;
            }
            try {
                jwtProvider.validateAccessToken(token);
                String username = jwtProvider.getUsernameFromToken(token);

                // Kiểm duyệt xem mật khẩu của user có bị đổi sau thời điểm token phát hành không
                LocalDateTime passwordChangedAt = userRepository.findPasswordChangedAtByUsername(username).orElse(null);
                if (passwordChangedAt != null) {
                    Date issuedAtDate = jwtProvider.getIssuedAtFromToken(token);
                    if (issuedAtDate != null) {
                        LocalDateTime issuedAt = LocalDateTime.ofInstant(issuedAtDate.toInstant(), ZoneId.systemDefault());
                        if (issuedAt.isBefore(passwordChangedAt)) {
                            throw new JwtException("Token has been invalidated due to password change");
                        }
                    }
                }

                List<String> roles = jwtProvider.getRolesFromToken(token);
                List<SimpleGrantedAuthority> authorities = roles
                        .stream().map(SimpleGrantedAuthority::new)
                        .toList();

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(username, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }

            } catch (JwtException e) {
                logger.warn("JWT validation failed: " + e.getMessage());
                ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                        .message(e.getMessage())
                        .path(request.getRequestURI())
                        .build();

                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
