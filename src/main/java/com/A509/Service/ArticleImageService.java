package com.A509.Service;

import com.A509.DTO.ArticleImageDTO;
import com.A509.DTO.ArticleImageUpdateDTO;
import com.A509.Entity.ActivityLog;
import com.A509.Entity.Article;
import com.A509.Entity.ArticleImage;
import com.A509.Repository.ActivityLogRepository;
import com.A509.Repository.ArticleImageRepository;
import com.A509.Repository.ArticleRepository;
import com.A509.Repository.UserRepository;
import com.A509.Repository.UniformRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class ArticleImageService {

    private final ArticleImageRepository articleImageRepository;
    private final ArticleRepository articleRepository;
    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    private final UniformRepository uniformRepository;
    private final Cloudinary cloudinary;

    public ArticleImageService(
            ArticleImageRepository articleImageRepository,
            ArticleRepository articleRepository,
            ActivityLogRepository activityLogRepository,
            UserRepository userRepository,
            UniformRepository uniformRepository,
            Cloudinary cloudinary
    ) {
        this.articleImageRepository = articleImageRepository;
        this.articleRepository = articleRepository;
        this.activityLogRepository = activityLogRepository;
        this.userRepository = userRepository;
        this.uniformRepository = uniformRepository;
        this.cloudinary = cloudinary;
    }

    public ArticleImageDTO uploadImage(
            Long articleId,
            MultipartFile file,
            String username
    ) throws IOException {

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy bài viết")
                );

        if (article.getDeletedAt() != null) {
            throw new RuntimeException("Bài viết đã bị xóa");
        }

        // Upload lên Cloudinary
        Map<?, ?> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.emptyMap()
        );

        String imageUrl = uploadResult.get("secure_url").toString();

        // Tự động đặt ảnh mới ở cuối
        List<ArticleImage> currentImages =
                articleImageRepository
                        .findByArticleIdOrderBySortOrderAsc(articleId);

        int sortOrder = currentImages.size();

        ArticleImage image = ArticleImage.builder()
                .article(article)
                .imageUrl(imageUrl)
                .description(null)
                .sortOrder(sortOrder)
                .build();

        ArticleImage savedImage =
                articleImageRepository.save(image);

        // Tìm user thực hiện
        var user = userRepository.findByUsername(username)
                .orElse(null);

        // Ghi Activity Log
        ActivityLog log = ActivityLog.builder()
                .article(article)
                .user(user)
                .action("ADD_IMAGE")
                .description("Thêm ảnh vào bài viết")
                .build();

        activityLogRepository.save(log);

        return toDTO(savedImage);
    }

    public List<ArticleImageDTO> getImages(Long articleId) {

        return articleImageRepository
                .findByArticleIdOrderBySortOrderAsc(articleId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public List<ArticleImageDTO> getReferenceImages(Long uniformId) {
        return articleImageRepository
                .findByReferenceUniform_IdOrderBySortOrderAsc(uniformId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public void deleteImage(
            Long imageId,
            String username
    ) {

        ArticleImage image =
                articleImageRepository.findById(imageId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Không tìm thấy ảnh"
                                )
                        );

        Article article = image.getArticle();

        // Xóa bản ghi trong MySQL
        articleImageRepository.delete(image);

        // Ghi Activity Log
        var user = userRepository.findByUsername(username)
                .orElse(null);

        ActivityLog log = ActivityLog.builder()
                .article(article)
                .user(user)
                .action("DELETE_IMAGE")
                .description("Xóa ảnh khỏi bài viết")
                .build();

        activityLogRepository.save(log);

        /*
         * Tạm thời KHÔNG xóa ảnh trên Cloudinary.
         *
         * Mục đích:
         * - Tránh mất ảnh ngoài ý muốn.
         * - Có thể khôi phục/liên kết lại sau này.
         */
    }

    public ArticleImageDTO updateImage(Long imageId, ArticleImageUpdateDTO update) {
        ArticleImage image = articleImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ảnh"));

        String description = update == null ? null : update.getDescription();
        image.setDescription(description == null || description.isBlank() ? null : description.trim());

        Long referenceUniformId = update == null ? null : update.getReferenceUniformId();
        if (referenceUniformId == null) {
            image.setReferenceUniform(null);
        } else {
            image.setReferenceUniform(uniformRepository.findById(referenceUniformId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy hiện vật tham chiếu")));
        }

        return toDTO(articleImageRepository.save(image));
    }

    private ArticleImageDTO toDTO(ArticleImage image) {

        ArticleImageDTO dto = new ArticleImageDTO();

        dto.setId(image.getId());
        dto.setArticleId(image.getArticle().getId());
        dto.setImageUrl(image.getImageUrl());
        dto.setDescription(image.getDescription());
        dto.setSortOrder(image.getSortOrder());
        if (image.getReferenceUniform() != null) {
            dto.setReferenceUniformId(image.getReferenceUniform().getId());
            dto.setReferenceUniformName(image.getReferenceUniform().getName());
        }

        return dto;
    }
}
