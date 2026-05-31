package br.com.orbitfire.hotspots.infrastructure.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Caffeine caches with per-cache TTLs (the metadata, daily and monthly data have
 * different freshness needs). Uses {@link SimpleCacheManager} so each named cache
 * can have its own expiry.
 */
@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(CacheProperties props) {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                caffeineCache(CacheProperties.METADATA, props.metadataTtlMinutes()),
                caffeineCache(CacheProperties.DAILY_HOTSPOTS, props.dailyTtlMinutes()),
                caffeineCache(CacheProperties.MONTHLY_HOTSPOTS, props.monthlyTtlMinutes())));
        return manager;
    }

    private CaffeineCache caffeineCache(String name, long ttlMinutes) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .recordStats()
                .expireAfterWrite(Duration.ofMinutes(ttlMinutes))
                .build());
    }
}
