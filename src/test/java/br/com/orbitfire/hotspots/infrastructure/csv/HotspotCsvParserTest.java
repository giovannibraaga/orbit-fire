package br.com.orbitfire.hotspots.infrastructure.csv;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.orbitfire.hotspots.domain.model.Hotspot;

class HotspotCsvParserTest {

    private static final String HEADER =
            "id,lat,lon,data_hora_gmt,satelite,municipio,estado,pais,municipio_id,"
            + "estado_id,pais_id,numero_dias_sem_chuva,precipitacao,risco_fogo,bioma,frp";

    private final HotspotCsvParser parser = new HotspotCsvParser();

    @Test
    void parsesRowAndDerivesUf() {
        String csv = HEADER + "\n"
                + "abc123,-9.5,-56.1,2026/05/31 13:45:00,AQUA_M-T,SINOP,MATO GROSSO,Brasil,"
                + "5107883,51,33,5,0.0,0.7,Amazonia,12.4\n";

        List<Hotspot> result = parser.parse(csv.getBytes(StandardCharsets.UTF_8));

        assertThat(result).hasSize(1);
        Hotspot h = result.get(0);
        assertThat(h.id()).isEqualTo("abc123");
        assertThat(h.latitude()).isEqualTo(-9.5);
        assertThat(h.longitude()).isEqualTo(-56.1);
        assertThat(h.state()).isEqualTo("MATO GROSSO");
        assertThat(h.uf()).isEqualTo("MT");
        assertThat(h.daysWithoutRain()).isEqualTo(5);
        assertThat(h.fireRisk()).isEqualTo(0.7);
        assertThat(h.frp()).isEqualTo(12.4);
        assertThat(h.biome()).isEqualTo("Amazonia");
    }

    @Test
    void normalizesSentinelAndEmptyToNull() {
        String csv = HEADER + "\n"
                + "id1,-9.5,-56.1,2026/05/31 13:45:00,AQUA_M-T,SINOP,PARÁ,Brasil,"
                + "1,1,33,-999,,-999,Amazonia,\n";

        Hotspot h = parser.parse(csv.getBytes(StandardCharsets.UTF_8)).get(0);

        assertThat(h.daysWithoutRain()).isNull();
        assertThat(h.precipitation()).isNull();
        assertThat(h.fireRisk()).isNull();
        assertThat(h.frp()).isNull();
        assertThat(h.uf()).isEqualTo("PA");
    }

    @Test
    void mapsByHeaderNameSoColumnOrderDoesNotMatter() {
        String csv = "frp,estado,id,lat,lon\n"
                + "99.9,GOIÁS,x1,-16.0,-49.0\n";

        Hotspot h = parser.parse(csv.getBytes(StandardCharsets.UTF_8)).get(0);

        assertThat(h.id()).isEqualTo("x1");
        assertThat(h.frp()).isEqualTo(99.9);
        assertThat(h.uf()).isEqualTo("GO");
        assertThat(h.satellite()).isNull();
    }

    @Test
    void returnsEmptyForHeaderOnlyOrBlank() {
        assertThat(parser.parse((HEADER + "\n").getBytes(StandardCharsets.UTF_8))).isEmpty();
        assertThat(parser.parse(new byte[0])).isEmpty();
    }

    @Test
    void skipsBlankLines() {
        String csv = HEADER + "\n\n"
                + "id1,-9.5,-56.1,t,SAT,M,BAHIA,Brasil,1,1,33,1,0,0,Caatinga,1\n\n";
        assertThat(parser.parse(csv.getBytes(StandardCharsets.UTF_8))).hasSize(1);
    }
}
