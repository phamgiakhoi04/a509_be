package com.A509.Config;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.util.Map;
@RestControllerAdvice
public class SafeApiErrors {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> invalid(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", "Dữ liệu không hợp lệ. Kiểm tra định dạng và độ dài các trường."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<String> uploadTooLarge() {
        return ResponseEntity.status(413).body("Tệp hoặc tổng dung lượng ảnh vượt giới hạn của máy chủ. Hãy giảm dung lượng hoặc tải từng đợt nhỏ hơn.");
    }
}
