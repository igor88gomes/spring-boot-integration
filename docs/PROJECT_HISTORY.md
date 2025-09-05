# Utvecklingsresa och beslut

## Sammanfattning
- Asynkron integration: REST → JMS (ActiveMQ) → PostgreSQL.
- End-to-end (E2E) korrelations-ID (MDC + JMS header) med JSON-loggar.
- CI/CD i GitHub Actions; imagen publiceras till Docker Hub.
- Hela stacken körs containeriserad (Compose/Docker).
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

- **Kontrakttester för REST + JMS-header (Spring Cloud Contract/Pact)**  
  *Syfte:* göra dagens beteende bindande (skydd mot regressioner).

    - `POST /api/send` — två kontrakt:
        - **Ogiltigt:** tomt/blankt `message` ⇒ **400**.
        - **Giltigt:** `message` med innehåll ⇒ **200**.

    - Producer — headerkontrakt:
        - Sätt JMS-header **`messageId`** när **MDC** har värde.
        - Sätt **inte** headern när **MDC** saknas.

**Effekt:** om beteendet ändras av misstag faller kontraktstesterna och pipelinen stoppar tills kontraktet uppdateras med avsikt.

- **Flyway för databasversionering**  
  Introducera `V1__init.sql` för tabellen och lås schemautveckling via migrationer.  
  
**Effekt:** spårbar databas-historik och reproducerbara deployer.

- **Säkerhet & kryptering – baslinje (rekommenderad nu)**
    - **TLS mot ActiveMQ:** använd `ssl://` (t.ex. port **61617**) och servercertifikat.
    - **Logghygien:** logga inte meddelandekroppen i **INFO**; logga `messageId` + status.

**Effekt:** skyddar trafik i transit, minskar läckagerisk i loggar, noll intrång i payload.

### Medel sikt

- **CI-jobb med Testcontainers (PostgreSQL + ActiveMQ/Artemis)**  
  Kör integrationsflödet end-to-end i pipeline utan externa beroenden.

- **Observabilitet (grund)**  
- **Micrometer/Prometheus** + utökade **Actuator**-endpoints (dev/test).  

**Effekt:** metrics för kölatens, fel, throughput.

### Längre sikt

- **Central felhantering**  
  `@ControllerAdvice` + standardiserad felmodell (JSON) och korrelations-ID i svar.

- **OpenAPI/Swagger**  
  Generera och publicera API-spec för konsumenter.

- **Helm/Argo CD**  
  Deploy till Kubernetes/OpenShift med deklarativ drift.

- **Kryptering av payload**  
  End-to-end-kryptering med **AES-GCM**.
    - **Producer:** kryptera message → `byte[]` + GCM-tag.
    - **Consumer:** dekryptera och processa.
    - **Nyckelhantering:** lagra nyckel i t.ex. keystore eller KMS; plan för rotation.
    - **Header:** lämna `messageId` okrypterad för korrelation.  
      **Trade-off:** högre komplexitet (nycklar, rotation, felhantering) – görs bara om kravet finns.

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

#### #### 2025-08-31 — Kö-namn via property + observabilitet
- **Genomfört:** externaliserat kö-namn till `app.queue.name` med fallback `test-queue`.
- **Producer:** läser konfigurerat kö-namn och loggar “Aktiv kö (konfiguration): …” vid uppstart.
- **Consumer:** `@JmsListener(destination = "${app.queue.name:test-queue}")` — samma property.
- **Actuator:** `/actuator/info` visar nu `{ "queue": { "name": "<värde>" } }`.

**Effekt:** följer 12-factor (konfig per miljö via env `APP_QUEUE_NAME`), inga ändringar i tester (default kvar), och tydlig synlighet både i logg och Actuator.

#### 2025-09-01 — Tidszon standardiserad till UTC
- **Genomfört:** JVM i UTC (`JAVA_TOOL_OPTIONS`), Logback i UTC, PostgreSQL i UTC; Compose utan lokal TZ.
- **Verifierat:** JSON-loggar med `Z`-suffix, HTTP `Date` i **GMT**, databas `now()` visar **+00**.
- **Notis:** Vid behov konvertera från UTC till användarens tidszon.

**Effekt:** Konsekventa tidsstämplar end-to-end, inga avvikelser vid skiftet mellan sommartid och normaltid, enklare korrelation och felsökning.

#### 2025-09-02 — Konfiguration & säkerhet + CI-pipeline (förfining)

