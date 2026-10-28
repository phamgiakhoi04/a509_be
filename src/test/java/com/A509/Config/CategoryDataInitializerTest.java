package com.A509.Config;

import com.A509.Repository.UniformCategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class CategoryDataInitializerTest {
    @Test
    void localPreviewCanDisableDatabaseInitialization() {
        new ApplicationContextRunner()
                .withUserConfiguration(CategoryDataInitializer.class)
                .withPropertyValues("app.data-initializer.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean("syncUniformCategoryNames"));
    }

    @Test
    void defaultStartupDoesNotModifyExistingContent() {
        var repository = mock(UniformCategoryRepository.class);
        new ApplicationContextRunner()
                .withBean(UniformCategoryRepository.class, () -> repository)
                .withUserConfiguration(CategoryDataInitializer.class)
                .run(context -> assertThat(context).doesNotHaveBean("syncUniformCategoryNames"));
        verifyNoInteractions(repository);
    }
}
