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
import jakarta.servlet.DispatcherType;

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
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, error) -> response.setStatus(401))
                        .accessDeniedHandler((request, response, error) -> response.setStatus(403)))
                .authorizeHttpRequests(auth -> auth
                        // Preserve the original HTTP error; an ERROR dispatch has no fresh JWT authentication.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        // Auth endpoints - public
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/analytics/pageviews", "/api/contact").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/search").permitAll()

                        // OPTIONS requests - public
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // GET endpoints - PUBLIC (ai cũng xem được)
                        .requestMatchers(HttpMethod.GET, "/api/uniforms/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/countries/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/images/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/uniform-categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**", "/api/activity/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/articles/images/references").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/articles/admin").hasAnyAuthority("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/articles/**").permitAll()

                        // Reads above remain public; every other method in these namespaces is ADMIN only.
                        .requestMatchers("/api/articles/**", "/api/uniform-categories/**", "/api/categories/**",
                                "/api/uniforms/**", "/api/images/**", "/api/countries/**").hasAuthority("ADMIN")

                        // A user may only access their own profile; management APIs require ADMIN.
                        .requestMatchers(HttpMethod.GET, "/api/users/profile").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/users/profile").authenticated()
                        .requestMatchers("/api/users/**", "/api/activity/**").hasAuthority("ADMIN")

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
