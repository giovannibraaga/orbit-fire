package br.com.orbitfire.hotspots.api.dto;

/**
 * One municipality as a filter option: the id to send as {@code municipalityId}
 * and its display name.
 */
public record MunicipalityOption(String id, String name) {
}
