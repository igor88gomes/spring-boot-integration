# Utvecklingsresa och beslut

## Sammanfattning (TL;DR)
- Asynkron integration: REST → JMS (ActiveMQ) → PostgreSQL.
- End-to-end (E2E) korrelations-ID (MDC + JMS header) med JSON-loggar.
- CI/CD i GitHub Actions; imagen publiceras till Docker Hub.
- Hela stacken körs containeriserad (Compose/Podman).
- Enhetstester låser kontrakt (validering + korrelation).

## Bakgrund

Applikationen utvecklades från grunden i **Java 17 med Spring Boot 3.3.2**  
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

#### Byggkedjan från kod till container
  
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

> **Mål:** öka robusthet och tydlighet utan att överbygga.

### Nästa steg (liten insats → stor nytta)

- **Kö-namn via property (med default)**  
  Externalisera kö-namnet till `app.queue.name` med fallback `test-queue`.  
  **Effekt:** enklare att byta kö per miljö utan kodändring.

- **Kontrakttester för REST + JMS-header (Spring Cloud Contract/Pact)**  
  Verifiera:
    - `POST /api/send` → **400** vid tomt/blankt `message`; **200** annars.
    - Producer sätter **JMS-header** `messageId` när **MDC** har värde; sätter **inte** annars.  
      **Effekt:** bryter CI om kontraktet ändras av misstag.

- **Flyway för databasversionering**  
  Introducera `V1__init.sql` för tabellen och lås schemautveckling via migrationer.  
  **Effekt:** spårbar databas-historik och reproducerbara deployer.

- **Säkerhet & kryptering – baslinje (rekommenderad nu)**
    - **TLS mot ActiveMQ:** använd `ssl://` (t.ex. port **61617**) och servercertifikat.
    - **Hemligheter via env/Secrets:** broker user/pass i miljövariabler/GitHub Secrets.
    - **Logghygien:** logga inte meddelandekroppen i **INFO**; logga `messageId` + status.  
      **Effekt:** skyddar trafik i transit, minskar läckagerisk i loggar, noll intrång i payload.

### Medel sikt

- **CI-jobb med Testcontainers (PostgreSQL + ActiveMQ/Artemis)**  
  Kör integrationsflödet end‑to‑end i pipeline utan externa beroenden.

- **Observabilitet (grund)**  
  **Micrometer/Prometheus** + utökade **Actuator**‑endpoints (dev/test).  
  **Effekt:** metrics för kölatens, fel, throughput.

- **Konfiguration & säkerhet**  
  Standardisera hemligheter via env/Secrets; begränsa Actuator i prod (endast `health`).

### Längre sikt

- **Central felhantering**  
  `@ControllerAdvice` + standardiserad felmodell (JSON) och korrelations‑ID i svar.

- **OpenAPI/Swagger**  
  Generera och publicera API‑spec för konsumenter.

- **Helm/Argo CD**  
  Deploy till Kubernetes/OpenShift med deklarativ drift.

- **Kryptering av payload**  
  End‑to‑end‑kryptering med **AES‑GCM**.
    - **Producer:** kryptera message → `byte[]` + GCM‑tag.
    - **Consumer:** dekryptera och processa.
    - **Nyckelhantering:** lagra nyckel i t.ex. keystore eller KMS; plan för rotation.
    - **Header:** lämna `messageId` okrypterad för korrelation.  
      **Trade‑off:** högre komplexitet (nycklar, rotation, felhantering) – görs bara om kravet finns.

### Kontinuerliga förbättringar (löpande)

#### 2025-08-28 — Inputvalidering och end-to-end-korrelation
- `/api/send`: validering → **400 (Bad Request)** vid tom eller endast blanktecken i `message`.
- `MessageProducer`: sätter `messageId` som **JMS-header**.
- `MessageConsumer`: läser `messageId`-headern och sätter den i MDC → **samma `messageId`** i producer- och consumer-loggar (E2E-spårbarhet).

**Effekt:** stoppar tomma/ogiltiga meddelanden innan de når kön (renare flöde och färre fel i konsument/databas), etablerar E2E-spårbarhet i loggar (samma `messageId` i Producer/Consumer) för snabb felsökning, och lägger grunden för CI-tester/observabilitet (JSON-loggar + MDC) utan att ändra publik kontrakt mot klienter.

#### 2025-08-30 — Korrelation via MDC + JMS, selektiv MDC-städning och tester
- **Controller**: säkerställer `messageId` i MDC för `/api/send`; skapar UUID om det saknas och tar bort nyckeln i `finally` **endast** om den sattes här.
- **Producer**: läser `messageId` från MDC och skickar som JMS-header; rör inte MDC.
- **Consumer**: tar bort enbart `messageId` i `finally` (undviker att rensa andra nycklar).
- **Tester**: fall *MDC present*/*MDC missing* i `MessageProducerTest` + test i `MessageControllerTest` som assertar MDC put/remove.
- **Dokumentation**: README uppdaterad med avsnittet *Korrelationsflöde (MDC + JMS)*; nytt `docs/TESTS.md`.
- **Övrigt**: `.gitignore` justerad för att tillåta `docs/TESTS.md`.

**Effekt:** konsekvent E2E-spårbarhet, ingen onödig MDC-rensning och starkare kontraktstester i CI.
