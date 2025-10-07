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

- **Syfte:** Verifiera HTTP-lager (statuskoder, headers och valideringsformat) samt producentlogik utan externa beroenden.
- **Omfattning (exempel):**
  - Controller:
    - `POST /api/send`
      - **200** → `Content-Type: text/plain` (svart med enkel text).
      - **400** → `Content-Type: application/problem+json` (**RFC 7807**) med struktur `{"title","status","errors":[{"field":"message","message":"..."}],"path"}`.
    - `GET /api/all` → **200** + `application/json`.
  - Producer: sätter `messageId`-header när MDC finns; loggar fel utan att propagera undantag.
  - **Lokaliserade felmeddelanden:** skicka `Accept-Language: sv-SE` ⇒ texter från `ValidationMessages_sv.properties`.
- **Miljö:** Spring Boot Test (web-slice) + **MockMvc**/**Mockito**; inga externa tjänster.
- **Källor/Plats:**
  - `MessageControllerTest`, `MessageControllerHttpErrorsTest`, `MessageControllerValidationTest`,
  - `MessageProducerTest`, `MessageProducerErrorTest`
- **Körning:** Ingår i `mvn test` / `mvn verify` (Surefire).
- **Artefakter:** Täcks av JaCoCo-rapport i CI.
- **Felsökning:**
  - Vid 400 utan korrekt `Content-Type`: kontrollera att `ValidationErrorAdvice` sätter **`MediaType.APPLICATION_PROBLEM_JSON`**.
  - Vid 406/negotiation-problem: verifiera `produces` på controller/handlers (t.ex. `text/plain` för 200).
  - Om `errors[]` saknar fältet `message`: säkerställ att valideringen binder till rätt fältnamn.

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
- **Omfattning:**
  - `contracts/send/valid_message.groovy` → **200** + `text/plain`. 
  - **Negativa kontrakt (400 + `application/problem+json`, RFC 7807):**
    - `contracts/send/blank_message.groovy`. 
    - `contracts/send/invalid_chars_message.groovy`. 
    - `contracts/send/too_long_message.groovy`. 
      Varje felkontrakt verifierar att `errors[0].field == "message"` och att meddelandet är lokaliserat (sv-SE).
- **Miljö:** MVC-slice via `BaseContractTest` — inga externa tjänster krävs.
- **Källor/Plats:** `src/test/resources/contracts/send/*`
- **Körning:** Ingår automatiskt i `mvn verify` (SCC **generateTests** + **convert**) och körs sedan som vanliga JUnit-tester.
- **Artefakter:** Genererade JUnit-tester samt **`stubs.jar`** för konsumenter.
- **Felsökning:** Vid mismatch (status/`Content-Type`/payload), kontrollera kontrakten, att `BaseContractTest` mappar rätt controller, och att `ValidationErrorAdvice` returnerar `application/problem+json` vid 400.

## Sammanfattning: Körning & Artefakter

- **Lokal/CI:** `mvn verify` kör alla JUnit, Cucumber och **SCC**-tester.
- **Artefakter (CI):** JaCoCo-rapport, JavaDoc (**endast main**) och **stubs.jar** från SCC.
- **Coverage-badge:** CI uppdaterar badge **coverage** (JaCoCo) på `test`; den följer med i PR → `main`.

**Artefakter (CI/CD):** se [docs/ARTIFACTS.md](docs/ARTIFACTS.md).

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