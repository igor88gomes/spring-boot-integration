# Användarguide

## Förutsättningar

Det antas att **Docker** *eller* **Podman** redan är installerat. Instruktionerna visar **Docker** som standard,
men samma steg fungerar med **Podman** – ersätt bara `docker` med `podman` (och `docker compose` med `podman-compose`).

> Exempel:

 ```bash
docker compose up --build
# eller
podman-compose up --build
 ```

Du kan testa API:erna med **curl** (CLI) eller **Postman** (GUI). Oavsett om du kör i **Linux**, i en virtuell maskin eller via WSL i 
**Windows** kan du nå tjänsterna via http://localhost:PORT om portarna är exponerade.
---

## Snabbstart

### 1) Klona projektet

```bash
git clone https://github.com/IgorGomes01/spring-boot-integration.git
cd spring-boot-integration
```

### 2) Bygg och starta stacken (app + ActiveMQ + PostgreSQL)

```bash
docker compose up --build
# alt: podman-compose up --build
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

## Testa API:er

### Med curl (CLI)

```bash
# Skicka meddelande
curl -X POST "http://localhost:8080/api/send?message=HejTest"

# Hämta alla meddelanden
curl "http://localhost:8080/api/all"

# Hälsa
curl "http://localhost:8080/actuator/health"
```

**Tips:** Installera verktyget **jq** för att formatera och färgmarkera JSON-data direkt i terminalen, vilket gör både API-svar och loggfiler enklare att läsa.

**Installation (Debian/Ubuntu/WSL):**

```bash
 sudo apt update && sudo apt install jq -y
 ```

**Exempelanvändning med API-svar:**

 ```bash
curl http://localhost:8080/actuator/health | jq
```

boot-integration$ curl "http://localhost:8080/actuator/health" | jq
% Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
Dload  Upload   Total   Spent    Left  Speed
100   330    0   330    0     0   6430      0 --:--:-- --:--:-- --:--:--  6470
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

### Med Postman (GUI)

1. Öppna Postman.
2. Anropa:
    - **GET** `http://localhost:8080/actuator/health`
    - **POST** `http://localhost:8080/api/send?message=HejTest`
    - **GET** `http://localhost:8080/api/all`

<p align="center">
  <img src="docs/images/GET-API-ALL.png"
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

## Felsökning

- Port **8080** upptagen → stäng processen eller ändra port.
- ActiveMQ-konsol: `http://localhost:8161` (user: `admin`, pass: `admin`).
- Databasanslutning: verifiera att containern **postgres** körs och att strängen i `application.properties` pekar på `jdbc:postgresql://postgres:5432/integrationdb`.