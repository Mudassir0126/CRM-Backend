/**
 * Browser cross-origin configuration for the local React frontend.
 * An origin includes scheme, host, and port; localhost:5173 and localhost:8080 are different origins.
 */
package com.lankaswisstek.salesorder.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Register this class as Spring configuration; it adds to the default MVC setup.
@Configuration
public class WebConfig implements WebMvcConfigurer {
    /** Allow the development frontend to call API routes with the listed HTTP methods and headers.
     * CORS controls browser cross-origin access; it does not authenticate API callers. */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
