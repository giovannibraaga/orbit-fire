package br.com.orbitfire.hotspots.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.orbitfire.hotspots.domain.filter.HotspotFilter;
import br.com.orbitfire.hotspots.domain.metric.HotspotMetricsCalculator;
import br.com.orbitfire.hotspots.domain.model.Hotspot;
import br.com.orbitfire.hotspots.infrastructure.aws.s3.S3ObjectReader;
import br.com.orbitfire.hotspots.infrastructure.config.AwsProperties;
import br.com.orbitfire.hotspots.infrastructure.csv.HotspotCsvParser;
import br.com.orbitfire.hotspots.shared.pagination.PageResponse;

@ExtendWith(MockitoExtension.class)
class HotspotServiceTest {

    @Mock
    private S3ObjectReader objectReader;

    private HotspotService service;

    @BeforeEach
    void setUp() {
        AwsProperties props = new AwsProperties(
                "us-east-1", "",
                new AwsProperties.S3("orbitfire", "raw/daily", "raw/monthly", "metadata/available-periods.json"));
        HotspotDataLoader loader = new HotspotDataLoader(objectReader, new HotspotCsvParser());
        service = new HotspotService(loader, new HotspotMetricsCalculator(), props);
    }

    @Test
    void buildsExpectedMonthlyKey() {
        assertThat(service.monthlyKey(java.time.YearMonth.of(2026, 5)))
                .isEqualTo("raw/monthly/2026/05/focos_mensal_br_202605.csv");
    }

    @Test
    void buildsExpectedDailyKey() {
        assertThat(service.dailyKey(LocalDate.of(2026, 5, 31)))
                .isEqualTo("raw/daily/2026/05/focos_diario_br_20260531.csv");
    }

    @Test
    void readsParsesAndPaginatesDailyCsv() {
        String csv = "id,lat,lon,estado\n"
                + "a1,-9.5,-56.1,MATO GROSSO\n";
        when(objectReader.readBytes(eq("raw/daily/2026/05/focos_diario_br_20260531.csv")))
                .thenReturn(csv.getBytes(StandardCharsets.UTF_8));

        PageResponse<Hotspot> page = service.getDailyPoints(
                LocalDate.of(2026, 5, 31), HotspotFilter.none(), 0, 100);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).uf()).isEqualTo("MT");
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.totalPages()).isEqualTo(1);
    }

    @Test
    void appliesFilterBeforePaging() {
        String csv = "id,lat,lon,estado\n"
                + "a1,-9.5,-56.1,MATO GROSSO\n"
                + "b2,-12.0,-38.5,BAHIA\n";
        when(objectReader.readBytes(eq("raw/daily/2026/05/focos_diario_br_20260531.csv")))
                .thenReturn(csv.getBytes(StandardCharsets.UTF_8));

        HotspotFilter onlyBahia = new HotspotFilter(
                "BA", null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);

        PageResponse<Hotspot> page = service.getDailyPoints(
                LocalDate.of(2026, 5, 31), onlyBahia, 0, 100);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).id()).isEqualTo("b2");
        assertThat(page.totalElements()).isEqualTo(1);
    }
}
