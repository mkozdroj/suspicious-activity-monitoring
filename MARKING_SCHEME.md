# HSBC Graduate Software Engineering Programme 2026
## Project 04: Suspicious Activity Monitor (SAM) — Marking Scheme

**Total Marks: 100 + 5 bonus**
**Duration: 3 Sprints (Foundations → Java → Deployment)**

---

## Grade Descriptors

| Grade | Definition |
|-------|-----------|
| **Excellent** | Exceeds expectations; robust, well-designed, fully functional; excellent domain knowledge |
| **Good** | Meets requirements fully; minor gaps; mostly robust; good domain understanding |
| **Satisfactory** | Meets core requirements; some gaps or issues; basic domain understanding |
| **Needs Improvement** | Falls short of requirements; significant gaps; limited domain understanding |

---

## Sprint 1: Foundations (25% = 25 marks)

### 1.1 SQL Schema Design (8 marks)

| Criterion | Excellent | Good | Satisfactory | Needs Improvement |
|-----------|-----------|------|--------------|-------------------|
| **Entity Modeling & Relationships** | All 8 entities correctly designed (customer, account, txn, alert_rule, alert, investigation, watchlist, watchlist_match) with appropriate primary/foreign keys and cardinality | 7 entities correct; minor relationship issues | 6 entities correct; some relationship gaps | <6 entities or major structural flaws |
| **Domain Constraints** | Enforces risk ratings (LOW/MEDIUM/HIGH/SANCTIONED), rule types (structuring, smurfing, velocity, geography, pattern), alert statuses, investigation states | Most constraints enforced | Basic constraints present | Missing key constraints |
| **Indexing & Performance** | Strategic indexes on txn timestamp, customer_id, rule_id for query optimization | Adequate indexes | Minimal indexing | No indexing strategy |
| **Normalization** | Full 3NF; no redundancy; efficient structure | 3NF with minor denormalization | Some denormalization | Poor normalization |

**Marks: __ / 8**

### 1.2 Data Loading & SQL Queries (10 marks)

| Criterion | Excellent | Good | Satisfactory | Needs Improvement |
|-----------|-----------|------|--------------|-------------------|
| **SQL Data Loading** | Clean, efficient INSERT statements; handles bulk loads; sample data realistic to AML domain (e.g. customer KYC data, test transactions) | Minor inefficiencies; adequate sample data | Basic loading; limited sample data | Incomplete or broken loading |
| **AML Rule Queries** | 5+ complex queries covering: structuring detection, velocity rules, geographic anomalies, PEP screening joins, watchlist matches; queries are optimized | 4 queries; mostly correct; minor optimization issues | 3 queries; some correctness issues | <3 queries or major errors |
| **Alert Generation Logic** | Query correctly joins alert_rules with transactions, generates alerts with appropriate rule_id/severity assignment | Mostly correct; minor logic gaps | Basic join logic; incomplete severity logic | Broken or missing joins |
| **Investigation Queries** | Efficient queries for investigation state transitions, alert clustering, SAR filing status | Mostly functional | Basic queries present | Limited query functionality |

**Marks: __ / 10**

### 1.3 Shell Scripts & Documentation (7 marks)

| Criterion | Excellent | Good | Satisfactory | Needs Improvement |
|-----------|-----------|------|--------------|-------------------|
| **Shell Script Quality** | Well-structured scripts (setup.sh, data_load.sh, etc.); error handling; logging; automated schema/data setup | Good structure; minor error handling gaps | Basic scripts; limited error handling | Missing or non-functional scripts |
| **README & Documentation** | Clear setup instructions; schema diagram/description; example queries with output; domain glossary (SAR, PEP, OFAC, structuring, smurfing) | Good documentation; minor clarity gaps | Basic documentation present | Minimal or unclear documentation |
| **Reproducibility** | Script execution fully reproduces database state; idempotent; clear sequence | Mostly reproducible; minor issues | Partially reproducible | Not easily reproducible |

**Marks: __ / 7**

---

