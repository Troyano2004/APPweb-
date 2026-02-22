// Proyectofindecurso/Backend/src/main/java/com/erwin/backend/config/StaticResourceConfig.java
package com.erwin.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${app.uploads.images-dir:uploads/images}")
    private String imagesDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path absolutePath = Paths.get(imagesDir).toAbsolutePath().normalize();
        String resourceLocation = absolutePath.toUri().toString();

        registry.addResourceHandler("/uploads/images/**")
                .addResourceLocations(resourceLocation)
                .setCachePeriod(3600);
    }
}
