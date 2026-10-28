package com.A509.Security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/** Single-instance limiter. At multiple replicas, configure the same limits at the gateway. */
@Component
@Order(20)
public class RequestRateLimitFilter extends OncePerRequestFilter {
    private record Window(long start, int count) {}
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        int limit = switch (path) {
            case "/api/auth/login", "/api/auth/reset-password" -> 10;
            case "/api/auth/register", "/api/auth/forgot-password", "/api/contact" -> 5;
            case "/api/analytics/pageviews" -> 120;
            case "/api/search" -> 60;
            default -> 0;
        };
        if (limit == 0 || "OPTIONS".equals(request.getMethod())) { chain.doFilter(request,response); return; }
        long now = System.currentTimeMillis();
        long interval = path.startsWith("/api/auth/") && !path.endsWith("login") ? 900000 : 60000;
        // remoteAddr is supplied by the configured trusted reverse proxy, never a manually parsed XFF.
        String key = request.getRemoteAddr() + ":" + path;
        if (windows.size() > 10000) windows.entrySet().removeIf(e -> now - e.getValue().start() >= 900000);
        if (windows.size() > 20000 && !windows.containsKey(key)) { reject(response, 60); return; }
        Window value = windows.compute(key, (k, old) -> old == null || now - old.start() >= interval
                ? new Window(now,1) : new Window(old.start(), old.count() + 1));
        if (value.count() > limit) { reject(response, Math.max(1,(interval - (now-value.start())) / 1000)); return; }
        chain.doFilter(request,response);
    }
    private void reject(HttpServletResponse response, long seconds) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(seconds));
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"Quá nhiều yêu cầu. Vui lòng thử lại sau.\"}");
    }
}
