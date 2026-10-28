package com.A509.Security;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.io.IOException;
import java.util.Set;
public final class UploadPolicy {
    private UploadPolicy() {}
    public static void validate(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty() || file.getSize() > 5 * 1024 * 1024)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ảnh phải có dung lượng từ 1 byte đến 5MB.");
        byte[] b = file.getBytes();
        String mime = file.getContentType();
        boolean valid = b.length >= 12 && (
            ("image/jpeg".equals(mime) && b[0] == (byte)0xff && b[1] == (byte)0xd8 && b[2] == (byte)0xff) ||
            ("image/png".equals(mime) && b[0] == (byte)0x89 && b[1] == 80 && b[2] == 78 && b[3] == 71 && b[4] == 13 && b[5] == 10 && b[6] == 26 && b[7] == 10) ||
            ("image/gif".equals(mime) && new String(b,0,6,java.nio.charset.StandardCharsets.US_ASCII).matches("GIF8[79]a")) ||
            ("image/webp".equals(mime) && new String(b,0,4,java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF") && new String(b,8,4,java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP")));
        if (!valid) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ nhận ảnh JPEG, PNG, GIF hoặc WebP có định dạng hợp lệ.");
    }
}
