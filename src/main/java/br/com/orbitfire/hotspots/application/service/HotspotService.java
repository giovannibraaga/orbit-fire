package br.com.orbitfire.hotspots.application.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.orbitfire.hotspots.api.dto.HotspotSummaryResponse;
import br.com.orbitfire.hotspots.domain.filter.HotspotFilter;
import br.com.orbitfire.hotspots.domain.metric.HotspotMetricsCalculator;
import br.com.orbitfire.hotspots.domain.model.Hotspot;
import br.com.orbitfire.hotspots.infrastructure.config.AwsProperties;
import br.com.orbitfire.hotspots.shared.pagination.PageResponse;

/**
 * Orchestrates daily/monthly hotspot queries: resolve the S3 key, load (cached)
 * and parse, apply the domain filter, then paginate or aggregate. The load+parse
 * step is cached per key by {@link HotspotDataLoader}.
 */
@Service
public class HotspotService {

    private static final DateTimeFormatter YEAR = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM");
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter FILE_MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    private final HotspotDataLoader loader;
    private final HotspotMetricsCalculator metricsCalculator;
    private final String dailyPrefix;
    private final String monthlyPrefix;

    public HotspotService(HotspotDataLoader loader, HotspotMetricsCalculator metricsCalculator,
                          AwsProperties props) {
        this.loader = loader;
        this.metricsCalculator = metricsCalculator;
        this.dailyPrefix = props.s3().dailyPrefix();
        this.monthlyPrefix = props.s3().monthlyPrefix();
    }

    public PageResponse<Hotspot> getDailyPoints(LocalDate date, HotspotFilter filter, int page, int size) {
        List<Hotspot> filtered = filter(loader.loadDaily(dailyKey(date)), filter);
        return PageResponse.of(filtered, page, size);
    }

    public HotspotSummaryResponse getDailySummary(LocalDate date, HotspotFilter filter, int topN) {
        return metricsCalculator.calculate(filter(loader.loadDaily(dailyKey(date)), filter), topN);
    }

    public HotspotSummaryResponse getMonthlySummary(YearMonth month, HotspotFilter filter, int topN) {
        return metricsCalculator.calculate(filter(loader.loadMonthly(monthlyKey(month)), filter), topN);
    }

    private List<Hotspot> filter(List<Hotspot> hotspots, HotspotFilter filter) {
        return hotspots.stream().filter(filter::matches).toList();
    }

    /**
     * Builds e.g. {@code raw/daily/2026/05/focos_diario_br_20260531.csv}.
     */
    String dailyKey(LocalDate date) {
        return "%s/%s/%s/focos_diario_br_%s.csv".formatted(
                dailyPrefix, date.format(YEAR), date.format(MONTH), date.format(FILE_DATE));
    }

    /**
     * Builds e.g. {@code raw/monthly/2026/05/focos_mensal_br_202605.csv}.
     */
    String monthlyKey(YearMonth month) {
        return "%s/%s/%s/focos_mensal_br_%s.csv".formatted(
                monthlyPrefix, month.format(YEAR), month.format(MONTH), month.format(FILE_MONTH));
    }
}
