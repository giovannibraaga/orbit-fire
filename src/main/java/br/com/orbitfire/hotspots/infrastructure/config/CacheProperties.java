package br.com.orbitfire.hotspots.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code orbitfire.cache.*} block: per-cache TTLs (in minutes) used to
 * build the Caffeine caches. Avoids re-reading S3 and re-parsing CSV on every
 * request.
 */
@ConfigurationProperties(prefix = "orbitfire.cache")
public record CacheProperties(
        long metadataTtlMinutes,
        long dailyTtlMinutes,
        long monthlyTtlMinutes) {

    public static final String METADATA = "metadata";
    public static final String DAILY_HOTSPOTS = "dailyHotspots";
    public static final String MONTHLY_HOTSPOTS = "monthlyHotspots";
}
