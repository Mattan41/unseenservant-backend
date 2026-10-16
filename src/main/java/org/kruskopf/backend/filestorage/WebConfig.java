package org.kruskopf.backend.filestorage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves uploaded files from {@code /images/**}.
 *
 * <p>No CORS mapping here: {@code SecurityConfig} registers a
 * {@code CorsConfigurationSource} for {@code /**}, which covers this path, and it
 * runs first for every request. {@code DefaultCorsProcessor} then skips adding a
 * second header because the response already carries one.
 * {@code ImageCorsIT} pins that down — an image request must carry exactly one
 * {@code Access-Control-Allow-Origin}.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + uploadDir + "/")
                .setCachePeriod(3600)
                .resourceChain(true);
    }
}

