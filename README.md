# Northbridge Core Platform

Core deposit platform for Northbridge Federal Bank: customer information (CIF), demand-deposit
accounts (DDA), postings, funds holds, internal book transfers, end-of-day (EOD) processing and
customer statements. Online channels (teller console, mobile, payments hub) call this platform
through the REST contract in [`docs/API.md`](docs/API.md).

| Module | Port | Purpose |
|---|---|---|
| `nb-common` | – | Shared library: `Money`, `BankDates`, `AccountNumbers`, STMT-XML JAXB helpers, token cipher, audit logger. Also consumed by `boa-payments`. |
| `core-banking` | 8080 | CIF, DDA, postings, holds, book transfers, EOD batch, ops endpoints, SLO metrics. |
| `statements-service` | 8082 | Monthly statements (JSON and PDF) built from core-banking postings. |

Related repositories: `boa-payments` (payments hub, 8081), `boa-teller-web` (branch teller console),
`boa-android` and `boa-ios` (mobile banking).

## Build and run

Requires JDK 8 and Maven 3.6+.

```bash
mvn clean install                                   # build, unit + API tests, JaCoCo reports
java -jar core-banking/target/core-banking-*.jar    # http://localhost:8080
java -jar statements-service/target/statements-service-*.jar   # http://localhost:8082
```

The default profile runs on an in-memory H2 database (Oracle compatibility mode), loads the
reference customers from `data.sql` and generates roughly six years of posting history.
`-Dspring.profiles.active=oracle` targets the production database.

Quick check:

```bash
curl -u 'dana.whitfield:Welcome1!' http://localhost:8080/api/v1/customers/100000001/accounts
curl -u 'ops.batch:Ops#Batch1'     http://localhost:8080/api/v1/ops/health/slo
```

Swagger UI: http://localhost:8080/swagger-ui.html

## Operations

- EOD runs at 20:00 America/New_York on business days (`northbridge.eod.cron`): releases expired
  holds and accrues savings interest. Ops can trigger it with `POST /api/v1/ops/eod/run`.
- `/api/v1/ops/health/slo` reports rolling 15-minute p50/p95 latency and error rate per endpoint.
  Targets: p95 < 300 ms and error rate < 0.5 % for all customer-facing endpoints.
- Production log extracts used for incident reviews are kept in `ops/log-samples/`.

## Glossary

| Term | Meaning |
|---|---|
| CIF | Customer Information File number (9 digits) |
| DDA | Demand-deposit account; products `DDA-CHK` (checking) and `DDA-SAV` (savings) |
| GL-SUSP | Internal suspense general-ledger account used by the payments hub |
| Book transfer | Same-bank transfer between two Northbridge accounts, posted immediately |
| Hold | Amount reserved against available balance until `release_on` |
| EOD | End-of-day batch; cutoff is 20:00 ET, later postings take the next business date |
| STMT-XML | Statement interchange format received from the print/archive vendor |
