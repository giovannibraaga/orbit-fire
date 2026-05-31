package br.com.orbitfire.hotspots.api.controller;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.orbitfire.hotspots.infrastructure.aws.s3.S3ObjectNotFoundException;
import br.com.orbitfire.hotspots.infrastructure.aws.s3.S3ObjectReader;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PeriodControllerTest {

    private static final String METADATA_KEY = "metadata/available-periods.json";
    private static final String ALLOWED_ORIGIN = "http://localhost:3000";

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

    private static final String SAMPLE_JSON = """
            {
              "updatedAt": "2026-05-31T14:00:00Z",
              "periods": [
                {
                  "mode": "daily",
                  "period": "2026-05-31",
                  "date": "2026-05-31",
                  "key": "raw/daily/2026/05/focos_diario_br_20260531.csv",
                  "sizeBytes": 135000,
                  "updatedAt": "2026-05-31T14:00:00Z"
                }
              ]
            }
            """;

    @Test
    void returnsPeriodsFromMetadata() throws Exception {
        when(objectReader.readBytes(METADATA_KEY))
                .thenReturn(SAMPLE_JSON.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/v1/periods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedAt").value("2026-05-31T14:00:00Z"))
                .andExpect(jsonPath("$.periods[0].mode").value("daily"))
                .andExpect(jsonPath("$.periods[0].key")
                        .value("raw/daily/2026/05/focos_diario_br_20260531.csv"));
    }

    @Test
    void cachesMetadataAcrossRequests() throws Exception {
        when(objectReader.readBytes(METADATA_KEY))
                .thenReturn(SAMPLE_JSON.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/v1/periods")).andExpect(status().isOk());
        mockMvc.perform(get("/v1/periods")).andExpect(status().isOk());

        verify(objectReader, times(1)).readBytes(METADATA_KEY);
    }

    @Test
    void returns404WhenMetadataMissing() throws Exception {
        when(objectReader.readBytes(METADATA_KEY))
                .thenThrow(new S3ObjectNotFoundException("orbitfire", METADATA_KEY, null));

        mockMvc.perform(get("/v1/periods"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"));
    }

    @Test
    void deniesNonGetMethods() throws Exception {
        mockMvc.perform(post("/v1/periods"))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsCorsPreflightFromFrontendOrigin() throws Exception {
        mockMvc.perform(options("/v1/periods")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
    }

    @Test
    void rejectsCorsPreflightFromUnknownOrigin() throws Exception {
        mockMvc.perform(options("/v1/periods")
                        .header(HttpHeaders.ORIGIN, "http://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }
}
