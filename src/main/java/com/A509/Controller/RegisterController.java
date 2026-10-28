package com.A509.Controller;

import com.A509.DTO.RegisterDTO;
import com.A509.DTO.UserDTO;
import com.A509.Service.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class RegisterController {

    @Autowired
    private RegisterService registerService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@jakarta.validation.Valid @RequestBody RegisterDTO registerDTO) {
        try {
            UserDTO newUser = registerService.registerNewUser(registerDTO);
            return ResponseEntity.ok(newUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Không thể đăng ký với thông tin này. Kiểm tra tên đăng nhập, email và mật khẩu.");
        }
    }
}