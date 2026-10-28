package com.A509.DTO;

import lombok.Data;

@Data
public class ArticleImageUpdateDTO {

    private String description;
    private Boolean sensitive;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private boolean referenceUniformProvided;

    /** Null clears the reference; a value links the image to that uniform. */
    private Long referenceUniformId;

    public void setReferenceUniformId(Long id) {
        referenceUniformId = id;
        referenceUniformProvided = true;
    }
}
