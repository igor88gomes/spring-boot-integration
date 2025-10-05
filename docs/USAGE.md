# Användarguide

Denna guide visar hur man kan starta och testa applikationen i containermiljö med Docker Compose. Syftet är att snabbt kunna köra 
hela flödet end-to-end med ActiveMQ, PostgreSQL och applikationen.

## Förutsättningar

Innan du börjar, se till att följande är installerat på systemet:

- **Docker** och **Docker Compose**  
  *eller*
- **Podman** och **Podman Compose**

Instruktionerna nedan använder **Docker** som standard.  
Om du använder **Podman**, ersätt helt enkelt `docker` med `podman` och  
`docker compose` med `podman-compose`.

> **Obs:** Exemplen är anpassade för Linux/Unix. I andra miljöer kan vissa kommandon behöva justeras.

---

## Snabbstart

### 1) Klona projektet

```bash
git clone https://github.com/igor88gomes/spring-boot-integration.git
cd spring-boot-integration
```

### 2) Kopiera mallfilen `.env.example` till en lokal `.env` (obligatoriskt för att starta stacken) 

```bash
cp .env.example .env
```

> Rekommenderat: fyll i skarpa värden i .env för ökad säkerhet (ignoreras av Git). I övrigt räcker kopieringen från `.env.example` till `.env`

### 3) Bygg och starta stacken (app + ActiveMQ + PostgreSQL)

```bash
docker compose up --build -d
```

### 4) Kontrollera körande containrar

```bash
docker ps
```

### 5) Stoppa och rensa nätverk/containers

```bash
docker compose down
```

---

## Testa API:erna

Du kan testa API:erna med **curl** (CLI) eller **Postman** (GUI).  
GET-endpoints kan även öppnas direkt i webbläsaren.

Tjänsterna är tillgängliga via `http://localhost:PORT` om portarna är exponerade.

### Med curl (CLI)

```bash
# Skicka meddelande
curl -X POST "http://localhost:8080/api/send?message=HejTest"

# Hämta alla meddelanden
curl "http://localhost:8080/api/all"

# Hälsa
curl "http://localhost:8080/actuator/health"
```

**Tips:** Verktyget **jq** kan installeras för att formatera och färgmarkera JSON-data direkt i terminalen.  
Det gör både API-svar och loggfiler enklare att läsa, men är helt valfritt.

**Exempelanvändning med API-svar:**

 ```bash
curl http://localhost:8080/actuator/health | jq
```
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 1081101176832,
        "free": 1016195850240,
        "threshold": 10485760,
        "path": "/app/.",
        "exists": true
      }
    },
    "jms": {
      "status": "UP",
      "details": {
        "provider": "ActiveMQ"
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}

```
**Obs: validering:** Följande anrop ska ge **400**:
```bash
curl -i -X POST "http://localhost:8080/api/send?message=%20%20%20"  # URL-enkodat whitespace
# alt:
curl -i -X POST http://localhost:8080/api/send -d "message=   "     # form body
```

### Med Postman (GUI)

1. Öppna Postman.
2. Anropa:
    - **GET** `http://localhost:8080/actuator/health`
    - **GET** `http://localhost:8080/actuator/info`
    - **POST** `http://localhost:8080/api/send?message=HejTest`
    - **GET** `http://localhost:8080/api/all`

<p align="center">
  <img src="images/GET_API_ALL.png"
       alt="Postman – Exempel på GET /api/all"
       width="900">
</p>
<p align="center"><em><strong>Bild 3.</strong> Exempel på GET <code>/api/all</code> i Postman som visar alla lagrade meddelanden.</em></p>

> Tips: Skapa en liten **collection** i Postman med ovan fyra requests för snabb regressionstest.

## Loggar och spårbarhet

Applikationen loggar i **JSON-format** (Logback + MDC) direkt till **stdout**.  
I container-miljö läses loggar via `docker logs`.

### Visa loggar Exempel

**Som JSON (med jq):**
```bash
docker logs integration-app 2>&1 | jq -R 'fromjson? | select(. != null)'

```
**Råa loggar (utan jq):**
```bash
docker logs integration-app
```

### Verifiera end-to-end-korrelation i loggar (samma `messageId`)

**1) Skicka några testmeddelanden**

```bash
curl -X POST "http://localhost:8080/api/send?message=Test-1"
```

**2) Filtrera enbart producentens loggar (timestamp, message, messageId)**

```bash
docker logs integration-app 2>&1 \
| grep -E '^\{' \
| jq -r 'select(.logger_name=="com.igorgomes.integration.MessageProducer")
         | [.["@timestamp"], .message, .messageId] | @tsv'
```

Exempelutdata (30 sep 2025):

```text
2025-09-30T18:47:34.806701896Z  Aktiv kö (konfiguration): test-queue
2025-09-30T18:47:51.358145779Z  Skickar meddelande till kön: Test-1     486846ae-803d-40d7-b469-d912ceb1667a
2025-09-30T18:47:51.539331336Z  Meddelandet skickades framgångsrikt!    486846ae-803d-40d7-b469-d912ceb1667a
2025-09-30T20:49:37.49683169Z   Skickar meddelande till kön: Test-2     0a2af829-05c2-4989-82ab-142e9052668a
2025-09-30T20:49:37.781549651Z  Meddelandet skickades framgångsrikt!    0a2af829-05c2-4989-82ab-142e9052668a
2025-09-30T20:49:46.598262695Z  Skickar meddelande till kön: Test-3     3df5b8dd-fce8-44f3-a551-19accf5644a9
2025-09-30T20:49:46.607811895Z  Meddelandet skickades framgångsrikt!    3df5b8dd-fce8-44f3-a551-19accf5644a9
```

**3) Filtrera enbart konsumentens loggar (timestamp, message, messageId)**

```bash
docker logs integration-app 2>&1 \
| grep -E '^\{' \
| jq -r 'select(.logger_name=="com.igorgomes.integration.MessageConsumer")
         | [.["@timestamp"], .message, .messageId] | @tsv'
```

Exempelutdata (30 sep 2025):

```text
2025-09-30T18:47:51.605472875Z  Meddelande mottaget från kön: Test-1    486846ae-803d-40d7-b469-d912ceb1667a
2025-09-30T18:47:52.034032618Z  Meddelande sparat i databasen!          486846ae-803d-40d7-b469-d912ceb1667a
2025-09-30T20:49:37.801860444Z  Meddelande mottaget från kön: Test-2    0a2af829-05c2-4989-82ab-142e9052668a
2025-09-30T20:49:38.318218772Z  Meddelande sparat i databasen!          0a2af829-05c2-4989-82ab-142e9052668a
2025-09-30T20:49:46.603613308Z  Meddelande mottaget från kön: Test-3    3df5b8dd-fce8-44f3-a551-19accf5644a9
2025-09-30T20:49:46.668624393Z  Meddelande sparat i databasen!          3df5b8dd-fce8-44f3-a551-19accf5644a9
```

## Databasåtkomst (PostgreSQL)

Öppna en psql-session i Postgrescontainern:

```bash
docker exec -it postgres psql -U integration -d integrationdb
```
Kör exempel-fråga:

```sql
SELECT * FROM message_entity;
```
Avsluta: 

```sql
\q
```