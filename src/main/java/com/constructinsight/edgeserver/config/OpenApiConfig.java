package com.constructinsight.edgeserver.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger Configuration
 * Customizes the API documentation interface
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI edgeServerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EdgeServer IoT API")
                        .description("""
                                API REST para gestión de dispositivos IoT con controles de privacidad.
                                
                                **Características principales:**
                                - Binding/Unbinding de dispositivos a usuarios
                                - Filtrado automático por propietario (ownerId)
                                - KPIs en tiempo real
                                - Carga masiva de dispositivos
                                
                                **Arquitectura:**
                                - Domain-Driven Design (DDD)
                                - Hexagonal Architecture
                                - Privacy-First Design
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ConstructInsight Team")
                                .email("support@constructinsight.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Servidor de Desarrollo")
                ));
    }
}

