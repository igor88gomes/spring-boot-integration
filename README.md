# Spring Boot Integration Demo – ICC & CI/CD

**Täcker:** integration | säkerhet | observabilitet | testbarhet | containerisering | CI/CD

[![CI – main](https://github.com/igor88gomes/spring-boot-integration/actions/workflows/ci.yaml/badge.svg?branch=main)](https://github.com/igor88gomes/spring-boot-integration/actions/workflows/ci.yaml)
[![Test Coverage](https://github.com/igor88gomes/spring-boot-integration/raw/main/.github/badges/jacoco.svg)](docs/ARTIFACTS.md#ci-artifacts)
[![Code scanning](https://img.shields.io/badge/Code%20scanning-enabled-blue)](https://github.com/igor88gomes/spring-boot-integration/security/code-scanning)
[![Secret Scan](https://github.com/igor88gomes/spring-boot-integration/actions/workflows/secret-scan.yaml/badge.svg?branch=main)](https://github.com/igor88gomes/spring-boot-integration/actions/workflows/secret-scan.yaml)
[![CD – main](https://github.com/igor88gomes/spring-boot-integration/actions/workflows/docker-publish.yaml/badge.svg?branch=main)](https://github.com/igor88gomes/spring-boot-integration/actions/workflows/docker-publish.yaml)
[![SBOM](https://img.shields.io/badge/SBOM-CycloneDX-blue)](docs/USAGE.md#cd-artifacts)
[![Multi-arch](https://img.shields.io/badge/multi--arch-amd64%20%7C%20arm64-blue)](docs/USAGE.md#verifiera-multi-arch-amd64--arm64)
[![Docker Hub](https://img.shields.io/badge/Docker%20Hub-image-blue)](https://hub.docker.com/r/igor88gomes/spring-boot-integration/tags)
[![Dependabot](https://img.shields.io/badge/Dependabot-enabled-blue)](https://github.com/igor88gomes/spring-boot-integration/pulls?q=is%3Apr+author%3Aapp%2Fdependabot)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

> Av Igor Gomes — DevOps Engineer

> **Obs (säkerhet):** Code scanning visar **aktiva findings** via **Trivy**, **Dependabot** och **Gitleaks** som en del av den 
kontinuerliga säkerhetsövervakningen i CI/CD-flödet. Dessa findings ingår som en del av den löpande säkerhetskontrollen. Kritiska 
sårbarheter blockeras automatiskt, medan högre nivåer rapporteras som **SARIF** i *GitHub repository → Security → Code scanning 
alerts*. Se [SECURITY.md](SECURITY.md) för policy och hantering av findings.

## Projektöversikt

Detta projekt visar hur man kan bygga en **spårbar integrationskedja** (**REST → JMS → DB**) med inbyggd **säkerhet** och **CI/CD**.

Syftet är att:
- Förenkla felsökning genom **korrelations-ID** och tydliga loggar.
- Öka observabiliteten med automatiserad testbarhet och mätning.
- Möjliggöra **reproducerbara deploymenter** via containerisering och pipelines.

> Fokus ligger på att visa hur integration, spårbarhet och DevSecOps kan kombineras i ett modernt DevOps-flöde snarare än att täcka alla möjliga integrationsscenarier.

Projektet består av tre huvuddelar: **Applikationen**, **CI/CD-pipeline** och **Infrastrukturen**.

### Applikationen
En meddelandebaserad integrationskedja (**REST → JMS → DB**) med spårbarhet (korrelations-ID), validering, JSON-loggar, observabilitet och en komplett testpyramid (enhetstest/kontrakt/BDD/E2E).

### CI/CD-pipeline
En **CI/CD-pipeline** som kör testerna automatiskt, mäter **kodtäckning med JaCoCo** och publicerar rapport/badge. Den integrerar också DevSecOps-principer (sårbarhetsskanning med Trivy,
hemlighetsskanning med Gitleaks, SBOM med CycloneDX och reproducerbara multi-arch Docker-images).

### Infrastrukturen (runtime)
Kör applikationen i **Docker Compose** tillsammans med ActiveMQ och PostgreSQL. Här används **exakt samma image/digest** som pipelinen har byggt, testat, skannat och publicerade på **Docker Hub**.

### Vilket problem löser projektet?
- Integrationer misslyckas ofta inte bara på kodnivå, utan i hur de byggs, testas och körs i drift.  
  → Projektet visar hur detta kan hanteras med en spårbar och containeriserad kedja **REST → JMS → DB**.
- Brist på spårbarhet gör det svårt att följa ett anrop end-to-end.  
  → Här används **korrelations-ID i hela flödet** för att möjliggöra felsökning på detaljnivå.
- Otillräcklig testbarhet gör det svårt att reproducera kedjan i CI.  
  → Projektet innehåller en **fullständig testpyramid (enhetstest, kontrakt, BDD/E2E)** som körs automatiskt i CI.
- Låg insyn i leveransflödet (osäkra images, saknade SBOM).  
  → Här integreras **DevSecOps-principer** med sårbarhetsskanning (Trivy), hemlighetsskanning (Gitleaks), SBOM (CycloneDX) och reproducerbara multi-arch Docker-images.

### Varför är det viktigt?
- Utan spårbarhet och robust testning blir integrationer svåra att underhålla och ännu svårare att felsöka i drift.
- Genom att kombinera **korrelation med `messageId`**, **automatiska tester i CI (H2 + inbäddad ActiveMQ)**, **kontraktstester (SCC)** och **BDD/E2E-scenarier** kan projektet visa hur kvalitet säkras redan innan kod når produktion.
- Att dessutom integrera **säkerhet i flödet** med **sårbarhetsskanning (Trivy)**, **hemlighetsskanning (Gitleaks)** och **SBOM (CycloneDX)** gör att leveranskedjan blir transparent och pålitlig.

### Vilket värde skapar det i drift?
I praktiken innebär detta att samma artefakt kan följas från kod → test → produktion:
- **Multi-arch Docker-images** byggs i CI, skannas, och **publiceras först som kandidat i GHCR**.
- Efter **preflight-inspect**, **retry vid transienta fel** och **promotion av exakt samma digest** publiceras den till Docker Hub.
- Detta ger **reproducerbara deploymenter**, kortare MTTR tack vare korrelationsloggar i JSON, och en tryggare kedja där både **kvalitet och säkerhet** är inbyggda från början.
- I drift används **exakt samma image** i Docker Compose, vilket säkerställer konsekvent beteende mellan pipeline och runtime.
- Eftersom imaget är multi-arch och publiceras med reproducerbara digest kan det lika enkelt användas i Kubernetes eller andra containerplattformar.

---

## Applikationen

### Arkitektur & korrelation (översikt)

Två huvudflöden finns i applikationen:

- **Skrivflöde (`POST /api/send`):**  
  `Klient → Controller → Producer → [ActiveMQ] → Consumer → JPA → DB`

- **Läsflöde (`GET /api/all`):**  
  `Klient → Controller → JPA → DB`

> Persistens sker via **JPA**. Vald databas beror på körmiljö (t.ex. H2 i CI, PostgreSQL i Compose).

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
Se [docs/USAGE.md](docs/USAGE.md) för fler detaljer.

### Testbarhet (översikt)
Applikationen valideras med en komplett testpyramid:
- Enhetstester för **Controller**, **Producer**, **Consumer** och **Repository**
- **Kontraktstester (SCC)** för HTTP-kontrakt
- **BDD/E2E (Cucumber)** som verifierar kedjan **HTTP → JMS → DB**

Detaljer om scenarier och kommandon finns i [docs/TESTS.md](docs/TESTS.md).

## CI/CD-pipeline

<p align="center">
  <img src="docs/images/cicd-pipeline-diagram.png"
       alt="CI/CD-pipeline – Spring Boot, Docker och GitHub Actions"
       width="900">
</p>
<p align="center"><em><strong>Bild 1.</strong> Översikt av CI/CD-pipeline. Diagrammet visar huvudflödet: bygg, test, säkerhetsskanning samt image-publicering och distribution.</em></p>

### Plattform & verktyg (CI/CD)
**Pipeline:** GitHub Actions (CI/CD), Docker Buildx (multi-arch), GHCR (candidate), Docker Hub (public release)  
**Säkerhet:** Trivy (container scanning), Gitleaks (secret scanning), Dependabot (uppdateringar), CycloneDX SBOM  
**Artefakter:** JaCoCo (coverage), Javadoc, SCC-stubs, SARIF (security findings), SBOM

> **Policy:** Lokalt Compose återanvänder **samma digest** som byggs i CD (GHCR → promotion till Docker Hub) för reproducerbara deploymenter.

### Flöde i korthet
1. **CI (build & test):** `mvn verify` (H2 + embedded ActiveMQ) → JaCoCo-rapport → badges/artefakter.
2. **Image (candidate):** Buildx bygger **linux/amd64, linux/arm64** och pushar till **GHCR** med rika OCI-etiketter.
3. **Skanning:** Trivy kör quality gate (CRITICAL blockerar; HIGH rapporteras som **SARIF**).
4. **Preflight:** `imagetools inspect` på exakta digestet.
5. **Promotion:** **Samma digest** pushas till Docker Hub med retry/backoff och steg-timeouts.
6. **SBOM & rapporter:** CycloneDX SBOM + SARIF + övriga artefakter kan laddas ner från Actions.

**Snabblänkar för Mer detaljer om pipelinestrategin, flöden och quality gates finns i :**
- **CI/CD-pipeline** [docs/PIPELINE.md](docs/PIPELINE.md)
- **Artefakter** [docs/ARTIFACTS.md](docs/ARTIFACTS.md)

## Infrastrukturen (runtime)

<p align="center">
  <img src="docs/images/architecture-diagram.png"
       alt="Arkitekturöversikt – Spring Boot, ActiveMQ och PostgreSQL"
       width="900">
</p>
<p align="center"><em><strong>Bild 2.</strong> Runtime-arkitektur – applikationen körs i Docker Compose tillsammans med ActiveMQ och PostgreSQL. <strong>Arkitekturstil:</strong> modulär monolit (meddelandebaserad via JMS).</em></p>

## Körning 

**Hela stacken körs containeriserad med Docker Compose** (stöd för Podman Compose):

- `integration-app` – applikationen (image: `igor88gomes/spring-boot-integration:latest`, **byggs och pushas av CI/CD-pipelinen**)
- `activemq` – ActiveMQ (JMS) (image: `rmohr/activemq:5.15.9`)
- `postgres` – PostgreSQL (image: `postgres:13`)

**Hälsa & startordning:** Alla tjänster har `healthcheck`; `integration-app` startar först när **ActiveMQ** och **PostgreSQL** är *healthy*.  
**Loggning:** `integration-app` Kör med profilen `stdout-only` så att loggar går till stdout/stderr (12-factor).  
**Java:** Applikationsimagen innehåller **Java 17 (JRE)** och inget lokalt JDK krävs.

Se [docs/USAGE.md](docs/USAGE.md) för fler detaljer och körningskommandon.

## Projektstruktur

```text
spring-boot-integration/
│
├── .github/workflows/      # CI/CD-workflows (ci.yaml, docker-publish.yaml, secret-scan.yaml)
├── .gitignore              # Ignorerade filer (t.ex. target/, .env)
├── .dockerignore           # Utesluter onödiga filer från Docker build-context
├── .gitleaks.toml          # Regler för secret scanning (Gitleaks)
├── dependabot.yaml         # Automatiska dependency-uppdateringar
├── .env.example            # Exempel på miljövariabler (inga hemligheter i koden)
├── docs/                   # Dokumentation (USAGE.md, TESTS.md) & bilder (docs/images)
├── src/                    # Källkod (main/) och tester (test/)
├── pom.xml                 # Maven-konfiguration (beroenden/plugins)
├── Dockerfile              # Bygger applikationsimagen
├── docker-compose.yaml     # Lokal orkestrering (app, ActiveMQ, PostgreSQL)
├── SECURITY.md             # Säkerhetspolicy & hantering av findings
├── LICENSE                 # MIT License för projektet
└── README.md               # Projektöversikt
```

## Relaterade dokument

- **Användning:** [docs/USAGE.md](docs/USAGE.md)  
- **Tester:** [docs/TESTS.md](docs/TESTS.md)
- **CI/CD-pipeline** [docs/PIPELINE.md](docs/PIPELINE.md)
- **Artefakter** [docs/ARTIFACTS.md](docs/ARTIFACTS.md)
- **Säkerhetspolicy:** [SECURITY.md](SECURITY.md)
- **Licens:** [LICENSE](LICENSE)

## Kontakt

Igor Gomes — DevOps Engineer  
[LinkedIn](https://www.linkedin.com/in/igor-gomes-5b6184290) 
[Docker Hub](https://hub.docker.com/r/igor88gomes/spring-boot-integration/tags)  
**E-post:** [igor88gomes@gmail.com](mailto:igor88gomes@gmail.com)