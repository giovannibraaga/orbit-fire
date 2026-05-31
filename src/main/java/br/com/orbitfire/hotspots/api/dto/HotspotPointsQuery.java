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
        validate();
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

    public void validate() {
        validateRange("risk", riskMin, riskMax, 0.0, 1.0);
        validateRange("frp", frpMin, frpMax, 0.0, null);
        validateRange("precipitation", precipitationMin, precipitationMax, 0.0, null);
        validateRange("daysWithoutRain", daysWithoutRainMin, daysWithoutRainMax, 0, null);
        validateHour("hourStart", hourStart);
        validateHour("hourEnd", hourEnd);
        if (hourStart != null && hourEnd != null && hourStart > hourEnd) {
            throw new IllegalArgumentException("hourStart must be less than or equal to hourEnd");
        }
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
            double minLon = Double.parseDouble(parts[0].trim());
            double minLat = Double.parseDouble(parts[1].trim());
            double maxLon = Double.parseDouble(parts[2].trim());
            double maxLat = Double.parseDouble(parts[3].trim());
            validateCoordinate("minLon", minLon, -180.0, 180.0);
            validateCoordinate("maxLon", maxLon, -180.0, 180.0);
            validateCoordinate("minLat", minLat, -90.0, 90.0);
            validateCoordinate("maxLat", maxLat, -90.0, 90.0);
            if (minLon > maxLon) {
                throw new IllegalArgumentException("bbox minLon must be less than or equal to maxLon");
            }
            if (minLat > maxLat) {
                throw new IllegalArgumentException("bbox minLat must be less than or equal to maxLat");
            }
            return new HotspotFilter.BoundingBox(minLon, minLat, maxLon, maxLat);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("bbox values must be numeric", e);
        }
    }

    private static void validateRange(String name, Double min, Double max, Double lowerBound, Double upperBound) {
        validateBound(name + "Min", min, lowerBound, upperBound);
        validateBound(name + "Max", max, lowerBound, upperBound);
        if (min != null && max != null && min > max) {
            throw new IllegalArgumentException(name + "Min must be less than or equal to " + name + "Max");
        }
    }

    private static void validateRange(String name, Integer min, Integer max, Integer lowerBound, Integer upperBound) {
        validateBound(name + "Min", min, lowerBound, upperBound);
        validateBound(name + "Max", max, lowerBound, upperBound);
        if (min != null && max != null && min > max) {
            throw new IllegalArgumentException(name + "Min must be less than or equal to " + name + "Max");
        }
    }

    private static void validateBound(String name, Double value, Double lowerBound, Double upperBound) {
        if (value == null) {
            return;
        }
        if (lowerBound != null && value < lowerBound) {
            throw new IllegalArgumentException(name + " must be greater than or equal to " + lowerBound);
        }
        if (upperBound != null && value > upperBound) {
            throw new IllegalArgumentException(name + " must be less than or equal to " + upperBound);
        }
    }

    private static void validateBound(String name, Integer value, Integer lowerBound, Integer upperBound) {
        if (value == null) {
            return;
        }
        if (lowerBound != null && value < lowerBound) {
            throw new IllegalArgumentException(name + " must be greater than or equal to " + lowerBound);
        }
        if (upperBound != null && value > upperBound) {
            throw new IllegalArgumentException(name + " must be less than or equal to " + upperBound);
        }
    }

    private static void validateHour(String name, Integer value) {
        validateBound(name, value, 0, 23);
    }

    private static void validateCoordinate(String name, double value, double lowerBound, double upperBound) {
        if (value < lowerBound || value > upperBound) {
            throw new IllegalArgumentException(
                    name + " must be between " + lowerBound + " and " + upperBound);
        }
    }
}
