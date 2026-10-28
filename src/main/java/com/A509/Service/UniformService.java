package com.A509.Service;

import com.A509.DTO.UniformDTO;
import com.A509.Entity.Country;
import com.A509.Entity.Image;
import com.A509.Entity.Uniform;
import com.A509.Entity.UniformCategory;
import com.A509.Repository.CountryRepository;
import com.A509.Repository.ImageRepository;
import com.A509.Repository.ArticleImageRepository;
import com.A509.Repository.UniformRepository;
import com.A509.Repository.UniformCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UniformService {

    @Autowired
    private UniformRepository uniformRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ImageService imageService;

    @Autowired
    private UniformCategoryRepository categoryRepository;

    @Autowired
    private ArticleImageRepository articleImageRepository;

    public List<Uniform> getAllUniforms() {
        return uniformRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Uniform> getAllUniforms(Pageable pageable) {
        return loadPageImages(uniformRepository.findAll(pageable));
    }

    @Transactional
    public Uniform addUniform(UniformDTO dto) throws IOException {
        if (uniformRepository.existsByName(dto.getName())) {
            throw new RuntimeException("Tên quân phục '" + dto.getName() + "' đã tồn tại!");
        }

        Country country = countryRepository.findById(dto.getCountryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Quốc gia với ID: " + dto.getCountryId()));

        UniformCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại quân phục với ID: " + dto.getCategoryId()));

        Uniform uniform = new Uniform();
        uniform.setName(dto.getName());
        uniform.setDescription(dto.getDescription());
        uniform.setHistory(dto.getHistory());
        uniform.setMaterial(dto.getMaterial());
        uniform.setCountry(country);
        uniform.setCategory(category);
        applyPeriod(uniform, dto);

        Uniform savedUniform = uniformRepository.save(uniform);

        if (dto.getImageFiles() != null && !dto.getImageFiles().isEmpty()) {
            for (int index = 0; index < dto.getImageFiles().size(); index++) {
                MultipartFile file = dto.getImageFiles().get(index);
                if (!file.isEmpty()) {
                    String imageUrl = imageService.uploadImage(file);
                    Image image = new Image();
                    image.setUniform(savedUniform);
                    image.setImageUrl(imageUrl);
                    String description = dto.getImageDescriptions() != null && index < dto.getImageDescriptions().size()
                            ? dto.getImageDescriptions().get(index)
                            : null;
                    image.setDescription(description == null || description.isBlank()
                            ? "Ảnh chi tiết của " + savedUniform.getName()
                            : description.trim());
                    image.setSensitive(parseSensitive(dto.getImageSensitive(), index));
                    image.setDisplayMode(parseDisplayMode(dto.getImageDisplayModes(), index));

                    imageRepository.save(image);
                }
            }
        }

        return savedUniform;
    }

    // Sửa ở đây: dùng findById đã có @EntityGraph
    public Optional<Uniform> getUniformById(Long id) {
        return uniformRepository.findById(id);
    }

    public List<Uniform> searchUniforms(String keyword) {
        return uniformRepository.findByNameContainingIgnoreCase(keyword);
    }

    @Transactional
    public Uniform updateUniform(Long id, UniformDTO dto) throws IOException {
        Uniform existingUniform = uniformRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quân phục ID: " + id));

        existingUniform.setName(dto.getName());
        existingUniform.setDescription(dto.getDescription());
        existingUniform.setHistory(dto.getHistory());
        existingUniform.setMaterial(dto.getMaterial());

        if (dto.getCountryId() != null) {
            Country country = countryRepository.findById(dto.getCountryId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Quốc gia ID: " + dto.getCountryId()));
            existingUniform.setCountry(country);
        }

        if (dto.getCategoryId() != null) {
            UniformCategory category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy loại quân phục ID: " + dto.getCategoryId()));
            existingUniform.setCategory(category);
        }

        applyPeriod(existingUniform, dto);

        if (dto.getImageFiles() != null && !dto.getImageFiles().isEmpty()) {
            for (int index = 0; index < dto.getImageFiles().size(); index++) {
                MultipartFile file = dto.getImageFiles().get(index);
                if (!file.isEmpty()) {
                    String imageUrl = imageService.uploadImage(file);
                    Image image = new Image();
                    image.setUniform(existingUniform);
                    image.setImageUrl(imageUrl);
                    String description = dto.getImageDescriptions() != null && index < dto.getImageDescriptions().size()
                            ? dto.getImageDescriptions().get(index)
                            : null;
                    image.setDescription(description == null || description.isBlank()
                            ? "Ảnh chi tiết của " + existingUniform.getName()
                            : description.trim());
                    image.setSensitive(parseSensitive(dto.getImageSensitive(), index));

                    image.setDisplayMode(parseDisplayMode(dto.getImageDisplayModes(), index));

                    imageRepository.save(image);
                }
            }
        }

        return uniformRepository.save(existingUniform);
    }

    @Transactional
    public void deleteUniform(Long id) {
        Uniform uniform = uniformRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quân phục không tồn tại để xóa!"));

        Set<Image> images = uniform.getImages();
        if (images != null && !images.isEmpty()) {
            for (Image img : images) {
                imageService.deleteImageFromCloud(img.getImageUrl());
            }
        }
        articleImageRepository.clearReferencesToUniform(id);
        uniformRepository.delete(uniform);
    }

    @Transactional(readOnly = true)
    public Page<Uniform> getUniformsByCategory(Long categoryId, Pageable pageable) {
        return loadPageImages(uniformRepository.findByCategoryId(categoryId, pageable));
    }

    private Page<Uniform> loadPageImages(Page<Uniform> page) {
        if (!page.isEmpty()) uniformRepository.fetchImagesForIds(page.getContent().stream().map(Uniform::getId).toList());
        return page;
    }

    private boolean parseSensitive(List<String> values, int index) {
        return values != null && index < values.size() && Boolean.parseBoolean(values.get(index));
    }

    private Image.DisplayMode parseDisplayMode(List<String> values, int index) {
        return values == null || index >= values.size() ? Image.DisplayMode.GRID
                : Image.DisplayMode.valueOf(values.get(index));
    }

    private void applyPeriod(Uniform uniform, UniformDTO dto) {
        if (uniform.getCategory().getCategoryType() != UniformCategory.CategoryType.REENACTMENT) return;
        Long periodId = dto.getPeriodCategoryId();
        if (periodId == null && uniform.getPeriodCategory() != null) periodId = uniform.getPeriodCategory().getId();
        if (periodId == null) throw new IllegalArgumentException("Hãy chọn giai đoạn phục dựng.");
        UniformCategory period = categoryRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Giai đoạn không tồn tại."));
        validatePeriod(period, uniform.getCountry().getId(), uniform.getCategory().getId());
        uniform.setPeriodCategory(period);
        uniform.setPeriod(period.getSlug());
    }

    private void validatePeriod(UniformCategory period, Long countryId, Long categoryId) {
        if (period.getCategoryType() != UniformCategory.CategoryType.REENACTMENT || period.getParent() == null
                || !period.getParent().getId().equals(categoryId) || !countryId.equals(period.getCountryId())) {
            throw new IllegalArgumentException("Giai đoạn phải thuộc đúng quốc gia và lực lượng đã chọn.");
        }
    }

    public record ShowcaseItem(Long id, String name, String description, String history, List<Image> images) {}

    @Transactional(readOnly = true)
    public List<ShowcaseItem> getShowcase(Long countryId, Long categoryId, Long periodId) {
        UniformCategory period = categoryRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Giai đoạn không tồn tại."));
        validatePeriod(period, countryId, categoryId);
        return uniformRepository.findShowcase(countryId, categoryId, periodId, period.getSlug()).stream()
                .map(uniform -> new ShowcaseItem(uniform.getId(), uniform.getName(), uniform.getDescription(), uniform.getHistory(),
                        uniform.getImages() == null ? List.of() : uniform.getImages().stream()
                                .sorted(java.util.Comparator.comparing(Image::getId)).toList())).toList();
    }
}
