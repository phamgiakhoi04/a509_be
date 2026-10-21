package com.A509.DTO;

import lombok.Data;

@Data
public class ArticleImageUpdateDTO {

    private String description;

    /** Null clears the reference; a value links the image to that uniform. */
    private Long referenceUniformId;
}
