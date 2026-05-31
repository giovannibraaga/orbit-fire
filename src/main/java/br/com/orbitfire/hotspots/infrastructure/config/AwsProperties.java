package br.com.orbitfire.hotspots.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code orbitfire.aws.*} block from application.yaml.
 *
 * <p>{@code profile} is optional: when present we authenticate with that named
 * profile (local dev); when blank we fall back to the default credential chain
 * (IAM Role in the cloud).
 */
@ConfigurationProperties(prefix = "orbitfire.aws")
public record AwsProperties(
        String region,
        String profile,
        S3 s3) {

    public record S3(
            String bucketName,
            String dailyPrefix,
            String monthlyPrefix,
            String metadata) {
    }

    public boolean hasProfile() {
        return profile != null && !profile.isBlank();
    }
}
