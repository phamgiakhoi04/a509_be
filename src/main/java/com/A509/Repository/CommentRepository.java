package com.A509.Repository;

import com.A509.Entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByUniform_IdOrderByCreatedAtDesc(Long uniformId);

    List<Comment> findByArticle_IdOrderByCreatedAtDesc(Long articleId);

    List<Comment> findByUserId(Long userId);
}
