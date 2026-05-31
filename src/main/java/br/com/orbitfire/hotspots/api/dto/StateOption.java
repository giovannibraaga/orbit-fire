package br.com.orbitfire.hotspots.api.dto;

/**
 * One Brazilian state as a filter option: the two-letter code to send as
 * {@code uf} param, and the display name for the UI.
 */
public record StateOption(String uf, String name) {
}
