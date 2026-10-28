package com.A509.DTO;

import lombok.Data;

@Data
public class ArticleImageDTO {

    private Long id;

    private Long articleId;

    private String imageUrl;
    private boolean sensitive;
    private String referenceWarning;

    private String description;

    private Integer sortOrder;

    private Long referenceUniformId;

    private String referenceUniformName;
}
