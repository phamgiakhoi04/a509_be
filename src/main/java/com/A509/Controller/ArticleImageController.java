package com.A509.Controller;

import com.A509.DTO.ArticleImageDTO;
import com.A509.DTO.ArticleImageUpdateDTO;
import com.A509.Service.ArticleImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/articles")
@CrossOrigin
public class ArticleImageController {

    private final ArticleImageService articleImageService;

    public ArticleImageController(
            ArticleImageService articleImageService
    ) {
        this.articleImageService = articleImageService;
    }

    @PostMapping("/{articleId}/images")
    public ResponseEntity<ArticleImageDTO> uploadImage(
            @PathVariable Long articleId,
            @RequestParam("file") MultipartFile file,
            Principal principal
    ) throws IOException {

        ArticleImageDTO result =
                articleImageService.uploadImage(
                        articleId,
                        file,
                        principal.getName()
                );

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{articleId}/images")
    public ResponseEntity<List<ArticleImageDTO>> getImages(
            @PathVariable Long articleId
    ) {

        return ResponseEntity.ok(
                articleImageService.getImages(articleId)
        );
    }

    @GetMapping("/images/references")
    public ResponseEntity<List<ArticleImageDTO>> getReferenceImages(
            @RequestParam Long uniformId
    ) {
        return ResponseEntity.ok(articleImageService.getReferenceImages(uniformId));
    }

    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long imageId,
            Principal principal
    ) {

        articleImageService.deleteImage(
                imageId,
                principal.getName()
        );

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/images/{imageId}")
    public ResponseEntity<ArticleImageDTO> updateImage(
            @PathVariable Long imageId,
            @RequestBody ArticleImageUpdateDTO body
    ) {
        return ResponseEntity.ok(articleImageService.updateImage(imageId, body));
    }
}
