package com.A509.Service;

import com.A509.Entity.Uniform;
import com.A509.Repository.UniformRepository;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

@Component
public class CaptionReferenceResolver {
    private final UniformRepository uniforms;
    public CaptionReferenceResolver(UniformRepository uniforms) { this.uniforms = uniforms; }
    public record Result(boolean specified, Uniform uniform, String warning) {}

    public static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    public Result resolve(String caption) {
        if (caption == null) return new Result(false, null, null);
        var names = caption.lines().map(String::trim)
                .filter(line -> normalize(line).matches("^do vat\\s*:.*"))
                .map(line -> line.substring(line.indexOf(':') + 1).trim()).toList();
        if (names.isEmpty()) return new Result(false, null, null);
        if (names.size() != 1 || names.getFirst().contains(",")) {
            return new Result(true, null, "Mỗi ảnh hiện gắn một đồ vật. Hãy ghi một tên sau 'Đồ vật:' hoặc chọn đồ vật thủ công.");
        }
        String name = normalize(names.getFirst());
        if (name.isEmpty()) return new Result(true, null, "Hãy nhập tên đồ vật sau 'Đồ vật:'.");
        var matches = uniforms.findReferenceNames().stream().filter(item -> normalize(item.getName()).equals(name)).toList();
        if (matches.size() != 1) return new Result(true, null, matches.isEmpty()
                ? "Không tìm thấy đồ vật: " + names.getFirst() + ". Ảnh được lưu nhưng chưa gắn Reference."
                : "Tên đồ vật trùng nhiều mục. Hãy bỏ dòng 'Đồ vật:' và chọn đồ vật thủ công.");
        return new Result(true, uniforms.findById(matches.getFirst().getId()).orElseThrow(), null);
    }
}
