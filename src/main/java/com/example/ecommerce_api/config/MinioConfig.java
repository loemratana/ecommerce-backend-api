package com.example.ecommerce_api.config;


import io.minio.MinioClient;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class MinioConfig {

    @Value("${app.minio.url}")
    private String url ;

    @Value("${app.minio.access.name}")
    private String accessKey;

    @Value("${app.minio.access.secret}")
    private String accessSecret;


    public MinioClient minioClient()
    {

        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey,accessSecret)
                .build();
    }


}
