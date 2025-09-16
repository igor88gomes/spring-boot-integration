# Användarguide

## Förutsättningar

Innan du börjar, se till att följande är installerat på systemet:

- **Docker** och **Docker Compose**  
  *eller*
- **Podman** och **Podman Compose**

Instruktionerna nedan använder **Docker** som standard.  
Om du använder **Podman**, ersätt helt enkelt `docker` med `podman` och  
`docker compose` med `podman-compose`.

> **Obs!** Vill du köra tester lokalt utan Docker?
> Se [docs/TESTS.md](docs/TESTS.md#kör-tester-lokalt).

---

## Snabbstart

### 1) Klona projektet

```bash
git clone https://github.com/igor88gomes/spring-boot-integration.git
cd spring-boot-integration
```
### 2) Bygg och starta stacken (app + ActiveMQ + PostgreSQL)

```bash
docker compose up --build -d
# alt: podman-compose up --build -d
```

### 3) Kontrollera körande containrar

```bash
docker ps
# alt: podman ps
```

### 4) Stoppa och rensa nätverk/containers

```bash
docker compose down
# alt: podman-compose down
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

Applikationen loggar i **JSON-format** (Logback + Logstash Encoder) till **konsol** och roterande **fil** i `logs/`.

- Aktiv fil: `logs/app.log` (dagens loggar)
- Rotation: `logs/app.YYYY-MM-DD.log` (daglig), historik 7 dagar

> På den första logghändelsen efter midnatt roteras gårdagens logg till `logs/app.YYYY-MM-DD.log`

**Exempelanvändning med loggfiler 

> Loggarna är i JSON-format och kan läsas direkt med cat/tail eller formateras med jq.

 ```bash
jq . logs/app.log
```

Visa loggar från applikationscontainern:
```bash
docker logs integration-app
# alt: podman logs integration-app
```

### Verifiera end-to-end-korrelation i loggar (samma `messageId`)

**1) Skicka några testmeddelanden**

```bash
curl -X POST "http://localhost:8080/api/send?message=Test-1"
```
> Tips: Exempel använder **jq** för enklare filtrering, men du kan lika gärna läsa `logs/app.log` direkt med `cat` eller `tail`.

**2) Visa producentens rader (tid, text, messageId)**

```bash
tail -n 50 logs/app.log | jq -r '
  select(.logger_name=="com.igorgomes.integration.MessageProducer")
  | [.["@timestamp"], .message, .messageId] | @tsv
'
```

Exempelutdata (1 sep 2025):

```text
2025-09-01T00:49:26.818886199+02:00     Aktiv kö (konfiguration): test-queue
2025-09-01T00:51:54.187148005+02:00     Skickar meddelande till kön: Test-1     50de7a3b-b8e0-4fe6-8497-3663012fe7e5
2025-09-01T00:51:54.412839282+02:00     Meddelandet skickades framgångsrikt!    50de7a3b-b8e0-4fe6-8497-3663012fe7e5
2025-09-01T00:52:02.644401358+02:00     Skickar meddelande till kön: Test-2     244fb974-76a0-422e-9dde-248e1ea536cb
2025-09-01T00:52:02.669139559+02:00     Meddelandet skickades framgångsrikt!    244fb974-76a0-422e-9dde-248e1ea536cb
2025-09-01T00:52:09.308283094+02:00     Skickar meddelande till kön: Test-3     ee36e1c6-bbce-4e7e-b4eb-3efa31a8dfec
2025-09-01T00:52:09.325314642+02:00     Meddelandet skickades framgångsrikt!    ee36e1c6-bbce-4e7e-b4eb-3efa31a8dfec
```

**3) Visa konsumentens rader (tid, text, messageId)**

```bash
tail -n 50 logs/app.log | jq -r '
  select(.logger_name=="com.igorgomes.integration.MessageConsumer")
  | [.["@timestamp"], .message, .messageId] | @tsv
'
```

Exempelutdata (1 sep 2025):

```text
2025-09-01T00:51:54.4817882+02:00       Meddelande mottaget från kön: Test-1    50de7a3b-b8e0-4fe6-8497-3663012fe7e5
2025-09-01T00:51:54.775159808+02:00     Meddelande sparat i databasen!          50de7a3b-b8e0-4fe6-8497-3663012fe7e5
2025-09-01T00:52:02.650607228+02:00     Meddelande mottaget från kön: Test-2    244fb974-76a0-422e-9dde-248e1ea536cb
2025-09-01T00:52:02.667178526+02:00     Meddelande sparat i databasen!          244fb974-76a0-422e-9dde-248e1ea536cb
2025-09-01T00:52:09.312866768+02:00     Meddelande mottaget från kön: Test-3    ee36e1c6-bbce-4e7e-b4eb-3efa31a8dfec
2025-09-01T00:52:09.323411022+02:00     Meddelande sparat i databasen!          ee36e1c6-bbce-4e7e-b4eb-3efa31a8dfec
```

## Databasåtkomst (PostgreSQL)

Öppna en psql-session i Postgrescontainern:

```bash
docker exec -it postgres psql -U integration -d integrationdb
# alt: podman exec -it postgres psql -U integration -d integrationdb
```
Kör exempel-fråga:

```sql
SELECT * FROM message_entity;
```
Avsluta: 

```sql
\q
```

## Felsökning

- Port **8080** upptagen → stäng processen eller ändra port.
- ActiveMQ-konsol: `http://localhost:8161` (user: `admin`, pass: `admin`).
- Databasanslutning: verifiera att containern **postgres** körs och att strängen i `application.properties` pekar på `jdbc:postgresql://postgres:5432/integrationdb`.

---

## Artefakter (CI/CD)

Alla artifacts hämtas via **Actions** i GitHub:

1. Gå till **Actions** och öppna körningen för ditt commit.
2. Under **Artifacts**, klicka på namnet och ladda ner ZIP:en.

> Obs: Alla artifacts lagras i **14 dagar** och ingår inte i Docker-image (ignoreras i `.gitignore`).

### CI-artifacts

- **JaCoCo-rapport** (`main` och `test`)  
  → Öppna `index.html` i ZIP:en för täckningen.

- **JavaDoc** (endast `main`)  
  → Öppna `index.html` i `target/site/apidocs/`.

- **Stubs** (endast `main`)  
  → Använd innehållet som **WireMock-stubs** för konsumenttester.

### CD-artifacts

- **SBOM (CycloneDX)**  
  → Öppna `sbom.cdx.json` i ZIP:en.

#### Visa säkerhetsfynd (Code scanning)
1. Öppna **Security → Code scanning** i GitHub.
2. Filtrera på verktyg: **Trivy**.
3. **CRITICAL** blockerar i **Trivy quality gate**; **HIGH** rapporteras här som **SARIF**.

> Tips: Du hittar även digesten och multi-arch-info via `docker buildx imagetools inspect <image>:<tag>` om du vill dubbelkolla promotionen.

---

## Miljövariabler (valfritt)

`.env` är **valfritt**. Om filen saknas körs stacken med interna dev-standarder.  
Vill du anpassa konfigurationen, skapa en lokal `.env`.

## Miljövariabler

Skapa en lokal **`.env`** för din miljö. Se **`.env.example`** som mall.  
Inga standardvärden kontrolleras in i koden.

Nycklar som stöds:
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- `BROKER_URL`, `BROKER_USER`, `BROKER_PASS`
- `APP_QUEUE_NAME`

```

> **Obs:** Pipelines och artifacts är fullt fungerande i detta repo och kan granskas direkt via **Actions**-fliken.  
> Vid automatiska tester i CI används en in-memory **H2**-databas istället för PostgreSQL för snabbare körning och enklare underhåll.  
> Att reproducera pipeline i en egen miljö kräver uppdatering av konfiguration och credentials i GitHub Actions samt peka om publiceringen till ett eget **Docker Hub**-konto (container registry). Detta är utanför projektets huvudsyfte.
