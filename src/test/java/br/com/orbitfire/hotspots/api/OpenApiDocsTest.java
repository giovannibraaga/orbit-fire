package br.com.orbitfire.hotspots.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocsArePublicAndListEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("OrbitFire Hotspots MS"))
                .andExpect(jsonPath("$.paths['/v1/periods']").exists())
                .andExpect(jsonPath("$.paths['/v1/hotspots/daily/points']").exists())
                .andExpect(jsonPath("$.paths['/v1/hotspots/daily/summary']").exists())
                .andExpect(jsonPath("$.paths['/v1/hotspots/monthly/summary']").exists())
                .andExpect(jsonPath("$.paths['/v1/filters/municipalities']").exists())
                .andExpect(jsonPath("$.paths['/v1/filters/satellites']").exists());
    }

    @Test
    void apiDocsIncludeRequestExamples() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/v1/hotspots/monthly/summary'].get.description",
                        containsString("month=2026-04&uf=TO&satellite=AQUA_M-T")))
                .andExpect(jsonPath("$.paths['/v1/hotspots/daily/points'].get.description",
                        containsString("date=2026-05-31&uf=TO&satellite=AQUA_M-T")));
    }

    @Test
    void swaggerUiIsReachable() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
