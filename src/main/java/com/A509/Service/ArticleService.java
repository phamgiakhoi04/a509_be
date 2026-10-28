package com.A509.Service;

import com.A509.DTO.ArticleDTO;
import com.A509.Security.MediaVisibility;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.A509.Entity.ActivityLog;
import com.A509.Entity.Article;
import com.A509.Entity.ArticleStatus;
import com.A509.Entity.Category;
import com.A509.Entity.User;
import com.A509.Repository.ActivityLogRepository;
import com.A509.Repository.ArticleRepository;
import com.A509.Repository.CategoryRepository;
import com.A509.Repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Comparator;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;

    public ArticleService(
            ArticleRepository articleRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            ActivityLogRepository activityLogRepository
    ) {
        this.articleRepository = articleRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.activityLogRepository = activityLogRepository;
    }

    @Transactional
    public List<ArticleDTO> getLatestArticles() {

        return articleRepository
                .findTop5ByStatusAndDeletedAtIsNullOrderByPublishedAtDesc(
                        ArticleStatus.PUBLISHED
                )
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /** Public list of all published, non-deleted articles. */
    @Transactional
    public List<ArticleDTO> getPublishedArticles() {
        return articleRepository
                .findByStatusAndDeletedAtIsNullOrderByPublishedAtDesc(
                        ArticleStatus.PUBLISHED
                )
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public List<ArticleDTO> getFeaturedArticles() {

        return articleRepository
                .findTop5ByStatusAndFeaturedTrueAndDeletedAtIsNullOrderByFeaturedOrderAscPublishedAtDesc(
                        ArticleStatus.PUBLISHED
                )
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public List<ArticleDTO> getArticlesByCategory(String categorySlug) {

    return articleRepository
            .findTop5ByStatusAndCategoriesSlugAndDeletedAtIsNullOrderByPublishedAtDesc(
                    ArticleStatus.PUBLISHED,
                    categorySlug
            )
            .stream()
            .map(this::toDTO)
            .toList();
    }

    /** Full article list for the admin editor, including drafts and archived rows. */
    @Transactional
    public List<ArticleDTO> getAllForAdmin() {
        return articleRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        Article::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public ArticleDTO getBySlug(String slug) {

        Article article = articleRepository.findBySlug(slug)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài viết")
                );

        // The slug endpoint is public.  Never expose drafts or archived/deleted
        // articles through it; admin preview can be added as a separate
        // authenticated endpoint later.
        if (article.getDeletedAt() != null
                || article.getStatus() != ArticleStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bài viết không tồn tại");
        }

        return toDTO(article);
    }

    @Transactional
    public ArticleDTO create(
            ArticleDTO dto,
            String username
    ) {

        User author = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy người dùng")
                );

        if (articleRepository.existsBySlug(dto.getSlug())) {
            throw new RuntimeException("Slug đã tồn tại");
        }

        Article article = new Article();

        article.setTitle(dto.getTitle());
        article.setSlug(dto.getSlug());
        article.setExcerpt(dto.getExcerpt());
        article.setContent(dto.getContent());
        article.setThumbnailUrl(dto.getThumbnailUrl());
        article.setThumbnailSensitive(Boolean.TRUE.equals(dto.getThumbnailSensitive()));

        article.setAuthor(author);

        article.setStatus(
                dto.getStatus() != null
                        ? dto.getStatus()
                        : ArticleStatus.DRAFT
        );

        article.setFeatured(dto.isFeatured());
        article.setFeaturedOrder(dto.getFeaturedOrder());

        if (article.getStatus() == ArticleStatus.PUBLISHED) {
            article.setPublishedAt(
                    dto.getPublishedAt() != null
                            ? dto.getPublishedAt()
                            : LocalDateTime.now()
            );
        }

        if (dto.getCategoryIds() != null) {

            HashSet<Category> categories = new HashSet<>(
                    categoryRepository.findAllById(dto.getCategoryIds())
            );

            article.setCategories(categories);
        }

        Article savedArticle = articleRepository.save(article);

        // Ghi lịch sử tạo bài
        saveActivity(
                savedArticle,
                author,
                "CREATE",
                "Tạo bài viết mới"
        );

        // Nếu bài được ghim ngay khi tạo
        if (savedArticle.isFeatured()) {
            saveActivity(
                    savedArticle,
                    author,
                    "FEATURE",
                    "Ghim bài viết vào Tin nổi bật"
            );
        }

        return toDTO(savedArticle);
    }

    @Transactional
    public ArticleDTO update(
            Long id,
            ArticleDTO dto
    ) {

        Article article = articleRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy bài viết")
                );

        if (article.getDeletedAt() != null) {
            throw new RuntimeException("Bài viết đã bị xóa");
        }

        // Lưu trạng thái cũ để phát hiện ghim / bỏ ghim
        boolean oldFeatured = article.isFeatured();

        if (dto.getTitle() != null) {
            article.setTitle(dto.getTitle());
        }

        if (dto.getSlug() != null &&
                !dto.getSlug().equals(article.getSlug())) {

            if (articleRepository.existsBySlug(dto.getSlug())) {
                throw new RuntimeException("Slug đã tồn tại");
            }

            article.setSlug(dto.getSlug());
        }

        if (dto.getExcerpt() != null) {
            article.setExcerpt(dto.getExcerpt());
        }

        if (dto.getContent() != null) {
            article.setContent(dto.getContent());
        }

        if (dto.getThumbnailUrl() != null) {
            article.setThumbnailUrl(dto.getThumbnailUrl());
        }
        if (dto.getThumbnailSensitive() != null) {
            article.setThumbnailSensitive(dto.getThumbnailSensitive());
        }

        if (dto.getStatus() != null) {

            if (dto.getStatus() == ArticleStatus.PUBLISHED) {
                article.setPublishedAt(
                        dto.getPublishedAt() != null
                                ? dto.getPublishedAt()
                                : (article.getPublishedAt() != null
                                ? article.getPublishedAt()
                                : LocalDateTime.now())
                );
            }

            article.setStatus(dto.getStatus());
        }

        // Cho phép admin hiệu chỉnh ngày đăng khi bài đã xuất bản.
        if (dto.getPublishedAt() != null
                && dto.getStatus() != ArticleStatus.PUBLISHED) {
            article.setPublishedAt(dto.getPublishedAt());
        }

        article.setFeatured(dto.isFeatured());
        article.setFeaturedOrder(dto.getFeaturedOrder());

        if (dto.getCategoryIds() != null) {

            HashSet<Category> categories = new HashSet<>(
                    categoryRepository.findAllById(dto.getCategoryIds())
            );

            article.setCategories(categories);
        }

        Article savedArticle = articleRepository.save(article);

        // Phát hiện thay đổi Featured
        if (!oldFeatured && savedArticle.isFeatured()) {

            saveActivity(
                    savedArticle,
                    null,
                    "FEATURE",
                    "Ghim bài viết vào Tin nổi bật"
            );

        } else if (oldFeatured && !savedArticle.isFeatured()) {

            saveActivity(
                    savedArticle,
                    null,
                    "UNFEATURE",
                    "Bỏ ghim bài viết khỏi Tin nổi bật"
            );
        }

        // Ghi log cập nhật nội dung
        saveActivity(
                savedArticle,
                null,
                "UPDATE",
                "Cập nhật nội dung bài viết"
        );

        return toDTO(savedArticle);
    }

    @Transactional
    public void delete(Long id) {

        Article article = articleRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy bài viết")
                );

        /*
         * Không xóa vật lý.
         * Chỉ đánh dấu deleted_at.
         */
        article.setDeletedAt(LocalDateTime.now());

        Article savedArticle = articleRepository.save(article);

        // Ghi log xóa bài
        saveActivity(
                savedArticle,
                null,
                "DELETE",
                "Xóa bài viết"
        );
    }

    /**
     * Tạo một bản ghi ActivityLog.
     */
    private void saveActivity(
            Article article,
            User user,
            String action,
            String description
    ) {

        ActivityLog log = ActivityLog.builder()
                .article(article)
                .user(user)
                .action(action)
                .description(description)
                .build();

        activityLogRepository.save(log);
    }

    private ArticleDTO toDTO(Article article) {

        ArticleDTO dto = new ArticleDTO();

        dto.setId(article.getId());

        dto.setTitle(article.getTitle());
        dto.setSlug(article.getSlug());

        dto.setExcerpt(article.getExcerpt());
        dto.setContent(article.getContent());

        dto.setThumbnailSensitive(article.isThumbnailSensitive());
        dto.setThumbnailUrl(MediaVisibility.url(article.getThumbnailUrl(), article.isThumbnailSensitive()));

        dto.setStatus(article.getStatus());

        dto.setFeatured(article.isFeatured());
        dto.setFeaturedOrder(article.getFeaturedOrder());

        dto.setPublishedAt(article.getPublishedAt());
        dto.setCreatedAt(article.getCreatedAt());
        dto.setUpdatedAt(article.getUpdatedAt());

        if (article.getAuthor() != null) {
            dto.setAuthorId(article.getAuthor().getId());
            dto.setAuthorName(article.getAuthor().getUsername());
        }

        dto.setCategoryIds(
                article.getCategories()
                        .stream()
                        .map(Category::getId)
                        .collect(java.util.stream.Collectors.toSet())
        );

        return dto;
    }
}
