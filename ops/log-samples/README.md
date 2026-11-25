# Production log extracts

Sanitised extracts from the `core-banking` production cluster (boacore-app-01..04), kept for
incident reviews. Account numbers are masked to the last four digits; user names are tokenised.

| File | Window (America/New_York) |
|---|---|
| `core-banking-app01-2026-11-17.log` | 2026-11-17 16:00–20:30, normal payroll-eve traffic |
| `core-banking-app01-2026-11-18.log` | 2026-11-18 16:00–20:30, INC-4471 (teller console and mobile "transactions not loading") |
| `slo-snapshots-2026-11-18.jsonl` | `/api/v1/ops/health/slo` polled every 5 minutes during INC-4471 |
