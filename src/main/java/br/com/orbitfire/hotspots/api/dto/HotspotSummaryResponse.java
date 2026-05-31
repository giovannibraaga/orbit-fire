package br.com.orbitfire.hotspots.api.dto;

import java.util.List;
import java.util.Map;

/**
 * Aggregated indicators for {@code GET /v1/hotspots/{daily|monthly}/summary},
 * recomputed over the filtered hotspots.
 *
 * <p>Nullable averages/max are {@code null} when no hotspot carried that value.
 * {@code byHour} is keyed 0–23; {@code byRiskLevel} carries all INPE classes
 * (zeros included) in severity order.
 */
public record HotspotSummaryResponse(
        long totalHotspots,
        long veryHighRiskHotspots,
        Double averageRisk,
        Double maxFrp,
        Double averageDaysWithoutRain,
        List<CountEntry> topStates,
        List<CountEntry> topBiomes,
        List<CountEntry> topMunicipalities,
        Map<Integer, Long> byHour,
        Map<String, Long> bySatellite,
        Map<String, Long> byRiskLevel) {
}
