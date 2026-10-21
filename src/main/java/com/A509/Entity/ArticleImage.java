package com.A509.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "article_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "article_image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reference_uniform_id")
    private Uniform referenceUniform;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
