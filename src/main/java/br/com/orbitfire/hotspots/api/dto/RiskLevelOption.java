package br.com.orbitfire.hotspots.api.dto;

/**
 * One INPE risk class as a filter option: the {@code level} name to use as
 * {@code riskMin}/{@code riskMax} bounds, and the numeric thresholds.
 */
public record RiskLevelOption(String level, String name, double min, double max) {
}
