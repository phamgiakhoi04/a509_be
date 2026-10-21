package com.A509.DTO;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentDTO {
    private Long id;
    private String content;
    private Long uniformId;
    private Long articleId;
    private Long userId;
    private String username;
    private String userAvatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
