package com.example.MyProject.Controller;

import com.example.MyProject.User.Dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * Real image upload for products. The stored filename (returned as imageUrl) is what gets
 * saved on the product; /images/** serves it back (see WebConfig).
 *
 * Security: the file TYPE is decided from the file's actual first bytes. The client's
 * Content-Type header and filename are never trusted, because both can be faked
 * (e.g. an .html file sent as "image/png" used to be saved as .html and served publicly).
 */
@RestController
@RequestMapping("/api/admin/products")
public class ImageUploadController {

    @Value("${app.upload.dir:uploads/images}")
    private String uploadDir;

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5 MB

    @PostMapping(value = "/upload-image", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            return bad("Please choose an image file.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            return bad("Image is too large - max size is 5MB.");
        }

        // SVG and HTML are deliberately NOT accepted: they can contain scripts, and these
        // files are served publicly.
        String extension = detectImageExtension(file);
        if (extension == null) {
            return bad("Only JPG, PNG, WEBP, or GIF images are allowed.");
        }

        // Server-chosen name: no collisions, no client-controlled path or extension.
        String storedFilename = UUID.randomUUID() + "." + extension;

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        Path targetPath = uploadPath.resolve(storedFilename).normalize();
        if (!targetPath.startsWith(uploadPath)) {
            return bad("Invalid file name.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                .success(true)
                .message("Image uploaded")
                .data(Map.of("imageUrl", storedFilename))
                .build());
    }

    /** Returns jpg/png/gif/webp from the file's magic bytes, or null if it is anything else. */
    private String detectImageExtension(MultipartFile file) throws IOException {
        byte[] h;
        try (InputStream in = file.getInputStream()) {
            h = in.readNBytes(12);
        }
        if (h.length < 12) return null;

        if ((h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) return "jpg";
        if ((h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') return "png";
        if (h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8') return "gif";
        if (h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') return "webp";
        return null;
    }

    private ResponseEntity<ApiResponse<Map<String, String>>> bad(String message) {
        return ResponseEntity.badRequest().body(
                ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message(message)
                        .build());
    }
}