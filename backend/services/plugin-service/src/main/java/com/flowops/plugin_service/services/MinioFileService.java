package com.flowops.plugin_service.services;

import java.io.InputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MinioFileService implements FileService {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket.name}")
    private String bucketName;

    @Value("${minio.url}")
    private String minioUrl;

    @PostConstruct
    public void initializeBucket() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Created Minio bucket: {}", bucketName);
            } else {
                log.info("Minio bucket already exists: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Error initializing Minio bucket", e);
            throw new RuntimeException("Failed to initialize Minio bucket", e);
        }
    }

    @Override
    public String uploadFile(MultipartFile file, String pluginId, String version) {
        
        try {
            String fileName = pluginId + "-" + version + ".jar";
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );

            log.info("File uploaded to Minio: {}/{}", bucketName, fileName);
            return "/" + bucketName + "/" + fileName;
        }
        catch (Exception e) {
            log.error("Error uploading file to Minio", e);
            throw new RuntimeException("Failed to upload file to Minio", e);
        }
        
    }

    @Override
    public InputStream downloadFile(String pluginId, String version) {
        try {
        String fileName = pluginId + "-" + version + ".jar";

        log.info("Downloading file from Minio: {}/{}", bucketName, fileName);
        
        GetObjectResponse response = minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bucketName)
                .object(fileName)
                .build()
        );
        
        log.info("File downloaded from Minio: {}/{}", bucketName, fileName);
        return response;
        
        } catch (Exception e) {
            log.error("Error downloading file from Minio: {}-{}", pluginId, version, e);
            throw new RuntimeException("Failed to download file from Minio", e);
        }
    }

    @Override
    public void deleteFile(String pluginId, String version) {
        
        try {
            String fileName = pluginId + "-" + version + ".jar";
            minioClient.removeObject(
                io.minio.RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .build()
            );
            log.info("File deleted from Minio: {}/{}", bucketName, fileName);
        } catch (Exception e) {
            log.error("Error deleting file from Minio: {}-{}", pluginId, version, e);
            throw new RuntimeException("Failed to delete file from Minio", e);
        }
        
    }
}
