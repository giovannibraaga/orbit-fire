package br.com.orbitfire.hotspots.api.dto;

import br.com.orbitfire.hotspots.domain.filter.HotspotFilter;

/**
 * Query parameters for {@code GET /v1/hotspots/daily/points}, bound from the
 * request. Translates into a domain {@link HotspotFilter} plus pagination.
 *
 * <p>{@code bbox} is the string {@code "minLon,minLat,maxLon,maxLat"}.
 */
public record HotspotPointsQuery(
        String uf,
        String municipalityId,
        String biome,
        String satellite,
        Double riskMin,
        Double riskMax,
        Double frpMin,
        Double frpMax,
        Integer daysWithoutRainMin,
        Integer daysWithoutRainMax,
        Double precipitationMin,
        Double precipitationMax,
        Integer hourStart,
        Integer hourEnd,
        String bbox,
        Integer page,
        Integer size) {

    public static final int DEFAULT_SIZE = 500;
    public static final int MAX_SIZE = 5000;

    public HotspotFilter toFilter() {
        return new HotspotFilter(
                uf, municipalityId, biome, satellite,
                riskMin, riskMax, frpMin, frpMax,
                daysWithoutRainMin, daysWithoutRainMax,
                precipitationMin, precipitationMax,
                hourStart, hourEnd,
                parseBbox(bbox));
    }

    public int pageOrDefault() {
        return page == null || page < 0 ? 0 : page;
    }

    public int sizeOrDefault() {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private static HotspotFilter.BoundingBox parseBbox(String bbox) {
        if (bbox == null || bbox.isBlank()) {
            return null;
        }
        String[] parts = bbox.split(",");
        if (parts.length != 4) {
            throw new IllegalArgumentException(
                    "bbox must be 'minLon,minLat,maxLon,maxLat'");
        }
        try {
            return new HotspotFilter.BoundingBox(
                    Double.parseDouble(parts[0].trim()),
                    Double.parseDouble(parts[1].trim()),
                    Double.parseDouble(parts[2].trim()),
                    Double.parseDouble(parts[3].trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("bbox values must be numeric", e);
        }
    }
}
