package com.A509.Controller;

import com.A509.Service.ForgotPasswordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ForgotPasswordController {

    @Autowired
    private ForgotPasswordService forgotPasswordService;

    @Autowired private com.A509.Service.PasswordResetDispatch dispatch;

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        try {
            String email = body == null ? null : body.get("email");
            if (email == null || email.length() > 254 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
                return ResponseEntity.badRequest().body("Vui lòng nhập email!");
            }
            email = email.trim();

            dispatch.submit(email);
            return ResponseEntity.ok("Nếu email hợp lệ, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        try {
            String token = body == null ? null : body.get("token");
            String newPassword = body == null ? null : body.get("newPassword");

            if (token == null || newPassword == null) {
                return ResponseEntity.badRequest().body("Thiếu thông tin token hoặc mật khẩu mới!");
            }

            forgotPasswordService.resetPassword(token, newPassword);
            return ResponseEntity.ok("Đổi mật khẩu thành công! Hãy đăng nhập lại.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
