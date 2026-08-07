package com.edupaste.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentStorageService.class);

    private final Path rootLocation = Paths.get("uploads");

    @Value("${aws.s3.bucket-name:}")
    private String bucketName;

    @Value("${aws.s3.region:us-east-1}")
    private String awsRegion;

    @Value("${aws.s3.access-key-id:}")
    private String accessKeyId;

    @Value("${aws.s3.secret-access-key:}")
    private String secretAccessKey;

    public String storeDocument(Long schoolId, String applicationNumber, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file.");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "document");
        String extension = "";
        int i = originalFilename.lastIndexOf('.');
        if (i >= 0) {
            extension = originalFilename.substring(i);
        }

        String storedFileName = UUID.randomUUID().toString() + extension;
        String relativePath = "uploads/" + schoolId + "/admissions/" + applicationNumber + "/" + storedFileName;

        boolean isS3Configured = StringUtils.hasText(bucketName) && StringUtils.hasText(accessKeyId) && StringUtils.hasText(secretAccessKey);

        if (isS3Configured) {
            logger.info("Uploading document to Amazon S3 bucket: {}, key: {}", bucketName, relativePath);
            try (S3Client s3Client = S3Client.builder()
                    .region(Region.of(awsRegion))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                    ))
                    .build()) {

                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(relativePath)
                        .contentType(file.getContentType())
                        .contentLength(file.getSize())
                        .build();

                s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
                logger.info("Successfully uploaded document to Amazon S3: {}", relativePath);
                return relativePath;
            } catch (Exception e) {
                logger.error("Failed to upload to Amazon S3, falling back to local storage: {}", e.getMessage(), e);
                // Fall back to local disk storage if S3 upload fails
                return storeLocally(schoolId, applicationNumber, storedFileName, file);
            }
        } else {
            logger.info("Amazon S3 credentials not configured. Saving document locally: {}", relativePath);
            return storeLocally(schoolId, applicationNumber, storedFileName, file);
        }
    }

    private String storeLocally(Long schoolId, String applicationNumber, String storedFileName, MultipartFile file) throws IOException {
        Path destinationDirectory = rootLocation.resolve(String.valueOf(schoolId))
                .resolve("admissions")
                .resolve(applicationNumber);

        Files.createDirectories(destinationDirectory);

        Path destinationFile = destinationDirectory.resolve(storedFileName);
        Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

        return destinationDirectory.resolve(storedFileName).toString().replace('\\', '/');
    }

    public org.springframework.core.io.Resource loadDocumentAsResource(String storagePath) {
        boolean isS3Configured = StringUtils.hasText(bucketName) && StringUtils.hasText(accessKeyId) && StringUtils.hasText(secretAccessKey);

        if (isS3Configured && storagePath.startsWith("uploads/")) {
            // Need to download from S3
            try (S3Client s3Client = S3Client.builder()
                    .region(Region.of(awsRegion))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                    ))
                    .build()) {
                
                software.amazon.awssdk.services.s3.model.GetObjectRequest getObjectRequest = software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(storagePath)
                        .build();

                software.amazon.awssdk.core.ResponseInputStream<software.amazon.awssdk.services.s3.model.GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
                return new org.springframework.core.io.InputStreamResource(s3Object);
            } catch (Exception e) {
                logger.error("Failed to download from S3: {}", e.getMessage(), e);
                // Fall back to local if file exists locally
            }
        }

        try {
            Path filePath = Paths.get(storagePath).normalize();
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found " + storagePath);
            }
        } catch (java.net.MalformedURLException ex) {
            throw new RuntimeException("File not found " + storagePath, ex);
        }
    }
}
