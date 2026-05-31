package br.com.orbitfire.hotspots.domain.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import br.com.orbitfire.hotspots.domain.model.Hotspot;

class HotspotFilterNormalizedTest {

    private Hotspot hotspot(String biome, String satellite) {
        return new Hotspot("id", -9.0, -56.0, "t", satellite, "MUN", "STATE", "MT",
                "Brasil", "1", "1", "33", 3, 0.0, 0.5, biome, 10.0);
    }

    @Test
    void biomeFilterMatchesWithAccent() {
        HotspotFilter f = new HotspotFilter(null, null, "Amazônia", null, null, null,
                null, null, null, null, null, null, null, null, null);

        assertThat(f.matches(hotspot("Amazonia", "SAT"))).isTrue();
        assertThat(f.matches(hotspot("AMAZÔNIA", "SAT"))).isTrue();
        assertThat(f.matches(hotspot("Cerrado", "SAT"))).isFalse();
    }

    @Test
    void biomeFilterMatchesWithoutAccent() {
        HotspotFilter f = new HotspotFilter(null, null, "Amazonia", null, null, null,
                null, null, null, null, null, null, null, null, null);

        assertThat(f.matches(hotspot("Amazônia", "SAT"))).isTrue();
    }

    @Test
    void satelliteFilterIsCaseInsensitive() {
        HotspotFilter f = new HotspotFilter(null, null, null, "aqua_m-t", null, null,
                null, null, null, null, null, null, null, null, null);

        assertThat(f.matches(hotspot("Cerrado", "AQUA_M-T"))).isTrue();
        assertThat(f.matches(hotspot("Cerrado", "TERRA"))).isFalse();
    }
}
