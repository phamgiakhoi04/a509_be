package com.A509.Controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Public contact form endpoint. Messages are forwarded to the configured site mailbox. */
@RestController
@RequestMapping("/api/contact")
public class ContactController {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String recipient;

    public ContactController(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @PostMapping
    public ResponseEntity<?> send(@RequestBody Map<String, String> body) {
        String name = value(body, "name");
        String email = value(body, "email");
        String subject = value(body, "subject");
        String messageText = value(body, "message");
        if (name.isBlank() || email.isBlank() || subject.isBlank() || messageText.isBlank()) {
            return ResponseEntity.badRequest().body("Vui lòng điền đầy đủ thông tin liên hệ.");
        }
        if (name.length() > 150 || subject.length() > 200 || messageText.length() > 10000
                || email.length() > 254 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")
                || subject.contains("\r") || subject.contains("\n") || name.contains("\r") || name.contains("\n"))
            return ResponseEntity.badRequest().body("Thông tin liên hệ không hợp lệ hoặc quá dài.");
        if (recipient.isBlank()) {
            return ResponseEntity.status(503).body("Hệ thống chưa cấu hình email nhận liên hệ.");
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(recipient.trim());
            message.setTo(recipient.trim());
            message.setReplyTo(email.trim());
            message.setSubject("[A509 Liên hệ] " + subject);
            message.setText("Người gửi: " + name + " (" + email + ")\n\n" + messageText);
            mailSender.send(message);
            return ResponseEntity.ok(Map.of("message", "Đã gửi tin nhắn thành công."));
        } catch (MailException ex) {
            return ResponseEntity.status(503).body("Không thể gửi email lúc này. Vui lòng thử lại sau.");
        }
    }

    private static String value(Map<String, String> body, String key) {
        String value = body == null ? null : body.get(key);
        return value == null ? "" : value.trim();
    }
}
