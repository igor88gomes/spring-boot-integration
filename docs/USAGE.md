# Användarguide

## Förutsättningar

Innan du börjar, se till att följande är installerat på systemet:

- **Docker** och **Docker Compose**  
  *eller*
- **Podman** och **Podman Compose**

Instruktionerna nedan använder **Docker** som standard.  
Om du använder **Podman**, ersätt helt enkelt `docker` med `podman` och  
`docker compose` med `podman-compose`.

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
[
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
]

```
**Obs – validering:** Följande anrop ska ge **400**:
```bash
curl -i -X POST "http://localhost:8080/api/send?message=%20%20%20"  # URL-enkodat whitespace
# alt:
curl -i -X POST http://localhost:8080/api/send -d "message=   "     # form body
```

### Med Postman (GUI)

1. Öppna Postman.
2. Anropa:
    - **GET** `http://localhost:8080/actuator/health`
    - **POST** `http://localhost:8080/api/send?message=HejTest`
    - **GET** `http://localhost:8080/api/all`

<p align="center">
  <img src="images/GET_API_ALL.png"
       alt="Postman – Exempel på GET /api/all"
       width="900">
</p>
<p align="center"><em><strong>Bild 3.</strong> Exempel på GET <code>/api/all</code> i Postman som visar alla lagrade meddelanden.</em></p>

> Tips: Skapa en liten **collection** i Postman med ovan tre requests för snabb regressionstest.

## Loggar och spårbarhet

Applikationen loggar i **JSON-format** (Logback + Logstash Encoder) till **konsol** och roterande **fil** i `logs/`.

- Aktiv fil: `logs/app.log` (dagens loggar)
- Rotation: `logs/app.YYYY-MM-DD.log` (daglig), historik 7 dagar

> På den första logghändelsen efter midnatt roteras gårdagens logg till `logs/app.YYYY-MM-DD.log`

**Exempelanvändning med loggfiler samt återanvändning av `jq`:**

 ```bash
jq . logs/app.log
```

Visa loggar från applikationscontainern:
```bash
docker logs integration-app
# alt: podman logs integration-app
```

### Verifiera end-to-end-korrelation i loggar (samma `messageId`)

> Förutsättning: du kör med loggvolymen `./logs:/app/logs` och har `jq` installerat.

**1) Skicka några testmeddelanden**
```bash
curl -X POST "http://localhost:8080/api/send?message=Test-1"
curl -X POST "http://localhost:8080/api/send?message=Test-2"
curl -X POST "http://localhost:8080/api/send?message=Test-3"
```
**2) Visa producentens rader (tid, text, messageId)**

```bash
tail -n 50 logs/app.log | jq -r '
  select(.logger_name=="com.igorgomes.integration.MessageProducer")
  | [.["@timestamp"], .message, .messageId] | @tsv
'
```
Exempelutdata (28 aug 2025):
  
2025-08-28T23:28:40.151588744+02:00    Skickar meddelande till kön: Test-1    b6ac63b2-48ba-4302-8157-a66bca80ef63
2025-08-28T23:28:40.323012015+02:00    Meddelandet skickades framgångsrikt!    b6ac63b2-48ba-4302-8157-a66bca80ef63
2025-08-28T23:28:50.228941906+02:00    Skickar meddelande till kön: Test-2    25da904c-b786-4082-922a-9d2aa8b699be
2025-08-28T23:28:50.252255905+02:00    Meddelandet skickades framgångsrikt!    25da904c-b786-4082-922a-9d2aa8b699be
2025-08-28T23:29:09.858531378+02:00    Skickar meddelande till kön: Test-3    b9b08a24-43c0-4061-867a-f7cf50dad76d
2025-08-28T23:29:09.871739857+02:00    Meddelandet skickades framgångsrikt!    b9b08a24-43c0-4061-867a-f7cf50dad76d

**3) Visa konsumentens rader (tid, text, messageId)**

```bash
tail -n 50 logs/app.log | jq -r '
  select(.logger_name=="com.igorgomes.integration.MessageConsumer")
  | [.["@timestamp"], .message, .messageId] | @tsv
'
```

Exempelutdata (28 aug 2025):  

2025-08-28T23:28:40.330647854+02:00    Meddelande mottaget från kön: Test-1    b6ac63b2-48ba-4302-8157-a66bca80ef63
2025-08-28T23:28:40.56074628+02:00     Meddelande sparat i databasen!         b6ac63b2-48ba-4302-8157-a66bca80ef63
2025-08-28T23:28:50.246452919+02:00    Meddelande mottaget från kön: Test-2    25da904c-b786-4082-922a-9d2aa8b699be
2025-08-28T23:28:50.255022952+02:00    Meddelande sparat i databasen!         25da904c-b786-4082-922a-9d2aa8b699be
2025-08-28T23:29:09.865085119+02:00    Meddelande mottaget från kön: Test-3    b9b08a24-43c0-4061-867a-f7cf50dad76d
2025-08-28T23:29:09.87307005+02:00     Meddelande sparat i databasen!         b9b08a24-43c0-4061-867a-f7cf50dad76d

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
`\q`
```

## Felsökning

- Port **8080** upptagen → stäng processen eller ändra port.
- ActiveMQ-konsol: `http://localhost:8161` (user: `admin`, pass: `admin`).
- Databasanslutning: verifiera att containern **postgres** körs och att strängen i `application.properties` pekar på `jdbc:postgresql://postgres:5432/integrationdb`.

---

## CI-artifacts

### Hämta JaCoCo-rapport (`main` och `test`)

1. Gå till **Actions** i GitHub-repot och öppna körningen för ditt commit.
2. Under **Artifacts**, klicka på **`jacoco-report`** och ladda ner ZIP:en.
3. Öppna `index.html` i ZIP:en för att se täckningen.

> Obs: Rapporten lagras som artifact i **14 dagar** och ingår inte i Docker-image (ignoreras i `.gitignore`).

### Hämta JavaDoc (endast `main`)

1. Gå till **Actions** i GitHub-repot och öppna körningen för ditt commit på `main`.
2. Under **Artifacts**, klicka på **`javadoc`** och ladda ner ZIP:en.
3. Öppna `index.html` i mappen `target/site/apidocs/` i ZIP:en.

> Obs: JavaDoc lagras som artifact i **14 dagar** och ingår inte i Docker-image (ignoreras i `.gitignore`).
---

> **Obs:** Pipelines och artifacts är fullt fungerande i detta repo och kan granskas direkt via **Actions**-fliken.  
> Vid automatiska tester i CI används en in-memory **H2**-databas istället för PostgreSQL för snabbare körning och enklare underhåll.  
> Att reproducera pipeline i en egen miljö kräver uppdatering av konfiguration och credentials i GitHub Actions samt peka om publiceringen till ett eget **Docker Hub**-konto (container registry). Detta är utanför projektets huvudsyfte.
