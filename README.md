# OrbitFire Hotspots Microservice

OrbitFire Hotspots is a REST microservice for querying, filtering and aggregating wildfire and fire-hotspot data published by Brazil's National Institute for Space Research (INPE).

Daily and monthly source data is ingested as CSV files into Amazon S3. The service loads the required data on demand, applies in-memory filters and exposes both individual hotspot records and aggregated metrics for the OrbitFire frontend.

## Stack

- **Java 17** and **Spring Boot 4.0.6** with modular starters for Web MVC, Security, Jackson, Validation, Cache and Actuator
- **Jackson 3** (`tools.jackson.*`) for JSON serialization
- **AWS SDK for Java 2.x** for Amazon S3 access
- **Caffeine** for in-memory caching
- **springdoc-openapi** for Swagger and OpenAPI documentation
- **Maven** with the Maven Wrapper (`./mvnw`)
- **Docker** for containerized execution

## Architecture

The codebase follows a layered architecture under `br.com.orbitfire.hotspots`:

| Layer | Packages | Responsibility |
| --- | --- | --- |
| API | `api.controller`, `api.dto`, `api.exception` | REST endpoints, request/response DTOs and global error handling |
| Application | `application.service` | Orchestration: resolves S3 keys, loads and parses data, applies caching, filters records and calculates metrics |
| Domain | `domain.model`, `domain.filter`, `domain.metric` | `Hotspot` model, in-memory filtering, INPE risk classes and metric calculation |
| Infrastructure | `infrastructure.aws.s3`, `infrastructure.csv`, `infrastructure.config` | S3 object access, CSV parsing and AWS, cache, security and OpenAPI configuration |
| Shared | `shared.pagination`, `shared.text` | Generic pagination and text normalization |

### Request flow

1. The controller receives a date or month and optional filters.
2. `HotspotService` builds the S3 object key, for example `raw/daily/2026/05/focos_diario_br_20260531.csv`.
3. `HotspotDataLoader` reads and parses the CSV from S3. Parsed results are cached by object key.
4. `HotspotFilter` applies the requested in-memory filters.
5. The result is either paginated for `/points` or aggregated by `HotspotMetricsCalculator` for `/summary`.

## Solution architecture diagrams

The diagrams use C4-style views to present the system at different levels of detail.

### Figure 1. OrbitFire system context diagram

<img width="882" height="491" alt="orbitfire-solution-architecture-Context drawio" src="https://github.com/user-attachments/assets/1b798da0-f418-4f6c-80b4-86bd94d9280a" />

*Figure 1. High-level relationship between the OrbitFire website, the OrbitFire microservice and external data sources.*

### Figure 2. OrbitFire container diagram

<img width="1401" height="931" alt="orbitfire-solution-architecture-Container" src="https://github.com/user-attachments/assets/b4bd1547-158f-4328-9230-0e75239305d8" />

*Figure 2. Main application containers, AWS services and the data-ingestion flow.*

### Figure 3. OrbitFire component diagram

<img width="2176" height="1221" alt="orbitfire-solution-architecture-Component" src="https://github.com/user-attachments/assets/5ad2096a-2e6a-4fb1-91a7-42ad8a1502f5" />

*Figure 3. Internal components involved in request handling, S3 access, CSV parsing, filtering, aggregation and caching.*

## API

The API base path is `/v1`. All read endpoints (`GET /v1/**`) are publicly available in the MVP.

### Hotspots - `/v1/hotspots`

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/daily/points` | Individual hotspots for a day, with filtering and pagination |
| `GET` | `/daily/summary` | Daily metrics and aggregations |
| `GET` | `/monthly/summary` | Monthly metrics and aggregations |

Examples:

```http
GET /v1/hotspots/daily/points?date=2026-05-31&uf=TO&satellite=AQUA_M-T&page=0&size=500
GET /v1/hotspots/daily/summary?date=2026-05-31&uf=TO&biome=Cerrado&riskMin=0.7&topN=10
GET /v1/hotspots/monthly/summary?month=2026-04&uf=TO&satellite=AQUA_M-T&topN=10
```

### Available filters

All filters are optional and can be combined:

- `uf`
- `municipalityId`
- `biome`
- `satellite`
- `riskMin` and `riskMax` from 0 to 1
- `frpMin` and `frpMax`
- `daysWithoutRainMin` and `daysWithoutRainMax`
- `precipitationMin` and `precipitationMax`
- `hourStart` and `hourEnd` from 0 to 23
- `bbox` in the format `minLon,minLat,maxLon,maxLat`

Pagination uses `page` (default `0`) and `size` (default `500`, maximum `5000`). Ranking queries accept `topN` from 1 to 100 (default `10`).

Summary responses include the total number of hotspots, very-high-risk hotspots (risk >= 0.70), average risk, maximum Fire Radiative Power (FRP), average days without rain, rankings by state, biome and municipality, and distributions by hour, satellite and risk level.

### Periods - `/v1/periods`

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/v1/periods` | Lists available periods from `metadata/available-periods.json` in S3 |

