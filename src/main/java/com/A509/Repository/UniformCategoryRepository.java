package com.A509.Repository;

import com.A509.Entity.UniformCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UniformCategoryRepository extends JpaRepository<UniformCategory, Long> {

    boolean existsByCategoryName(String categoryName);

    boolean existsByCountryId(Long countryId);

    List<UniformCategory> findByCategoryNameContainingIgnoreCase(String name);

    Optional<UniformCategory> findByCategoryName(String categoryName);

    // Hierarchical category queries
    // Get root categories (categories without parent)
    @Query("SELECT c FROM UniformCategory c WHERE c.parent IS NULL ORDER BY c.sortOrder")
    List<UniformCategory> findRootCategories();

    // Get root categories by type
    @Query("SELECT c FROM UniformCategory c WHERE c.parent IS NULL AND c.categoryType = :type ORDER BY c.sortOrder")
    List<UniformCategory> findRootCategoriesByType(@Param("type") UniformCategory.CategoryType type);

    // Get child categories of a parent
    @Query("SELECT c FROM UniformCategory c WHERE c.parent.id = :parentId ORDER BY c.sortOrder")
    List<UniformCategory> findByParentId(@Param("parentId") Long parentId);

    // Get all categories by type
    @Query("SELECT c FROM UniformCategory c WHERE c.categoryType = :type ORDER BY c.sortOrder")
    List<UniformCategory> findByCategoryType(@Param("type") UniformCategory.CategoryType type);
}
