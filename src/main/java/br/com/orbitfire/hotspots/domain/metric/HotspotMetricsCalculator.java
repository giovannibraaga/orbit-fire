package br.com.orbitfire.hotspots.domain.metric;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import br.com.orbitfire.hotspots.api.dto.CountEntry;
import br.com.orbitfire.hotspots.api.dto.HotspotSummaryResponse;
import br.com.orbitfire.hotspots.domain.model.Hotspot;
import br.com.orbitfire.hotspots.domain.model.HotspotTime;

/**
 * Computes the {@link HotspotSummaryResponse} indicators over an already-filtered
 * list of hotspots. Pure function of its input — no I/O.
 */
@Component
public class HotspotMetricsCalculator {

    public HotspotSummaryResponse calculate(List<Hotspot> hotspots, int topN) {
        long veryHigh = hotspots.stream().filter(h -> RiskLevel.isVeryHigh(h.fireRisk())).count();

        return new HotspotSummaryResponse(
                hotspots.size(),
                veryHigh,
                round(average(hotspots, Hotspot::fireRisk), 4),
                round(max(hotspots, Hotspot::frp), 2),
                round(averageInt(hotspots, Hotspot::daysWithoutRain), 2),
                top(hotspots, Hotspot::uf, topN),
                top(hotspots, Hotspot::biome, topN),
                top(hotspots, Hotspot::municipality, topN),
                byHour(hotspots),
                bySatellite(hotspots),
                byRiskLevel(hotspots));
    }

    private List<CountEntry> top(List<Hotspot> hotspots, Function<Hotspot, String> keyFn, int topN) {
        return hotspots.stream()
                .map(keyFn)
                .filter(k -> k != null && !k.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(topN)
                .map(e -> new CountEntry(e.getKey(), e.getValue()))
                .toList();
    }

    private Map<Integer, Long> byHour(List<Hotspot> hotspots) {
        Map<Integer, Long> counts = new TreeMap<>();
        for (Hotspot h : hotspots) {
            Integer hour = HotspotTime.extractHour(h.dateTimeGmt());
            if (hour != null) {
                counts.merge(hour, 1L, Long::sum);
            }
        }
        return counts;
    }

    private Map<String, Long> bySatellite(List<Hotspot> hotspots) {
        return hotspots.stream()
                .map(Hotspot::satellite)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    private Map<String, Long> byRiskLevel(List<Hotspot> hotspots) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (RiskLevel level : RiskLevel.values()) {
            counts.put(level.name(), 0L);
        }
        for (Hotspot h : hotspots) {
            RiskLevel level = RiskLevel.classify(h.fireRisk());
            if (level != null) {
                counts.merge(level.name(), 1L, Long::sum);
            }
        }
        return counts;
    }

    private Double average(List<Hotspot> hotspots, Function<Hotspot, Double> valueFn) {
        return hotspots.stream()
                .map(valueFn)
                .filter(v -> v != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .stream().boxed().findFirst().orElse(null);
    }

    private Double averageInt(List<Hotspot> hotspots, Function<Hotspot, Integer> valueFn) {
        return hotspots.stream()
                .map(valueFn)
                .filter(v -> v != null)
                .mapToInt(Integer::intValue)
                .average()
                .stream().boxed().findFirst().orElse(null);
    }

    private Double max(List<Hotspot> hotspots, Function<Hotspot, Double> valueFn) {
        return hotspots.stream()
                .map(valueFn)
                .filter(v -> v != null)
                .mapToDouble(Double::doubleValue)
                .max()
                .stream().boxed().findFirst().orElse(null);
    }

    private Double round(Double value, int decimals) {
        if (value == null) {
            return null;
        }
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}
