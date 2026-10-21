package com.A509.Security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class CustomFilterSecurity {

    @Autowired
    private CustomUserDetailService customUserDetailService;

    @Autowired
    private CustomJwtFilter customJwtFilter;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new LegacyCompatiblePasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder auth = http.getSharedObject(AuthenticationManagerBuilder.class);
        auth.userDetailsService(customUserDetailService)
                .passwordEncoder(passwordEncoder());
        return auth.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Auth endpoints - public
                        .requestMatchers("/api/auth/**").permitAll()

                        // OPTIONS requests - public
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // GET endpoints - PUBLIC (ai cũng xem được)
                        .requestMatchers(HttpMethod.GET, "/api/uniforms/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/countries/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/images/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/uniform-categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/articles/images/references").permitAll()

                        // POST/PUT/DELETE - ADMIN only
                        .requestMatchers(HttpMethod.POST, "/api/uniforms/**", "/api/images/**", "/api/countries/**").hasAnyAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/uniforms/**", "/api/images/**", "/api/countries/**").hasAnyAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/images/**").hasAnyAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/uniforms/**", "/api/images/**", "/api/countries/**").hasAnyAuthority("ADMIN")

                        // Comments endpoints
                        .requestMatchers(HttpMethod.GET, "/api/comments/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/comments/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/comments/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/comments/**").authenticated()

                        // Tất cả request còn lại - cần đăng nhập
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(customJwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
