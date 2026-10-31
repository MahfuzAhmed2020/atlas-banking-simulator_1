# Master prompt status

This is a runnable extension, not a claim that all 36 sections are complete. The supplied prompt is retained as MASTER_REQUIREMENTS.md. Its numbering jumps from 28 to 30; Swagger is tracked as section 29 based on the existing project's documentation.

| Section | Current implementation | Still required |
| --- | --- | --- |
| 1 Objective | Existing modular Spring/React simulator extended in place | Complete specialist modules and full acceptance verification |
| 2 Stack | Java 21, Spring/JPA/Security/Mail, React/TS/Vite, MySQL/Flyway, OpenAPI; PDFBox added | libphonenumber, Datafaker, OCR adapters, full QA toolchain; dependency support/security upgrade review |
| 3 Roles | Customer/admin/employee screens and server ownership/assignment rules | Branch manager/teller/fraud/support permission matrix and dual approvals |
| 4 Onboarding | Registration, email MFA/verification, password reset, basic address fields | Full identity/profile fields, ZIP/geocoder adapters, robust phone normalization/unique ownership |
| 5 Accounts | Checking/savings/business, applications, atomic journals; money market/CD type applications | Joint ownership, specialist credit products, interest/maturity, holds/available balance and restrictions engine |
| 6 Dashboard | Accounts, notifications and service navigation | Complete pending activity/card/loan/eligibility summaries |
| 7 Checks | Front/back upload, manual review, duplicates, approval-gated instant fee/standard release; settlement row lock added | Tesseract OCR, quality/endorsement validation, configurable cutoff, richer review/state/reversal workflow |
| 8 Transfers | Internal verified-recipient transfers; scheduled/weekly/monthly transfers added | Simulated external/ACH account verification, reversals, risk engine, configurable daily limits |
| 9 Bills | One-time and future bills; saved billers, weekly/monthly recurrence, pending cancellation and receipts added | Complete saved-biller selection/editing, retry policy and reconciliation/reversal tooling |
| 10 Advances | Existing $100 eligibility, payroll-linked repayment; confirmation PDF added | Configurable terms, manual decisions and more complete repayment policy/tests |
| 11 Referrals | Existing codes/reward posting; paid-reward letter added | Configurable qualification/anti-abuse rules and pending sponsor reconciliation |
| 12 FEMA | Existing optional ingestion/admin county verification/deferrals; decision letter added | Census geocoding, configurable rules and external-data/runtime tests |
| 13 Preferences | Account balance/large-transaction alerts; personal settings added | Complete category-specific per-account alert engine |
| 14 Notifications | Persisted notifications and test mail; explicit PDF attachments and delivery records added | Outbox/retries, full preference routing and template management |
| 15 Administration | Employee assignment, reviews, audits; service queues/product toggles/document tools added | Fine-grained roles, branch settings, configurable fees/limits, reports and fraud investigations |
| 16 Security | Sessions/CSRF/BCrypt/email MFA; encrypted TOTP, recognition/revocation and endpoint throttle added | Recovery codes, fresh transaction step-up, stronger risk/device binding, complete security review and negative tests |
| 17 Database | Additive V2-V4 migrations, foreign keys and indexes | Schema for specialist credit/card/fraud/joint ownership and production-scale retention |
| 18 Seed/scalability | Existing fictional customer generator up to 1000 | Datafaker integration, realistic dataset diversity and measured 100/1000-user load results |
| 19 Responsive/PWA | Existing shell, responsive service cards and safe-area mobile navigation | Real iOS/Android installation and accessibility audit |
| 20 Cloudflare | Existing private/Access setup guidance | Verify actual HTTPS deployment and protected services |
| 21 Pages | Existing screens plus cards/services/messages/settings/documents/security/schedules/billers/More/Pay/products/staff tools | Remaining specialist pages with working business workflows; screenshot references were not attached here |
| 22 APIs | New service/document/authenticator/session/product controllers in Swagger | Full DTO/validation coverage, pagination and uniform error contracts |
| 23 Tests | Expanded JUnit/MockMvc database/security/PDF/TOTP tests | REST Assured, Selenium/TestNG, JMeter, actual MySQL concurrency and browser/mobile end-to-end runs |
| 24 End-to-end scenario | Existing flows preserved; new workflows have automated integration checks | Execute entire scenario using MySQL, MailHog and mobile/browser clients |
| 25 Structure | Existing backend/frontend/docs/scripts/tests retained | Further domain module separation as specialist features grow |
| 26 Phased execution | Actual source, migrations, integrated screens and tests | Continue remaining phases with verified acceptance checks |
| 27 Deliverables | Source, API docs, Windows scripts, JAR and source ZIP workflow | Final acceptance artifacts for every unfinished module |
| 28 Definition of done | Partial | Full master-prompt completion has not been reached |
| 29 Swagger | Existing documentation covers implemented mappings, including new APIs | Specialist APIs and detailed examples as implemented |
| 30 Cards | Debit issuance review, activation, lock/unlock, lost/stolen/replacement, limits/controls, hashed PIN and notifications | Credit-card debt/settlement, merchant history/recurrence, wallet mock connection state, temporary limit approvals and dedicated fraud tooling |
| 31 Pay & Transfer | Working hub, activity, schedules/recurrence, bills, saved billers, cancellation and receipts | External account/ACH simulation and fresh transaction step-up |
| 32 More/support | Working service grid, persisted secure messaging and service requests, preference settings/help | Dedicated support chat/ticket assignment/status, configurable branches, specialist request resolution |
| 33 Advanced alerts | Existing threshold alerts and notification history | Full categorized account/card/credit/security/bill/program alert preferences and event dispatch |
| 34 UI/PWA | Green responsive shell, React Router, four customer navigation destinations, service forms and feedback | Complete visual/accessibility/device QA, skeletons/transitions and design-reference matching |
| 35 Products/services | Six deposit product types with enable/disable rules; application history/closure/request queue | Credit/loan product origination, beneficiary/joint ownership changes, disputes/stop-payment execution and restrictions |
| 36 PDFs | Branded statements/balance/history/direct-deposit info; eight receipt/letter sources, monthly job, snapshots/reconciliation, secure download/audit, explicit MailHog delivery records | Credit-card statements, amortization schedules, offers/promotions, all event-triggered generation, retry/backfill and full MySQL/SMTP acceptance tests |

Unavailable specialist products start disabled and cannot be enabled until their actual workflow is implemented. A tracked service request is not represented as a completed ownership change, refund, credit decision or payment reversal. Wallets and payment networks remain simulations/unavailable provider integrations.
