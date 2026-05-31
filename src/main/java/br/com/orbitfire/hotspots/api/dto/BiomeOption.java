package br.com.orbitfire.hotspots.api.dto;

/**
 * One biome as a filter option: the value to send as {@code biome} param and
 * the display name for the UI (same value, kept symmetric with other options).
 */
public record BiomeOption(String value, String name) {
}
