# OrbitFire Hotspots MS

Microsserviço REST para **consulta, filtragem e agregação de focos de queimadas/incêndio do INPE**. Os dados (focos diários e mensais) são ingeridos como arquivos CSV no Amazon S3; o serviço os carrega sob demanda, aplica filtros em memória e expõe tanto os pontos individuais quanto métricas agregadas para o front-end OrbitFire.

## Stack

- **Java 17** + **Spring Boot 4.0.6** (starters modulares: `webmvc`, `security`, `jackson`, `validation`, `cache`, `actuator`)
- **Jackson 3** (`tools.jackson.*`) para serialização JSON
- **AWS SDK v2** (S3) para leitura dos arquivos de origem
- **Caffeine** como cache em memória
- **springdoc-openapi** para documentação Swagger
- **Maven** (com wrapper `./mvnw`)

## Arquitetura

O código segue um corte em camadas dentro de `br.com.orbitfire.hotspots`:

| Camada | Pacote | Responsabilidade |
|--------|--------|------------------|
| API | `api.controller`, `api.dto`, `api.exception` | Endpoints REST, DTOs de requisição/resposta e tratamento global de erros |
| Aplicação | `application.service` | Orquestração: resolve a chave do S3, carrega/parseia (com cache), filtra e agrega |
| Domínio | `domain.model`, `domain.filter`, `domain.metric` | Modelo `Hotspot`, filtro em memória, classes de risco INPE e cálculo de métricas |
| Infraestrutura | `infrastructure.aws.s3`, `infrastructure.csv`, `infrastructure.config` | Leitura de objetos do S3, parsing de CSV, configurações (AWS, cache, segurança, OpenAPI) |
| Compartilhado | `shared.pagination`, `shared.text` | Paginação genérica e normalização de texto |

**Fluxo de uma consulta:** o controller recebe a data/mês e os filtros → `HotspotService` monta a chave do objeto no S3 (ex.: `raw/daily/2026/05/focos_diario_br_20260531.csv`) → `HotspotDataLoader` lê do S3 e parseia o CSV (resultado cacheado por chave) → o `HotspotFilter` é aplicado em memória → o resultado é paginado (`/points`) ou agregado pelo `HotspotMetricsCalculator` (`/summary`).

## Endpoints

Base da API: `/v1`. Todos os endpoints de leitura (`GET /v1/**`) são públicos.

### Focos (`/v1/hotspots`)

| Método | Caminho | Descrição |
|--------|---------|-----------|
| `GET` | `/daily/points` | Focos individuais de um dia, com filtros e paginação |
| `GET` | `/daily/summary` | Métricas e agregações de um dia |
| `GET` | `/monthly/summary` | Métricas e agregações de um mês |

Exemplos:

```
GET /v1/hotspots/daily/points?date=2026-05-31&uf=TO&satellite=AQUA_M-T&page=0&size=500
GET /v1/hotspots/daily/summary?date=2026-05-31&uf=TO&biome=Cerrado&riskMin=0.7&topN=10
GET /v1/hotspots/monthly/summary?month=2026-04&uf=TO&satellite=AQUA_M-T&topN=10
```

**Filtros disponíveis** (todos opcionais, combináveis): `uf`, `municipalityId`, `biome`, `satellite`, faixas `riskMin/riskMax` (0–1), `frpMin/frpMax`, `daysWithoutRainMin/Max`, `precipitationMin/Max`, janela de horário `hourStart/hourEnd` (0–23) e `bbox` no formato `"minLon,minLat,maxLon,maxLat"`. A paginação usa `page` (default 0) e `size` (default 500, máx. 5000); `topN` nos rankings aceita 1–100 (default 10).

O resumo retorna: total de focos, focos de risco muito alto (≥ 0.70), risco médio, FRP máximo, média de dias sem chuva, rankings (top estados/biomas/municípios), além de distribuições por hora, por satélite e por nível de risco.

### Períodos (`/v1/periods`)

| Método | Caminho | Descrição |
|--------|---------|-----------|
| `GET` | `/v1/periods` | Catálogo de períodos disponíveis, lido de `metadata/available-periods.json` no S3 |

### Opções de filtro (`/v1/filters`)

Valores para popular dropdowns no front-end:

