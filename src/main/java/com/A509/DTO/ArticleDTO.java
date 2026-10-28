package com.A509.DTO;

import com.A509.Entity.ArticleStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class ArticleDTO {

    private Long id;

    private String title;
    private String slug;

    private String excerpt;
    private String content;

    private String thumbnailUrl;
    private Boolean thumbnailSensitive;

    private ArticleStatus status;

    private boolean featured;
    private int featuredOrder;

    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long authorId;
    private String authorName;

    private Set<Long> categoryIds;
}
