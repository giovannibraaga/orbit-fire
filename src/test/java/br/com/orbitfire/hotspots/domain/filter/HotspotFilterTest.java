package br.com.orbitfire.hotspots.domain.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import br.com.orbitfire.hotspots.domain.model.Hotspot;

class HotspotFilterTest {

    private Hotspot hotspot(String uf, Double lat, Double lon, Double risk, String dateTime) {
        return new Hotspot("id", lat, lon, dateTime, "SAT", "MUN", "STATE", uf, "Brasil",
                "1", "1", "33", 3, 0.0, risk, "Amazonia", 10.0);
    }

    @Test
    void noneMatchesEverything() {
        assertThat(HotspotFilter.none().matches(hotspot("MT", -9.0, -56.0, 0.5, "2026/05/31 10:00:00")))
                .isTrue();
    }

    @Test
    void filtersByUfCaseInsensitive() {
        HotspotFilter f = filterWithUf("mt");
        assertThat(f.matches(hotspot("MT", -9.0, -56.0, 0.5, "t"))).isTrue();
        assertThat(f.matches(hotspot("BA", -9.0, -56.0, 0.5, "t"))).isFalse();
    }

    @Test
    void riskRangeExcludesNullValueWhenBounded() {
        HotspotFilter f = new HotspotFilter(null, null, null, null, 0.5, null, null, null,
                null, null, null, null, null, null, null);
        assertThat(f.matches(hotspot("MT", -9.0, -56.0, 0.9, "t"))).isTrue();
        assertThat(f.matches(hotspot("MT", -9.0, -56.0, 0.2, "t"))).isFalse();
        assertThat(f.matches(hotspot("MT", -9.0, -56.0, null, "t"))).isFalse();
    }

    @Test
    void boundingBoxMatchesInsideOnly() {
        HotspotFilter f = new HotspotFilter(null, null, null, null, null, null, null, null,
                null, null, null, null, null, null,
                new HotspotFilter.BoundingBox(-60.0, -15.0, -50.0, -5.0));
        assertThat(f.matches(hotspot("MT", -9.0, -56.0, 0.5, "t"))).isTrue();
        assertThat(f.matches(hotspot("MT", -20.0, -56.0, 0.5, "t"))).isFalse();
    }

    @Test
    void hourWindowUsesDateTimeGmt() {
        HotspotFilter f = new HotspotFilter(null, null, null, null, null, null, null, null,
                null, null, null, null, 9, 12, null);
        assertThat(f.matches(hotspot("MT", -9.0, -56.0, 0.5, "2026/05/31 10:30:00"))).isTrue();
        assertThat(f.matches(hotspot("MT", -9.0, -56.0, 0.5, "2026-05-31T14:30:00"))).isFalse();
    }

    private HotspotFilter filterWithUf(String uf) {
        return new HotspotFilter(uf, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }
}
