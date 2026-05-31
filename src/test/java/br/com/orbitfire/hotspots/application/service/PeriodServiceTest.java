package br.com.orbitfire.hotspots.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.orbitfire.hotspots.api.dto.PeriodMetadataResponse;
import br.com.orbitfire.hotspots.infrastructure.aws.s3.S3ObjectNotFoundException;
import br.com.orbitfire.hotspots.infrastructure.aws.s3.S3ObjectReader;
import br.com.orbitfire.hotspots.infrastructure.config.AwsProperties;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PeriodServiceTest {

    private static final String METADATA_KEY = "metadata/available-periods.json";

    @Mock
    private S3ObjectReader objectReader;

    private PeriodService service;

    @BeforeEach
    void setUp() {
        AwsProperties props = new AwsProperties(
                "us-east-1", "",
                new AwsProperties.S3("orbitfire", "raw/daily", "raw/monthly", METADATA_KEY));
        service = new PeriodService(objectReader, new ObjectMapper(), props);
    }

    @Test
    void parsesDailyAndMonthlyEntriesIgnoringUnknownFields() {
        String json = """
                {
                  "updatedAt": "2026-05-31T14:00:00Z",
                  "extraTopLevel": "ignored",
                  "periods": [
                    {
                      "mode": "daily",
                      "period": "2026-05-31",
                      "date": "2026-05-31",
                      "key": "raw/daily/2026/05/focos_diario_br_20260531.csv",
                      "sizeBytes": 135000,
                      "updatedAt": "2026-05-31T14:00:00Z"
                    },
                    {
                      "mode": "monthly",
                      "period": "2026-04",
                      "month": "2026-04",
                      "key": "raw/monthly/2026/04/focos_mensal_br_202604.csv",
                      "sizeBytes": 980000,
                      "updatedAt": "2026-05-02T14:00:00Z",
                      "unexpected": true
                    }
                  ]
                }
                """;
        when(objectReader.readBytes(METADATA_KEY)).thenReturn(json.getBytes(StandardCharsets.UTF_8));

        PeriodMetadataResponse result = service.getAvailablePeriods();

        assertThat(result.updatedAt()).isEqualTo("2026-05-31T14:00:00Z");
        assertThat(result.periods()).hasSize(2);
        assertThat(result.periods().get(0).mode()).isEqualTo("daily");
        assertThat(result.periods().get(0).date()).isEqualTo("2026-05-31");
        assertThat(result.periods().get(0).month()).isNull();
        assertThat(result.periods().get(0).sizeBytes()).isEqualTo(135_000L);
        assertThat(result.periods().get(1).mode()).isEqualTo("monthly");
        assertThat(result.periods().get(1).month()).isEqualTo("2026-04");
        assertThat(result.periods().get(1).date()).isNull();
    }

    @Test
    void wrapsMalformedJsonAsIllegalState() {
        when(objectReader.readBytes(METADATA_KEY))
                .thenReturn("{ not valid json".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.getAvailablePeriods())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(METADATA_KEY);
    }

    @Test
    void propagatesMissingObject() {
        when(objectReader.readBytes(METADATA_KEY))
                .thenThrow(new S3ObjectNotFoundException("orbitfire", METADATA_KEY, null));

        assertThatThrownBy(() -> service.getAvailablePeriods())
                .isInstanceOf(S3ObjectNotFoundException.class);
    }
}