## Sprint 2: Java Service Layer (30% = 30 marks)

### 2.1 Core Service Classes & AML Logic (12 marks)

| Criterion | Excellent | Good | Satisfactory | Needs Improvement |
|-----------|-----------|------|--------------|-------------------|
| **Rule Engine Design** | Clean abstraction: rule interface/implementations for Structuring, Velocity, Geography, Pattern rules; extensible for new rule types; configurable thresholds | Good design; minor extensibility gaps | Basic rule classes; limited flexibility | Monolithic/hard-coded rules |
| **Alert Generation Service** | AlertService correctly evaluates rules, generates alerts with severity (derived from rule type + txn context), links to investigation; handles SAR thresholds | Mostly functional; minor gaps in severity logic | Basic alert generation; limited SAR logic | Incomplete alert logic |
| **Watchlist Screening Service** | WatchlistScreeningService matches customer/account against OFAC/UN/HMT lists (via watchlist entity); sets risk rating SANCTIONED; blocks transactions appropriately | Good matching logic; minor edge cases | Basic matching; limited coverage | Incomplete screening |
| **Investigation State Machine** | Investigation moves through states (NEW → UNDER_REVIEW → SAR_FILED → CLOSED); correct state transitions; idempotent operations | Mostly correct; minor state gaps | Basic state transitions | Incorrect or incomplete states |

**Marks: __ / 12**

### 2.2 JDBC Integration & Data Access (8 marks)

| Criterion | Excellent | Good | Satisfactory | Needs Improvement |
|-----------|-----------|------|--------------|-------------------|
| **JDBC Implementation** | DAO classes for each entity; proper Connection management (try-with-resources); parameterized queries (no SQL injection); efficient batch operations | Good JDBC usage; minor resource issues | Basic JDBC; some SQL injection risk | Poor connection handling or SQL injection risk |
| **Object Mapping** | Clean POJOs (Customer, Account, Transaction, AlertRule, Alert, Investigation); proper getters/setters; equals/hashCode where needed | Mostly correct; minor mapping gaps | Basic POJOs; some missing fields | Incomplete or incorrect POJOs |
| **Transaction Handling** | Appropriate use of JDBC transactions for multi-step operations (e.g. create alert + investigation); rollback on error | Mostly correct; minor edge cases | Basic txn logic | Poor txn handling |
| **Query Efficiency** | Queries avoid N+1 problems; batch fetches where appropriate; proper use of indexes | Good efficiency; minor optimization gaps | Acceptable queries | Inefficient queries |

**Marks: __ / 8**

### 2.3 Unit Testing (10 marks)

| Criterion | Excellent | Good | Satisfactory | Needs Improvement |
|-----------|-----------|------|--------------|-------------------|
| **Test Coverage** | ≥75% code coverage (JaCoCo); all critical paths tested: rule matching, alert generation, watchlist screening, state transitions, edge cases (null, boundary values) | 60-75% coverage; most paths tested | 45-60% coverage; some gaps | <45% coverage or incomplete tests |
| **JUnit Test Quality** | Well-organized test classes; clear test names (given-when-then pattern); independent tests; proper setup/teardown; no test interdependencies | Mostly well-organized; minor clarity gaps | Basic test structure; some interdependencies | Poor organization or interdependencies |
| **Mockito Usage** | Proper mocking of DAO/external services; verification of calls; stub responses for edge cases | Good mocking; minor issues | Basic mocking present | Limited or incorrect mocking |
| **AML Domain Tests** | Tests cover AML scenarios: structuring (small transactions over time), smurfing (multiple accounts same customer), velocity (high txn count), geographic anomalies, PEP/watchlist hits | Most AML scenarios covered; minor gaps | Some AML scenarios; limited coverage | Few or no AML-specific tests |

**Marks: __ / 10**

---

## Sprint 3: Deployment & REST API (30% = 30 marks)

### 3.1 Spring Boot REST API (10 marks)

