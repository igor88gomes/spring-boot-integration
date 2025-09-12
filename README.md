# Spring Boot Integration – ICC Demo  

[![CI – main](https://github.com/igor88gomes/spring-boot-integration/actions/workflows/ci.yaml/badge.svg?branch=main)](https://github.com/igor88gomes/spring-boot-integration/actions/workflows/ci.yaml)
[![CD – main](https://github.com/igor88gomes/spring-boot-integration/actions/workflows/docker-publish.yaml/badge.svg?branch=main)](https://github.com/igor88gomes/spring-boot-integration/actions/workflows/docker-publish.yaml)
[![Secret Scan](https://github.com/igor88gomes/spring-boot-integration/actions/workflows/secret-scan.yaml/badge.svg?branch=main)](https://github.com/igor88gomes/spring-boot-integration/actions/workflows/secret-scan.yaml)
[![Code scanning](https://img.shields.io/badge/Code%20scanning-enabled-blue)](https://github.com/igor88gomes/spring-boot-integration/security/code-scanning)
[![SBOM](https://img.shields.io/badge/SBOM-CycloneDX-blue)](docs/USAGE.md#cd-artifacts)
[![Multi-arch](https://img.shields.io/badge/multi--arch-amd64%20%7C%20arm64-blue)](docs/USAGE.md#verifiera-multi-arch-amd64--arm64)
[![Docker Hub](https://img.shields.io/badge/Docker%20Hub-image-blue)](https://hub.docker.com/r/igor88gomes/spring-boot-integration/tags)

> Av Igor Gomes — DevOps Engineer

<p align="center">
  <img src="docs/images/architecture-diagram.png"
       alt="Arkitekturöversikt – Spring Boot, ActiveMQ och PostgreSQL"
       width="900">
</p>
<p align="center"><em><strong>Bild 1.</strong> Arkitekturöversikt – Spring Boot, ActiveMQ och PostgreSQL. <strong>Arkitekturstil:</strong> modulär monolit (meddelandebaserad via JMS).</em></p>

<p align="center">
  <img src="docs/images/cicd-pipeline-diagram.png"
       alt="CI/CD-pipeline – Spring Boot, Docker och GitHub Actions"
       width="900">
</p>
<p align="center"><em><strong>Bild 2.</strong> CI/CD-pipeline – bygg, test (JaCoCo/JavaDoc) och publicering till Docker Hub.</em></p>

## Projektinformation

Detta projekt visar en komplett integrationslösning inspirerad av ICC-mönster, utvecklad från grunden med
moderna teknologier och etablerade arkitekturmönster. Lösningen kombinerar asynkron kommunikation,
spårbarhet, testbarhet och fullständig automatisering genom CI/CD-pipelines och Docker-baserad
distribution.

Applikationen är byggd med Java och Spring Boot 3, och använder ActiveMQ (JMS) för meddelandehantering,
PostgreSQL för datalagring samt Docker för containerisering. GitHub Actions kör automatiska tester i CI (H2) och 
**pushar först en candidate image till GHCR (privat)**; efter **Trivy quality gate** (**CRITICAL blockerar**) **promoteras med samma digest** 
till Docker Hub (`:latest`).

### Teknisk sammanfattning

- **12-factor (Config):** konfiguration och hemligheter via **miljövariabler**; `.env.example` för dev; **inga hemligheter i koden**.
- **Säkerhet (översikt):** env utan hemligheter; **Trivy quality gate före publicering**; **SBOM och **OCI-etiketter** för supply-chain-spårbarhet.
- **Secret scanning (Gitleaks)** på push/PR och veckovis (inga hemligheter i repo).
- **Backing services:** ActiveMQ (JMS) och PostgreSQL behandlas som utbytbara resurser (t.ex. `BROKER_URL`, `DB_*`).
- **Observerbarhet:** **JSON-loggar** (Logback/Logstash), **MDC/korrelations-ID**, **/actuator/health**, tidsstämplar i **UTC** för spårbar analys.

## CI/CD (GitHub Actions) i korthet

- **(CI)** `ci.yaml`
  - Validerar och bygger (Maven)
  - Testar med H2 (isolerade tester)
  - Publicerar **JaCoCo** (artefakt)
  - Genererar **JavaDoc** (artefakt)
  - Genererar **Stubs** (artefakt)

- **(CD)** `docker-publish.yaml`
  - Bygger **multi-arch** (`linux/amd64`, `linux/arm64`)
  - Pushar **candidate image** till **GHCR (privat)**
  - Kör **Trivy quality gate** — **CRITICAL blockerar**, **HIGH** → **SARIF** i *Security → Code scanning*
  - **Promoterar samma digest** till Docker Hub `:latest`
  - Sätter **OCI-etiketter** (revision/created/source) + **SBOM** (artefakt)
  - **Concurrency-skydd** avbryter parallella körningar

För en översiktlig bild av pipelinen/pipelineflödet, se **Bild 2**. 

För flera detaljer om pipelinen, se:

- [Bygg / CI & dokumentation (GitHub Actions)](#bygg--ci--dokumentation-github-actions)
- [Distribution (CD) (GitHub Actions)](#distribution-cd-github-actions)
- [docs/USAGE.md#ci-artifacts](docs/USAGE.md#ci-artifacts)
- [docs/USAGE.md#cd-artifacts](docs/USAGE.md#cd-artifacts)

---

## Arkitektur & korrelation (översikt)

Se **Bild 1** ovan. Diagrammet visar den asynkrona, meddelandebaserade integrationsarkitekturen i 
applikationen.

1. Klienten (t.ex. Postman eller curl) skickar ett meddelande via REST-endpointen `/api/send`.

2. `MessageProducer` publicerar meddelandet till en ActiveMQ-kö.

3. `MessageConsumer` lyssnar på kön och lagrar meddelandet i PostgreSQL-databasen.

4. Klienten kan hämta alla sparade meddelanden via `/api/all`, vilket anropar `MessageController` och 
   hämtar data med hjälp av JPA.

> Arkitekturen möjliggör spårbar och tillförlitlig kommunikation i en modulär och lättunderhållen lösning.

### Affärs-API (REST)

| Metod | Endpoint                 | Beskrivning                    |
|------:|--------------------------|--------------------------------|
| POST  | `/api/send?message=TEXT` | Skicka meddelande till kön     |
| GET   | `/api/all`               | Hämta alla sparade meddelanden |

```bash
# Exempel (Affärs-API)
curl -X POST "http://localhost:8080/api/send?message=TestIntegration"
curl http://localhost:8080/api/all
```
> **Validering:** `POST /api/send` returnerar **400 (Bad Request)** om parametern `message` är tom eller endast blanktecken (*whitespace*).

```bash
# Exempel: parametern `message` är tom/blank
curl -i -X POST http://localhost:8080/api/send -d "message= "
```

### Övervakning (Actuator-API)

| Metod | Endpoint             | Beskrivning                      |
|------:|----------------------|----------------------------------|
| GET   | `/actuator/health`   | Hälsa/status för appen           |
| GET   | `/actuator/info`     | Konfigurationsmetadata (kö-namn) |

```bash
# Exempel (health)
curl http://localhost:8080/actuator/health 
```

```bash
# Exempel (info)
curl http://localhost:8080/actuator/info
```

> **(12‑factor):** Sätt kö-namn via `app.queue.name` (fallback `test-queue`) eller env `APP_QUEUE_NAME` för konfiguration per miljö.

## Spårbarhet & korrelations-ID

### Loggexempel

**Affärs-anrop:**
```bash
curl -X POST "http://localhost:8080/api/send?message=TestIntegration"
```

**`logs/app.log`**

```json
[
  {
    "@timestamp": "2025-08-28T21:39:27.407+02:00",
    "level": "INFO",
    "logger_name": "com.igorgomes.integration.MessageProducer",
    "message": "Skickar meddelande till kön: TestIntegration",
    "messageId": "03c5e1af-e53d-4a9c-890c-1259457ca6bd"
  },
  {
    "@timestamp": "2025-08-28T21:39:27.504+02:00",
    "level": "INFO",
    "logger_name": "com.igorgomes.integration.MessageConsumer",
    "message": "Meddelande mottaget från kön: TestIntegration",
    "messageId": "03c5e1af-e53d-4a9c-890c-1259457ca6bd"
  }
]
```
> Exemplet visar end-to-end-korrelation: producenten skickar `messageId` i **JMS-headern** och konsumenten läser headern och sätter samma `messageId` i MDC. Därmed kan samma ID följas genom hela flödet.

### Korrelationsflöde (MDC + JMS)
- **Controller**: Säkerställer att `messageId` finns i MDC för varje anrop; skapar UUID om det saknas och tar bort nyckeln i `finally` endast om den sattes här.
- **Producer**: Läser `messageId` från MDC och skickar som **JMS-header**; rör inte MDC.
- **Consumer**: Läser headern `messageId`, lägger in i MDC under bearbetning för loggkorrelation och tar bort just den nyckeln i `finally`.

Detta gör att samma `messageId` kan följas från HTTP-ingången, via kön, till persistens. 
Se [docs/USAGE.md](docs/USAGE.md) för fler detaljer och körningskommandon.

### Tester (korrelationskontrakt)
- Producer sätter `messageId` som JMS-header när MDC har värde; annars inte.
- Controller ser till att `messageId` finns i MDC för `/api/send`.
- Se `MessageProducerTest` för fallen *MDC present* och *MDC missing*.
- Dessa kontraktstester används även för att generera **WireMock-stubs**, som lagras som artifacts i CI.

Se [docs/TESTS.md](docs/TESTS.md) för fler detaljer och [docs/USAGE.md](docs/USAGE.md) för exempel på end-to-end-verifiering i loggar.

## Funktionalitet

### Applikation
- Exponerar **REST-API** (Spring Boot)
- Hanterar **asynkron meddelandeöverföring** via ActiveMQ (JMS)
- Lagrar **persistent data** i PostgreSQL (JPA)
- **Korrelation & observabilitet:** JSON-loggar (Logback + MDC), korrelations-ID, **/actuator/health**
- **Enhetstester** med JUnit 5 och Mockito
- Körs **containeriserat** 
- **Automatiska tester** i CI-miljö med **H2-databas**

### Plattform / DevOps
- **Gated CD** (GHCR → **Trivy**; CRITICAL blockerar) med **promotion per digest** till Docker Hub `:latest`
- **Multi-arch builds** (`linux/amd64`, `linux/arm64`)
- **Supply-chain metadata:** **SBOM (CycloneDX)** + **OCI-etiketter**
- **Kontraktstester** **(Spring Cloud Contract)** med automatisk generering av stubs (artefakt i CI)
- **Code scanning** i GitHub (SARIF)
- **Concurrency-skydd** och **retention** av **candidate images** (14 dagar)

## Teknologier

### Applikation
| Teknologi             | Användning                                |
|-----------------------|-------------------------------------------|
| Spring Boot 3.3.x     | Huvudramverk                              |
| ActiveMQ              | Meddelandekö (JMS)                        |
| PostgreSQL            | Databashanterare via JPA (containermiljö) |
| H2 Database           | In-memory databas för tester i CI         |
| Spring Data JPA       | Hantering av entiteter och datalagring    |
| Logback + MDC         | Strukturerad loggning i JSON-format       |
| JUnit + Mockito       | Enhetstester                              |
| Spring Cloud Contract | Kontraktstester                           |

| Teknologi / Tjänst      | Användning                                             |
|-------------------------|--------------------------------------------------------|
| GitHub Actions          | CI/CD-automation                                       |
| Docker Engine           | Container-runtime (lokalt och i build-pipeline)        |
| Docker Compose          | Orkestrering lokalt (app, ActiveMQ, PostgreSQL)        |
| Docker Buildx / QEMU    | **Multi-arch builds** (amd64/arm64)                    |
| Trivy                   | **Sårbarhetsskanning** + **SARIF** (Code scanning)     |
| SBOM (CycloneDX)        | Supply-chain spårbarhet i build-steget                 |
| OCI-etiketter           | `revision`, `created`, `source` (härkomst/metadata)    |
| GHCR / Docker Hub       | Candidate image → **promotion per digest** (`:latest`) |
| Concurrency / Retention | Stoppar parallella körningar; **14 dagar** i GHCR      |
| Kontraktsartefakter (CI)| Stub-generering och lagring i Actions                  |

## Körning (Runtime)

**Hela stacken körs containeriserad med Docker Compose** (stöd för Podman Compose):
    
- `integration-app` – applikationen (image: `igor88gomes/spring-boot-integration:latest`, **byggs och pushas av CD-pipelinen**)
- `activemq` – ActiveMQ (JMS)
- `postgres` – PostgreSQL

→ > ** Java 17 ingår i applikationsimagen; inget lokalt JDK krävs.**

### Bygg / CI & dokumentation (GitHub Actions)

- **Workflow:** `.github/workflows/ci.yaml`
- **Trigger:** push/PR till `main` och `test`

- **Steg:**
  - **Steg 1 – Checkout & JDK 17:** checka ut källkod och konfigurera Java.
  - **Steg 2 – Bygg & tester (Maven/H2):** kör `mvn verify` med H2 för isolerade tester.
  - **Steg 3 – Kodtäckning (JaCoCo):** generera rapport och ladda upp som artefakt.
  - **Steg 4 – JavaDoc (endast `main`):** generera och ladda upp som artefakt.
  - **Steg 5 – Stubs (SCC), (endast `main`): ** generera WireMock-stubs från kontrakt och ladda upp.

- **Artefakter:**
  - `jacoco-report` (main/test, **retention 14 dagar**)
  - `javadocs` (endast `main`, **14 dagar**)
  - `stubs` (endast `main`, **14 dagar**) – genererade av Spring Cloud Contract för konsumenttester

### Distribution (CD) – Docker-image (GitHub Actions)

- **Workflow:** `.github/workflows/docker-publish.yaml`
- **Trigger:** `push` till `main` *(ej PR)*

- **Publiceringsflöde:**
  1. **Build (candidate, privat):** Buildx bygger **multi-arch** (`linux/amd64, linux/arm64`) och pushar en **candidate-image** till **GHCR** (privat) med rika **OCI-etiketter**  
     – taggar baserade på commit SHA.
  2. **Trivy – quality gate:** container-skanning av candidate-imagen.  
     – **CRITICAL** sårbarheter blockerar; **HIGH** rapporteras som **SARIF** till *Security → Code scanning*.
  3. **Promotion:** om spärren passerar, **promoteras exakt samma digest** till **Docker Hub** som `igor88gomes/spring-boot-integration:latest`.
  4. **SBOM (insyn):** **CycloneDX SBOM** (`sbom.cdx.json`) laddas upp som **Actions-artefakt** (**retention 14 dagar**).
  5. **Rensning:** temporära Docker Desktop-buildfiler (`*.dockerbuild`) tas bort i jobben för att hålla run-loggen ren.

- **Säkerhet & kontroll:**
  - **Concurrency:** `concurrency.group=docker-publish-${{ github.ref }}` och `cancel-in-progress: true` förhindrar parallella dubbletter.
  - **Retention (GHCR):** candidate-images märks bl.a. med  
    `org.opencontainers.image.ref.name=candidate` och `ghcr.io/retention-days=14` → autosanering efter 14 dagar.
  - **Behörigheter:** workflow ger **packages: write**, **security-events: write**, **contents: read** för att kunna publicera images och skicka SARIF.

- **Resultat:**
  - **GHCR (privat):** *candidate* (per-commit) – för skanning och spårbarhet.
  - **Docker Hub (publik):** `:latest` – **multi-arch** och redo för `docker/podman-compose`.

## Projektstruktur

```text
spring-boot-integration/
│
├── .github/workflows/      # CI/CD-pipelines (ci.yaml, docker-publish.yaml)
├── .gitignore              # Ignorerade filer (t.ex. target/, .env)
├── .dockerignore           # Exkluderar onödiga filer från Docker build-context
├── .env.example            # Exempel på miljövariabler (inga hemligheter i koden)
├── docs/                   # Dokumentation & bilder (diagram i docs/images/)
├── logs/                   # Lokala loggar (skapas vid körning, ej versionerad)
├── src/                    # Källkod & tester (main/ och test/)
├── pom.xml                 # Maven-konfiguration (beroenden/plugins)
├── Dockerfile              # Bygger applikationsimagen 
├── docker-compose.yaml     # Lokalt orkestreringsstöd (app, ActiveMQ, PostgreSQL)
└── README.md               # Projektöversikt
```

## Relaterade dokument

- **Användning:** [docs/USAGE.md](docs/USAGE.md)  
- **Tester:** [docs/TESTS.md](docs/TESTS.md)

## Kontakt

Igor Gomes — DevOps Engineer  
[LinkedIn](https://www.linkedin.com/in/igor-gomes-5b6184290) · [Docker Hub](https://hub.docker.com/r/igor88gomes/spring-boot-integration/tags)  
**E-post:** [igor88gomes@gmail.com](mailto:igor88gomes@gmail.com)