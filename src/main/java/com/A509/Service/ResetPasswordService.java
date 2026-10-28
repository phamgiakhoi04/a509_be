package com.A509.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ResetPasswordService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${app.frontend-url:http://localhost:5173/}")
    private String frontendUrl;

    public void sendResetPasswordEmail(String toEmail, String token) {
        String resetLink = UriComponentsBuilder.fromUriString(frontendUrl.replaceAll("/+$", "")
                        + "/reset-password")
                .queryParam("token", token).build().encode().toUriString();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailUsername);
        message.setTo(toEmail);
        message.setSubject("Yêu cầu đặt lại mật khẩu - A509");
        message.setText("Chào bạn,\n\n"
                + "Bạn vừa yêu cầu đặt lại mật khẩu. Vui lòng nhấn vào link bên dưới để tiếp tục:\n"
                + resetLink + "\n\n"
                + "Link này sẽ hết hạn sau 15 phút.\n"
                + "Nếu bạn không yêu cầu, vui lòng bỏ qua email này.");

        mailSender.send(message);
        System.out.println("Mail sent successfully to " + toEmail);
    }
}
