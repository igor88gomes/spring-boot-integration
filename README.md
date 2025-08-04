# Spring Boot Integration – ICC Demo

Ett professionellt och tekniskt genomarbetat demo-projekt som visar modern systemintegration med Spring Boot, ActiveMQ, H2 och CI/CD via GitHub Actions. Projektet är inspirerat av ett verkligt integrationsscenario och syftar till att vidareutveckla mina kunskaper inom DevOps, automatisering och integration mellan mikrotjänster.

## Funktionalitet

- REST API för att skicka och hämta meddelanden
- ActiveMQ (JMS) för asynkron meddelandehantering
- Persistens med H2 in-memory databas
- JSON-strukturerad loggning med Logback + MDC
- Enhetstester med JUnit + Mockito (94% täckning via JaCoCo)
- Fullständig CI/CD-pipeline med GitHub Actions
- Dockeriserad och klar att köra lokalt eller i molnbaserad miljö

## Arkitekturöversikt

```mermaid
flowchart LR
    A[REST API - MessageProducer] -->|POST /api/send| B[ActiveMQ Queue]
    B --> C[MessageConsumer]
    C -->|Sparar| D[(H2 Database)]
    D -->|GET /api/all| A
```

## Teknologier

| Teknologi             | Användning                             |
|-----------------------|----------------------------------------|
| Spring Boot 3.3.2     | Huvudramverk                           |
| ActiveMQ              | Meddelandekö (JMS)                     |
| H2 Database           | In-memory databas för test och logg    |
| Spring Data JPA       | Hantering av entiteter och datalagring |
| JUnit + Mockito       | Enhetstester                           |
| JaCoCo                | Kodtäckningsrapport                    |
| Logback + MDC         | Strukturerad loggning i JSON-format    |
| GitHub Actions        | CI/CD-pipeline                         |
| Docker + Compose      | Paketering och lokal drift             |

## API Endpoints (testade i Postman)

### Skicka ett meddelande

```http
POST /api/send?message=HejIntegration
```

**Svar:**
```
Meddelande skickat till kön: HejIntegration
```

### Hämta alla meddelanden

```http
GET /api/all
```

**Svar:**
```json
[
  {
    "content": "HejIntegration",
    "receivedAt": "2025-07-27T12:34"
  }
]
```

## Test och kodtäckning

Kör tester och generera täckningsrapport:

```bash
mvn clean verify
```

Öppna rapporten:

```
target/site/jacoco/index.html
```

**Täckning:** 94%

## Loggning

Loggar genereras i JSON-format och sparas i:

```
logs/app.log
```

Exempel:

```json
{
  "@timestamp": "2025-07-28T02:05:16.735+02:00",
  "level": "INFO",
  "message": "Meddelande skickat till kön: HejIntegration",
  "logger_name": "com.igorgomes.integration.MessageProducer",
  "messageId": "uuid..."
}
```

## Bygga och köra lokalt

```bash
git clone https://github.com/ditt-anvandarnamn/spring-boot-integration.git
cd spring-boot-integration

mvn clean install
mvn spring-boot:run
```

Testa i webbläsaren eller med Postman:

- http://localhost:8080/api/send?message=HejIntegration  
- http://localhost:8080/api/all

## Docker: Bygg och kör med Compose

```bash
docker compose up --build
```

Applikationen körs då på: http://localhost:8080  
ActiveMQ-konsolen finns på: http://localhost:8161 (användare: admin / lösenord: admin)

## Continuous Integration (CI)

Projektet använder **GitHub Actions** för att automatiskt:

- Bygga projektet
- Köra tester
- Generera kodtäckning (JaCoCo)
- Bygga Docker-image

```yaml
# .github/workflows/ci.yaml

name: Java CI with Maven

on:
  push:
    branches: [ "main" ]
  pull_request:
    branches: [ "main" ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Check out code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build with Maven
        run: mvn clean install

      - name: Run tests and generate coverage report
        run: mvn verify

      - name: Upload JaCoCo report
        if: success()
        uses: actions/upload-artifact@v4
        with:
          name: jacoco-report
          path: target/site/jacoco

      - name: Build Docker image
        run: docker build -t icc-demo-app .
```

## JavaDoc

JavaDoc genereras manuellt via `mvn javadoc:javadoc` eller via IntelliJ IDEA. Dokumentationen hittas under:

```
target/site/apidocs/index.html
```

## Projektstruktur

```
.
├── src/
├── Dockerfile
├── docker-compose.yaml
├── pom.xml
├── README.md
└── .github/workflows/ci.yaml
```

---

**Utvecklad av:** Igor Gomes – DevOps Engineer med fokus på integration och CI/CD  
**LinkedIn:** [https://www.linkedin.com/in/igor-lopes-gomes-5b6184290](https://www.linkedin.com/in/igor-lopes-gomes-5b6184290)
