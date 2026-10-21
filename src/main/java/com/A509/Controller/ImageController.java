package com.A509.Controller;

import com.A509.Entity.Image;
import com.A509.DTO.ImageUpdateDTO;
import com.A509.Service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @GetMapping("/uniform/{uniformId}")
    public List<Image> getByUniform(@PathVariable Long uniformId) {
        return imageService.getImagesByUniformId(uniformId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteImage(@PathVariable Long id) {
        try {
            imageService.deleteImage(id);
            return ResponseEntity.ok("Đã xóa ảnh thành công (DB + Cloud)!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateImage(
            @PathVariable Long id,
            @RequestBody ImageUpdateDTO request
    ) {
        try {
            return ResponseEntity.ok(imageService.updateImage(id, request.getDescription(), request.getSensitive()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
