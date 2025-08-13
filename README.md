# Spring Boot Integration – ICC Demo  

[![CI](https://github.com/IgorGomes01/spring-boot-integration/actions/workflows/ci.yaml/badge.svg)](https://github.com/IgorGomes01/spring-boot-integration/actions/workflows/ci.yaml)
[![CD](https://github.com/IgorGomes01/spring-boot-integration/actions/workflows/docker-publish.yaml/badge.svg)](https://github.com/IgorGomes01/spring-boot-integration/actions/workflows/docker-publish.yaml)
[![Docker Hub](https://img.shields.io/badge/Docker%20Hub-image-blue)](https://hub.docker.com/u/igor88gomes)

> Av Igor Lopes Gomes — DevOps Engineer

<p align="center">
  <img src="docs/images/architecture-diagram.png"
       alt="Arkitekturöversikt – Spring Boot, ActiveMQ och PostgreSQL"
       width="900">
</p>

<p align="center">
  <img src="docs/images/cicd-pipeline-diagram.png"
       alt="CI/CD-pipeline – Spring Boot, Docker och GitHub Actions"
       width="900">
</p>

# Projektinformation

Detta projekt visar en komplett integrationslösning inspirerad av ICC-mönster, utvecklad från grunden med 
moderna teknologier och etablerade arkitekturmönster. Lösningen kombinerar asynkron kommunikation, 
spårbarhet, testbarhet och fullständig automatisering genom CI/CD-pipelines och Docker-baserad 
distribution.

Applikationen är byggd med Java och Spring Boot 3, och använder ActiveMQ (JMS) för meddelandehantering, 
PostgreSQL för datalagring samt Docker för containerisering. CI/CD-pipelines implementeras med GitHub 
Actions och publicerar automatiskt applikations-image till Docker Hub.

Loggningen är strukturerad i JSON-format med Logback och Logstash Encoder och skrivs både till konsol
(stdout) och roterande loggfiler.

Två separata pipelines hanterar applikationens CI/CD-flöde:

- **(CI)** `ci.yaml` validerar, bygger (Maven), testar och genererar kodtäckningsrapport (JaCoCo) vid
  ändringar som pushas till brancherna `main` och `test`. JaCoCo-rapporten publiceras som **artefakt**
  i varje CI-körning och kan laddas ner från Actions-sidan. På `main` genereras även JavaDoc och
  publiceras som artefakt.

- **(CD)** `docker-publish.yaml` bygger och publicerar applikationens Docker-image till Docker Hub vid 
  push till `main` *(taggar Docker-imagen som `latest` och `<commit-SHA>` för spårbarhet)*.

För detaljer om pipelinen, se:
- [.github/workflows/ci.yaml](.github/workflows/ci.yaml)
- [.github/workflows/docker-publish.yaml](.github/workflows/docker-publish.yaml)
- [docs/USAGE.md#ci-artifacts](docs/USAGE.md#ci-artifacts)

Projektet är paketerat med Docker och körs lokalt med docker-compose, som konfigurerar tre containrar:
Spring Boot-applikationen (integration-app), ActiveMQ-broker (activemq) och PostgreSQL-databas (postgres).

Samma containerkonfiguration återanvänds även i CI/CD-miljön via GitHub Actions.
---

## Arkitekturöversikt

Diagrammet i bilden ovan illustrerar den asynkrona, meddelandebaserade integrationsarkitekturen i 
applikationen.

1. Klienten (t.ex. Postman eller curl) skickar ett meddelande via REST-endpointen `/api/send`.

2. `MessageProducer` publicerar meddelandet till en ActiveMQ-kö.

3. `MessageConsumer` lyssnar på kön och lagrar meddelandet i PostgreSQL-databasen.

4. Klienten kan hämta alla sparade meddelanden via `/api/all`, vilket anropar `MessageController` och 
   hämtar data med hjälp av JPA.

## Affärs-API (REST)
| Metod | Endpoint                 | Beskrivning                    |
|------:|--------------------------|--------------------------------|
| POST  | `/api/send?message=TEXT` | Skicka meddelande till kön     |
| GET   | `/api/all`               | Hämta alla sparade meddelanden |

```bash
# Exempel (Affärs-API)
curl -X POST "http://localhost:8080/api/send?message=TestIntegration"
curl http://localhost:8080/api/all
```

## Övervakning (Actuator-API)
| Metod | Endpoint            | Beskrivning            |
|------:|---------------------|------------------------|
| GET   | `/actuator/health`  | Hälsa/status för appen |
| GET   | `/actuator/info`    | App-information        |

```bash
# Exempel (health)
curl http://localhost:8080/actuator/health 
```

### Spårbarhet i loggar (kort exempel)
<details>
<summary>Visa/dölj</summary>

**Affärs-anrop:**
```bash
curl -X POST "http://localhost:8080/api/send?message=TestIntegration"
```

**`logs/app.log` (förkortad JSON):**
```json
[
  {
    "@timestamp": "2025-08-09T08:55:57.173+02:00",
    "level": "INFO",
    "logger_name": "com.igorgomes.integration.MessageProducer",
    "message": "Skickar meddelande till kön: TestIntegration",
    "messageId": "a43c8dfe-3703-4a32-94a0-c89824099a93"
  },
  {
    "@timestamp": "2025-08-09T08:55:57.201+02:00",
    "level": "INFO",
    "logger_name": "com.igorgomes.integration.MessageConsumer",
    "message": "Meddelande mottaget från kön: TestIntegration",
    "messageId": "8c68a715-a4bf-4f2e-97c9-c624a31775b4"
  }
]
```

> Exemplet visar producer → consumer och hur `messageId` (MDC) kan följas end-to-end.
> Fler kommandon och hel-loggar: se [docs/USAGE.md](docs/USAGE.md).
</details>

Arkitekturen möjliggör spårbar och tillförlitlig kommunikation i en modulär och lättunderhållen lösning.
---

## Funktionalitet

- Exponerar REST API med Spring Boot
- Hanterar asynkron meddelandeöverföring med ActiveMQ (JMS)
- Lagrar persistent data i PostgreSQL via JPA
- Loggar i JSON-format med Logback + MDC med daglig rotation, sparas i 7 dagar
- Kör enhetstester med JUnit 5 och Mockito
- Körs i containeriserad miljö via Docker Compose

## Teknologier

| Teknologi         | Användning                             |
|-------------------|----------------------------------------|
| Spring Boot 3.3.2 | Huvudramverk                           |
| ActiveMQ          | Meddelandekö (JMS)                     |
| PostgreSQL        | Databashanterare via JPA               |
| Spring Data JPA   | Hantering av entiteter och datalagring |
| Logback + MDC     | Strukturerad loggning i JSON-format    |
| JUnit + Mockito   | Enhetstester                           |

## Körning (Runtime)
- **Hela stacken körs containeriserad med **Docker Compose** (stöd för Podman Compose):
  - `integration-app` – applikationen (image: `igor88gomes/spring-boot-integration:latest`, **byggs och 
     pushas av CD-pipelinen**)
  - `activemq` – ActiveMQ (JMS)
  - `postgres` – PostgreSQL
- **Java 17 ingår i applikations-image; inget lokalt JDK krävs.**

### Bygg / CI & dokumentation

- Maven – bygg- och beroendehantering
- GitHub Actions (`.github/workflows/ci.yaml`) – CI (bygge + tester)
- JaCoCo – kodtäckning (artefakt i CI)
- JavaDoc – API-dokumentation (artefakt i CI)

### Distribution (CD)
- GitHub Actions (`.github/workflows/docker-publish.yaml`) – bygger/publicerar image vid push till `main`
- Docker Hub – `igor88gomes/spring-boot-integration:latest` - Applikationens Docker-image

## Projektstruktur

``` TEXT
spring-boot-integration/
│
├── src/                    # Java-källkod & tester
├── pom.xml                 # Maven-konfiguration
├── Dockerfile              # Bygg applikationens Docker-image
├── docker-compose.yaml     # Lokalt orkestreringsstöd (app, ActiveMQ, PostgreSQL)
├── .github/workflows/      # CI/CD-pipelines (ci.yaml, docker-publish.yaml)
└── docs/                   # Dokumentation & bilder
├── USAGE.md                # Körinstruktioner (steg-för-steg)
├── PROJECT_HISTORY.md      # Utvecklingsresa
└── images/                 # Bilder/diagram
├── architecture-diagram.png
└── cicd-pipeline-diagram.png

```

## Relaterade dokument

- **Användning:** [docs/USAGE.md](docs/USAGE.md)  
- **Utvecklingsresa:** [docs/PROJECT_HISTORY.md](docs/PROJECT_HISTORY.md)  

## Kontakt

Igor Lopes Gomes — DevOps Engineer  
[LinkedIn](https://www.linkedin.com/in/igor-lopes-gomes-5b6184290) · [Docker Hub](https://hub.docker.com/u/igor88gomes)  
**E-post:** [igor88gomes@gmail.com](mailto:igor88gomes@gmail.com)