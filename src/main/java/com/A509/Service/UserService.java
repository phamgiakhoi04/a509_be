package com.A509.Service;

import com.A509.DTO.LoginDTO;
import com.A509.DTO.RegisterDTO;
import com.A509.DTO.ProfileDTO;
import com.A509.DTO.UserDTO;
import com.A509.Entity.Role;
import com.A509.Entity.User;
import com.A509.Repository.RoleRepository;
import com.A509.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ImageService imageService;

    @Transactional(readOnly = true)
    public java.util.List<UserDTO> getUsersForAdmin() {
        return userRepository.findAll().stream().map(this::convertToUserDTO).toList();
    }

    @Transactional
    public UserDTO setUserStatus(Long id, boolean enabled, String actingUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
        if (!enabled && user.getUsername().equals(actingUsername)) {
            throw new IllegalArgumentException("Bạn không thể tự khóa tài khoản đang đăng nhập.");
        }
        user.setStatus(enabled);
        return convertToUserDTO(userRepository.save(user));
    }

    public User registerUser(RegisterDTO registerDTO) {
        if (userRepository.existsByUsername(registerDTO.getUsername())) {
            throw new RuntimeException("Username đã tồn tại!");
        }
        if (userRepository.existsByEmail(registerDTO.getEmail())) {
            throw new RuntimeException("Email đã tồn tại!");
        }

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setEmail(registerDTO.getEmail());
        user.setFullName(registerDTO.getFullName());
        user.setPhoneNumber(registerDTO.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));

        Role defaultRole = roleRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Lỗi: Chưa có Role ID=1 trong Database!"));
        user.setRole(defaultRole);

        user.setStatus(true);

        return userRepository.save(user);
    }

    public User loginUser(LoginDTO loginDTO) {
        User user = userRepository.findByUsername(loginDTO.getUsername())
                .orElseThrow(() -> new RuntimeException("Sai tên đăng nhập hoặc mật khẩu!"));

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("Sai tên đăng nhập hoặc mật khẩu!");
        }

        if (!user.isStatus()) {
            throw new RuntimeException("Tài khoản đã bị khóa!");
        }

        return user;
    }

    public UserDTO getMyProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));
        return convertToUserDTO(user);
    }

    @Transactional
    public UserDTO updateProfile(String username, ProfileDTO dto) throws IOException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User không tồn tại!"));

        if (dto.getFullName() != null && !dto.getFullName().isEmpty()) user.setFullName(dto.getFullName());
        if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().isEmpty()) user.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            if (!user.getEmail().equals(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
                throw new RuntimeException("Email mới đã được sử dụng!");
            }
            user.setEmail(dto.getEmail());
        }

        if (dto.getAvatarFile() != null && !dto.getAvatarFile().isEmpty()) {
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                imageService.deleteImageFromCloud(user.getAvatarUrl());
            }
            // Upload ảnh mới
            String newAvatarUrl = imageService.uploadImage(dto.getAvatarFile());
            user.setAvatarUrl(newAvatarUrl);
        }

        User updatedUser = userRepository.save(user);
        return convertToUserDTO(updatedUser);
    }

    private UserDTO convertToUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .status(user.isStatus())
                .roleName(user.getRole().getName())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
