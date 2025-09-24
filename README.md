# Spring Boot Integration Demo – ICC & CI/CD

[![CI – main](https://github.com/igor88gomes/spring-boot-integration/actions/workflows/ci.yaml/badge.svg?branch=main)](https://github.com/igor88gomes/spring-boot-integration/actions/workflows/ci.yaml)
[![Test Coverage](https://github.com/igor88gomes/spring-boot-integration/raw/main/.github/badges/jacoco.svg)](docs/ARTIFACTS.md#ci-artifacts)
[![Code scanning](https://img.shields.io/badge/Code%20scanning-enabled-blue)](https://github.com/igor88gomes/spring-boot-integration/security/code-scanning)
[![Secret Scan](https://github.com/igor88gomes/spring-boot-integration/actions/workflows/secret-scan.yaml/badge.svg?branch=main)](https://github.com/igor88gomes/spring-boot-integration/actions/workflows/secret-scan.yaml)
[![CD – main](https://github.com/igor88gomes/spring-boot-integration/actions/workflows/docker-publish.yaml/badge.svg?branch=main)](https://github.com/igor88gomes/spring-boot-integration/actions/workflows/docker-publish.yaml)
[![SBOM](https://img.shields.io/badge/SBOM-CycloneDX-blue)](docs/USAGE.md#cd-artifacts)
[![Multi-arch](https://img.shields.io/badge/multi--arch-amd64%20%7C%20arm64-blue)](docs/USAGE.md#verifiera-multi-arch-amd64--arm64)
[![Docker Hub](https://img.shields.io/badge/Docker%20Hub-image-blue)](https://hub.docker.com/r/igor88gomes/spring-boot-integration/tags)
[![Dependabot](https://img.shields.io/badge/Dependabot-enabled-blue)](https://github.com/igor88gomes/spring-boot-integration/pulls?q=is%3Apr+author%3Aapp%2Fdependabot)

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
<p align="center"><em><strong>Bild 2.</strong> Översikt av CI/CD-pipeline. Diagrammet visar huvudflödet: bygg, test, säkerhetsskanning samt image-publicering och distribution.</em></p>

## Projektinformation

Detta projekt är en **demonstration** av integrationsmönster inspirerade av ICC (Integration Competency Center).  
Fokus ligger på att visa **asynkron kommunikation**, **spårbarhet** och **observabilitet**, samt en komplett **CI/CD-pipeline** med säkerhets och kvalitetskontroller.

Målet är inte att täcka alla integrationsscenarier, utan att ge en praktisk inblick i hur olika tekniker kan kombineras i ett modernt DevOps-flöde.

**Höjdpunkter**
- Händelsedriven väg: REST API → JMS/ActiveMQ → konsument → JPA/PostgreSQL.
- Spårbarhet: JSON-loggar med korrelations-ID (MDC/JMSCorrelationID) end-to-end.
- Testbarhet: enhet/web-slice, JPA/H2, BDD (Cucumber) och kontrakt (SCC).
- Leverans: GitHub Actions kör tester i CI (H2) och **pushar först en kandidat-image till GHCR (privat)**; efter **Trivy-kvalitetsgrind** (**CRITICAL blockerar**) **promoteras med samma digest** till Docker Hub (`:latest`).
- **Säkerhet & supply chain:** Trivy-gate (CRITICAL stop), **SBOM/OCI-etiketter**, **Gitleaks (PR-gate)** och **Dependabot**.

**Teknikstack (kort):** Java/Spring Boot 3, ActiveMQ (JMS), PostgreSQL, Docker/Compose.  
*Test/CI-miljö:* H2 (in-memory, profil `test`). Inbäddad ActiveMQ (`vm://embedded`) startas för BDD/E2E; SCC kör mot MVC-slice (utan broker).

### Principer (kort)

- **12-factor (Config):** miljövariabler; `.env.example`; **inga hemligheter i koden**.
- **Observerbarhet:** **JSON-loggar**, **MDC/korrelations-ID**, **/actuator/health**, tidsstämplar i **UTC**.
- **Backing services:** ActiveMQ (JMS) och PostgreSQL konfigureras via env (`BROKER_URL`, `DB_*`).

> **CI/CD:** För detaljer, se avsnitten nedan. Se även **Bild 2** för pipelineflödet.

**Snabblänkar (CI/CD):**

- [Bygg / CI & dokumentation (GitHub Actions)](#bygg--ci--dokumentation-github-actions)
- [Distribution (CD) – Docker-image (GitHub Actions)](#distribution-cd--docker-image-github-actions)
- [docs/ARTIFACTS.md#ci-artifacts](docs/ARTIFACTS.md#ci-artifacts)
- [docs/ARTIFACTS.md#cd-artifacts](docs/ARTIFACTS.md#cd-artifacts)
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

## Spårbarhet & korrelations-ID (översikt)


Applikationen loggar i **JSON** (Logback + MDC) och inkluderar ett **messageId** som följer hela flödet (Controller → Producer/JMS → Consumer/JPA). 
I container-miljö körs **stdout-only** – loggar läses via `docker|podman logs`.

**Affärs-anrop:**

```bash
curl -X POST "http://localhost:8080/api/send?message=TestIntegration"
```

**Loggexempel (förkortat):**

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
> Producenten skickar `messageId` i **JMS-headern**; konsumenten läser headern och sätter samma `messageId` i MDC.  
> Detta gör att samma `messageId` kan följas från HTTP-ingången, via kön, till persistens.

### Korrelationsflöde (MDC + JMS)
- **Controller**: Säkerställer att `messageId` finns i MDC för varje anrop; skapar UUID om det saknas och tar bort nyckeln i `finally` endast om den sattes här.
- **Producer**: Läser `messageId` från MDC och skickar som **JMS-header**; rör inte MDC.
- **Consumer**: Läser headern `messageId`, lägger in i MDC under bearbetning för loggkorrelation och tar bort just den nyckeln i `finally`.

Detta gör att samma `messageId` kan följas från HTTP-ingången, via kön, till persistens. 
Se [docs/USAGE.md](docs/USAGE.md) för fler detaljer och körningskommandon.

## Testöversikt

- **Controller:** Konsistenta statuskoder (**200/400**) och korrekta headers.
- **Producer (JMS):** Korrelation via `messageId` (MDC → JMS-header) och korrekt felhantering (logga, ej propagera).
- **Consumer (JMS):** Läser `messageId`, persisterar meddelanden och bibehåller korrelations-ID.
- **Repository (JPA/H2):** Baspersistens, `@NotBlank`-validering och att `receivedAt` sätts.
- **Kontrakt/BDD:** Säkerställer 200/400 och kedjan **HTTP → JMS → DB** end-to-end.

Se [docs/TESTS.md](docs/TESTS.md) för fler detaljer om testerna.  
**Artefakter (CI/CD):** se [docs/ARTIFACTS.md](docs/ARTIFACTS.md).

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
| H2-databas            | In-memory databas för tester i CI         |
| Spring Data JPA       | Hantering av entiteter och datalagring    |
| Logback + MDC         | Strukturerad loggning i JSON-format       |
| JUnit + Mockito       | Enhetstester                              |
| Spring Cloud Contract | Kontraktstester                           |

### Plattform / DevOps
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

> **Obs:** Java 17 ingår i applikationsimagen; inget lokalt JDK krävs.

### Branch-strategi (kort)

- Dagligt arbete i `test`; PR → `main` med **squash-merge**.
- Obligatoriska checks på PR: **CI** + **Secret Scan (Gitleaks)**.
- Release: **push till `main`** triggar CD och publicering till Docker Hub.
- **Regler (`main`):** **squash-only**, **linear history**, **blockera force pushes**, **kräver uppdaterad branch före merge**, och **obligatoriska status checks:** `build` + `gitleaks`.

### Bygg / CI & dokumentation (GitHub Actions)

- **Workflow:** `.github/workflows/ci.yaml`
- **Trigger:** push till `main` och `test`; PR till `main` 

- **Steg:**
  - **Steg 1 – Checkout & JDK 17:** checka ut källkod och konfigurera Java.
  - **Steg 2 – Bygg & tester (Maven/H2):** kör `mvn verify` med H2 för isolerade tester.
  - **Steg 3 – Kodtäckning (JaCoCo):** generera rapport och ladda upp som artefakt.
  - **Steg 4 – JavaDoc (endast på `main` när commit-meddelandet innehåller `"[javadoc]"`):** generera och ladda upp som artefakt.
  - **Steg 5 – Stubs (SCC), (endast `main`):** stubs **genereras av Spring Cloud Contract under `mvn verify`** och CI **laddar upp `*-stubs.jar` som artefakt** (om finns).
  - **Steg 6 – Felsökning (endast vid fel):** ladda upp testrapporter (**Surefire/Failsafe**) – `target/surefire-reports/**`, `target/failsafe-reports/**` + dumpfiler för att förenkla felsökning i Actions.

- **Artefakter:**
  - `jacoco-report` (main/test, **retention 14 dagar**)
  - `javadocs` (endast `main`, **14 dagar**)
  - `stubs` (endast `main`, **14 dagar**) – genererade av Spring Cloud Contract för konsumenttester
  - `surefire-reports` (**endast vid fel**, **14 dagar**) – **Surefire/Failsafe**-rapporter och dumpfiler för felsökning

#### Secret scanning (Gitleaks)

- **Workflow (fristående):** `secret-scan.yaml` körs separat från CI.
- **Schema (UTC):** måndagar **03:00 UTC** (full historikskanning + SARIF).
- **Policy:** PR blockeras vid fynd (exit ≠ 0).
- **SARIF:** genereras och laddas upp **vid push till `main`** och vid den schemalagda körningen till *Security → Code scanning*.
- **Beteende:**
  - PR = snabb skanning av ändringar (`--no-git`)
  - Push/schedule = full historik (checkout `fetch-depth: 0`) + SARIF
- **Konfig:** `.gitleaks.toml` (ignorerar `.env.example`, `application-test.properties`; placeholders: `changeme`, `to-be-set`, `example`, `dummy`).

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

#### Säkerhet & kontroll

- **Concurrency:** `concurrency.group=docker-publish-${{ github.ref }}` och `cancel-in-progress: true` förhindrar parallella dubbletter.
- **Retention (GHCR):** candidate-images märks bl.a. med  
  `org.opencontainers.image.ref.name=candidate` och `ghcr.io/retention-days=14` → autosanering efter 14 dagar.
- **Behörigheter:** workflow ger **packages: write**, **security-events: write**, **contents: read** för att kunna publicera images och skicka SARIF.

- **Resultat:**

  - **GHCR (privat):** *candidate* (per-commit) – för skanning och spårbarhet.
  - **Docker Hub (publik):** `:latest` – **multi-arch** och redo för `docker/podman-compose`.

## Säkerhet & Underhåll

### Beroendehantering (Dependabot)

- **Schema (UTC) & flöde**
  - **Maven (app):** **måndagar 01:00 UTC** → PR till `test`  
    – *en körning, två grupper*:
    - `maven-security` – **endast säkerhetsuppdateringar**
    - `maven-patch-minor` – **patch/minor för direkta beroenden**
  - **GitHub Actions (CI):** **måndagar 01:15 UTC** → PR till `test`  
    – versionbumps **grupperas** (patch/minor) och säkerhetsuppdateringar kommer när advisories finns.
  - **Tidszon:** alla tider är **UTC** för konsekventa körningar året runt.

- **Policy**
  - PR-gruppering och **auto-rebase**; **target branch:** `test`.
  - **Majors** (t.ex. `spring-boot`) ignoreras här och planeras separat.
  - **Branchskydd:** PR kräver **grön CI** (build + tester) och **Gitleaks** innan merge.

### Kodskanning (SARIF)

Alla säkerhetsresultat från både **Gitleaks** (hemligheter) och **Trivy** (image-skanning) laddas upp som **SARIF** till *Security → Code scanning* för central analys och historik.

## Projektstruktur

```text
spring-boot-integration/
│
├── .github/workflows/      # CI/CD-workflows (ci.yaml, docker-publish.yaml, secret-scan.yaml)
├── .gitignore              # Ignorerade filer (t.ex. target/, logs/, .env)
├── .dockerignore           # Utesluter onödiga filer från Docker build-context
├── .gitleaks.toml          # Regler för secret scanning (Gitleaks)
├── dependabot.yaml         # Automatiska dependency-uppdateringar
├── .env.example            # Exempel på miljövariabler (inga hemligheter i koden)
├── docs/                   # Dokumentation (USAGE.md, TESTS.md) & bilder (docs/images)
├── src/                    # Källkod (main/) och tester (test/)
├── pom.xml                 # Maven-konfiguration (beroenden/plugins)
├── Dockerfile              # Bygger applikationsimagen
├── docker-compose.yaml     # Lokal orkestrering (app, ActiveMQ, PostgreSQL)
└── README.md               # Projektöversikt
```

## Relaterade dokument

- **Användning:** [docs/USAGE.md](docs/USAGE.md)  
- **Tester:** [docs/TESTS.md](docs/TESTS.md)
- **Artefakter** [docs/ARTIFACTS.md](docs/ARTIFACTS.md)

## Kontakt

Igor Gomes — DevOps Engineer  
[LinkedIn](https://www.linkedin.com/in/igor-gomes-5b6184290) 
[Docker Hub](https://hub.docker.com/r/igor88gomes/spring-boot-integration/tags)  
**E-post:** [igor88gomes@gmail.com](mailto:igor88gomes@gmail.com)