# Tester

## Översikt

Enhetstester täcker Controller, Producer, Consumer och Repository. Fokus ligger på kontrakt för korrelation (MDC + JMS) och indata-validering.

## Klasser och kontrakt

**MessageProducerTest**

- *MDC present*: sätter `messageId` som JMS-header.
- *MDC missing*: sätter inte header.
- Lättviktskontroll: anropar `convertAndSend` med `MessagePostProcessor`. :contentReference[oaicite:0]{index=0}

**MessageControllerTest**

- 400 (Bad Request) för tomt/blankt `message`.
- 200 OK för giltigt `message`; delegerar till Producer.
- Sätter korrelations-id i MDC om det saknas och tar bort nyckeln i finally endast om den sattes här.

**MessageConsumerTest**

- Persisterar meddelandet och loggar samma `messageId` som läses från JMS-headern.
- MDC-hanteringen (lägga in/ta bort) verifieras end-to-end via loggar; kan testas separat vid behov.

**MessageRepositoryTest**

- Grundläggande persistens mot H2 i profil `test`.

## Täckning

Kontrakten är avsedda att fånga regressionsfel i korrelationsflödet och indata-validering.
