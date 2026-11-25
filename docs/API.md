# Northbridge Core Platform — API contract

All services speak JSON over HTTP. Amounts are decimal strings with two places (`"125.40"`), dates are
`yyyy-MM-dd`, timestamps are ISO-8601 in America/New_York. Account numbers are 12-digit DDA numbers
(3-digit branch, 8-digit serial, Luhn check digit) and are always masked (`********3455`) in any UI
or log line.

Authentication is HTTP Basic against the core-banking user store. Roles: `CUSTOMER` (own accounts only),
`TELLER` (any account, holds, reversals), `OPS` (admin and batch endpoints).

| service | port | base path |
|---|---|---|
| core-banking | 8080 | `/api/v1` |
| boa-payments (payments hub, separate repo) | 8081 | `/api/v1/payments` |
| statements-service | 8082 | `/api/v1/statements` |

## Seed users and data (local profile)

| username | password | role | CIF |
|---|---|---|---|
| dana.whitfield | Welcome1! | CUSTOMER | 100000001 |
| marcus.oyelaran | Welcome1! | CUSTOMER | 100000002 |
| teller01 | Teller#2019 | TELLER | – |
| ops.batch | Ops#Batch1 | OPS | – |

| account | CIF | product | opening balance |
|---|---|---|---|
| 104000123455 | 100000001 | DDA-CHK (Advantage Checking) | 4,812.33 |
| 104000123463 | 100000001 | DDA-SAV (Advantage Savings) | 22,150.00 |
| 211000048218 | 100000002 | DDA-CHK | 1,045.12 |
| 211000048226 | 100000002 | DDA-SAV | 9,300.00 |
| 307000555010 | – | GL-SUSP (suspense GL) | 0.00 |

## core-banking (`:8080/api/v1`)

### Customers
- `GET /customers?lastName=Oye` (TELLER, OPS) → `[Customer]` prefix match on last name
- `GET /customers/{cif}` → `{ "cif": "100000001", "firstName": "Dana", "lastName": "Whitfield", "email": "...", "segment": "PREFERRED", "kycStatus": "VERIFIED" }`
- `GET /customers/{cif}/accounts` → `[Account]`

### Accounts
- `GET /accounts/{accountNumber}` → `Account`
  ```json
  { "accountNumber": "104000123455", "maskedNumber": "********3455", "cif": "100000001",
    "productCode": "DDA-CHK", "nickname": "Advantage Checking", "status": "OPEN",
    "ledgerBalance": "4812.33", "availableBalance": "4712.33", "holdAmount": "100.00",
    "openedOn": "2014-03-11" }
  ```
- `GET /accounts/{accountNumber}/transactions?from=yyyy-MM-dd&to=yyyy-MM-dd&page=0&size=50` → `{ "content": [Transaction], "page": 0, "size": 50, "total": 812 }`
  ```json
  { "id": 99812, "accountNumber": "104000123455", "postedAt": "2026-11-02T14:03:11",
    "businessDate": "2026-11-02", "type": "DEBIT", "channel": "CARD", "amount": "-42.18",
    "runningBalance": "4812.33", "description": "WHOLE FDS MKT #10233", "status": "POSTED" }
  ```
  `channel` ∈ `CARD, ACH, WIRE, P2P, BILLPAY, BRANCH, ATM, INTEREST, FEE, TRANSFER`.
- `POST /accounts/{accountNumber}/holds` (TELLER) `{ "amount": "100.00", "reason": "CHK-DEP-HOLD", "releaseOn": "2026-11-28" }` → `201 { "holdId": 51, ... }`

### Transactions
- `GET /transactions/search?q=coffee&accountNumber=104000123455` → `[Transaction]` (free-text over description; known slow path)

### Transfers (internal book transfers; also used by boa-payments to post)
- `POST /transfers` (201 Created) `{ "fromAccount": "104000123455", "toAccount": "104000123463", "amount": "250.00", "memo": "Rainy day", "idempotencyKey": "uuid" }`
  → `201 { "transferId": "TRF-20261125-000231", "status": "POSTED", "postedAt": "...", "businessDate": "..." }`
  Errors: `422 { "code": "NSF", "message": "Insufficient available funds" }`, `422 LIMIT_EXCEEDED`, `404 ACCOUNT_NOT_FOUND`, `409 DUPLICATE`.
- `GET /transfers/{transferId}` → transfer record.

### Ops
- `GET /ops/health/slo` (OPS) → per-endpoint p95 latency and error rate for the last 15 minutes.

## boa-payments (`:8081/api/v1/payments`)
- `GET /payees?cif=100000001` → `[ { "payeeId": "PY-1001", "name": "Con Edison", "category": "UTILITY", "accountRef": "****7781" } ]`
- `POST /bill` `{ "cif", "fromAccount", "payeeId", "amount", "sendOn" }` → `201 { "paymentId": "BP-...", "status": "SCHEDULED" }`
- `POST /p2p` `{ "cif", "fromAccount", "recipient": { "email" | "phone" }, "amount", "memo" }` → `201 { "paymentId": "P2P-...", "status": "SENT" }`
- `GET /history?cif=100000001` → `[Payment]`
Payments posts money movement through core-banking `POST /transfers` (to the GL-SUSP suspense account) and shares `nb-common` (Money, AccountNumbers, BankDates) with core-banking.

## statements-service (`:8082/api/v1/statements`)
- `GET /{accountNumber}?month=2026-10` → `{ "accountNumber", "period", "openingBalance", "closingBalance", "lines": [Transaction] }`
- `GET /{accountNumber}/pdf?month=2026-10` → `application/pdf`
