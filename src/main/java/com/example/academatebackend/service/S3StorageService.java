package com.example.academatebackend.service;

import com.example.academatebackend.config.S3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    /**
     * Generate a pre-signed PUT URL so the client can upload directly to S3.
     *
     * @param folder     e.g. "avatars", "content"
     * @param fileName   original file name (used to derive extension)
     * @param contentType MIME type, e.g. "video/mp4"
     * @return pre-signed upload URL
     */
    public String generateUploadUrl(String folder, String fileName, String contentType) {
        String key = buildKey(folder, fileName);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(s3Properties.getBucketName())
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(s3Properties.getPresignedUrlExpiration())
                .putObjectRequest(putRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
        log.info("Generated pre-signed upload URL for key: {}", key);
        return presigned.url().toString();
    }

    /**
     * Returns the public (or CDN) URL for a stored object.
     */
    public String getObjectUrl(String key) {
        return "https://" + s3Properties.getBucketName()
                + ".s3." + s3Properties.getRegion()
                + ".amazonaws.com/" + key;
    }

    /**
     * Delete an object from S3 by its full key.
     */
    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(s3Properties.getBucketName())
                .key(key)
                .build());
        log.info("Deleted S3 object: {}", key);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildKey(String folder, String fileName) {
        String ext = "";
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0) ext = fileName.substring(dot);
        return folder + "/" + UUID.randomUUID() + ext;
    }
}
