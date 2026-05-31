package br.com.orbitfire.hotspots.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A single available period as published in {@code metadata/available-periods.json}.
 *
 * <p>{@code date} is populated for daily entries; monthly entries may instead
 * carry a {@code month} field — unknown fields are ignored so the contract stays
 * resilient to the ingestion Lambda's output.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PeriodEntryResponse(
        String mode,
        String period,
        String date,
        String month,
        String key,
        Long sizeBytes,
        String updatedAt) {
}
