package com.syxs.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI syxsOpenApi() {
        return new OpenAPI().info(
            new Info()
                .title("尚有新生 API")
                .version("v2.0")
                .description("Spring Boot 3 + Vue 3 + MySQL 8 接口服务")
        );
    }
}
