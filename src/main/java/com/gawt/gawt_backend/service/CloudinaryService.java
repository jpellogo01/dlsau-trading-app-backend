package com.gawt.gawt_backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Uploads images to Cloudinary using the bean from CloudinaryConfig.
 */
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    public static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final Cloudinary cloudinary;

    /** Checks type and size; throws 400 with a readable message if invalid. */
    public void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please choose a photo");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo must be JPEG, PNG or WebP");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo must be 5MB or smaller");
        }
    }

    /**
     * Uploads an image into the given Cloudinary folder.
     *
     * @return the HTTPS URL of the uploaded image
     */
    public String uploadImage(MultipartFile file, String folder) {
        validateImage(file);
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "image"));
            return (String) result.get("secure_url");
        } catch (IOException | RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Photo upload failed, please try again", e);
        }
    }
}
