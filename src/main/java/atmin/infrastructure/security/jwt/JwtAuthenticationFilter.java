package atmin.infrastructure.security.jwt;

import atmin.common.response.ApiErrorResponse;
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
import atmin.repository.TokenBlacklistRepository;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;
    private final TokenBlacklistRepository tokenBlacklistRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws IOException, ServletException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (tokenBlacklistRepository.existsByToken(token)) {
                ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                        .timestamp(java.time.LocalDateTime.now())
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
                        .timestamp(java.time.LocalDateTime.now())
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
        filterChain.doFilter(request,response);
    }
}
