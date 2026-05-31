package br.com.orbitfire.hotspots.shared.pagination;

import java.util.List;

/**
 * Lightweight pagination envelope used by list endpoints. We page in-memory over
 * the parsed CSV (no database), so this carries only what the frontend needs.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PageResponse<T> of(List<T> all, int page, int size) {
        int total = all.size();
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        int from = (int) Math.min((long) page * size, total);
        int to = Math.min(from + size, total);
        List<T> slice = from >= to ? List.of() : List.copyOf(all.subList(from, to));
        return new PageResponse<>(slice, page, size, total, totalPages);
    }
}
