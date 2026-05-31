package br.com.orbitfire.hotspots.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response of {@code GET /v1/periods}: the available periods catalog read from
 * {@code metadata/available-periods.json} in S3.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PeriodMetadataResponse(
        String updatedAt,
        List<PeriodEntryResponse> periods) {
}
