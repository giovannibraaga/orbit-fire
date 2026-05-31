package br.com.orbitfire.hotspots.api.dto;

/**
 * A ranked count entry (e.g. one state/biome/municipality and how many hotspots
 * it had after filtering).
 */
public record CountEntry(String key, long count) {
}
