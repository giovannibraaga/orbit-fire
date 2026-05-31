package br.com.orbitfire.hotspots.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code orbitfire.security.*} block from application.yaml.
 *
 * <p>{@code allowedOrigin} is the single browser origin (the frontend) allowed
 * by CORS. Direct calls (cURL, Postman) are not blocked by CORS — see the
 * security notes in the project doc.
 */
@ConfigurationProperties(prefix = "orbitfire.security")
public record SecurityProperties(
        String allowedOrigin) {
}
