package com.A509.Config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

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
