package br.com.orbitfire.hotspots.domain.model;

/**
 * The 27 Brazilian federative units (UF code + display name). Single source of
 * truth for state options and for {@link BrazilianStateResolver}.
 */
public enum BrazilianState {

    AC("Acre"),
    AL("Alagoas"),
    AP("Amapá"),
    AM("Amazonas"),
    BA("Bahia"),
    CE("Ceará"),
    DF("Distrito Federal"),
    ES("Espírito Santo"),
    GO("Goiás"),
    MA("Maranhão"),
    MT("Mato Grosso"),
    MS("Mato Grosso do Sul"),
    MG("Minas Gerais"),
    PA("Pará"),
    PB("Paraíba"),
    PR("Paraná"),
    PE("Pernambuco"),
    PI("Piauí"),
    RJ("Rio de Janeiro"),
    RN("Rio Grande do Norte"),
    RS("Rio Grande do Sul"),
    RO("Rondônia"),
    RR("Roraima"),
    SC("Santa Catarina"),
    SP("São Paulo"),
    SE("Sergipe"),
    TO("Tocantins");

    private final String displayName;

    BrazilianState(String displayName) {
        this.displayName = displayName;
    }

    public String uf() {
        return name();
    }

    public String displayName() {
        return displayName;
    }
}
