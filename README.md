
# Excel Connector (Zeenea) — Candidate Implementation (Local-friendly)

This repository is a fully self-contained scaffold that implements the **Excel Connector** requirements (L1, L2, L3).
It includes a lightweight adapter of the Zeenea SDK interfaces so you can develop and test locally without the real SDK.

## What is included
- Java 17 project (Gradle Kotlin DSL)
- Source implementation for:
  - L1 Inventory & Schema extraction
  - L2 Profiling & Enrichment
  - L3 Field-level Lineage (basic formula parsing)
- Unit tests (JUnit 5)
- `LocalRunner` to run locally (no Docker required)
- `Dockerfile` (for later submission if needed)
- Example `test-data/` folder (place your `sales.xlsx`, `report.xlsx`, `complex.xlsx` here)

## Build & Run (local)
1. Build:
   ```
   ./gradlew clean shadowJar
   ```
2. Run locally (use bundled runner):
   ```
   java -jar build/libs/excel-connector-1.0.0.jar ./test-data
   ```
   Or:
   ```
   ./gradlew runLocal
   ```

## Tests
```
./gradlew test
```

## Notes
- This scaffold contains a small `com.zeenea.sdk` adapter that mimics minimal SDK behavior the connector needs.
- Replace adapter classes with the real Zeenea SDK interfaces when integrating for the final submission.

