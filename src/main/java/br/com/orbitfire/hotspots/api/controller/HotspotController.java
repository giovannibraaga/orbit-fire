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

@RestController
@RequestMapping("/v1/hotspots")
public class HotspotController {

    private final HotspotService hotspotService;

    public HotspotController(HotspotService hotspotService) {
        this.hotspotService = hotspotService;
    }

    @GetMapping("/daily/points")
    public PageResponse<Hotspot> dailyPoints(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @ModelAttribute HotspotPointsQuery query) {
        return hotspotService.getDailyPoints(
                date, query.toFilter(), query.pageOrDefault(), query.sizeOrDefault());
    }

    @GetMapping("/daily/summary")
    public HotspotSummaryResponse dailySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @ModelAttribute HotspotPointsQuery query,
            @RequestParam(defaultValue = "10") int topN) {
        return hotspotService.getDailySummary(date, query.toFilter(), topN);
    }

    @GetMapping("/monthly/summary")
    public HotspotSummaryResponse monthlySummary(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @ModelAttribute HotspotPointsQuery query,
            @RequestParam(defaultValue = "10") int topN) {
        return hotspotService.getMonthlySummary(month, query.toFilter(), topN);
    }
}
