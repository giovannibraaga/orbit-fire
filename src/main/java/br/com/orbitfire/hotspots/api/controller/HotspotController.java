package br.com.orbitfire.hotspots.api.controller;

import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.orbitfire.hotspots.api.dto.HotspotPointsQuery;
import br.com.orbitfire.hotspots.api.dto.HotspotSummaryResponse;
import br.com.orbitfire.hotspots.application.service.HotspotService;
import br.com.orbitfire.hotspots.domain.model.Hotspot;
import br.com.orbitfire.hotspots.shared.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/v1/hotspots")
public class HotspotController {

    private static final int TOP_N_MAX = 100;

    private final HotspotService hotspotService;

    public HotspotController(HotspotService hotspotService) {
        this.hotspotService = hotspotService;
    }

    @Operation(
            summary = "Consultar pontos diários",
            description = """
                    Retorna focos individuais de um dia, com filtros opcionais e paginação.

                    Exemplo:
                    GET /v1/hotspots/daily/points?date=2026-05-31&uf=TO&satellite=AQUA_M-T&page=0&size=500
                    """)
    @GetMapping("/daily/points")
    public PageResponse<Hotspot> dailyPoints(
            @Parameter(example = "2026-05-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @ModelAttribute HotspotPointsQuery query) {
        return hotspotService.getDailyPoints(
                date, query.toFilter(), query.pageOrDefault(), query.sizeOrDefault());
    }

    @Operation(
            summary = "Consultar resumo diário",
            description = """
                    Calcula métricas e agregações para um dia, recalculadas sobre os filtros enviados.

                    Exemplo:
                    GET /v1/hotspots/daily/summary?date=2026-05-31&uf=TO&biome=Cerrado&riskMin=0.7&topN=10
                    """)
    @GetMapping("/daily/summary")
    public HotspotSummaryResponse dailySummary(
            @Parameter(example = "2026-05-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @ModelAttribute HotspotPointsQuery query,
            @Parameter(example = "10", description = "Quantidade máxima de itens nos rankings. Aceita valores de 1 a 100.")
            @RequestParam(defaultValue = "10") int topN) {
        return hotspotService.getDailySummary(date, query.toFilter(), validateTopN(topN));
    }

    @Operation(
            summary = "Consultar resumo mensal",
            description = """
                    Calcula métricas e agregações para um mês, recalculadas sobre os filtros enviados.

                    Exemplo:
                    GET /v1/hotspots/monthly/summary?month=2026-04&uf=TO&satellite=AQUA_M-T&topN=10
                    """)
    @GetMapping("/monthly/summary")
    public HotspotSummaryResponse monthlySummary(
            @Parameter(example = "2026-04")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @ModelAttribute HotspotPointsQuery query,
            @Parameter(example = "10", description = "Quantidade máxima de itens nos rankings. Aceita valores de 1 a 100.")
            @RequestParam(defaultValue = "10") int topN) {
        return hotspotService.getMonthlySummary(month, query.toFilter(), validateTopN(topN));
    }

    private int validateTopN(int topN) {
        if (topN < 1 || topN > TOP_N_MAX) {
            throw new IllegalArgumentException("topN must be between 1 and " + TOP_N_MAX);
        }
        return topN;
    }
}
