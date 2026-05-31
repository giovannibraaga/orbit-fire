package br.com.orbitfire.hotspots.domain.filter;

import br.com.orbitfire.hotspots.domain.model.Hotspot;
import br.com.orbitfire.hotspots.domain.model.HotspotTime;
import br.com.orbitfire.hotspots.shared.text.TextNormalizer;

/**
 * Domain filter applied in-memory over parsed hotspots. Every field is optional;
 * a {@code null} criterion is simply not applied. String matches are
 * case-insensitive; {@code bbox} is {@code [minLon, minLat, maxLon, maxLat]}.
 */
public record HotspotFilter(
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
        BoundingBox bbox) {

    public record BoundingBox(double minLon, double minLat, double maxLon, double maxLat) {
    }

    public static HotspotFilter none() {
        return new HotspotFilter(null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }

    public boolean matches(Hotspot h) {
        return matchesText(uf, h.uf())
                && matchesText(municipalityId, h.municipalityId())
                && matchesNormalized(biome, h.biome())
                && matchesNormalized(satellite, h.satellite())
                && inRange(h.fireRisk(), riskMin, riskMax)
                && inRange(h.frp(), frpMin, frpMax)
                && inRangeInt(h.daysWithoutRain(), daysWithoutRainMin, daysWithoutRainMax)
                && inRange(h.precipitation(), precipitationMin, precipitationMax)
                && inHourWindow(h.dateTimeGmt())
                && inBoundingBox(h.longitude(), h.latitude());
    }

    private boolean matchesText(String criterion, String value) {
        if (criterion == null || criterion.isBlank()) {
            return true;
        }
        return value != null && value.equalsIgnoreCase(criterion);
    }

    private boolean matchesNormalized(String criterion, String value) {
        if (criterion == null || criterion.isBlank()) {
            return true;
        }
        return value != null
                && TextNormalizer.normalize(criterion).equals(TextNormalizer.normalize(value));
    }

    private boolean inRange(Double value, Double min, Double max) {
        if (min == null && max == null) {
            return true;
        }
        if (value == null) {
            return false;
        }
        return (min == null || value >= min) && (max == null || value <= max);
    }

    private boolean inRangeInt(Integer value, Integer min, Integer max) {
        if (min == null && max == null) {
            return true;
        }
        if (value == null) {
            return false;
        }
        return (min == null || value >= min) && (max == null || value <= max);
    }

    private boolean inHourWindow(String dateTimeGmt) {
        if (hourStart == null && hourEnd == null) {
            return true;
        }
        Integer hour = HotspotTime.extractHour(dateTimeGmt);
        if (hour == null) {
            return false;
        }
        return (hourStart == null || hour >= hourStart) && (hourEnd == null || hour <= hourEnd);
    }

    private boolean inBoundingBox(Double lon, Double lat) {
        if (bbox == null) {
            return true;
        }
        if (lon == null || lat == null) {
            return false;
        }
        return lon >= bbox.minLon() && lon <= bbox.maxLon()
                && lat >= bbox.minLat() && lat <= bbox.maxLat();
    }
}
