# Spring Boot Integration – ICC Demo  

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
  varje ändring som pushas till brancherna `main` och `test`. JaCoCo-rapporten publiceras som artifact i
  varje CI-körning och kan laddas ner från Actions-sidan. På `main` genereras även JavaDoc och 
  publiceras som artifact.

- **(CD)** `docker-publish.yaml` bygger och publicerar automatiskt en ny version av applikationens Docker
  image till Docker Hub vid push till `main`-branchen.

För en fullständig beskrivning av varje steg i CI/CD-flödet, se:
- [.github/workflows/ci.yaml](./.github/workflows/ci.yaml)
- [.github/workflows/docker-publish.yaml](./.github/workflows/docker-publish.yaml)

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

Arkitekturen möjliggör spårbar och tillförlitlig kommunikation i en modulär och lättunderhållen lösning.
---

## Funktionalitet

- Exponerar REST API med Spring Boot
- Hanterar asynkron meddelandeöverföring med ActiveMQ (JMS)
- Lagrar persistent data i PostgreSQL via JPA
- Loggar i JSON-format med Logback + MDC
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
- Hela stacken körs containeriserad via **Docker Compose**:
    - `integration-app` – applikationen (image: `igor88gomes/spring-boot-integration:latest`, byggd av CD)
    - `activemq` – ActiveMQ (JMS)
    - `postgres` – PostgreSQL
- Ingen lokal JDK krävs för att **köra** med Compose.
- För **lokal build/test utanför container** krävs JDK 17 och att stödtjänsterna är igång (t.ex. starta 
  dem med `docker compose up`).
- Kommandon för start, loggar och felsökning finns i [docs/USAGE.md](docs/USAGE.md).

### Bygg / CI & dokumentation

- Maven – bygg- och beroendehantering
- GitHub Actions (`.github/workflows/ci.yaml`) – CI (bygge + tester) 
- JaCoCo – kodtäckning (artefakt i CI)
- JavaDoc – API-dokumentation (artefakt i CI)

### Distribution (CD)
- GitHub Actions (`.github/workflows/docker-publish.yaml`) – bygger/pushar image vid push till `main`
- Docker Hub – `igor88gomes/spring-boot-integration:latest`

## Projektstruktur

```text
spring-boot-integration/
│
├── src/                    # Java-källkod & tester
├── pom.xml                 # Maven-konfiguration
├── Dockerfile              # Image för Spring Boot appen
├── docker-compose.yaml     # Kör både app och ActiveMQ
├── .github/workflows/      # CI/CD pipelines
└── docs/                   # Dokumentation
```

**Detaljer:**
- [USAGE](docs/USAGE.md)
- [PROJECT_HISTORY](docs/PROJECT_HISTORY.md)
- [images/](docs/images/)

## Loggning

Applikationen loggar i JSON-format (Logback + Logstash Encoder) till **konsol** och **fil**.

- Aktiv fil: `logs/app.log` för den aktuella dagen.
- Daglig rotation: vid **första logghändelsen efter midnatt** roteras gårdagens logg till `logs/app.YYYY-MM-DD.log`.
- Historik: äldre loggar sparas i **7 dagar**.

Detaljerade kommandon för att visa loggar (docker/podman) och exempel på loggutdata finns i
[docs/USAGE.md](docs/USAGE.md).

## Test och kodtäckning

Tester och kodtäckning (JaCoCo) körs i **CI-pipelinen** (GitHub Actions) vid push/PR till `main` 
och `test`.

**Så hämtar du rapporten:**
1. Gå till **Actions** i GitHub-repot och öppna körningen för ditt commit.
2. Under **Artifacts** klicka på **`jacoco-report`** och ladda ner ZIP:en.
3. Öppna `index.html` i den nedladdade mappen för att se täckningen.

> Obs: Rapporten lagras som artifact i 14 dagar och ingår inte i Docker-image eller i Git-repot.

## JavaDoc

JavaDoc genereras i **CI-pipelinen** (GitHub Actions) och publiceras som artifact (endast på `main`).

**Så hämtar du dokumentationen:**
1. Gå till **Actions** i GitHub-repot och öppna körningen för ditt commit.
2. Under **Artifacts** klicka på **`javadoc`** och ladda ner ZIP:en.
3. Öppna `index.html` i den nedladdade mappen (`target/site/apidocs/` i ZIP:en).

> Obs: JavaDoc ingår inte i Docker-image eller i Git-repot (ignoreras i `.gitignore`).

> Lösningen är utformad för att demonstrera moderna integrationsarkitekturer och inkluderar design, 
implementation och automatisering av hela livscykeln för en applikation, från kod till driftsättning, med
ett tydligt DevOps-perspektiv.

## Kontakt

Igor Lopes Gomes 
[LinkedIn](https://www.linkedin.com/in/igor-lopes-gomes-5b6184290) 
[Docker Hub](https://hub.docker.com/u/igor88gomes)
