package ru.live4code.social_network.ai.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MinioConfig {

    @Value("${minio.user.name}")
    private String minioUserName;

    @Value("${minio.user.password}")
    private String minioUserPassword;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .credentials(minioUserName, minioUserPassword)
                .endpoint("http://minio:9000")
                .build();
    }

}
