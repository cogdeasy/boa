# BofA Core Platform — API contract

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
| 440000528719 | 100000001 | CC-VISA (Customized Cash Rewards Visa) | -1,284.56 (card balance owed) |
| 211000048218 | 100000002 | DDA-CHK | 1,045.12 |
| 211000048226 | 100000002 | DDA-SAV | 9,300.00 |
| 307000555010 | – | GL-SUSP (suspense GL) | 0.00 |

## core-banking (`:8080/api/v1`)

### Customers
- `GET /customers?lastName=Oye` (TELLER, OPS) → `[Customer]` prefix match on last name
- `GET /customers/me` (CUSTOMER) → the signed-in customer's `Customer` record; mobile apps call it after login to resolve the CIF
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
- `productCode` ∈ `DDA-CHK` (checking), `DDA-SAV` (savings), `CC-VISA` (Customized Cash Rewards Visa credit card),
  `GL-*` (internal general-ledger accounts, e.g. `GL-SUSP`). A `CC-VISA` `ledgerBalance` is negative while a balance is owed;
  a transfer into the card is a card payment and moves it towards zero. A payment above the amount owed posts in full and leaves a positive (credit) balance.
- Per-transfer limits by destination product (`product_limit` table, source of truth for every channel):

  | productCode | product | perTransfer |
  |---|---|---|
  | DDA-CHK | Advantage Checking | 10000.00 |
  | DDA-SAV | Advantage Savings | 25000.00 |
  | CC-VISA | Customized Cash Rewards Visa | 15000.00 |

  `GL-*` destinations are internal postings and have no per-transfer limit. Any other product without a row is refused.
- `POST /accounts/{accountNumber}/holds` (TELLER) `{ "amount": "100.00", "reason": "CHK-DEP-HOLD", "releaseOn": "2026-11-28" }` → `201 { "holdId": 51, ... }`

### Transactions
- `GET /transactions/search?q=coffee&accountNumber=104000123455` → `[Transaction]` (free-text over description; known slow path)

### Transfers (internal book transfers; also used by boa-payments to post)
- `POST /transfers` (201 Created) `{ "fromAccount": "104000123455", "toAccount": "104000123463", "amount": "250.00", "memo": "Rainy day", "idempotencyKey": "uuid" }`
  → `201 { "transferId": "TRF-20261125-000231", "status": "POSTED", "postedAt": "...", "businessDate": "..." }`
  Errors: `422 { "code": "NSF", "message": "Insufficient available funds" }`, `422 LIMIT_EXCEEDED`, `422 PRODUCT_LIMIT_NOT_CONFIGURED`, `404 ACCOUNT_NOT_FOUND`, `409 DUPLICATE`.
  - Every transfer is checked against the destination product's per-transfer limit (see Accounts), before funds and daily limits:
    `422 { "code": "LIMIT_EXCEEDED", "message": "Amount exceeds the per-transfer limit of 15000.00 for CC-VISA" }`.
    Customer-initiated transfers are also checked against the source account's daily transfer limit (`422 LIMIT_EXCEEDED`).
  - A destination product with no `product_limit` row fails closed: `422 PRODUCT_LIMIT_NOT_CONFIGURED`.
  - Card payment example: `{ "fromAccount": "104000123463", "toAccount": "440000528719", "amount": "250.00" }` → `201`.
- `GET /transfers/{transferId}` → transfer record.

### Ops
- `GET /ops/health/slo` (OPS) → per-endpoint p95 latency and error rate for the last 15 minutes.

## boa-payments (`:8081/api/v1/payments`)
- `GET /payees?cif=100000001` → `[ { "payeeId": "PY-1001", "name": "Con Edison", "category": "UTILITY", "accountRef": "****7781" } ]`
- `POST /bill` `{ "cif", "fromAccount", "payeeId", "amount", "sendOn" }` → `201 { "paymentId": "BP-...", "status": "SCHEDULED" }`
- `POST /p2p` `{ "cif", "fromAccount", "recipient": { "email" | "phone" }, "amount", "memo" }` → `201 { "paymentId": "P2P-...", "status": "SENT" }`
- `GET /history?cif=100000001` → `[Payment]`
Payments posts money movement through core-banking `POST /transfers` (to the GL-SUSP suspense account) and shares `boa-common` (Money, AccountNumbers, BankDates) with core-banking.

## statements-service (`:8082/api/v1/statements`)
- `GET /{accountNumber}?month=2026-10` → `{ "accountNumber", "period", "openingBalance", "closingBalance", "lines": [Transaction] }`
- `GET /{accountNumber}/pdf?month=2026-10` → `application/pdf`
