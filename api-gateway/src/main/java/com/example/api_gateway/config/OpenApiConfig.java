package com.example.api_gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI gatewayOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Roomix API Gateway")
                .description("Aggregated OpenAPI entry point for Roomix services")
                .version("v1"));
    }
}
