# CI/CD-pipeline
Här beskrivs hur leveranskedjan är uppbyggd från bygg och test till säker skanning och publicering av Docker-images.  
Målet är att visa en **praktisk och spårbar DevSecOps-implementering** på GitHub Actions med fokus på **kvalitet, säkerhet och reproducerbarhet**.

> Pipeline-flödet är utformat för stabilitet och insyn, där varje steg bidrar till tillförlitliga releaser.

## Teknikstack (översikt)
**CI/CD-plattform:** GitHub Actions (ubuntu-latest), Docker Buildx, GHCR (candidate, privat), Docker Hub (publik) 
**Säkerhetsverktyg:** Trivy (quality gate + SARIF), Gitleaks (schema + SARIF), Dependabot   
**Kvalitet & dokumentation:** JaCoCo (coverage + badge), JavaDoc, Spring Cloud Contract stubs   
**Artefakt/insyn:** Actions Artifacts (retention 14 dagar), CycloneDX SBOM   
**Byggmiljö:** Maven, JDK 17, multi-arch (amd64/arm64), OCI-labels/annotations, `imagetools inspect`, retry/backoff + timeouts 

> **Image-policy:** Lokalt Compose återanvänder **samma digest** som byggs i CD (GHCR → promotion till Docker Hub), vilket ger konsekvent körning mellan CI/CD och lokalt.

## Branch-strategi (kort)
- Dagligt arbete i `test`; PR → `main` med **squash-merge**.
- Obligatoriska checks på PR: **CI** + **Secret Scan (Gitleaks)**.
- Release: **push till `main`** triggar CD och publicering till Docker Hub.
- **Regler (`main`):** **squash-only**, **linear history**, **blockera force pushes**, **kräver uppdaterad branch före merge**, och **obligatoriska status checks:** `build` + `gitleaks`.

---

## Bygg / CI & dokumentation (GitHub Actions)

- **Workflow:** `.github/workflows/ci.yaml`
- **Trigger:** push till `main` och `test`; PR till `main`

- **Steg:**
    - **Steg 1 – Checkout & JDK 17:** checka ut källkod och konfigurera Java.
    - **Steg 2 – Bygg & tester (Maven/H2):** kör `mvn verify` med H2 för isolerade tester.
    - **Steg 3 – Kodtäckning (JaCoCo + badge):** generera rapport, ladda upp som artefakt och uppdatera coverage-badge.
    - **Steg 4 – JavaDoc (endast på `main`):** generera och ladda upp som artefakt.
    - **Steg 5 – Stubs (SCC), (endast `main`):** stubs **genereras av Spring Cloud Contract under `mvn verify`** och CI **laddar upp `*-stubs.jar` som artefakt** (om finns).
    - **Steg 6 – Felsökning (endast vid fel):** ladda upp testrapporter (**Surefire/Failsafe**) – `target/surefire-reports/**`, `target/failsafe-reports/**` + dumpfiler för att förenkla felsökning i Actions.

- **Artefakter:**
    - `jacoco-report` (main/test, **retention 14 dagar**)
    - `javadocs` (endast `main`, **14 dagar**)
    - `stubs` (endast `main`, **14 dagar**) – genererade av Spring Cloud Contract för konsumenttester
    - `surefire-reports` (**endast vid fel**, **14 dagar**) – **Surefire/Failsafe**-rapporter och dumpfiler för felsökning

**Artefakter (CI/CD):** För mer information, se [docs/ARTIFACTS.md](docs/ARTIFACTS.md).

## Secret scanning (Gitleaks)

- **Workflow (fristående):** `secret-scan.yaml` körs separat från CI.
- **Schema (UTC):** måndagar **03:00 UTC** (full historikskanning + SARIF).
- **Policy:** PR blockeras vid fynd (exit ≠ 0).
- **SARIF:** genereras och laddas upp **vid push till `main`** och vid den schemalagda körningen till *Security → Code scanning*.
- **Beteende:**
    - PR = snabb skanning av ändringar (`--no-git`)
    - Push/schedule = full historik (checkout `fetch-depth: 0`) + SARIF
- **Konfig:** `.gitleaks.toml` (ignorerar `.env.example`, `application-test.properties`; placeholders: `changeme`, `to-be-set`, `example`, `dummy`).

