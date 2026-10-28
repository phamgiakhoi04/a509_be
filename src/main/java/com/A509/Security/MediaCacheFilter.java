package com.A509.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class MediaCacheFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/api/articles") || path.startsWith("/api/uniforms") || path.startsWith("/api/images")) {
            response.setHeader("Cache-Control", "no-store");
            response.addHeader("Vary", "Authorization");
        }
        chain.doFilter(request, response);
    }
}
