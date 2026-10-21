package com.A509.DTO;

import com.A509.Entity.UniformCategory;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UniformCategoryRequest {
    private String categoryName;
    private String slug;
    private String description;
    private UniformCategory.CategoryType categoryType;
    private Long parentId;
    private Long countryId;
    private Integer sortOrder;
    private String icon;
}
