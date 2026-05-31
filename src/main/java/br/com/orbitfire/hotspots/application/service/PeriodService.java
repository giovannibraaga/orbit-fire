package br.com.orbitfire.hotspots.application.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import br.com.orbitfire.hotspots.api.dto.PeriodMetadataResponse;
import br.com.orbitfire.hotspots.infrastructure.aws.s3.S3ObjectReader;
import br.com.orbitfire.hotspots.infrastructure.config.AwsProperties;
import br.com.orbitfire.hotspots.infrastructure.config.CacheProperties;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads {@code metadata/available-periods.json} from S3 and exposes the
 * available periods catalog, caching the parsed metadata for the configured TTL.
 */
@Service
public class PeriodService {

    private final S3ObjectReader objectReader;
    private final ObjectMapper objectMapper;
    private final String metadataKey;

    public PeriodService(S3ObjectReader objectReader, ObjectMapper objectMapper, AwsProperties props) {
        this.objectReader = objectReader;
        this.objectMapper = objectMapper;
        this.metadataKey = props.s3().metadata();
    }

    @Cacheable(cacheNames = CacheProperties.METADATA, key = "'available-periods'")
    public PeriodMetadataResponse getAvailablePeriods() {
        byte[] json = objectReader.readBytes(metadataKey);
        try {
            return objectMapper.readValue(json, PeriodMetadataResponse.class);
        } catch (JacksonException e) {
            throw new IllegalStateException(
                    "Failed to parse periods metadata at key '%s'".formatted(metadataKey), e);
        }
    }
}
