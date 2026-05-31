package br.com.orbitfire.hotspots.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

import java.util.List;

/**
 * OpenAPI metadata for the Swagger UI ({@code /swagger-ui.html}) and the spec
 * ({@code /v3/api-docs}). Endpoints are documented automatically from the
 * controllers.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orbitfireOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("OrbitFire Hotspots MS")
                        .version("v1")
                        .description("API REST para consulta, filtragem e agregação de focos de "
                                + "queimadas/incêndio do INPE (OrbitFire)."))
                .servers(List.of(new Server().url("http://localhost:8080").description("Local")));
    }
}
