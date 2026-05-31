package br.com.orbitfire.hotspots.application.service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import br.com.orbitfire.hotspots.api.dto.BiomeOption;
import br.com.orbitfire.hotspots.api.dto.MunicipalityOption;
import br.com.orbitfire.hotspots.api.dto.PeriodEntryResponse;
import br.com.orbitfire.hotspots.api.dto.RiskLevelOption;
import br.com.orbitfire.hotspots.api.dto.StateMunicipalitiesOption;
import br.com.orbitfire.hotspots.api.dto.StateOption;
import br.com.orbitfire.hotspots.domain.metric.RiskLevel;
import br.com.orbitfire.hotspots.domain.model.Biome;
import br.com.orbitfire.hotspots.domain.model.BrazilianState;
import br.com.orbitfire.hotspots.domain.model.Hotspot;
import br.com.orbitfire.hotspots.infrastructure.config.CacheProperties;

/**
 * Provides filter option lists derived from domain enums and ingested hotspot
 * files.
 *
 * <p>States and biomes are backed by their respective enums.
 * Risk levels expose the INPE thresholds so the front end can build
 * {@code riskMin}/{@code riskMax} pairs without hardcoding numbers.
 */
@Service
public class FilterOptionsService {

    private static final String DAILY = "daily";
    private static final String MONTHLY = "monthly";

    private final PeriodService periodService;
    private final HotspotDataLoader dataLoader;

    public FilterOptionsService(PeriodService periodService, HotspotDataLoader dataLoader) {
        this.periodService = periodService;
        this.dataLoader = dataLoader;
    }

    public List<StateOption> getStates() {
        return Arrays.stream(BrazilianState.values())
                .map(s -> new StateOption(s.uf(), s.displayName()))
                .toList();
    }

    public List<BiomeOption> getBiomes() {
        return Arrays.stream(Biome.values())
                .map(b -> new BiomeOption(b.displayName(), b.displayName()))
                .toList();
    }

    public List<RiskLevelOption> getRiskLevels() {
        return List.of(
                new RiskLevelOption("MINIMO", "Mínimo", 0.0, RiskLevel.BAIXO_MIN),
                new RiskLevelOption("BAIXO", "Baixo", RiskLevel.BAIXO_MIN, RiskLevel.MEDIO_MIN),
                new RiskLevelOption("MEDIO", "Médio", RiskLevel.MEDIO_MIN, RiskLevel.ALTO_MIN),
                new RiskLevelOption("ALTO", "Alto", RiskLevel.ALTO_MIN, RiskLevel.CRITICO_MIN),
                new RiskLevelOption("CRITICO", "Crítico", RiskLevel.CRITICO_MIN, 1.0));
    }

    @Cacheable(cacheNames = CacheProperties.METADATA, key = "'filter-municipalities-' + #mode.toLowerCase()")
    public List<StateMunicipalitiesOption> getMunicipalities(String mode) {
        Map<String, StateMunicipalitiesAccumulator> byState = new TreeMap<>();

        for (Hotspot hotspot : loadHotspots(normalizeMode(mode))) {
            if (isBlank(hotspot.uf()) || isBlank(hotspot.municipality())) {
                continue;
            }
            byState.computeIfAbsent(hotspot.uf(), StateMunicipalitiesAccumulator::new)
                    .put(hotspot.municipalityId(), hotspot.municipality());
        }

        return byState.values().stream()
                .map(StateMunicipalitiesAccumulator::toResponse)
                .toList();
    }

    @Cacheable(cacheNames = CacheProperties.METADATA, key = "'filter-satellites-' + #mode.toLowerCase()")
    public List<String> getSatellites(String mode) {
        return loadHotspots(normalizeMode(mode)).stream()
                .map(Hotspot::satellite)
                .filter(s -> !isBlank(s))
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private List<Hotspot> loadHotspots(String mode) {
        List<PeriodEntryResponse> periods = periodService.getAvailablePeriods().periods();
        if (periods == null) {
            return List.of();
        }
        return periods.stream()
                .filter(period -> mode.equalsIgnoreCase(period.mode()))
                .map(PeriodEntryResponse::key)
                .filter(key -> !isBlank(key))
                .flatMap(key -> loadPeriod(mode, key).stream())
                .toList();
    }

    private List<Hotspot> loadPeriod(String mode, String key) {
        if (DAILY.equals(mode)) {
            return dataLoader.loadDaily(key);
        }
        return dataLoader.loadMonthly(key);
    }

    private String normalizeMode(String mode) {
        if (mode == null) {
            throw new IllegalArgumentException("mode must be 'daily' or 'monthly'");
        }
        String normalized = mode.trim().toLowerCase(Locale.ROOT);
        if (!DAILY.equals(normalized) && !MONTHLY.equals(normalized)) {
            throw new IllegalArgumentException("mode must be 'daily' or 'monthly'");
        }
        return normalized;
    }

    private String stateName(String uf) {
        try {
            return BrazilianState.valueOf(uf).displayName();
        } catch (IllegalArgumentException e) {
            return uf;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private final class StateMunicipalitiesAccumulator {
        private final String uf;
        private final Map<String, MunicipalityOption> municipalities = new LinkedHashMap<>();

        private StateMunicipalitiesAccumulator(String uf) {
            this.uf = uf;
        }

        private void put(String id, String name) {
            String key = !isBlank(id) ? id : name.trim().toLowerCase(Locale.ROOT);
            municipalities.putIfAbsent(key, new MunicipalityOption(id, name));
        }

        private StateMunicipalitiesOption toResponse() {
            return new StateMunicipalitiesOption(
                    uf,
                    stateName(uf),
                    municipalities.values().stream()
                            .filter(Objects::nonNull)
                            .sorted(Comparator.comparing(MunicipalityOption::name, String.CASE_INSENSITIVE_ORDER))
                            .toList());
        }
    }
}