| Método | Caminho | Descrição |
|--------|---------|-----------|
| `GET` | `/states` | Estados brasileiros (UF + nome) |
| `GET` | `/biomes` | Biomas |
| `GET` | `/risk-levels` | Classes de risco INPE com seus limiares |
| `GET` | `/municipalities?mode=daily\|monthly` | Municípios agrupados por estado, derivados dos dados ingeridos |
| `GET` | `/satellites?mode=daily\|monthly` | Satélites presentes nos dados ingeridos |

### Documentação e observabilidade

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI spec:** `http://localhost:8080/v3/api-docs`
- **Actuator:** `/actuator/health`, `/actuator/info`, `/actuator/metrics`

## Níveis de risco (INPE)

O campo `risco_fogo` (0–1) é classificado em:

| Classe | Faixa |
|--------|-------|
| `MINIMO` | [0.00, 0.15) |
| `BAIXO` | [0.15, 0.40) |
| `MEDIO` | [0.40, 0.70) |
| `ALTO` | [0.70, 0.95) |
| `CRITICO` | [0.95, 1.00] |

"Risco muito alto" = risco ≥ 0.70 (ALTO + CRITICO).

## Configuração

Definida em [src/main/resources/application.yaml](src/main/resources/application.yaml) e parametrizada por variáveis de ambiente:

| Variável | Descrição |
|----------|-----------|
| `BUCKET_NAME` | Bucket S3 com os arquivos de focos |
| `DAILY_PREFIX` | Prefixo dos CSVs diários |
| `MONTHLY_PREFIX` | Prefixo dos CSVs mensais |
| `METADATA` | Chave do JSON de períodos disponíveis |
| `FRONTEND_URL` | Origem permitida em CORS |
| `AWS_PROFILE` | (opcional) perfil AWS; se ausente, usa a cadeia padrão de credenciais |

Região padrão: `us-east-1`. TTLs de cache (em minutos): metadados 10, diário 30, mensal 120.

## Segurança

MVP **stateless, sem autenticação**. A proteção se resume a:

- CORS restrito à origem `FRONTEND_URL` (métodos `GET`/`OPTIONS`)
- Health check público; `GET /v1/**` e Swagger liberados; qualquer outra rota negada
- CSRF desabilitado (API JSON sem sessões/cookies)

## Como executar

### Local

```bash
export BUCKET_NAME=meu-bucket
export DAILY_PREFIX=raw/daily
export MONTHLY_PREFIX=raw/monthly
export METADATA=metadata/available-periods.json
export FRONTEND_URL=http://localhost:5173
# export AWS_PROFILE=meu-perfil   # opcional

./mvnw spring-boot:run
```

O serviço sobe na porta `8080`.

### Testes

```bash
./mvnw test
```

### Build do JAR

```bash
./mvnw clean package
java -jar target/orbitfire-hotspots-ms-0.0.1-SNAPSHOT.jar
```

### Docker

```bash
docker build -t orbitfire-hotspots-ms .
docker run -p 8080:8080 \
  -e BUCKET_NAME=meu-bucket \
  -e DAILY_PREFIX=raw/daily \
  -e MONTHLY_PREFIX=raw/monthly \
  -e METADATA=metadata/available-periods.json \
  -e FRONTEND_URL=http://localhost:5173 \
  orbitfire-hotspots-ms
```

O [Dockerfile](Dockerfile) usa build multi-stage (Temurin 17 JDK para compilar, JRE para rodar) e executa a aplicação como usuário não-root.

## CI/CD (GitHub Actions + AWS ECS)

Foi adicionado o workflow `/.github/workflows/ci-cd-ecs.yml` para:

1. Rodar `./mvnw test` em push para `main`
2. Buildar a imagem Docker
3. Publicar a imagem no Amazon ECR
4. Atualizar e fazer deploy da task definition no Amazon ECS

### Pré-requisitos no GitHub

Configure os itens abaixo no repositório:

- **Secret**
  - `AWS_ROLE_TO_ASSUME`: ARN da role AWS usada pelo GitHub Actions (OIDC) para acessar ECR/ECS

- **Variables**
  - `AWS_REGION`: região AWS (ex.: `us-east-1`)
  - `ECR_REPOSITORY`: nome do repositório no ECR
  - `ECS_CLUSTER`: nome do cluster ECS
  - `ECS_SERVICE`: nome do serviço ECS
  - `ECS_TASK_DEFINITION`: nome/ARN da task definition base
  - `ECS_CONTAINER_NAME`: nome do container na task definition que receberá a nova imagem
