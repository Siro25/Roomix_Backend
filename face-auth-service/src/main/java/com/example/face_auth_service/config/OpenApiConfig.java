package com.example.face_auth_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI faceAuthServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Roomix Face Auth Service API")
                .description("Face authentication endpoints")
                .version("v1"));
    }
}
