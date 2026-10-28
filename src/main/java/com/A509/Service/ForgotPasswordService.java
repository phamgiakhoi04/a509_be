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
import org.springframework.web.util.UriComponentsBuilder;

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

    @Value("${app.frontend-url:http://localhost:5173/}")
    private String frontendUrl;

    public String forgotPassword(String email) {
        User user = userRepository.findByEmail(email.trim()).orElse(null);
        if (user == null || !user.isStatus()) return "Nếu email hợp lệ, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu.";

        byte[] random = new byte[32];
        new java.security.SecureRandom().nextBytes(random);
        String token = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        user.setResetToken(hash(token));
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15)); // Hết hạn sau 15p
        userRepository.save(user);
        try {
            sendEmail(email, token);
        } catch (MailException ex) {
            user.setResetToken(null);
            user.setResetTokenExpiry(null);
            userRepository.save(user);
            // Same response for unavailable mail and unknown accounts.
        }

        return "Nếu email hợp lệ, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu.";
    }

    @org.springframework.transaction.annotation.Transactional
    public void resetPassword(String token, String newPassword) {
        com.A509.Security.PasswordPolicy.validate(newPassword);
        if (token == null || token.length() > 200) throw new IllegalArgumentException("Link không hợp lệ.");
        User user = userRepository.findByResetToken(hash(token))
                .orElseThrow(() -> new RuntimeException("Link không hợp lệ hoặc sai token!"));

        if (!user.isStatus() || user.getResetTokenExpiry() == null || !user.getResetTokenExpiry().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Link đã hết hạn! Vui lòng gửi yêu cầu mới.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setResetToken(null);
        user.setResetTokenExpiry(null);

        userRepository.save(user);
    }

    static String hash(String token) {
        try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }

    private void sendEmail(String toEmail, String token) {
        String resetLink = UriComponentsBuilder.fromUriString(frontendUrl.replaceAll("/+$", "")
                        + "/reset-password")
                .queryParam("token", token).build().encode().toUriString();

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

    }
}