## Distribution (CD) – Docker-image (GitHub Actions)

- **Workflow:** `.github/workflows/docker-publish.yaml`
- **Trigger:** `push` till `main` *(ej PR)*

- **Publiceringsflöde:**
    1. **Build (candidate, privat):** Buildx bygger **multi-arch** (`linux/amd64, linux/arm64`) och pushar en **candidate-image** till **GHCR** (privat) med rika **OCI-etiketter** (taggar baserade på commit-SHA).
    2. **Trivy – quality gate:** container-skanning av kandidaten. **CRITICAL** blockerar; **HIGH** rapporteras som **SARIF** till *Security → Code scanning*.
    3. **Preflight (manifest):** pipeline gör `docker buildx imagetools inspect` på **exakt digest** i GHCR för att säkerställa att manifestet finns och är läsbart.
    4. **Promotion (reproducerbar):** **samma digest** **promoteras** till Docker Hub som `igor88gomes/spring-boot-integration:latest`. Steget har **retry med felklassificering** (nätverks/502–504/timeouts → backoff) och **steg-timeout**.
    5. **SBOM (insyn):** **CycloneDX SBOM** (`sbom.cdx.json`) laddas upp som **Actions-artefakt** (retention 14 dagar).
    6. **Rensning:** temporära Docker Desktop-buildfiler (`*.dockerbuild`) tas bort för renare loggar.

> **Nyheter i flödet:** Preflight-kontroll av GHCR-manifest, **retry med felklassificering** vid promotion och **timeouts** per jobb/steg för att undvika hängningar.

## Säkerhet & kontroll

- **Concurrency:** `concurrency.group=docker-publish-${{ github.ref }}` och `cancel-in-progress: true` förhindrar parallella dubbletter.
- **Retention (GHCR):** candidate-images märks bl.a. med  
  `org.opencontainers.image.ref.name=candidate` och `ghcr.io/retention-days=14` → autosanering efter 14 dagar.
- **Behörigheter:** workflow ger **packages: write**, **security-events: write**, **contents: read** för att kunna publicera images och skicka SARIF.
- **Timeouts:** Jobbet har övergripande timeout (**15 min**) och promotion-steget egen timeout (**6 min**) för att stoppa hängningar tidigt.
- **Felklassificerad retry:** Promotion försöker om **transienta** fel (t.ex. 502/503/504/timeout/EOF) med **exponentiell backoff**; **icke-återställbara** fel (auth/permissions/ogiltig referens) failar direkt.

- **Resultat:**

    - **GHCR (privat):** *candidate* (per-commit) – för skanning och spårbarhet.
    - **Docker Hub (publik):** `:latest` – **multi-arch** och redo för `docker/podman-compose`.

## Säkerhet & Underhåll

> **Policy:** Se [SECURITY.md](SECURITY.md) för hur findings hanteras (ansvarsfull rapportering, scope och SLA).

## Beroendehantering (Dependabot)

- **Schema (UTC) & flöde**
    - **Maven (app):** **måndagar 01:00 UTC** → PR till `test`  
      – *en körning, två grupper*:
        - `maven-security` – **endast säkerhetsuppdateringar**
        - `maven-patch-minor` – **patch/minor för direkta beroenden**
    - **GitHub Actions (CI):** **måndagar 01:15 UTC** → PR till `test`  
      – versionbumps **grupperas** (patch/minor) och säkerhetsuppdateringar kommer när advisories finns.
    - **Tidszon:** alla tider är **UTC** för konsekventa körningar året runt.

- **Policy**
    - PR-gruppering och **auto-rebase**; **target branch:** `test`.
    - **Majors** (t.ex. `spring-boot`) ignoreras här och planeras separat.
    - **Branchskydd:** PR kräver **grön CI** (build + tester) och **Gitleaks** innan merge.

## Kodskanning (SARIF)

Alla säkerhetsresultat från både **Gitleaks** (hemligheter) och **Trivy** (image-skanning) laddas upp som **SARIF** till *Security → Code scanning* för central analys och historik.

> För reproduktion av pipeline i egen miljö krävs egna credentials och registry-konfiguration.
