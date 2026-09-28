package api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Step 7 fix: the dashboard runs on localhost:4200, the API on
 * localhost:8080 -- different ports count as different origins to the
 * browser, so without this, every request from the dashboard gets
 * rejected before it reaches the controller.
 *
 * Scoped to exactly the dashboard's dev-server origin, not "*" --
 * loosening this to allow any origin would defeat the point of the
 * API key check in ApiKeyFilter.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:4200")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("Content-Type", "X-API-Key");
    }
}
