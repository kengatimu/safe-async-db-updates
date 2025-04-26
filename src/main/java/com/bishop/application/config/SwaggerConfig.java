package com.bishop.application.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("Safe Async Database Update API")
                        .description("API for processing credit transfer transactions with safe asynchronous database updates")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Kennedy Gatimu")
                                .url("https://www.linkedin.com/in/kengatimu/")
                                .email("kengatimu@gmail.com")
                        )
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")
                        )
                )
                .externalDocs(new ExternalDocumentation()
                        .description("Project GitHub Repository")
                        .url("https://github.com/kengatimu/safe-async-db-updates"));
    }
}
