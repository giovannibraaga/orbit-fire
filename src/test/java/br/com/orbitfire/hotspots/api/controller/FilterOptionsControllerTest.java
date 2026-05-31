package br.com.orbitfire.hotspots.api.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.orbitfire.hotspots.infrastructure.aws.s3.S3ObjectReader;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FilterOptionsControllerTest {

    private static final String METADATA_KEY = "metadata/available-periods.json";
    private static final String DAILY_KEY_1 = "raw/daily/2026/05/focos_diario_br_20260530.csv";
    private static final String DAILY_KEY_2 = "raw/daily/2026/05/focos_diario_br_20260531.csv";
    private static final String MONTHLY_KEY = "raw/monthly/2026/05/focos_mensal_br_202605.csv";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private S3ObjectReader objectReader;

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    void statesReturns27WithUfAndName() throws Exception {
        mockMvc.perform(get("/v1/filters/states"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(27))
                .andExpect(jsonPath("$[?(@.uf=='MT')].name").value("Mato Grosso"))
                .andExpect(jsonPath("$[?(@.uf=='TO')].name").value("Tocantins"))
                .andExpect(jsonPath("$[?(@.uf=='SP')].name").value("São Paulo"));
    }

    @Test
    void biomesReturns6WithDisplayNames() throws Exception {
        mockMvc.perform(get("/v1/filters/biomes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[?(@.value=='Amazônia')]").exists())
                .andExpect(jsonPath("$[?(@.value=='Cerrado')]").exists())
                .andExpect(jsonPath("$[?(@.value=='Pantanal')]").exists());
    }

    @Test
    void riskLevelsReturns5WithThresholds() throws Exception {
        mockMvc.perform(get("/v1/filters/risk-levels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[?(@.level=='MINIMO')].min").value(0.0))
                .andExpect(jsonPath("$[?(@.level=='ALTO')].min").value(0.7))
                .andExpect(jsonPath("$[?(@.level=='CRITICO')].min").value(0.95))
                .andExpect(jsonPath("$[?(@.level=='CRITICO')].max").value(1.0));
    }

    @Test
    void allFilterEndpointsArePublic() throws Exception {
        mockMvc.perform(get("/v1/filters/states")).andExpect(status().isOk());
        mockMvc.perform(get("/v1/filters/biomes")).andExpect(status().isOk());
        mockMvc.perform(get("/v1/filters/risk-levels")).andExpect(status().isOk());
    }

    @Test
    void municipalitiesReturnsDailyMunicipalitiesGroupedByState() throws Exception {
        mockAvailablePeriods();
        when(objectReader.readBytes(DAILY_KEY_1)).thenReturn("""
                id,estado,municipio,municipio_id,satelite
                a1,RIO DE JANEIRO,Niterói,3303302,AQUA_M-T
                a2,RIO DE JANEIRO,Rio de Janeiro,3304557,NOAA-20
                """.getBytes(StandardCharsets.UTF_8));
        when(objectReader.readBytes(DAILY_KEY_2)).thenReturn("""
                id,estado,municipio,municipio_id,satelite
                b1,SÃO PAULO,São Paulo,3550308,AQUA_M-T
                b2,RIO DE JANEIRO,Niterói,3303302,NOAA-20
                """.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/v1/filters/municipalities").param("mode", "daily"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.uf=='RJ')].state").value("Rio de Janeiro"))
                .andExpect(jsonPath("$[?(@.uf=='RJ')].municipalities.length()").value(2))
                .andExpect(jsonPath("$[?(@.uf=='RJ')].municipalities[?(@.id=='3303302')].name")
                        .value("Niterói"))
                .andExpect(jsonPath("$[?(@.uf=='SP')].municipalities[0].name").value("São Paulo"));
    }

    @Test
    void satellitesReturnsMonthlySatellites() throws Exception {
        mockAvailablePeriods();
        when(objectReader.readBytes(MONTHLY_KEY)).thenReturn("""
                id,estado,municipio,municipio_id,satelite
                c1,RIO DE JANEIRO,Niterói,3303302,NOAA-20
                c2,SÃO PAULO,São Paulo,3550308,AQUA_M-T
                c3,SÃO PAULO,São Paulo,3550308,NOAA-20
                """.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/v1/filters/satellites").param("mode", "monthly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("AQUA_M-T"))
                .andExpect(jsonPath("$[1]").value("NOAA-20"));
    }

    @Test
    void returns400WhenFilterModeIsInvalid() throws Exception {
        mockMvc.perform(get("/v1/filters/satellites").param("mode", "weekly"))
                .andExpect(status().isBadRequest());
    }

    private void mockAvailablePeriods() {
        when(objectReader.readBytes(METADATA_KEY)).thenReturn("""
                {
                  "updatedAt": "2026-05-31T14:00:00Z",
                  "periods": [
                    {
                      "mode": "daily",
                      "period": "2026-05-30",
                      "date": "2026-05-30",
                      "key": "raw/daily/2026/05/focos_diario_br_20260530.csv"
                    },
                    {
                      "mode": "daily",
                      "period": "2026-05-31",
                      "date": "2026-05-31",
                      "key": "raw/daily/2026/05/focos_diario_br_20260531.csv"
                    },
                    {
                      "mode": "monthly",
                      "period": "2026-05",
                      "month": "2026-05",
                      "key": "raw/monthly/2026/05/focos_mensal_br_202605.csv"
                    }
                  ]
                }
                """.getBytes(StandardCharsets.UTF_8));
    }
}
