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

## Notering
Detta dokument är medvetet **kort**: detaljerade testfall finns i koden och i genererade SCC-tester. Syftet här är att ge en snabb översikt av **vad** som testas och **varför** (robusthet, kontrakt och korrelation).