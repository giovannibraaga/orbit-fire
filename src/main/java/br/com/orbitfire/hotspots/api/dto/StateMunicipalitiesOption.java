package br.com.orbitfire.hotspots.api.dto;

import java.util.List;

/**
 * Municipalities grouped by their Brazilian state.
 */
public record StateMunicipalitiesOption(
        String uf,
        String state,
        List<MunicipalityOption> municipalities) {
}
