# Tester

## Översikt
Testerna täcker **Controller**, **Producer**, **Consumer**, **Repository** och **HTTP-kontrakt**. Fokus: **indata-validering**, **korrelation (MDC + JMS)** och **stabila HTTP-svar**.

## Testtyper & ramverk
- **Enhet/web-slice**: JUnit 5, Mockito, Spring Boot Test, **MockMvc**.
- **Kontraktstester (SCC)**: Spring Cloud Contract Verifier (genererar tester från kontrakt) som körs mot **MVC-slice** via `BaseContractTest` (`@WebMvcTest` + `@MockBean` på `MessageProducer`) — inga externa brokers/databaser krävs.
- **Persistens (H2)**: JPA-tester i profil `test`.

## Vad som verifieras (huvudpunkter)
- **Controller**
    - `POST /api/send`: **400** vid tomt/blankt `message`; **200** vid giltig input med **`Content-Type: text/plain`**.
    - `GET /api/all`: **200** och **`application/json`**; returnerar `[]` när repo är tomt.
- **Producer (JMS)**
    - Sätter `messageId` som **JMS-header** när **MDC** finns; sätter **inte** headern när MDC saknas.
    - Felväg: loggar fel från `JmsTemplate` men **propagerar inte undantaget**.
- **Consumer (JMS)**
    - Läser `messageId` från headern, persisterar meddelandet och loggar samma korrelations-ID (E2E-spårbarhet).
- **Repository (JPA/H2)**
    - Baspersistens: ID genereras, fält bevaras, `receivedAt` ej `null`.
    - Validering: blankt `content` avvisas (Bean Validation/DB-kontrakt).

## Kontrakt (SCC)
- `invalid_message.groovy`: `POST /api/send` med blankt `message` ⇒ **400**.
- `valid_message.groovy`: `POST /api/send` med giltigt `message` ⇒ **200** + `Content-Type: text/plain`.

## Körning
- **Lokal/CI**: `mvn verify` (profilsättning `test` sker via Surefire).
- Artefakter i CI: JaCoCo-rapport, JavaDoc (main), samt **stubs.jar** från SCC när genererad.

## BDD/E2E (Cucumber)

- **Syfte:** Verifierar kedjan end-to-end: HTTP → JMS-kö → Consumer → JPA-persistens, inkl. loggkorrelation via `messageId` (MDC + JMS-header).
- **Plats:**
  - Scenarier: `src/test/resources/features/message_integration.feature`
  - Steg/konfiguration: `src/test/java/com/igorgomes/integration/bdd/*`
- **Vad som testas (exempel):**
  - Giltigt meddelande ⇒ `/api/send` svarar **200**, meddelandet skickas till kön och sparas i DB.
  - Blankt meddelande ⇒ `/api/send` svarar **400**, **ingen** ny rad i DB.

## Körning
- **Alla tester**: `mvn test`
- **Endast E2E**: `mvn -Dtest=CucumberTest test`
  - (Valfritt) Taggfiltrering om du märker scenarier: `-Dcucumber.filter.tags="@smoke"` eller `@regression`
- **Testmiljö:**
  - Inbäddad ActiveMQ via testprofil: `spring.activemq.broker-url=vm://embedded?broker.persistent=false&broker.useShutdownHook=false`
  - **Awaitility** används för robust väntan på asynkrona effekter (JMS → DB).
  - In-memory H2 för persistens i profil `test`.
- **Integration med CI/Maven:**
  - Körs under **Surefire** tillsammans med övriga JUnit 5-tester (ingen extern broker/databas krävs).
- **Felsökning (kort):**
  - Om JMS-anslutning misslyckas, kontrollera att testprofilen är aktiv och att `broker-url` är satt som ovan.

## Notering
Detta dokument är medvetet **kort**: detaljerade testfall finns i koden och i genererade SCC-tester. Syftet här är att ge en snabb översikt av **vad** som testas och **varför** (robusthet, kontrakt och korrelation).