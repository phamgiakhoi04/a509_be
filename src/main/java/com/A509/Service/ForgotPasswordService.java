package com.A509.Service;

import com.A509.Entity.User;
import com.A509.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ForgotPasswordService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public String forgotPassword(String email) {
        User user = userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống!"));

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15)); // Hết hạn sau 15p
        userRepository.save(user);
        try {
            sendEmail(email, token);
        } catch (MailException ex) {
            user.setResetToken(null);
            user.setResetTokenExpiry(null);
            userRepository.save(user);
            throw new RuntimeException("Không thể gửi email đặt lại mật khẩu. Vui lòng kiểm tra cấu hình MAIL_USERNAME/MAIL_PASSWORD.");
        }

        return "Đã gửi link đặt lại mật khẩu vào email: " + email;
    }

    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("Link không hợp lệ hoặc sai token!"));

        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Link đã hết hạn! Vui lòng gửi yêu cầu mới.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);

        userRepository.save(user);
    }

    private void sendEmail(String toEmail, String token) {
        String resetLink = "http://localhost:5173/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        if (mailUsername == null || mailUsername.isBlank()) {
            throw new MailException("MAIL_USERNAME chưa được cấu hình") {};
        }
        message.setFrom(mailUsername);
        message.setTo(toEmail);
        message.setSubject("Yêu cầu đặt lại mật khẩu - A509");
        message.setText("Chào bạn,\n\n"
                + "Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản A509.\n"
                + "Vui lòng nhấn vào đường dẫn bên dưới để thiết lập mật khẩu mới:\n\n"
                + resetLink + "\n\n"
                + "⚠️ Đường dẫn này sẽ hết hạn sau 15 phút.\n"
                + "Nếu bạn không yêu cầu, vui lòng bỏ qua email này.");

        mailSender.send(message);
        System.out.println(">>> Đã gửi mail thành công đến: " + toEmail);
    }
}
