package br.com.orbitfire.hotspots.api.controller;

import static org.mockito.ArgumentMatchers.eq;
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

import br.com.orbitfire.hotspots.infrastructure.aws.s3.S3ObjectNotFoundException;
import br.com.orbitfire.hotspots.infrastructure.aws.s3.S3ObjectReader;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HotspotControllerTest {

    private static final String DAILY_KEY = "raw/daily/2026/05/focos_diario_br_20260531.csv";
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

    private static final String TWO_ROWS_CSV =
            "id,lat,lon,estado,risco_fogo,frp\n"
            + "a1,-9.5,-56.1,MATO GROSSO,0.9,30\n"
            + "b2,-12.0,-38.5,BAHIA,0.2,5\n";

    @Test
    void returnsDailyPointsPaged() throws Exception {
        String csv = "id,lat,lon,estado\na1,-9.5,-56.1,MATO GROSSO\n";
        when(objectReader.readBytes(eq(DAILY_KEY)))
                .thenReturn(csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/v1/hotspots/daily/points").param("date", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("a1"))
                .andExpect(jsonPath("$.content[0].uf").value("MT"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void filtersByUf() throws Exception {
        when(objectReader.readBytes(eq(DAILY_KEY)))
                .thenReturn(TWO_ROWS_CSV.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/v1/hotspots/daily/points")
                        .param("date", "2026-05-31")
                        .param("uf", "BA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value("b2"));
    }

    @Test
    void filtersByRiskMin() throws Exception {
        when(objectReader.readBytes(eq(DAILY_KEY)))
                .thenReturn(TWO_ROWS_CSV.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/v1/hotspots/daily/points")
                        .param("date", "2026-05-31")
                        .param("riskMin", "0.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value("a1"));
    }

    @Test
    void returns400WhenBboxMalformed() throws Exception {
        when(objectReader.readBytes(eq(DAILY_KEY)))
                .thenReturn(TWO_ROWS_CSV.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/v1/hotspots/daily/points")
                        .param("date", "2026-05-31")
                        .param("bbox", "1,2,3"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsDailySummary() throws Exception {
        when(objectReader.readBytes(eq(DAILY_KEY)))
                .thenReturn(TWO_ROWS_CSV.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/v1/hotspots/daily/summary").param("date", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHotspots").value(2))
                .andExpect(jsonPath("$.veryHighRiskHotspots").value(1))
                .andExpect(jsonPath("$.maxFrp").value(30.0))
                .andExpect(jsonPath("$.byRiskLevel.ALTO").value(1))
                .andExpect(jsonPath("$.topStates.length()").value(2))
                .andExpect(jsonPath("$.topStates[?(@.key=='MT')].count").value(1));
    }

    @Test
    void summaryRespectsFilter() throws Exception {
        when(objectReader.readBytes(eq(DAILY_KEY)))
                .thenReturn(TWO_ROWS_CSV.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/v1/hotspots/daily/summary")
                        .param("date", "2026-05-31")
                        .param("uf", "BA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHotspots").value(1))
                .andExpect(jsonPath("$.veryHighRiskHotspots").value(0));
    }

    @Test
    void returnsMonthlySummary() throws Exception {
        when(objectReader.readBytes(eq(MONTHLY_KEY)))
                .thenReturn(TWO_ROWS_CSV.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/v1/hotspots/monthly/summary").param("month", "2026-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHotspots").value(2))
                .andExpect(jsonPath("$.veryHighRiskHotspots").value(1));
    }

    @Test
    void returns400WhenMonthMalformed() throws Exception {
        mockMvc.perform(get("/v1/hotspots/monthly/summary").param("month", "2026/05"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns404WhenCsvMissing() throws Exception {
        when(objectReader.readBytes(eq(DAILY_KEY)))
                .thenThrow(new S3ObjectNotFoundException("orbitfire", DAILY_KEY, null));

        mockMvc.perform(get("/v1/hotspots/daily/points").param("date", "2026-05-31"))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns400WhenDateMissing() throws Exception {
        mockMvc.perform(get("/v1/hotspots/daily/points"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns400WhenDateMalformed() throws Exception {
        mockMvc.perform(get("/v1/hotspots/daily/points").param("date", "31-05-2026"))
                .andExpect(status().isBadRequest());
    }
}
