# Suspicious Activity Monitor (SAM)

Suspicious Activity Monitor is a team project built to detect potentially suspicious financial transactions.
The system screens transactions using configurable rules, checks customers against watchlists, creates alerts, and supports case investigation management.

---

## Contents

- [Sprint 1](#sprint-1)
- [Sprint 2](#sprint-2)
- [Sprint 3](#sprint-3)
- [Project Structure](#project-structure)
- [Environment Setup](#environment-setup)
- [Running the Application](#running-the-application)
- [Running Tests](#running-tests)
- [JaCoCo Coverage Report](#jacoco-coverage-report)
- [API Examples](#api-examples)
- [E/R Diagram](#er-diagram)
- [Team](#team)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.13 |
| ORM | Spring Data JPA / Hibernate |
| Database (prod) | MySQL 8 |
| Database (test) | H2 in-memory |
| Build | Maven |
| Coverage | JaCoCo 0.8.11 |
| API Docs | Springdoc OpenAPI 2.8.16 |
| Utilities | Lombok, Spring Dotenv |

---

## Sprint 1

### Database Schema

Eight tables designed with proper constraints, indexes, check conditions, and foreign keys:

| Table | Purpose |
|---|---|
| `customer` | Customer profiles with KYC status, risk rating, and PEP flag |
| `account` | Bank accounts linked to customers (CURRENT, SAVINGS, TRADING, etc.) |
| `txn` | Financial transactions with USD-normalised amounts for rule evaluation |
| `alert_rule` | Configurable rules with thresholds, lookback windows, and severity |
| `alert` | Raised alerts linking a rule, account, and triggering transaction |
| `investigation` | Case management records linked to alerts |
| `watchlist` | Sanctions and PEP list entries (OFAC, UN, EU, HMT, INTERPOL, etc.) |
| `watchlist_match` | Match records linking transactions to watchlist hits with confidence scores |

### Views

Read-only access to the database is provided through two views:

- **`open_alerts_vw`** — joins alerts with transaction and customer detail for all open and under-review cases
- **`high_risk_accounts_vw`** — surfaces HIGH risk customers with alerts triggered in the last 30 days

### Stored Procedures

All write operations go through stored procedures — no direct SQL from the application:

- **`raise_alert`** — creates a new alert record with a severity-based risk score (CRITICAL → 95, HIGH → 75, MEDIUM → 55, LOW → 35)
- **`screen_transaction`** — iterates active alert rules, calls `raise_alert` for each match, then marks the transaction as SCREENED
- **`match_watchlist`** — fuzzy-matches entity names against watchlist entries (exact = 100, partial = 85) above a configurable threshold

### Shell Scripts

| Script                        | Purpose                                                                                                                                     |
|-------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| `db_create.sh`                | Drops and recreates the database, loads tables, seed data, procedures, and views in dependency order                                        |
| `db_simulate_transactions.sh` | Simulates incoming transactions by inserting additional rows from `data/09_simulate_transactions_data.sql` one by one with a configurable delay |
| `db_dump.sh`                  | Creates a timestamped MySQL dump including routines and triggers                                                                            |
| `db_reload.sh`                | Restores the database from a dump file                                                                                                      |
| `docker_db_init.sh`           | Used to containerize database                                                                                                               |
| `rebuild_indexes.sh`          | Runs `OPTIMIZE` and `ANALYZE` on tables for DBA maintenance                                                                                 |
| `open_cases_report.sh`        | Exports open, under-review, and escalated alerts to CSV                                                                                     |

After running `db_create.sh`, the base seed data is already present in the database.
If you want to simulate new transactions arriving over time, run:

```bash
cd scripts
./db_simulate_transactions.sh
```

The script reads additional transaction statements from `data/09_simulate_transactions_data.sql` and inserts them one by one every `SLEEP_SECONDS` seconds.
This simulates a live stream of incoming transactions instead of loading everything at once.

When the application scheduler is enabled, newly inserted transactions with status `COMPLETED` can then be picked up automatically for screening.
Depending on the screening result, transaction statuses may move to values such as `SCREENED`, `PENDING`, or `BLOCKED`, and matching rules may raise new alerts.

The application also supports an internal open-cases reporting job:

- `POST /api/v1/reports/open-cases` generates the CSV report on demand
- `POST /api/v1/reports/open-cases/email` generates the CSV report and emails it using a Thymeleaf HTML template
- `POST /api/v1/reports/investigator-workload` generates a CSV workload report per investigator
- `POST /api/v1/reports/investigator-workload/email` generates the CSV workload report and emails it with an HTML summary
- `POST /api/v1/reports/rule-effectiveness` generates a CSV rule effectiveness report per AML rule
- `POST /api/v1/reports/rule-effectiveness/email` generates the CSV rule effectiveness report and emails it with a business summary
- a Spring scheduler can generate the open-cases report every 24 hours
- a Spring scheduler can generate and email the investigator workload report weekly
- a Spring scheduler can generate and email the rule effectiveness report weekly
- generated files are written to the configured `reports/` directory by default

Relevant properties in `src/main/resources/application.properties`:

```properties
sam.reports.open-cases.output-dir=reports
sam.reports.open-cases.file-prefix=open_cases_report
sam.reports.open-cases.scheduler.enabled=true
sam.reports.open-cases.scheduler.fixed-delay-ms=86400000
sam.reports.open-cases.scheduler.initial-delay-ms=60000
sam.reports.investigator-workload.output-dir=reports
sam.reports.investigator-workload.file-prefix=investigator_workload_report
sam.reports.investigator-workload.scheduler.enabled=true
sam.reports.investigator-workload.scheduler.cron=0 0 8 * * MON
sam.reports.investigator-workload.scheduler.zone=Europe/Warsaw
sam.reports.rule-effectiveness.output-dir=reports
sam.reports.rule-effectiveness.file-prefix=rule_effectiveness_report
sam.reports.rule-effectiveness.scheduler.enabled=true
sam.reports.rule-effectiveness.scheduler.cron=0 30 8 * * MON
sam.reports.rule-effectiveness.scheduler.zone=Europe/Warsaw
sam.reports.open-cases.email.enabled=false
sam.reports.open-cases.email.from=
sam.reports.open-cases.email.recipients=
sam.reports.open-cases.email.subject=Open Cases Report
sam.reports.open-cases.email.preview-rows=10
sam.reports.investigator-workload.email.enabled=false
sam.reports.investigator-workload.email.from=
sam.reports.investigator-workload.email.recipients=
sam.reports.investigator-workload.email.subject=Investigator Workload Report
sam.reports.rule-effectiveness.email.enabled=false
sam.reports.rule-effectiveness.email.from=
sam.reports.rule-effectiveness.email.recipients=
sam.reports.rule-effectiveness.email.subject=Rule Effectiveness Report
```

The default weekly workload schedule is every Monday at 08:00 in the `Europe/Warsaw` time zone.
The default weekly rule-effectiveness schedule is every Monday at 08:30 in the `Europe/Warsaw` time zone.

SMTP is configured separately through the standard Spring mail properties:

```properties
spring.mail.host=smtp.example.com
spring.mail.port=587
spring.mail.username=your-user
spring.mail.password=your-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

The HTML email bodies are rendered from `src/main/resources/templates/email/reports/` and the generated CSV files are attached to the messages.

---

## Sprint 2

### Domain Model & Persistence
- Full JPA entity model mapped to the Sprint 1 schema
- Spring Data JPA repositories for all eight entities
- H2 in-memory database wired for the `test` profile — no MySQL required to run tests

### Rules Engine

Six AML rules implemented, each behind the `AmlRule` interface and independently testable:

| Rule | Category | What it detects |
|---|---|---|
| `ThresholdRule` | STRUCTURING | Single transaction exceeding a configured USD limit |
| `VelocityRule` | VELOCITY | Too many transactions within a rolling lookback window |
| `StructuringRule` | STRUCTURING | Cumulative amounts approaching (75–100%) the reporting threshold |
| `RoundNumberRule` | PATTERN | Suspiciously round amounts (multiples of 1 000 USD) |
| `PatternRule` | PATTERN | Same amount repeated ≥ N times — smurfing detection |
| `GeographyRule` | GEOGRAPHY | Transactions involving high-risk/sanctioned jurisdictions |

### Services

- **`RuleEngineService`** — loads all active `AlertRule` records, builds a `RuleContext` per transaction, runs every applicable rule, and calls the `raise_alert` stored procedure for each match
- **`WatchlistScreeningService`** — normalises customer names, scores against all active watchlist entries, persists `WatchlistMatch` records, and updates transaction status (BLOCKED / PENDING / SCREENED)

### Test Suite

| Layer | Test classes |
|---|---|
| Unit — Rules | `ThresholdRuleTest`, `VelocityRuleTest`, `RoundNumberRuleTest`, `StructuringRuleTest`, `PatternRuleTest`, `GeographyRuleTest` |
| Unit — Services | `RuleEngineServiceTest`, `AlertRaisingServiceTest` |
| Unit — DAOs | `AlertRuleDaoTest`, `TxnDaoTest` |
| Integration | `CustomerAccountTxnFlowIT`, `InvestigationFlowIT`, `WatchlistMatchFlowIT` |
| Context | `SamApplicationTests` |

---

## Sprint 3

### REST Controllers

Three REST controllers expose the current API surface under `/api/v1`:

| Controller | Responsibility | Implemented endpoints |
|---|---|---|
| `TransactionController` | Customer lookup and account transaction history | `GET /customers/{id}`, `GET /accounts/{id}/transactions` |
| `InvestigationController` | Open cases, retrieve case detail, append investigation notes | `POST /cases`, `GET /cases/{id}`, `POST /cases/{id}/notes` |
| `WatchlistController` | Search watchlist entities and review persisted watchlist matches | `GET /watchlist/search?name=...`, `GET /watchlist-matches` |

The controllers are intentionally thin: they validate request shape, delegate business logic to services, and rely on the global exception layer for consistent error responses.

### Exception Handling

Centralised exception handling is implemented through **`GlobalExceptionHandler`** using `@RestControllerAdvice`.

Handled exception categories include:

- **`DataNotFoundException`** — mapped to `404 RESOURCE_NOT_FOUND`
- **`InvalidInputException`** — mapped to `400 INVALID_INPUT`
- **`MethodArgumentNotValidException`** — mapped to `400 VALIDATION_FAILED` for request body validation
- **`ConstraintViolationException`** — mapped to `400 VALIDATION_FAILED` for parameter validation
- **`MethodArgumentTypeMismatchException`** — mapped to `400 TYPE_MISMATCH`
- **`IllegalArgumentException`** — mapped to `400 INVALID_ARGUMENT`
- **`IllegalStateException`** — mapped to `409 INVALID_STATE_TRANSITION`
- **`Exception`** — mapped to `500 INTERNAL_ERROR`

Exception classes currently present in the project:

- `DataNotFoundException`
- `GlobalExceptionHandler`
- `InvalidInputException`

This gives the API one consistent JSON error shape with `timestamp`, `status`, `errorCode`, `message`, and optional `details`.

### DTO Layer

Sprint 3 introduces and expands request/response DTO usage so the API returns purpose-built payloads instead of exposing internal service objects directly.

Request DTOs:

- `CaseNoteDto` — author and note text for appending case notes
- `OpenCaseRequestDto` — alert id, officer, and priority for opening a case

Response DTOs:

- `CaseResponseDto` — case detail including status, severity, timestamps, outcome, and accumulated findings
- `WatchlistMatchDto` — simplified watchlist match response model
- `ErrorResponseDto` — standardised error response structure documented in OpenAPI

Validation rules such as `@NotBlank`, `@Positive`, `@Pattern`, and `@Size` are applied directly on request DTOs and enforced automatically by Spring validation.

### OpenAPI Configuration

API documentation is now maintained in a standalone **OpenAPI YAML file** instead of being embedded in controller annotations.

Current setup:

- `src/main/resources/static/openapi.yaml` — source of truth for endpoint descriptions, examples, schemas, and response documentation
- `application.properties` — configures Swagger UI to load the static spec with `springdoc.swagger-ui.url=/openapi.yaml`
- `http://localhost:8080/swagger-ui.html` — interactive Swagger UI
- `http://localhost:8080/openapi.yaml` — raw OpenAPI document served by Spring Boot static resources

This keeps controller classes focused on routing and business flow while the OpenAPI spec owns:

- endpoint descriptions
- request/response examples
- schema descriptions
- seed-data-based sample payloads

### Running the application with Docker

The application can be easily run inside Docker containers for simplified setup and isolation. A Dockerfile and docker-compose.yml are included in the project root.


#### Prerequisites

- Docker installed and running on your machine
- Docker Compose installed (usually included with Docker Desktop)
- MySQL credentials configured in the `.env` file (see Environment Setup section above)

#### Steps to Run

##### 1. Start the application and database

From the project root, run:

```bash
docker-compose up
```
##### 2. Stop the containers

```bash
docker-compose down -v
```
##### 3. Running database scripts

If you need to execute SQL scripts inside MySQL container:

###### 1. Start the containers in detached mode

```bash
docker-compose up -d
```
###### 2. Access MySQL shell

```bash
docker-compose exec mysql mysql -u appuser -p${DB_NON_ROOT_PASSWORD} ${DB_NAME}
```

### Notes Workflow

The case notes endpoint now uses the case ID in the path to identify the investigation:

- `POST /api/v1/cases/{id}/notes`

and the request body contains the note content itself:

```json
{
  "author": "Alice Morgan",
  "noteText": "Customer provided supporting documents for the wire transfer. Sanctions screening remains under review."
}
```

The note is appended into the investigation `findings` field in a timestamped format:

```text
[2026-04-20 06:40 | Alice Morgan] Customer provided supporting documents for the wire transfer. Sanctions screening remains under review.
```

---

## Project Structure

```
sam/
├── src/main/java/com/grad/sam/
│   ├── controller/       # REST controllers
│   ├── enums/            # AlertSeverity, AlertStatus, RuleCategory, …
│   ├── dto/              # Request and response DTOs
│   ├── exception/        # Custom exceptions + global handler
│   ├── model/            # JPA entities
│   ├── repository/       # Spring Data JPA interfaces
│   ├── rules/            # AmlRule interface + 6 rule implementations
│   └── service/          # Screening, investigation, alert, and watchlist services
├── src/main/resources/
│   ├── application.properties
│   └── static/
│       └── openapi.yaml  # Standalone OpenAPI specification used by Swagger UI
├── src/test/java/com/grad/sam/
│   ├── integration/
│   ├── repository/
│   ├── rules/
│   └── service/
├── src/test/resources/
│   └── application-test.properties   # H2 config, activated by @ActiveProfiles("test")
├── schema/
│   ├── tables/                       # One .sql file per table
│   ├── views/                        # One .sql file per view
│   ├── stored_procedures/            # One .sql file per procedure
│   └── diagram.png                   # E/R diagram
├── scripts/                          # Shell scripts for db automation
└── .env                              # Local credentials — never commit this file
```

---

## Environment Setup

The project uses [spring-dotenv](https://github.com/paulschwarz/spring-dotenv) to load credentials from a `.env` file at the project root. This keeps secrets out of `application.properties` and out of version control.

Create a file named `.env` in the project root (same level as `pom.xml`):

```env
DB_HOST=localhost
DB_PORT=3306
DB_NAME=sam
DB_USER=root
DB_PASSWORD=your_password_here
DB_NON_ROOT_USER=appuser
DB_NON_ROOT_PASSWORD=app_password_here
```

> `.env` is in `.gitignore` — never commit it. Each team member keeps their own local copy.

These values are injected into `application.properties` at startup:

```properties
spring.datasource.url=jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
```

If a variable is absent from `.env`, the defaults in `application.properties` kick in: `localhost`, `3306`, `sam`, `root`.

---

## Running the Application

```bash
# Make sure MySQL is running and the sam database exists
# Make sure your .env file is configured (see above)

mvn spring-boot:run
```

The app starts on `http://localhost:8080`.
Swagger UI is available at `http://localhost:8080/swagger-ui.html`.

---

## Running Tests

Tests use an H2 in-memory database. **No MySQL or `.env` file required.**

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=ThresholdRuleTest

# Run all rule unit tests
mvn test -Dtest="*RuleTest"

# Run integration tests only
mvn test -Dtest="*FlowIT"
```

Test results are written to `target/surefire-reports/`.

---

## JaCoCo Coverage Report

JaCoCo generates a coverage report automatically after every `mvn test` run. No extra command needed.

```bash
# 1. Run tests (report is generated automatically)
mvn test

# 2. Open the report

# macOS
open target/site/jacoco/index.html

# Windows
start target/site/jacoco/index.html

# Linux
xdg-open target/site/jacoco/index.html
```

The report is at `target/site/jacoco/index.html`. Enum classes under `com.grad.sam.enums` are excluded from coverage metrics by configuration.

---

## API Examples

Current implemented endpoints:

- `GET /api/v1/customers/{id}`
- `GET /api/v1/accounts/{id}/transactions`
- `POST /api/v1/cases`
- `GET /api/v1/cases/{id}`
- `POST /api/v1/cases/{id}/notes`
- `GET /api/v1/watchlist/search?name={name}`
- `GET /api/v1/watchlist-matches`

Swagger UI:

- `http://localhost:8080/swagger-ui.html`
- `http://localhost:8080/openapi.yaml`

Example `curl` calls:

```bash
curl http://localhost:8080/api/v1/customers/1
```

```bash
curl http://localhost:8080/api/v1/accounts/1/transactions
```

```bash
curl -X POST http://localhost:8080/api/v1/cases \
  -H "Content-Type: application/json" \
  -d '{"alertId":6,"assignedOfficer":"Alice Morgan","priority":"MEDIUM"}'
```

```bash
curl http://localhost:8080/api/v1/cases/3
```

```bash
curl -X POST http://localhost:8080/api/v1/cases/3/notes \
  -H "Content-Type: application/json" \
  -d '{"author":"Alice Morgan","noteText":"Customer provided supporting documents for the wire transfer. Sanctions screening remains under review."}'
```

```bash
curl "http://localhost:8080/api/v1/watchlist/search?name=Viktor"
```

```bash
curl http://localhost:8080/api/v1/watchlist-matches
```

```bash
curl "http://localhost:8080/api/v1/watchlist-matches?txnId=5"
```

---

## E/R Diagram

![E/R Diagram](docs/diagram.png)

---

## Team

- Teammate 1 – Zofia Grabowska
- Teammate 2 – Rumbidzai Jinjika
- Teammate 3 – Patrycja Kościelniak
- Teammate 4 – Marta Kozdrój
