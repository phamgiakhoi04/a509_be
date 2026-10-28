package com.A509.Security;

import com.A509.Untils.JwtUntilHelper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CustomJwtFilter extends OncePerRequestFilter {

    @Autowired
    private CustomUserDetailService userDetailsService;

    @Autowired
    private JwtUntilHelper jwtUntilHelper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String token = authHeader.substring(7);
                String username = jwtUntilHelper.getSubject(token);
                if (username != null) {
                    // Reload each request so disabling an account revokes existing tokens.
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (userDetails.isEnabled() && userDetails.isAccountNonLocked()
                            && userDetails.isAccountNonExpired() && userDetails.isCredentialsNonExpired()
                            && jwtUntilHelper.verifyToken(token, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (org.springframework.dao.DataAccessException unavailable) {
                SecurityContextHolder.clearContext();
                // A temporary account-store outage must not invalidate a valid browser session.
                response.setStatus(503);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\":\"Dịch vụ tài khoản tạm thời không khả dụng.\"}");
                return;
            } catch (Exception ex) {
                // Invalid, expired or unresolvable credentials behave as anonymous.
                // Do not catch exceptions from the downstream application/filter chain.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