| Criterion | Excellent | Good | Satisfactory | Needs Improvement |
|-----------|-----------|------|--------------|-------------------|
| **Endpoint Design** | RESTful endpoints: GET /customers/{id}, GET /accounts/{id}/transactions, POST /investigations, GET /investigations/{id}, GET /alerts, GET /watchlist-matches; proper HTTP verbs/status codes; versioning | Good endpoint coverage; minor REST gaps | Basic endpoints; inconsistent HTTP usage | Incomplete or non-RESTful endpoints |
| **Request/Response DTOs** | Clean DTOs with validation annotations (@NotNull, @Positive, etc.); proper serialization; includes domain context (e.g. alert severity, investigation state) | Mostly clean; minor validation gaps | Basic DTOs; limited validation | Poor DTO design |
| **Error Handling** | Global exception handler (@ControllerAdvice); meaningful error responses (HTTP 400/404/409/500); domain-specific error codes for SAM (e.g. "WATCHLIST_MATCH_BLOCKED") | Good error handling; minor gaps | Basic error responses | Limited error handling |
| **Documentation** | Swagger/OpenAPI annotations on endpoints; clear descriptions of request/response; example values; domain context (AML rules, investigation workflow) | Good documentation; minor gaps | Basic annotations; limited examples | Minimal documentation |

**Marks: __ / 10**

### 3.2 Docker & Docker-Compose (8 marks)

| Criterion | Excellent | Good | Satisfactory | Needs Improvement |
|-----------|-----------|------|--------------|-------------------|
| **Dockerfile** | Multi-stage build; minimal image size; non-root user; proper ENTRYPOINT/CMD; health check; environment variable configuration | Good Dockerfile; minor optimization gaps | Basic Dockerfile; some inefficiencies | Poor Dockerfile (e.g. root user, large image) |
| **Docker-Compose Setup** | Services for: Java app, PostgreSQL (with schema init), optional Redis/message queue; proper networking; volume mounts for data persistence; environment variable management | Good setup; minor network/volume gaps | Basic services; limited configuration | Incomplete setup |
| **Configuration Management** | Externalized configuration (application.properties, .env); secrets not hardcoded; easy to switch between dev/test/prod | Good configuration; minor gaps | Basic configuration present | Hardcoded values; poor configuration |
| **Container Health & Logging** | Proper logging to stdout; health checks for services; clear startup sequence; easy debugging | Good logging; minor gaps | Basic logging present | Limited logging |

**Marks: __ / 8**

### 3.3 CI/CD Pipeline (Jenkinsfile) (12 marks)

| Criterion | Excellent | Good | Satisfactory | Needs Improvement |
|-----------|-----------|------|--------------|-------------------|
| **Pipeline Structure** | Multi-stage pipeline: Checkout → Build → Test → Code Quality → Docker Build → Deploy; clear stage separation; consistent naming | Good structure; minor stage gaps | Basic pipeline; some unclear stages | Incomplete or monolithic pipeline |
| **Build & Test Stages** | Maven/Gradle build; JUnit tests run; code coverage report (JaCoCo); test results published; build fails on coverage < threshold | Mostly complete; minor reporting gaps | Basic build/test; limited reporting | Incomplete build/test stages |
| **Code Quality Gate** | SonarQube integration or equivalent (PMD, Checkstyle); configurable thresholds; pipeline fails on violations; reports quality metrics | Good quality gate; minor configuration | Basic quality checks; limited enforcement | Minimal or no quality gate |
| **Docker & Deployment** | Docker image built with proper tagging; image scanned for vulnerabilities; deployed to staging/production environment via docker-compose or orchestration; rollback strategy documented | Good deployment flow; minor gaps | Basic Docker deployment; limited strategy | Incomplete deployment |
| **Post-Build Actions** | Test/coverage reports published; artifacts archived; notifications (email/Slack) on failure; clear feedback loop | Good post-build actions; minor gaps | Basic artifact handling; limited notifications | Minimal post-build actions |

**Marks: __ / 12**

---

## Code Quality & Collaboration (15% = 15 marks)

### 4.1 Code Quality (7 marks)

