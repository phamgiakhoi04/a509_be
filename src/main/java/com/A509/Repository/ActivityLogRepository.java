package com.A509.Repository;

import com.A509.Entity.ActivityLog;
import com.A509.Entity.ArticleStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository
        extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findTop10ByActionInOrderByCreatedAtDesc(
            List<String> actions
    );

    @EntityGraph(attributePaths = "article")
    List<ActivityLog> findTop10ByActionInAndArticle_StatusAndArticle_DeletedAtIsNullOrderByCreatedAtDesc(
            List<String> actions, ArticleStatus status);
}
