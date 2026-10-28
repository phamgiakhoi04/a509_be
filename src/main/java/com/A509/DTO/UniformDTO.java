package com.A509.DTO;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Data
public class UniformDTO {
    private String name;
    private String description;
    private String history;
    private String material;
    private Long countryId;
    private Long categoryId;
    private Long periodCategoryId;
    private List<MultipartFile> imageFiles;
    private List<String> imageDescriptions;
    private List<String> imageSensitive;
    private List<String> imageDisplayModes;
}
