package com.example.MyProject.Controller;

import com.example.MyProject.User.Dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Real image upload for products - this is what was missing before: the
 * admin form only had a text box where you had to already know an exact
 * filename that existed in the codebase. Now an actual file gets uploaded,
 * stored, and its generated filename is what gets saved on the product.
 */
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class ImageUploadController {

    @Value("${app.upload.dir:uploads/images}")
    private String uploadDir;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5 MB

    @PostMapping(value = "/upload-image", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.<Map<String, String>>builder()
                            .success(false)
                            .message("Please choose an image file.")
                            .build());
        }

        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.<Map<String, String>>builder()
                            .success(false)
                            .message("Only JPG, PNG, WEBP, or GIF images are allowed.")
                            .build());
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.<Map<String, String>>builder()
                            .success(false)
                            .message("Image is too large - max size is 5MB.")
                            .build());
        }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image";
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex);
        }
        // Random filename - avoids collisions between admins uploading
        // files with the same name, and avoids trusting client input as a
        // filesystem path.
        String storedFilename = UUID.randomUUID() + extension;

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
        Files.createDirectories(uploadPath);

        Path targetPath = uploadPath.resolve(storedFilename).normalize();
        // Defense in depth: make sure the resolved path is still inside the
        // upload directory (protects against a crafted filename trying to
        // escape via "../..").
        if (!targetPath.startsWith(uploadPath)) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.<Map<String, String>>builder()
                            .success(false)
                            .message("Invalid file name.")
                            .build());
        }

        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // This is exactly what the product's imageUrl field expects - just
        // the filename, since productImageUrl() on the frontend (and the
        // /images/** resource handler here) prefix it consistently.
        return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                .success(true)
                .message("Image uploaded")
                .data(Map.of("imageUrl", storedFilename))
                .build());
    }
}
