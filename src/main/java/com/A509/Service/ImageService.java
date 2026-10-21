package com.A509.Service;

import com.A509.Entity.Image;
import com.A509.Repository.ImageRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class ImageService {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private Cloudinary cloudinary;

    public String uploadImage(MultipartFile file) throws IOException {
        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
        return uploadResult.get("url").toString();
    }

    public void deleteImageFromCloud(String imageUrl) {
        try {
            String publicId = getPublicIdFromUrl(imageUrl);

            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                System.out.println("Đã xóa ảnh trên Cloudinary: " + publicId);
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi xóa ảnh trên Cloudinary: " + e.getMessage());
        }
    }

    private String getPublicIdFromUrl(String url) {
        try {
            if (url == null || url.isEmpty()) return null;
            String fileName = url.substring(url.lastIndexOf("/") + 1);
            if (fileName.contains(".")) {
                return fileName.substring(0, fileName.lastIndexOf("."));
            }
            return fileName;
        } catch (Exception e) {
            return null;
        }
    }

    public List<Image> getImagesByUniformId(Long uniformId) {
        return imageRepository.findByUniformId(uniformId);
    }

    public Image addImage(Image image) {
        return imageRepository.save(image);
    }

    public Image updateImage(Long id, String description, Boolean sensitive) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ảnh không tồn tại với ID: " + id));
        if (description != null) {
            image.setDescription(description.trim());
        }
        if (sensitive != null) {
            image.setSensitive(sensitive);
        }
        return imageRepository.save(image);
    }

    public void deleteImage(Long id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ảnh không tồn tại với ID: " + id));

        deleteImageFromCloud(image.getImageUrl());

        imageRepository.deleteById(id);
    }
}
