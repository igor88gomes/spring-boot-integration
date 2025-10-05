# Tester

Testerna omfattar **Controller**, **Producer**, **Consumer**, **Repository** och **HTTP-kontrakt**. Fokus: **indata-validering**, **korrelation (MDC + JMS)** och **stabila HTTP-svar**.

> Alla tester körs i **byggfasen** (lokalt eller i CI) innan applikationsimage skapas och containermiljön kör den redan testade applikationen.

---

## Testtyper & ramverk

- **Enhet/Web-slice** — JUnit 5, Mockito, Spring Boot Test, **MockMvc**.
- **Persistens (H2)** — Spring Boot Test + JPA, in-memory H2 i **profil `test`**.
- **BDD/E2E** — Cucumber (JUnit 5) + Spring Boot Test + **Awaitility**; **inbäddad ActiveMQ (vm://embedded)** och H2.
- **Kontrakt (SCC)** — Spring Cloud Contract Verifier (genererar JUnit-tester och **stubs.jar**; kör mot MVC-slice via `BaseContractTest`).

*Ungefärlig progression: från enklare/snabbare till mer omfattande tester.*

## Enhet/Web-slice

- **Syfte:** Verifiera HTTP-lager (statuskoder, headers) och producentlogik utan externa beroenden.
- **Omfattning (exempel):**
  - Controller: `POST /api/send` (**200/400** + `Content-Type: text/plain`), `GET /api/all` (**200** + `application/json`).
  - Producer: sätter `messageId`-header när MDC finns; loggar fel utan att propagera undantag.
- **Miljö:** Spring Boot Test (web-slice) + **MockMvc**/**Mockito**; inga externa tjänster.
- **Källor/Plats:**
  - `MessageControllerTest`, `MessageControllerHttpErrorsTest`,
  - `MessageProducerTest`, `MessageProducerErrorTest`
- **Körning:** Ingår i `mvn test` / `mvn verify` (Surefire).
- **Artefakter:** Täcks av JaCoCo-rapport i CI.
- **Felsökning:** Vid fel `400`/`Content-Type`, kontrollera controller-annoteringar (t.ex. `produces = MediaType.TEXT_PLAIN_VALUE`) och validering av `message`.

## Persistens (H2)

- **Syfte:** Validera JPA-mappning och baspersistens mot in-memory DB.
- **Omfattning (exempel):** Spara/läsa `MessageEntity`, `@NotBlank content`, `receivedAt` sätts.
- **Miljö:** JPA-test med H2 i **profil `test`** (t.ex. `@DataJpaTest`/`@SpringBootTest` + `TestDatabase`).
- **Källor/Plats:** `MessageRepositoryTest`
- **Körning:** Ingår i `mvn test` / `mvn verify`.
- **Artefakter:** Ingår i JaCoCo-rapporten i CI.
- **Felsökning:** Säkerställ testprofilen och `application-test.properties` (H2, `ddl-auto=create-drop`) är aktiva.

## BDD/E2E (Cucumber)

- **Syfte:** Verifierar kedjan end-to-end: HTTP → JMS-kö → Consumer → JPA-persistens, inkl. loggkorrelation via `messageId` (MDC + JMS-header).
- **Omfattning (exempel):**
  - Giltigt meddelande ⇒ `/api/send` svarar **200**, meddelandet skickas till kön och sparas i DB.
  - Blankt meddelande ⇒ `/api/send` svarar **400**, **ingen** ny rad i DB.
- **Miljö:** Inbäddad ActiveMQ (`vm://embedded`), H2 (profil **`test`**), **Awaitility** för asynkrona kontroller.
- **Källor/Plats:**
  - Scenarier: `src/test/resources/features/message_integration.feature`
  - Steg/konfiguration: `src/test/java/com/igorgomes/integration/bdd/*`
- **Körning:**
  - **Alla tester:** `mvn test`
  - **Endast E2E:** `mvn -Dtest=CucumberTest test`
  - (Valfritt) Taggar: `-Dcucumber.filter.tags="@smoke"` eller `@regression`
- **Artefakter:** Körs under **Surefire** tillsammans med övriga JUnit 5-tester.
- **Felsökning:** Om JMS-anslutning misslyckas, säkerställ aktiv **testprofil** och `broker-url` som ovan.

## Kontrakt (SCC)

- **Syfte:** Säkerställa HTTP-kontraktet (statuskoder/validering och `Content-Type`) mot API:t.
- **Omfattning:** `contracts/valid_message.groovy` (200 + `text/plain`) och `contracts/invalid_message.groovy` (400).
- **Miljö:** MVC-slice via `BaseContractTest` — inga externa tjänster krävs.
- **Källor/Plats:** `src/test/resources/contracts/*`
- **Körning:** Ingår automatiskt i `mvn verify` (SCC **generateTests** + **convert**) och körs sedan som vanliga JUnit-tester.
- **Artefakter:** Genererade JUnit-tester samt **`stubs.jar`** för konsumenter.
- **Felsökning:** Vid mismatch (t.ex. fel status/`Content-Type`), kontrollera kontrakten och att `BaseContractTest` mappar rätt controller.

## Sammanfattning: Körning & Artefakter

- **Lokal/CI:** `mvn verify` kör alla JUnit, Cucumber och **SCC**-tester.
- **Artefakter (CI):** JaCoCo-rapport, JavaDoc (**endast main**) och **stubs.jar** från SCC.
- **Coverage-badge:** CI uppdaterar badge **coverage** (JaCoCo) på `test`; den följer med i PR → `main`. 

**Artefakter (CI/CD):** se [docs/USAGE.md#artefakter-cicd](docs/USAGE.md#artefakter-cicd).

---

## Kör tester lokalt

Du kan köra alla tester lokalt.

**Förutsättningar:** JDK 17 eller senare och Maven.

```bash
# Snabbkoll av miljön
mvn -v

# Snabbkörning lokalt (H2 + inbäddad ActiveMQ via profil "test")
mvn clean test

# Med täckningsrapport (JaCoCo) och genererade kontraktstester
mvn clean verify
```