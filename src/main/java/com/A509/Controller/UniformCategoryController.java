package com.A509.Controller;

import com.A509.DTO.UniformCategoryRequest;
import com.A509.Entity.UniformCategory;
import com.A509.Service.UniformCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/uniform-categories")
public class UniformCategoryController {

    @Autowired
    private UniformCategoryService categoryService;

    @GetMapping
    public List<UniformCategory> getAllCategories() {
        return categoryService.getAllCategories();
    }

    @GetMapping("/roots")
    public List<UniformCategory> getRootCategories() {
        return categoryService.getRootCategories();
    }

    @GetMapping("/roots/{type}")
    public List<UniformCategory> getRootCategoriesByType(@PathVariable UniformCategory.CategoryType type) {
        return categoryService.getRootCategoriesByType(type);
    }

    @GetMapping("/{id}/children")
    public List<UniformCategory> getChildCategories(@PathVariable Long id) {
        return categoryService.getChildCategories(id);
    }

    @GetMapping("/type/{type}")
    public List<UniformCategory> getCategoriesByType(@PathVariable UniformCategory.CategoryType type) {
        return categoryService.getCategoriesByType(type);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(@PathVariable Long id) {
        return categoryService.getCategoryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> addCategory(@RequestBody UniformCategoryRequest category) {
        try {
            return ResponseEntity.ok(categoryService.addCategory(category));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody UniformCategoryRequest category) {
        try {
            return ResponseEntity.ok(categoryService.updateCategory(id, category));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.ok("Đã xóa loại quân phục thành công");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchCategories(@RequestParam String keyword) {
        return ResponseEntity.ok(categoryService.searchCategories(keyword));
    }
}
