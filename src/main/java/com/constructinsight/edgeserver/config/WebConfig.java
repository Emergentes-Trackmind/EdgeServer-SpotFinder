package com.constructinsight.edgeserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/iot/**")
                        .allowedOrigins(
                                "http://localhost:4200",      // para desarrollo local
                                "https://edgeserverspot-dudqatdsf5cebwe3.eastus2-01.azurewebsites.net", // tu backend
                                "https://brave-mushroom-0031ada10.3.azurestaticapps.net"     // frontend desplegado (sin /auth/login)
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
