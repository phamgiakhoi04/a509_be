package com.A509.Repository;

import com.A509.Entity.ArticleImage;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleImageRepository
        extends JpaRepository<ArticleImage, Long> {

    List<ArticleImage> findByArticleIdOrderBySortOrderAsc(Long articleId);

    List<ArticleImage> findByReferenceUniform_IdOrderBySortOrderAsc(Long uniformId);

    @Modifying
    @Transactional
    @Query("update ArticleImage image set image.referenceUniform = null where image.referenceUniform.id = :uniformId")
    int clearReferencesToUniform(Long uniformId);
}
