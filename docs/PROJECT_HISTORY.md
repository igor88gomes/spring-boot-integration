# Utvecklingsresa och beslut

## Bakgrund

Applikationen utvecklades från grunden i **Java 17 med Spring Boot 3**  
för att demonstrera en komplett integrationslösning med **REST API, JMS och datalagring**.

I början kördes den som en enkel prototyp i IntelliJ med den inbyggda databasen **H2 (in-memory)**  
som tillhandahölls via Spring Boot, tillsammans med en lokalt installerad **ActiveMQ-tjänst**.  
Detta innebar att ActiveMQ behövde laddas ner och startas manuellt på utvecklingsmaskinen  
för att applikationen skulle fungera fullt ut.

## Viktiga steg och förändringar

### 1. Databasbyte: H2 → PostgreSQL   

**Varför:** 

- PostgreSQL används nu i den containeriserade miljön för att spegla en mer produktionslik setup, men **H2** används fortfarande vid testkörningar (profil `test`) för att få snabba, isolerade och pålitliga tester utan att vara beroende av en extern databas.
 
**Effekt:**

- Produktionslik och lokal utveckling i full stack använder PostgreSQL.
- CI/CD-pipelines och lokala enhetstester körs mot H2 (in-memory) vilket ger snabbare byggen och enklare felsökning.
- Möjliggör att tester kan köras utan att en databascontainer måste vara startad.

### 2. Köhantering med ActiveMQ

**Varför:** 

- För att implementera asynkron kommunikation enligt ICC-mönster.

**Effekt:**

- Simulerar ett verkligt scenario där meddelanden produceras och konsumeras oberoende av varandra, vilket ökar robustheten.

### 3. Containerisering och orkestrering

**Varför:** 

- Minska lokala beroenden och undvika "fungerar på min maskin"-problem genom att köra hela stacken automatiskt via **Docker Compose** i containrar istället för att installera tjänster manuellt på sin dator.

**Teknik:** 

- **Docker Compose** för att starta applikation, ActiveMQ och PostgreSQL i en gemensam stack.
- **Förutsättning:** Installerad OCI-kompatibel container-runtime (t.ex. Docker, Podman) och verktyg för Compose (t.ex. Docker Compose eller podman-compose).


### 4. Strukturerad loggning med JSON + MDC

**Varför:** 

- För att möjliggöra spårbarhet end-to-end och integration med logghanteringsverktyg (t.ex. ELK, Kibana, etc.).

**Effekt:** 

- Varje meddelande får ett korrelations-ID som följer hela flödet producer → konsumer → databas.

### 5. CI/CD i GitHub Actions med kontinuerlig leverans till Docker Hub

- **Varför:** Automatisera bygg, test och distribution för att säkerställa att den senaste versionen alltid finns tillgänglig.
- **Designbeslut:**
    - **CI-pipeline:** Bygger med Maven/Java 17, kör enhetstester mot H2, publicerar JaCoCo-rapport (`test` och `main`) samt JavaDoc (endast `main`).
    - **CD-pipeline:**
        - Bygger applikationens Docker-image vid varje commit till `main`.
        - Publicerar image till Docker Hub med två taggar:
            - `latest` för att alltid peka på den senaste stabila versionen.
            - Commit-specifik tagg (`<commit-SHA>`) för historik och reproducerbarhet.
        - Detta gör att vem som helst kan starta den uppdaterade applikationen direkt från Docker Hub utan att behöva bygga lokalt.
    - **Artefakter istället för incheckning:** Rapporter och dokumentation lagras som Actions-artefakter för att hålla containrar små och koden ren.

#### Byggkedjan från kod till container**  
  
    → Docker-imagen skapas direkt från projektets egen källkod (Spring Boot-applikationen och dess logik).  
    → Detta säkerställer att den publicerade imagen alltid motsvarar det faktiska projektet och dess utveckling.

---

## Lärdomar

- Att separera testdatabas (H2) från produktionsdatabas (PostgreSQL) ger både snabbhet och realism i olika miljöer.
- Vikten av separata miljökonfigurationer (`application.properties` vs `application-test.properties`) för att undvika miljöberoende problem.
- Hur asynkron arkitektur kräver tydlig spårbarhet och robust felhantering.
- Fördelen med automatiserad publicering till Docker Hub: alltid en uppdaterad, körbar version tillgänglig för test och demo.

---

## Möjliga framtida steg

> **Mål:** göra applikationen mer robust genom kontinuerliga förbättringar samt löpande förfining av kod och dokumentation.

- Införa **kontrakttester** för REST-API:t och meddelandeformat (t.ex. Spring Cloud Contract/Pact).
- Införa **Flyway** för versionshantering av databasen.
- Skapa en **separat CI-pipeline** för integrationstester (Testcontainers med PostgreSQL och ActiveMQ/Artemis).
- Skapa **Helm-chart** och/eller **Argo CD-applikationer** för drift i OpenShift/Kubernetes.
- Utöka observabilitet: fler **Actuator-metrik**, **Micrometer/Prometheus** och **Elastic APM**; larm och dashboards i **Kibana**.
- Förbättra felhantering: central **@ControllerAdvice**, standardiserad felmodell och tydligare loggning.
- Förstärka konfiguration och säkerhet: kö-namn via property med default, hemligheter via miljövariabler/GitHub Secrets, begränsad Actuator-exponering i produktion.
- Kodhygien: **JPA-auditering** (Instant/@CreatedDate), små refaktoreringar och **OpenAPI/Swagger** för API-dokumentation.

### Kontinuerliga förbättringar (löpande)

#### 2025-08-28 — Inputvalidering och end-to-end-korrelation
- `/api/send`: validering → **400 (Bad Request)** vid tom eller endast blanktecken i `message`.
- `MessageProducer`: sätter `messageId` som **JMS-header**.
- `MessageConsumer`: läser `messageId`-headern och sätter den i MDC → **samma `messageId`** i producer- och consumer-loggar (E2E-spårbarhet).
