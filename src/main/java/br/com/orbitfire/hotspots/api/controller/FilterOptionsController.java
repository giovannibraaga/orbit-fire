package br.com.orbitfire.hotspots.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.orbitfire.hotspots.api.dto.BiomeOption;
import br.com.orbitfire.hotspots.api.dto.RiskLevelOption;
import br.com.orbitfire.hotspots.api.dto.StateOption;
import br.com.orbitfire.hotspots.api.dto.StateMunicipalitiesOption;
import br.com.orbitfire.hotspots.application.service.FilterOptionsService;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Filter option endpoints — values the front end needs to populate
 * dropdowns and build valid filter params.
 */
@RestController
@RequestMapping("/v1/filters")
public class FilterOptionsController {

    private final FilterOptionsService filterOptionsService;

    public FilterOptionsController(FilterOptionsService filterOptionsService) {
        this.filterOptionsService = filterOptionsService;
    }

    @GetMapping("/states")
    public List<StateOption> states() {
        return filterOptionsService.getStates();
    }

    @GetMapping("/biomes")
    public List<BiomeOption> biomes() {
        return filterOptionsService.getBiomes();
    }

    @GetMapping("/risk-levels")
    public List<RiskLevelOption> riskLevels() {
        return filterOptionsService.getRiskLevels();
    }

    @GetMapping("/municipalities")
    public List<StateMunicipalitiesOption> municipalities(@RequestParam String mode) {
        return filterOptionsService.getMunicipalities(mode);
    }

    @GetMapping("/satellites")
    public List<String> satellites(@RequestParam String mode) {
        return filterOptionsService.getSatellites(mode);
    }
}
