package com.syxs.config;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Data
@Configuration
@ConfigurationProperties(prefix = "syxs.file")
public class FileStorageConfig implements WebMvcConfigurer {

    private String uploadDir = "./uploads";
    private String accessPath = "/uploads/**";

    @PostConstruct
    public void initUploadDir() throws IOException {
        Path preferred = Path.of(uploadDir).toAbsolutePath().normalize();
        Path parentUploads = Path.of("..", "uploads").toAbsolutePath().normalize();

        if (!Files.exists(preferred) && Files.exists(parentUploads)) {
            preferred = parentUploads;
        }

        Files.createDirectories(preferred);
        uploadDir = preferred.toString();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String resourceLocation = Path.of(uploadDir).toUri().toString();
        registry.addResourceHandler(accessPath).addResourceLocations(resourceLocation);
        if (!"/files/**".equals(accessPath)) {
            registry.addResourceHandler("/files/**").addResourceLocations(resourceLocation);
        }
    }
}
