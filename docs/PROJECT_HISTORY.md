# Project History – utvecklingsresa och beslut

## Bakgrund

Projektet började som en enkel prototyp i IntelliJ med **H2 (in-memory)** för snabb utveckling och enkel testning.  
Målet var att verifiera flödet **REST API → köhantering (JMS) → datalagring** utan att behöva extern infrastruktur.

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

- Minska lokala beroenden och undvika "fungerar på min maskin"-problem genom att köra hela stacken i containrar istället för att installera tjänster manuellt på sin dator.

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

---

## Lärdomar

- Att separera testdatabas (H2) från produktionsdatabas (PostgreSQL) ger både snabbhet och realism i olika miljöer.
- Vikten av separata miljökonfigurationer (`application.properties` vs `application-test.properties`) för att undvika miljöberoende problem.
- Hur asynkron arkitektur kräver tydlig spårbarhet och robust felhantering.
- Fördelen med automatiserad publicering till Docker Hub: alltid en uppdaterad, körbar version tillgänglig för test och demo.

---

## Möjliga nästa steg

- Införa **kontrakttester** för API och meddelandeformat.
- Använda **Flyway** eller liknande för versionshantering av databas.
- Skapa en **extra CI-pipeline** för att köra integrationstester mot en **PostgreSQL-container**.
- Skapa Helm/Argo CD-manifest för drift i **OpenShift** eller Kubernetes.
- Utöka övervakning med fler **Actuator-metrics** och **Elastic APM**, samt skapa larmregler och dashboards för visualisering i **Kibana**.

