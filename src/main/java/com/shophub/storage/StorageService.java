package com.shophub.storage;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final AmazonS3 amazonS3;

    @Value("${app.tigris.bucket-name}")
    private String bucketName;

    @Value("${app.tigris.endpoint}")
    private String endpoint;

    @Value("${app.tigris.access-key:}")
    private String accessKey;

    public String uploadFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                : "";
        String filename = UUID.randomUUID() + extension;

        // Try Tigris upload if real access key provided
        if (accessKey != null && !accessKey.equals("mock_access_key") && !accessKey.isBlank()) {
            try {
                String key = "products/" + filename;
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentType(file.getContentType());
                metadata.setContentLength(file.getSize());

                PutObjectRequest putRequest = new PutObjectRequest(
                        bucketName, key, file.getInputStream(), metadata)
                        .withCannedAcl(CannedAccessControlList.PublicRead);
                amazonS3.putObject(putRequest);

                String fileUrl = String.format("%s/%s/%s", endpoint, bucketName, key);
                log.info("Uploaded file to Tigris S3: {}", fileUrl);
                return fileUrl;
            } catch (Exception e) {
                log.warn("Tigris upload failed ({}), falling back to local file storage...", e.getMessage());
            }
        }

        // Fallback: Save to local uploads directory
        try {
            Path uploadDir = Paths.get("uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            Path filePath = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            String localUrl = "http://localhost:8080/uploads/" + filename;
            log.info("Saved file locally: {}", localUrl);
            return localUrl;
        } catch (IOException e) {
            log.error("Failed to store file locally: {}", e.getMessage());
            throw new RuntimeException("File upload failed", e);
        }
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null) return;
        try {
            if (fileUrl.contains("products/")) {
                String key = fileUrl.substring(fileUrl.indexOf("products/"));
                amazonS3.deleteObject(bucketName, key);
            } else if (fileUrl.contains("/uploads/")) {
                String filename = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
                Path filePath = Paths.get("uploads").resolve(filename);
                Files.deleteIfExists(filePath);
            }
        } catch (Exception e) {
            log.warn("Could not delete file {}: {}", fileUrl, e.getMessage());
        }
    }
}
