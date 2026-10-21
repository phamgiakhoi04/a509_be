package com.A509.Service;

import com.A509.DTO.UniformCategoryRequest;
import com.A509.Entity.UniformCategory;
import com.A509.Repository.UniformCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UniformCategoryService {

    @Autowired
    private UniformCategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<UniformCategory> getAllCategories() {
        List<UniformCategory> categories = categoryRepository.findAll();
        initializeChildren(categories);
        return categories;
    }

    @Transactional(readOnly = true)
    public Optional<UniformCategory> getCategoryById(Long id) {
        Optional<UniformCategory> category = categoryRepository.findById(id);
        category.ifPresent(this::initializeChildren);
        return category;
    }

    @Transactional(readOnly = true)
    public List<UniformCategory> getRootCategories() {
        List<UniformCategory> categories = categoryRepository.findRootCategories();
        initializeChildren(categories);
        return categories;
    }

    @Transactional(readOnly = true)
    public List<UniformCategory> getRootCategoriesByType(UniformCategory.CategoryType type) {
        List<UniformCategory> categories = categoryRepository.findRootCategoriesByType(type);
        initializeChildren(categories);
        return categories;
    }

    @Transactional(readOnly = true)
    public List<UniformCategory> getChildCategories(Long parentId) {
        List<UniformCategory> categories = categoryRepository.findByParentId(parentId);
        initializeChildren(categories);
        return categories;
    }

    @Transactional(readOnly = true)
    public List<UniformCategory> getCategoriesByType(UniformCategory.CategoryType type) {
        List<UniformCategory> categories = categoryRepository.findByCategoryType(type);
        initializeChildren(categories);
        return categories;
    }

    @Transactional
    public UniformCategory addCategory(UniformCategoryRequest request) {
        UniformCategory category = toEntity(request);
        if (category.getParent() != null) {
            List<UniformCategory> siblings = categoryRepository.findByParentId(category.getParent().getId());
            boolean exists = siblings.stream()
                    .anyMatch(c -> c.getCategoryName().equalsIgnoreCase(category.getCategoryName()));
            if (exists) {
                throw new RuntimeException("Loại phục dựng đã tồn tại trong danh mục này!");
            }
        } else if (categoryRepository.existsByCategoryName(category.getCategoryName())) {
            throw new RuntimeException("Loại phục dựng đã tồn tại!");
        }
        return categoryRepository.save(category);
    }

    @Transactional
    public UniformCategory updateCategory(Long id, UniformCategoryRequest request) {
        UniformCategory existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loại phục dựng không tồn tại"));
        UniformCategory parent = resolveParent(request.getParentId());

        if (!existingCategory.getCategoryName().equals(request.getCategoryName())) {
            List<UniformCategory> siblings = parent == null
                    ? categoryRepository.findRootCategories()
                    : categoryRepository.findByParentId(parent.getId());
            boolean exists = siblings.stream()
                    .anyMatch(c -> c.getCategoryName().equalsIgnoreCase(request.getCategoryName())
                            && !c.getId().equals(id));
            if (exists) {
                throw new RuntimeException("Tên loại phục dựng đã được sử dụng!");
            }
        }

        existingCategory.setCategoryName(request.getCategoryName());
        existingCategory.setSlug(request.getSlug());
        existingCategory.setDescription(request.getDescription());
        existingCategory.setCategoryType(request.getCategoryType());
        existingCategory.setSortOrder(request.getSortOrder());
        existingCategory.setIcon(request.getIcon());
        existingCategory.setCountryId(request.getCountryId());
        if (parent != null && !parent.getId().equals(id)) {
            existingCategory.setParent(parent);
        } else if (parent == null) {
            existingCategory.setParent(null);
        }
        return categoryRepository.save(existingCategory);
    }

    @Transactional(readOnly = true)
    public List<UniformCategory> searchCategories(String keyword) {
        List<UniformCategory> categories = categoryRepository.findByCategoryNameContainingIgnoreCase(keyword);
        initializeChildren(categories);
        return categories;
    }

    private UniformCategory toEntity(UniformCategoryRequest request) {
        UniformCategory category = new UniformCategory();
        category.setCategoryName(request.getCategoryName());
        category.setSlug(request.getSlug());
        category.setDescription(request.getDescription());
        category.setCategoryType(request.getCategoryType());
        category.setSortOrder(request.getSortOrder());
        category.setIcon(request.getIcon());
        category.setCountryId(request.getCountryId());
        category.setParent(resolveParent(request.getParentId()));
        return category;
    }

    private UniformCategory resolveParent(Long parentId) {
        if (parentId == null) {
            return null;
        }
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException("Danh mục cha không tồn tại"));
    }

    /** Initialize the complete tree before the transaction closes. */
    private void initializeChildren(List<UniformCategory> categories) {
        if (categories != null) {
            categories.forEach(this::initializeChildren);
        }
    }

    private void initializeChildren(UniformCategory category) {
        if (category != null && category.getChildren() != null) {
            category.getChildren().forEach(this::initializeChildren);
        }
    }

    @Transactional
    public void deleteCategory(Long id) {
        UniformCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loại phục dựng không tồn tại"));
        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            throw new RuntimeException("Không thể xóa danh mục này vì có danh mục con bên trong.");
        }
        if (category.getUniforms() != null && !category.getUniforms().isEmpty()) {
            throw new RuntimeException("Không thể xóa loại phục dựng vì đã có dữ liệu liên quan.");
        }
        categoryRepository.deleteById(id);
    }
}
