# Working in this repository

- Build with `mvn clean install` from the repo root (JDK 8, Maven 3.6+). It runs every module's tests
  and writes JaCoCo reports to `*/target/site/jacoco/`.
- Run a single module's tests with `mvn -pl core-banking -am test`.
- `nb-common` is a published library: `boa-payments` pins it by version. Any change to its public
  API or its Java/Spring baseline needs a matching change in `boa-payments`.
- `docs/API.md` is the contract for teller web, mobile and payments clients. Keep it in sync with
  controller changes in the same PR.
- Money is `BigDecimal` with two decimals and HALF_EVEN rounding, serialised as strings. Business
  dates are `America/New_York`; use `BankDates`, never the JVM default zone.
- Local credentials for the seeded users are listed in `docs/API.md`.
- CI is the `Jenkinsfile` (build, tests, coverage, OWASP dependency check).
