package br.com.orbitfire.hotspots.domain.metric;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.orbitfire.hotspots.api.dto.HotspotSummaryResponse;
import br.com.orbitfire.hotspots.domain.model.Hotspot;

class HotspotMetricsCalculatorTest {

    private final HotspotMetricsCalculator calc = new HotspotMetricsCalculator();

    private Hotspot h(String uf, String biome, String municipality, String satellite,
                      Double risk, Double frp, Integer days, String dateTime) {
        return new Hotspot("id", -9.0, -56.0, dateTime, satellite, municipality, "STATE", uf,
                "Brasil", "1", "1", "33", days, 0.0, risk, biome, frp);
    }

    @Test
    void computesAggregatesAndRiskBuckets() {
        List<Hotspot> hotspots = List.of(
                h("MT", "Amazonia", "SINOP", "AQUA", 0.9, 30.0, 10, "2026/05/31 10:00:00"),
                h("MT", "Amazonia", "SINOP", "AQUA", 0.8, 10.0, 4, "2026/05/31 10:30:00"),
                h("BA", "Caatinga", "JUAZEIRO", "TERRA", 0.1, 5.0, 0, "2026/05/31 14:00:00"));

        HotspotSummaryResponse s = calc.calculate(hotspots, 10);

        assertThat(s.totalHotspots()).isEqualTo(3);
        assertThat(s.veryHighRiskHotspots()).isEqualTo(2);          // 0.9 and 0.8 >= 0.7
        assertThat(s.averageRisk()).isEqualTo(0.6);                  // (0.9+0.8+0.1)/3
        assertThat(s.maxFrp()).isEqualTo(30.0);
        assertThat(s.averageDaysWithoutRain()).isEqualTo(4.67);      // (10+4+0)/3 rounded
        assertThat(s.topStates()).containsExactly(
                new br.com.orbitfire.hotspots.api.dto.CountEntry("MT", 2),
                new br.com.orbitfire.hotspots.api.dto.CountEntry("BA", 1));
        assertThat(s.byRiskLevel())
                .containsEntry("CRITICO", 0L)
                .containsEntry("ALTO", 2L)
                .containsEntry("MINIMO", 1L);
        assertThat(s.byHour()).containsEntry(10, 2L).containsEntry(14, 1L);
        assertThat(s.bySatellite()).containsEntry("AQUA", 2L).containsEntry("TERRA", 1L);
    }

    @Test
    void nullValuesYieldNullAveragesAndMax() {
        List<Hotspot> hotspots = List.of(
                h("MT", null, null, null, null, null, null, "bad-date"));

        HotspotSummaryResponse s = calc.calculate(hotspots, 10);

        assertThat(s.totalHotspots()).isEqualTo(1);
        assertThat(s.veryHighRiskHotspots()).isZero();
        assertThat(s.averageRisk()).isNull();
        assertThat(s.maxFrp()).isNull();
        assertThat(s.averageDaysWithoutRain()).isNull();
        assertThat(s.topBiomes()).isEmpty();
        assertThat(s.byHour()).isEmpty();
        assertThat(s.byRiskLevel()).containsEntry("MINIMO", 0L);
    }

    @Test
    void topNLimitsRankings() {
        List<Hotspot> hotspots = List.of(
                h("MT", "B1", "M1", "S", 0.5, 1.0, 1, "t"),
                h("BA", "B2", "M2", "S", 0.5, 1.0, 1, "t"),
                h("GO", "B3", "M3", "S", 0.5, 1.0, 1, "t"));

        HotspotSummaryResponse s = calc.calculate(hotspots, 2);

        assertThat(s.topStates()).hasSize(2);
    }
}
