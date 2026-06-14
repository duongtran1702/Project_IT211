package atmin.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class MdcLoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_MDC_KEY = "traceId";
    private static final String USERNAME_MDC_KEY = "username";
    private static final String METHOD_MDC_KEY = "method";
    private static final String URI_MDC_KEY = "uri";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString().substring(0, 8); // Short UUID for readability
        MDC.put(TRACE_ID_MDC_KEY, traceId);
        MDC.put(METHOD_MDC_KEY, request.getMethod());
        MDC.put(URI_MDC_KEY, request.getRequestURI());
        MDC.put(USERNAME_MDC_KEY, "anonymous");

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