### Filter options - `/v1/filters`

These endpoints provide values for frontend dropdowns and filters:

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/v1/filters/states` | Brazilian states with their abbreviations and names |
| `GET` | `/v1/filters/biomes` | Available biomes |
| `GET` | `/v1/filters/risk-levels` | INPE risk classes and thresholds |
| `GET` | `/v1/filters/municipalities?mode=daily\|monthly` | Municipalities grouped by state |
| `GET` | `/v1/filters/satellites?mode=daily\|monthly` | Satellites present in the ingested data |

### Documentation and observability

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI specification:** `http://localhost:8080/v3/api-docs`
- **Actuator:** `/actuator/health`, `/actuator/info`, `/actuator/metrics`

## INPE fire-risk levels

The `risco_fogo` field is classified on a scale from 0 to 1:

| Class | Range |
| --- | --- |
| `MINIMO` | `[0.00, 0.15)` |
| `BAIXO` | `[0.15, 0.40)` |
| `MEDIO` | `[0.40, 0.70)` |
| `ALTO` | `[0.70, 0.95)` |
| `CRITICO` | `[0.95, 1.00]` |

“Very high risk” means a fire-risk value greater than or equal to `0.70`, including the `ALTO` and `CRITICO` classes.

## Configuration

Configuration is defined in [`src/main/resources/application.yaml`](src/main/resources/application.yaml) and can be parameterized through environment variables:

| Variable | Description |
| --- | --- |
| `BUCKET_NAME` | S3 bucket containing hotspot files |
| `DAILY_PREFIX` | Prefix for daily CSV files |
| `MONTHLY_PREFIX` | Prefix for monthly CSV files |
| `METADATA` | Object key for the available-periods JSON file |
| `FRONTEND_URL` | Allowed CORS origin |
| `AWS_PROFILE` | Optional AWS profile; the default credential chain is used when it is absent |

The default AWS region is `us-east-1`. Cache TTLs are 10 minutes for metadata, 30 minutes for daily data and 120 minutes for monthly data.

## Security

The MVP is stateless and does not use user authentication. Its current protections include:

- CORS restricted to the `FRONTEND_URL` origin, with `GET` and `OPTIONS` methods
- Public health checks, `GET /v1/**` endpoints and Swagger documentation
- All other routes denied by default
- CSRF disabled because the API is JSON-based and does not use browser sessions or cookies

## Running the project

### Local execution

Set the required environment variables:

```bash
export BUCKET_NAME=my-bucket
export DAILY_PREFIX=raw/daily
export MONTHLY_PREFIX=raw/monthly
export METADATA=metadata/available-periods.json
export FRONTEND_URL=http://localhost:5173
# export AWS_PROFILE=my-profile  # optional
```

Start the service:

```bash
./mvnw spring-boot:run
```

The service starts on port `8080` by default.

### Tests

```bash
./mvnw test
```

### Build the JAR

```bash
./mvnw clean package
java -jar target/orbitfire-hotspots-ms-0.0.1-SNAPSHOT.jar
```

### Docker

```bash
docker build -t orbitfire-hotspots-ms .

docker run -p 8080:8080 \
  -e BUCKET_NAME=my-bucket \
  -e DAILY_PREFIX=raw/daily \
  -e MONTHLY_PREFIX=raw/monthly \
  -e METADATA=metadata/available-periods.json \
  -e FRONTEND_URL=http://localhost:5173 \
  orbitfire-hotspots-ms
```

The [`Dockerfile`](Dockerfile) uses a multi-stage build with a Temurin 17 JDK for compilation and a JRE image for execution. The application runs as a non-root user.

## Project status

OrbitFire Hotspots is a portfolio project demonstrating Java backend development, REST API design, AWS S3 integration, in-memory caching, data filtering, aggregation, observability and containerized deployment.
