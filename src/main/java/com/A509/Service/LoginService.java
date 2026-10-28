package com.A509.Service;

import com.A509.DTO.AuthDTO;
import com.A509.DTO.LoginDTO;
import com.A509.DTO.UserDTO;
import com.A509.Entity.User;
import com.A509.Repository.UserRepository;
import com.A509.Untils.JwtUntilHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class LoginService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUntilHelper jwtUntilHelper;

    @Autowired
    private UserRepository userRepository;

    public AuthDTO authenticateAndGetToken(LoginDTO loginDTO) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword())
        );

        if (authentication.isAuthenticated()) {
            User user = userRepository.findByUsername(loginDTO.getUsername())
                    .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy User!"));

            if (!user.isStatus()) {
                throw new RuntimeException("Tài khoản đã bị khóa! Vui lòng liên hệ Admin.");
            }

            String token = jwtUntilHelper.generateToken(loginDTO.getUsername(), user.getTokenVersion());

            return AuthDTO.builder()
                    .token(token)
                    .userInfo(convertToUserDTO(user))
                    .build();
        } else {
            throw new RuntimeException("Đăng nhập thất bại: Sai tên đăng nhập hoặc mật khẩu.");
        }
    }

    private UserDTO convertToUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .status(user.isStatus())
                .roleName(user.getRole() != null ? user.getRole().getName() : "UNKNOWN")
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}