**Genomfört:**
- `application.properties` läser hemligheter via miljövariabler (trygga dev-defaults).
- `docker-compose.yaml`: Postgres parametriserad via `${DB_*}`; ActiveMQ-konsolen endast på `localhost`.
- Actuator härdad: endast `health,info`; stängd `/env`; döljer configvärden.
- `ci.yaml`: förenklat till build/test; **services (Postgres/ActiveMQ) borttagna och reserverade för framtida integrations-/e2e-tester i container-miljö.**.
- `.env.example` tillagd; `.env` ignoreras i `.gitignore`.
- `Dockerfile`: definierar `JAVA_TOOL_OPTIONS` innan expansion (eliminerar varningsmeddelande).

**Effekt:**
- Lägre risk: hemligheter ut ur koden; tydlig separation dev/CI/prod.
- Minskad attackyta: ActiveMQ-konsolen endast lokalt.
- Inga onödiga tjänster i CI samt snabbare pipeline.

#### 2025-09-03 — Supply chain & gated CD, sårbarhetsskanning och multi-arch

**Genomfört:**
- **Gated release:** **candidate image** pushas till **GHCR (privat)** → **Trivy quality gate** (**CRITICAL** blockerar; **HIGH** rapporteras) → **promotion med samma digest** till Docker Hub `:latest`.
- **Multi-arch build:** `linux/amd64` + `linux/arm64` via Buildx/QEMU (behåll om kompatibilitet med Apple Silicon är önskad).
- **Supply chain:** **SBOM + attestations** aktiverat i build-steget.
- **OCI-etiketter:** `org.opencontainers.image.revision`, `created`, `source`, m.fl.
- **Concurrency:** `concurrency.group=docker-publish-${{ github.ref }}` med `cancel-in-progress: true` för att undvika parallella dubbletter vid täta pushes.
- **Retention för candidate images (GHCR):** annotations `org.opencontainers.image.ref.name=candidate` och `ghcr.io/retention-days=14`.
- **Säkerhetsrapport (SARIF):** Trivy publicerar resultat till **Security → Code scanning**.
- **GHCR-sökväg (ägarnamn):** normaliserad till gemener (lowercase) i workflow; `toLower()` togs bort (stöds ej i den nyckeln).
- **Beroenden:** uppgradering av **tomcat-embed** till **10.1.35** för att åtgärda **CVE-2025-24813**.

**Effekt:**
- Inget blir offentligt `:latest` förrän imagen passerar **sårbarhetsskanningen** (minskar exponeringsfönster).
- **Reproducerbar promotion** tack vare **digest** (samma artefakt från GHCR → Docker Hub).
- Bättre **kompatibilitet** (x86_64 och Apple Silicon) med en och samma tagg.
- **Automatisk städning** av **candidate images** i GHCR → lägre lagringskostnad/brus.
- **Synlig säkerhetsstatus** direkt i GitHub under *Code scanning*, plus **SBOM/attestations** för spårbarhet.

#### 2025-09-05 — CD-artefakter (SBOM/attestations), rensning och förfinad verifiering

**Genomfört:**
- **SBOM (CycloneDX):** genereras via Trivy från **candidate image** (via dess digest) och publiceras som Actions-artifact **`sbom`** (**14 dagar**).
- **Attestations (in-toto/SLSA):** hämtas från **GHCR** för manifest-index **och** per plattform (fallback); publiceras som Actions-artifact **`attestations`** (**14 dagar**). Loggar noterar om ingen attestation hittas.
- **Rensning:** separat **cleanup-jobb** tar bort Docker Build-artefakten (`*.dockerbuild`) från **Artifacts** för att minska brus (job summary-länken kvarstår).
- **Dokumentation:** README uppdaterad med not om städning i CD-flödet; **USAGE.md** fick avsnittet **CD-artifacts** (hur man hämtar `sbom`/`attestations`).
- **Promotion:** oförändrat — **promotion med samma digest** till Docker Hub `:latest` efter passerad **Trivy quality gate**.

**Effekt:**
- Granskare kan ladda ner **läsbar SBOM** och **proveniensfiler** direkt från Actions utan att bilden växer.
- **14 dagars retention** på båda artefakterna ger spårbarhet utan långvarig lagring.
- Renare artifacts-vy (endast relevanta filer), samtidigt som reproducibilitet och säkerhetsgrind bibehålls.
