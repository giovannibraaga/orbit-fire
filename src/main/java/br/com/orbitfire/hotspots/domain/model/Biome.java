package br.com.orbitfire.hotspots.domain.model;

/**
 * The six Brazilian biomes used by INPE. Display names follow IBGE spelling;
 * filtering normalizes accents/case so they still match raw CSV values.
 */
public enum Biome {

    AMAZONIA("Amazônia"),
    CAATINGA("Caatinga"),
    CERRADO("Cerrado"),
    MATA_ATLANTICA("Mata Atlântica"),
    PAMPA("Pampa"),
    PANTANAL("Pantanal");

    private final String displayName;

    Biome(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
