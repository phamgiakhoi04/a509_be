package com.A509.Controller;

import com.A509.DTO.CategoryDTO;
import com.A509.Entity.Category;
import com.A509.Repository.CategoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getAll() {
        return ResponseEntity.ok(categoryRepository.findAll().stream()
                .map(this::toDTO)
                .toList());
    }

    private CategoryDTO toDTO(Category category) {
        return new CategoryDTO(
                category.getId(), category.getName(), category.getSlug(),
                category.getDescription(), category.getSortOrder(), category.getIcon(),
                category.getParent() == null ? null : category.getParent().getId());
    }
}
