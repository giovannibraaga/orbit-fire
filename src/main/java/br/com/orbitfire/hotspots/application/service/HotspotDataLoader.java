package br.com.orbitfire.hotspots.application.service;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import br.com.orbitfire.hotspots.domain.model.Hotspot;
import br.com.orbitfire.hotspots.infrastructure.aws.s3.S3ObjectReader;
import br.com.orbitfire.hotspots.infrastructure.config.CacheProperties;
import br.com.orbitfire.hotspots.infrastructure.csv.HotspotCsvParser;

/**
 * Loads and parses hotspot CSVs from S3, caching the parsed result by object key.
 *
 * <p>Daily and monthly use separate caches (different TTLs) — hence two thin
 * methods rather than one, since the cache name must be fixed per annotation.
 */
@Component
public class HotspotDataLoader {

    private final S3ObjectReader objectReader;
    private final HotspotCsvParser parser;

    public HotspotDataLoader(S3ObjectReader objectReader, HotspotCsvParser parser) {
        this.objectReader = objectReader;
        this.parser = parser;
    }

    @Cacheable(cacheNames = CacheProperties.DAILY_HOTSPOTS, key = "#key")
    public List<Hotspot> loadDaily(String key) {
        return load(key);
    }

    @Cacheable(cacheNames = CacheProperties.MONTHLY_HOTSPOTS, key = "#key")
    public List<Hotspot> loadMonthly(String key) {
        return load(key);
    }

    private List<Hotspot> load(String key) {
        return parser.parse(objectReader.readBytes(key));
    }
}
