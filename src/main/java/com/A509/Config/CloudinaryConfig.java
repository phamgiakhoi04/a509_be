package com.A509.Config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(prefix = "app.media", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    public static Cloudinary requireForUpload(@Nullable Cloudinary cloudinary) {
        if (cloudinary == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ tải ảnh chưa được cấu hình.");
        }
        return cloudinary;
    }

    @Bean
    public Cloudinary cloudinary() {
        cloudName = cloudName == null ? "" : cloudName.trim();
        if (!cloudName.matches("[a-z0-9_-]+")) {
            throw new IllegalStateException("CLOUDINARY_CLOUD_NAME không hợp lệ; chỉ dùng cloud name, không dùng tên hiển thị có khoảng trắng.");
        }
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        config.put("api_key", apiKey);
        config.put("api_secret", apiSecret);
        return new Cloudinary(config);
    }
}
