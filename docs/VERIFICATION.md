# Verification � 2026-10-30

Executed on this Windows workspace:

- `mvn -f backend/pom.xml test -q`: PASS, 19 tests, zero failures/errors (5 rules, 13 integration/security/coverage, 1 documentation-disabled test).
- Flyway V1 migration executed against H2 in MySQL compatibility mode in integration tests.
- Generated OpenAPI verified to contain every implemented /api controller mapping; twelve groups verified, including filtered Money Transfers group.
- Access tests: anonymous/customer cannot read documentation; administrator needs MFA; YAML documentation also protected; mutations require CSRF; customer operations reject administrator role.
- Default-disabled docs tested to return 404 even for authenticated/MFA administrator.
- Database-backed transfer test: $800 initial funding, $200 transfer, $600 sender/$200 recipient, zero-sum postings, same-key retry returns existing transaction.
- Insufficient-funds transfer rejected. JSON response test verifies account fields and excludes password hashes/private image paths.
- `npm run build`: PASS, TypeScript compilation and Vite production bundle (30 modules). Build used installed Node 18.13.0; React Router warns it requires Node 20+. Use Node 20+ as specified for local use.

Not executed: clean MySQL runtime deployment, actual SMTP/email MFA delivery, live Swagger UI browser interaction, full end-to-end banking scenario, iPhone/Android installation, external FEMA API, Cloudflare deployment, Selenium, REST Assured, TestNG, JMeter or concurrency/load testing. H2 tests do not certify MySQL locking behavior.

Backend test reports are generated under backend/target/surefire-reports. Logs and caches are excluded from source packages. Re-run tests after dependency or domain changes. The final bundled JAR includes the successful React production build.

Packaging verification: `mvn -f backend/pom.xml -DskipTests package -q` passed after the test suite. The executable Spring Boot JAR includes React assets. The ZIP was opened and checked for the JAR, source tests, lockfile and setup documentation; no node_modules, dependency caches, logs or live credentials are included.

Alignment verification: the new regression test checks project version, public authentication security metadata, session-only authentication metadata and employee assignment grouping. Frontend rebuilt successfully with the conditional owner Swagger link. Jenkins pipeline changes were inspected but Jenkins was not run.
