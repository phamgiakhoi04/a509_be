package com.A509.Config;

import com.A509.Entity.UniformCategory;
import com.A509.Repository.UniformCategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class CategoryDataInitializer {

    @Bean
    @ConditionalOnProperty(name = "app.data-initializer.enabled", havingValue = "true", matchIfMissing = false)
    CommandLineRunner syncUniformCategoryNames(UniformCategoryRepository uniformRepository) {
        return args -> {
            ensureReenactmentRoot(uniformRepository);
            // Đồng bộ tên các danh mục Tài liệu trong uniform_categories với menu frontend.
            renameUniformCategory(uniformRepository, "Chia sẻ kinh nghiệm", "Ảnh tư liệu", "Ảnh và tư liệu phục dựng.");
            renameUniformCategory(uniformRepository, "Góc nhìn", "Hồi ức CCB", "Hồi ức cựu chiến binh.");
            renameUniformCategory(uniformRepository, "Nước ngoài", "Thư viện", "Tư liệu trong thư viện.");
            renameUniformCategory(uniformRepository, "Nghiên cứu", "Từ điển", "Tư liệu tra cứu và nghiên cứu.");
        };
    }

    private void ensureReenactmentRoot(UniformCategoryRepository repository) {
        boolean exists = repository.findRootCategoriesByType(UniformCategory.CategoryType.REENACTMENT)
                .stream()
                .anyMatch(category -> "Phục dựng trang phục".equals(category.getCategoryName()));
        if (exists) {
            return;
        }

        UniformCategory root = UniformCategory.builder()
                .categoryName("Phục dựng trang phục")
                .description("Danh mục chính phục dựng trang phục")
                .categoryType(UniformCategory.CategoryType.REENACTMENT)
                .sortOrder(1)
                .icon("shield")
                .build();
        repository.save(root);
    }

    private void renameUniformCategory(UniformCategoryRepository repository, String oldName, String newName, String description) {
        repository.findByCategoryName(oldName).ifPresent(category -> {
            if (repository.findByCategoryName(newName).isEmpty()) {
                category.setCategoryName(newName);
                category.setDescription(description);
                repository.save(category);
            }
        });
    }

}