| Criterion | Excellent | Good | Satisfactory | Needs Improvement |
|-----------|-----------|------|--------------|-------------------|
| **Code Style & Readability** | Consistent naming (domain-aware: e.g. `evaluateStructuringRule()`); clear variable names; proper indentation; follows Java conventions | Mostly consistent; minor naming gaps | Basic style; some unclear names | Inconsistent or unclear code |
| **SOLID Principles** | Single Responsibility (each class has one reason to change); Dependency Injection (Spring @Autowired, constructor injection); interfaces for extensibility | Good adherence; minor gaps | Basic adherence; some violations | Poor adherence |
| **Error Handling & Validation** | Defensive programming: null checks, input validation, meaningful exceptions (custom domain exceptions for AML logic) | Mostly defensive; minor gaps | Basic error handling | Limited validation |

**Marks: __ / 7**

### 4.2 Git Usage & Collaboration (5 marks)

| Criterion | Excellent | Good | Satisfactory | Needs Improvement |
|-----------|-----------|------|--------------|-------------------|
| **Commit History** | Clear, atomic commits; messages follow convention (e.g. "feat: add structuring rule engine"); commits tied to issues/tasks | Mostly clear; minor message gaps | Basic commits; inconsistent messages | Poor commit hygiene |
| **Branching Strategy** | Feature branches (feature/*, bugfix/*); code review via pull requests; meaningful PR descriptions linking to requirements | Good branching; minor gaps | Basic branching; limited PRs | Weak branching strategy |
| **Collaboration** | Meaningful code reviews; constructive feedback on PRs; prompt responses; team communication documented | Good collaboration; minor gaps | Basic collaboration | Limited team interaction |

**Marks: __ / 5**

### 4.3 Domain Understanding (3 marks)

| Criterion | Excellent | Good | Satisfactory | Needs Improvement |
|-----------|-----------|------|--------------|-------------------|
| **AML/SAM Concepts** | Demonstrates deep understanding of AML rules (structuring, smurfing, velocity, geography, pattern), SAR filing, PEP screening, OFAC/UN/HMT watchlists, investigation workflow, risk ratings; reflects in code design | Good understanding; minor gaps | Basic understanding; some gaps | Limited AML knowledge |

**Marks: __ / 3**

---

## Summary Breakdown

| Section | Marks | Weight |
|---------|-------|--------|
| Sprint 1: Foundations | 25 | 25% |
| Sprint 2: Java | 30 | 30% |
| Sprint 3: Deployment | 30 | 30% |
| Code Quality & Collaboration | 15 | 15% |
| **Total** | **100** | **100%** |

---

## Bonus Marks (Up to +5)

| Criterion | Marks |
|-----------|-------|
| **Innovation & Excellence** | +1–5 marks for exceptional work beyond requirements: e.g. advanced AML rule engine with machine learning, custom alert visualization dashboard, advanced investigation analytics, performance optimization (query tuning, caching), comprehensive security hardening, outstanding documentation, novel domain insights |

**Example applications:**
- Implement adaptive rule thresholds based on customer risk profile (+2)
- Build real-time alert dashboard with WebSocket updates (+2)
- Integrate machine learning for anomaly detection (+3)
- Implement multi-tenant architecture for scalability (+2)
- Complete penetration testing & security hardening report (+2)

---

## Marking Notes

1. **Grading per criterion:** Award marks based on grade descriptor (e.g. Excellent = full marks, Good = 90%, Satisfactory = 70%, Needs Improvement = 40%)
2. **Evidence:** Marks must be justified by code review, test results, Git history, and deployment pipeline visibility
3. **Partial credit:** Award proportionally within a grade band for near-misses
4. **Domain specificity:** Extra weight on criteria that reflect AML/SAM knowledge (rule engine, watchlist screening, investigation workflow)
5. **Team assessment:** Collaboration marks reflect individual contribution (verified via Git blame, PR history, peer feedback)

---

**Document Version:** 1.0 | **Date:** April 2026
**Assessor:** _________________ | **Date:** _________________
