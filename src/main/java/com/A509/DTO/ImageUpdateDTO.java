package com.A509.DTO;

import lombok.Data;

@Data
public class ImageUpdateDTO {
    private String description;
    private Boolean sensitive;
    private com.A509.Entity.Image.DisplayMode displayMode;
}
