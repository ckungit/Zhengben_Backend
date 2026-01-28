package com.zhangben.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    
    @Value("${cors.allowed-origin-patterns}")
    private String[] allowedOriginPatterns;
    
    @Value("${cors.allowed-methods}")
    private String[] allowedMethods;
    
    @Value("${cors.allowed-headers}")
    private String[] allowedHeaders;
    
    @Value("${cors.exposed-headers}")
    private String[] exposedHeaders;
    
    @Value("${cors.allow-credentials}")
    private Boolean allowCredentials;
    
    @Value("${cors.max-age}")
    private Long maxAge;
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods(allowedMethods)
                .allowedHeaders(allowedHeaders)  // 这里会包含 satoken
                .exposedHeaders(exposedHeaders)
                .allowCredentials(allowCredentials)
                .maxAge(maxAge);
    }
}