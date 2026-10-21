package com.A509.Controller;

import com.A509.DTO.CommentDTO;
import com.A509.Service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @GetMapping("/uniform/{uniformId}")
    public ResponseEntity<List<CommentDTO>> getComments(@PathVariable Long uniformId) {
        try {
            return ResponseEntity.ok(commentService.getCommentsByUniformId(uniformId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/article/{articleId}")
    public ResponseEntity<List<CommentDTO>> getArticleComments(@PathVariable Long articleId) {
        try {
            return ResponseEntity.ok(commentService.getCommentsByArticleId(articleId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> addComment(@RequestBody CommentDTO dto) {
        try {
            CommentDTO created = commentService.addComment(dto);
            return ResponseEntity.ok(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateComment(@PathVariable Long id, @RequestBody CommentDTO dto) {
        try {
            CommentDTO updated = commentService.updateComment(id, dto.getContent());
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("quyền") || e.getMessage().contains("chính mình")) {
                return ResponseEntity.status(403).body(e.getMessage());
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteComment(@PathVariable Long id) {
        try {
            commentService.deleteComment(id);
            return ResponseEntity.ok("Đã xóa bình luận thành công!");
        } catch (RuntimeException e) {
            if (e.getMessage().contains("quyền")) {
                return ResponseEntity.status(403).body(e.getMessage());
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
