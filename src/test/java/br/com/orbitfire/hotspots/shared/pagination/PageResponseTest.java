package br.com.orbitfire.hotspots.shared.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class PageResponseTest {

    @Test
    void returnsEmptyContentWhenPageOffsetExceedsIntegerRange() {
        PageResponse<String> page = PageResponse.of(List.of("a", "b"), Integer.MAX_VALUE, 5000);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.totalPages()).isEqualTo(1);
    }
}
