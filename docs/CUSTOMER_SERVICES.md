# Customer services extension

Updated 2026-10-31. All workflows use fictional data and simulated funds.

## Run on Windows

Use Java 21, Maven, Node 20 or newer, MySQL and MailHog as described in the README. Build the frontend with `npm run build` in `frontend`, copy its output with `scripts/build-single-origin.ps1`, and package with `mvn -f backend/pom.xml package`. The release JAR includes the frontend.

Start the rebuilt release with `scripts/start-release.ps1` after stopping the previous application process. Flyway applies V2 (customer services), V3 (security sessions) and V4 (authenticator credentials) automatically. Back up an existing local test database before schema upgrades. No Docker is required.

## Customer workflows

- More links to account services, card management, messages, preferences, documents, security sessions and existing banking workflows.
- Pay & Transfer links to immediate transfers, scheduled/recurring transfers, bills and saved billers. Activity lists show transfers and bill payments. Cancel a pending bill occurrence to stop its recurrence.
- Schedules support ONCE, WEEKLY and MONTHLY. A business-day worker executes them through the existing locked, atomic transfer workflow. A failed schedule stops and notifies the customer; it never silently retries a debit. Recurrence advances from the prior scheduled date using calendar weeks/months. Month-end dates may shorten in shorter months.
- Bills support the same frequencies. A successful payment creates exactly one next occurrence in the same transaction. Execution locks the bill and funding account. Retries must match the original account, biller, reference, date, amount and frequency.
- Request a debit card for an active checking account. An administrator reviews the request under Service Requests. The issued card starts INACTIVE; its owner can activate, lock/unlock, mark lost/stolen, configure allowed controls and set a fictional PIN. Lost/stolen/replaced/closed cards cannot be unlocked. PINs use BCrypt and are excluded from JSON. No complete card number or CVV is generated or stored.
- Card controls are simulation metadata; merchant authorization, credit-card debt, wallet provisioning and merchant recurring charges are not implemented.
- Account closure requires a zero balance, no pending transfers/bills/checks and no outstanding advance. Administrator approval closes the account and associated cards. Other specialist requests are tracked with review notes, but are not automatic beneficiary, ownership, dispute-refund or stop-payment changes.
- Secure messages are stored in MySQL. A customer sees only their conversation. Employees need a customer assignment; administrators can review any customer. A support response sends a notification without emailing the message body.
- Language preference, accessibility preference and optional marketing consent persist in MySQL. The interface currently uses English; selecting a language preference does not claim a translated interface.

## Authenticator and session security

Email MFA remains the default. To enable authenticator enrollment, configure a random 32-byte Base64 key in `ATLAS_TOTP_KEY`. Generate one locally in PowerShell:

```powershell
$keyBytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($keyBytes)
$env:ATLAS_TOTP_KEY = [Convert]::ToBase64String($keyBytes)
$rng.Dispose()
```

Keep that value private and stable across application restarts. Do not commit it. Enrollment requires an existing MFA session plus a valid code from the newly provisioned authenticator. Secrets are encrypted with AES-GCM. Once enrolled, email codes cannot bypass TOTP at login. The verifier uses six-digit SHA-1 TOTP, adjacent time windows, persisted replay protection and a five-attempt window. Authenticator recovery/reset is not yet available; retain the authenticator and encryption key.

Security & Devices lists sessions recorded after MFA. Revoking a session prevents its next API request; password reset revokes all recorded sessions. Disabled users are rejected on subsequent API requests. A random HttpOnly recognition cookie identifies previously used browsers without bypassing MFA. Historical records may outlive an already expired/logged-out HTTP session. New-browser sign-ins send security notifications. Existing sessions established before this upgrade are not retroactively recorded.

Public credential/code endpoints have a bounded, in-memory per-connection-address/per-endpoint throttle. This is a single-process simulator control, not a distributed anti-abuse system. Proxy deployments may share a connection address.

## PDF documents

Apache PDFBox 3.0.8 generates branded, paginated PDFs. Documents include fictional customer identity, masked account identifiers and simulation notices. Supported account documents: historical statements, transaction reports, current balance letters and direct-deposit setup information. Additional receipt/letter generators use persisted transfer, completed bill, released check, advance, loan obligation, FEMA decision, paid referral and account records. Loan statements currently summarize obligations; they do not create an amortization schedule.

Before creating account statements, the generator locks the account and reconciles its cached balance with the sum of ledger postings. Opening balance and period transactions come from the ledger. PDF bytes and source text snapshots persist together. An existing document for the same account/type/period is returned unchanged, even if activity later changes. Balance letters are immutable daily snapshots taken at first generation, not continuously updated attestations.

The monthly job runs at 05:00 America/New_York on the first day of each month for the previous month. It skips accounts opened after that period. It generates documents and in-app notifications, not unsolicited email attachments. Missed monthly runs are not automatically backfilled; generate historical periods manually.

Customer, employee and administrator document screens use the same ownership/assignment checks. Downloads use `Cache-Control: no-store`. Viewing, generation and explicit MailHog delivery attempts are audited. Sending an attachment is explicit; the recipient is always the registered fictional customer, never an arbitrary request parameter. Delivery attempts record SENT or FAILED. SMTP acceptance is not proof the recipient read the email.

Credit-card statements, amortization schedules, configurable promotional/product offers, automatic event-triggered letters for every workflow, delivery retry queues and tamper-evident document retention remain incomplete.

## API inventory

See administrator Swagger for request schemas and all mappings:

- `/api/customer/services/cards`, `/cards/{id}`
- `/api/customer/services/requests`, `/messages`, `/settings`
- `/api/customer/services/billers`, `/billers/{id}`
- `/api/customer/services/schedules`, `/schedules/{id}`
- `DELETE /api/customer/services/bills/{id}`
- `/api/customer/services/products`, `/branches`
- `/api/employee/services/requests`, `/requests/{id}/review`, `/messages/{customerId}`
- `/api/admin/products`, `/api/admin/products/{id}`
- `/api/documents`, `/api/documents/receipts`, `/{id}/download`, `/{id}/email`, `/{id}/deliveries`
- `/api/auth/sessions`, `/api/auth/sessions/{id}`
- `/api/auth/authenticator`, `/enroll`, `/confirm`

CSRF is required for mutations. Banking/customer-service/document operations require MFA, plus role and object authorization. Authenticator enrollment and session management explicitly require MFA even though they are under the authentication route.

## Verification boundaries

JUnit/MockMvc tests cover the new schema with Hibernate validation, card lifecycle/privacy, ownership failures, messages, settings, bill idempotency and recurrence, schedule execution/cancellation, session revocation/disabled accounts, MFA/CSRF, PDF ledger reconciliation, immutable snapshots, receipt ownership and RFC 6238 test vectors. The statement PDF was rendered with PDFBox and visually inspected.

H2 in MySQL mode is not a substitute for MySQL concurrency testing. Live SMTP, authenticator-app enrollment, real-device PWA installation, Cloudflare Access and browser end-to-end checks require separate execution. See MASTER_PROMPT_STATUS.md for the full remaining scope.
