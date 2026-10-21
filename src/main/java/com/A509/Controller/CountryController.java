package com.A509.Controller;

import com.A509.Entity.Country;
import com.A509.Service.CountryService;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/countries")
public class CountryController {

    @Autowired
    private CountryService countryService;

    @Autowired
    private Cloudinary cloudinary;

    @GetMapping
    public List<Country> getAllCountries() {
        return countryService.getAllCountries();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCountryById(@PathVariable Long id) {
        return countryService.getCountryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> addCountry(
            @RequestParam("countryName") String countryName,
            @RequestParam(value = "continent", required = false) String continent,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "flagFile", required = false) MultipartFile flagFile
    ) {
        try {
            Country country = new Country();
            country.setCountryName(countryName);
            country.setContinent(continent);
            country.setDescription(description);

            if (flagFile != null && !flagFile.isEmpty()) {
                Map<?, ?> uploadResult = cloudinary.uploader().upload(flagFile.getBytes(),
                        ObjectUtils.asMap("folder", "countries/flags", "resource_type", "auto"));
                country.setFlagImageUrl(uploadResult.get("secure_url").toString());
            }
            return ResponseEntity.ok(countryService.addCountry(country));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Lỗi khi upload file: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCountry(
            @PathVariable Long id,
            @RequestParam("countryName") String countryName,
            @RequestParam(value = "continent", required = false) String continent,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "flagFile", required = false) MultipartFile flagFile
    ) {
        try {
            Country existingCountry = countryService.getCountryById(id)
                    .orElseThrow(() -> new RuntimeException("Quốc gia không tồn tại"));

            existingCountry.setCountryName(countryName);
            existingCountry.setContinent(continent);
            existingCountry.setDescription(description);

            if (flagFile != null && !flagFile.isEmpty()) {
                deleteCloudinaryImage(existingCountry.getFlagImageUrl());
                Map<?, ?> uploadResult = cloudinary.uploader().upload(flagFile.getBytes(),
                        ObjectUtils.asMap("folder", "countries/flags", "resource_type", "auto"));
                existingCountry.setFlagImageUrl(uploadResult.get("secure_url").toString());
            }

            return ResponseEntity.ok(countryService.updateCountry(id, existingCountry));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Lỗi khi upload file: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCountry(@PathVariable Long id) {
        try {
            Country country = countryService.getCountryById(id)
                    .orElseThrow(() -> new RuntimeException("Quốc gia không tồn tại"));
            String flagImageUrl = country.getFlagImageUrl();

            // The service checks uniforms and reenactment categories first.
            // Do not remove the remote image when the database deletion is rejected.
            countryService.deleteCountry(id);
            deleteCloudinaryImage(flagImageUrl);
            return ResponseEntity.ok("Xóa quốc gia thành công!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private void deleteCloudinaryImage(String imageUrl) {
        String publicId = extractPublicId(imageUrl);
        if (publicId == null) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            // Database state is already correct; leave the remote image for manual cleanup.
            System.err.println("Không thể xóa ảnh Cloudinary: " + e.getMessage());
        }
    }

    private String extractPublicId(String imageUrl) {
        if (imageUrl == null || !imageUrl.contains("cloudinary.com")) {
            return null;
        }
        String[] parts = imageUrl.split("/upload/");
        if (parts.length < 2) {
            return null;
        }
        String pathAfterUpload = parts[1];
        int extensionIndex = pathAfterUpload.lastIndexOf('.');
        return extensionIndex > 0
                ? pathAfterUpload.substring(0, extensionIndex)
                : pathAfterUpload;
    }
}
