package ru.live4code.social_network.ai.external.minio.service;

import io.minio.*;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import ru.live4code.social_network.ai.external.minio.model.FilenameBytearray;
import ru.live4code.social_network.ai.utils.annotation.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    @Value("${minio.bucket.post-image.storage}")
    private String postImagesStorage;

    private final MinioClient minioClient;

    public boolean isPostImagesStorageExists() {
        var bucketExistsArgs = BucketExistsArgs.builder()
                .bucket(postImagesStorage)
                .build();
        try {
            return minioClient.bucketExists(bucketExistsArgs);
        } catch (Exception exception) {
            log.error(exception.getMessage());
            return false;
        }
    }

    @Nullable
    public InputStream getPostImageByName(String name) {
        var getObjectArgs = GetObjectArgs.builder()
                .bucket(postImagesStorage)
                .object(String.format("%s.png", name))
                .build();
        try {
            return minioClient.getObject(getObjectArgs);
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public boolean uploadPostImages(List<FilenameBytearray> images) {
        var currentDateTime = ZonedDateTime.now();
        var objects = images.stream().map(item -> {
            var imageBytes = item.imageBytes();
            return new SnowballObject(
                    String.format("%s.png", item.filename()),
                    new ByteArrayInputStream(imageBytes),
                    imageBytes.length,
                    currentDateTime
            );
        }).toList();
        var uploadSnowballObjectsArgs = UploadSnowballObjectsArgs.builder()
                .bucket(postImagesStorage)
                .objects(objects)
                .build();
        try {
            minioClient.uploadSnowballObjects(uploadSnowballObjectsArgs);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
    }

}
