package br.com.orbitfire.hotspots.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.orbitfire.hotspots.api.dto.PeriodMetadataResponse;
import br.com.orbitfire.hotspots.application.service.PeriodService;

@RestController
@RequestMapping("/v1/periods")
public class PeriodController {

    private final PeriodService periodService;

    public PeriodController(PeriodService periodService) {
        this.periodService = periodService;
    }

    @GetMapping
    public PeriodMetadataResponse listPeriods() {
        return periodService.getAvailablePeriods();
    }
}
