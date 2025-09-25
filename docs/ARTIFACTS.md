## Artefakter (CI/CD)

Alla artifacts hämtas via **Actions** i GitHub:

1. Gå till **Actions** och öppna körningen för ditt commit.
2. Under **Artifacts**, klicka på namnet och ladda ner ZIP:en.

> Obs: Alla artifacts lagras i **14 dagar** och ingår inte i Docker-image (ignoreras i `.gitignore`).

### CI-artifacts

- **JaCoCo-rapport** (`main` och `test`)  
  → Öppna `index.html` i ZIP:en för täckningen.

- **JavaDoc** (endast på `main`)  
  → Öppna `index.html` i `target/site/apidocs/`.

- **Stubs** (endast `main`)  
  → Använd innehållet som **WireMock-stubs** för konsumenttester.

- **Testrapporter (Surefire/Failsafe)** (**endast vid fel**, **14 dagar**)  
  → `target/surefire-reports/**`, `target/failsafe-reports/**`, `*-jvmRun*.dump`, `*.dumpstream` laddas upp automatiskt om bygget misslyckas (för felsökning i Actions).

### CD-artifacts

- **SBOM (CycloneDX)**  
  → Öppna `sbom.cdx.json` i ZIP:en.

#### Visa säkerhetsfynd (Code scanning)

1. Öppna **Security → Code scanning** i GitHub.
2. Filtrera på verktyg: **Trivy** och **Gitleaks**.
    - **Trivy:** **CRITICAL** blockerar i *quality gate*; **HIGH** rapporteras som **SARIF**.
    - **Gitleaks:** **SARIF** laddas upp vid **push till `main`** och **schemalagd körning**; i **PR** körs snabb skanning som gate (utan SARIF).

> Tips: Du hittar även digesten och multi-arch-info via `docker buildx imagetools inspect <image>:<tag>` om du vill dubbelkolla promotionen.

---

> För reproduktion av pipeline i egen miljö krävs egna credentials och registry-konfiguration.