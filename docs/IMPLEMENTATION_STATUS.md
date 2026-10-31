# Current scope

The 2026-10-31 extension is documented in [MASTER_PROMPT_STATUS.md](MASTER_PROMPT_STATUS.md) and [CUSTOMER_SERVICES.md](CUSTOMER_SERVICES.md). The original pass below is historical and does not include the new modules.

# Phase status and remaining work

The pre-existing project contains functional source for identity/email OTP, accounts, double-entry postings, internal transfers, manually reviewed check deposits, one-time/scheduled bills, demo payroll, advances, referrals, FEMA county matching, notifications, customer alerts and employee/admin operations. React provides approximately twenty integrated views. See README for the existing business limitations.

This implementation pass adds Swagger grouping, security and documentation, fixes backend compilation and frontend TypeScript, fixes JSON response serialization, adds paged/filterable ledger history, adds employee activation and assignment removal APIs, preserves failed OTP attempt counters independently of controller rollback, and adds integration tests and packaging. These changes do not complete every master-prompt module.

| Phase | Implemented foundation | Remaining to satisfy the full prompt |
|---|---|---|
| 1 Foundation | Spring Boot, React, MySQL migration, MailHog settings, health, optional Compose | Verify a clean MySQL/Windows install and mail delivery |
| 2 Identity | BCrypt, sessions, CSRF, email OTP, registration/reset, role dashboards | Authenticator TOTP, separate phone delivery, device/session management, rate limiting/lockout, full onboarding/address adapters |
| 3 Core banking | Accounts/applications, atomic ledger, internal transfers, transaction history | All eleven products, joint ownership, holds/current vs available, scheduled/recurring/external transfers, full lifecycle |
| 4 Advanced | Manual check approval and fees, standard deposit scheduling, one-time bills, simulated payroll | OCR/quality checks, complete holiday/cutoff policy, biller CRUD, AutoPay/recurrence/cancellation/retries |
| 5 Programs | Demo $100 advances, $500 referrals, cached FEMA ingestion/deferrals, alert thresholds | Configurable approval/terms engines, full credit repayments, Census geocoder/ZIP/libphonenumber adapters, notification retry/outbox |
| 6 Administration | Employee/customer lists/assignment, review queues, audits | Fine-grained employee permissions, dual approvals, settings/products/limits, fraud investigations, full reports/support cases |
| 7 Mobile | Responsive UI, camera file inputs, manifest/service worker, Cloudflare instructions | Real-device/PWA install verification, Cloudflare Access deployment verification |
| 8 QA/delivery | Maven tests, React build, Postman collection, Jenkinsfile, source ZIP script | REST Assured, Selenium/TestNG and JMeter frameworks, 100/1000 customer performance results, concurrent MySQL transaction tests, comprehensive business tests |
| 29 Swagger | All controller mappings, twelve groups, security filters documented, permissions/errors, CSRF requests, production defaults | Complete DTO replacement and field-specific examples for every Map payload, developer/customer Swagger testing mechanism with appropriate restricted access, live-browser verification |

No empty pages were added to inflate the page count. No real funds or external settlement are supported. Do not interpret this deliverable as completion of all 29 sections.

Project alignment: the owner dashboard discovers documentation availability before showing its Swagger link. Public authentication operations are marked public in OpenAPI, while session-only authentication endpoints retain their security requirements. Employee assignment deletion is grouped with employee management. OpenAPI uses the existing project version 0.1.0. The local profile honors DOCS_ENABLED=false. Build scripts use the committed npm lockfile; Jenkins builds fresh frontend assets into Spring Boot before packaging the JAR and ZIP. Jenkins itself has not been executed here.
