package br.com.orbitfire.hotspots.domain.model;

/**
 * A single fire hotspot (foco) as published by INPE.
 *
 * <p>Invalid sentinels ({@code -999}) and empty cells are normalized to
 * {@code null} by the parser. {@code uf} is the two-letter state code derived
 * from {@code state} (the CSV only carries the full state name); {@code state}
 * is kept for display.
 */
public record Hotspot(
        String id,
        Double latitude,
        Double longitude,
        String dateTimeGmt,
        String satellite,
        String municipality,
        String state,
        String uf,
        String country,
        String municipalityId,
        String stateId,
        String countryId,
        Integer daysWithoutRain,
        Double precipitation,
        Double fireRisk,
        String biome,
        Double frp) {
}
