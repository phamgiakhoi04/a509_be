package com.A509.Controller;

import com.A509.DTO.UniformDTO;
import com.A509.Entity.Uniform;
import com.A509.Service.UniformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Optional;

@RestController
@RequestMapping("/api/uniforms")
public class UniformController {

    @Autowired
    private UniformService uniformService;

    @GetMapping
    public ResponseEntity<Page<Uniform>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    )

    {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Uniform> uniformPage = uniformService.getAllUniforms(pageable);

        return ResponseEntity.ok(uniformPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Optional<Uniform> uniform = uniformService.getUniformById(id);
        if (uniform.isPresent()) {
            return ResponseEntity.ok(uniform.get());
        } else {
            return ResponseEntity.status(404).body("Không tìm thấy quân phục với ID: " + id);
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(@ModelAttribute UniformDTO uniformDTO) {
        try {
            Uniform result = uniformService.addUniform(uniformDTO);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Lỗi khi upload ảnh: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> update(@PathVariable Long id, @ModelAttribute UniformDTO uniformDTO) {
        try {
            Uniform updatedUniform = uniformService.updateUniform(id, uniformDTO);
            return ResponseEntity.ok(updatedUniform);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Lỗi khi upload ảnh: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            uniformService.deleteUniform(id);
            return ResponseEntity.ok("Đã xóa thành công quân phục có ID: " + id);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?> getByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Uniform> uniforms = uniformService.getUniformsByCategory(categoryId, pageable);
            return ResponseEntity.ok(uniforms);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
