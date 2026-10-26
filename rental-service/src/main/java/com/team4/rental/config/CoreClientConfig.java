package com.team4.rental.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class CoreClientConfig {
    @Bean
    RestClient coreRestClient(RestClient.Builder builder,
                             @Value("${core-service.url}") String url,
                             @Value("${core-service.connect-timeout}") Duration connectTimeout,
                             @Value("${core-service.read-timeout}") Duration readTimeout) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return builder.baseUrl(url).requestFactory(factory).build();
    }
}
