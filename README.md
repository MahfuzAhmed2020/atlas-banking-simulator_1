# Atlas Banking Simulator — Spring Boot + React + MySQL

> **Educational/test environment ONLY.** All accounts, check images, funds, payroll, billers and reward credits must be fictional. No ACH, Zelle, real SMS, real check clearing, actual KYC or production-grade financial provider is connected. Not safe for real banking customers or production financial operations.

## Latest implementation pass

Swagger/OpenAPI integration and build fixes are described in [docs/SWAGGER.md](docs/SWAGGER.md). See [docs/IMPLEMENTATION_STATUS.md](docs/IMPLEMENTATION_STATUS.md) for the phase-by-phase gap list. The source package is not completion of every master-prompt section.

For the bundled single-origin JAR, configure MySQL and MailHog, set `$env:DB_PASSWORD` to your local database password, then run `.\scripts\start-release.ps1`. Open http://localhost:3000 and complete owner MFA before opening Swagger. The local profile enables documentation; default/prod profiles disable it.

Rebuild the ZIP with `.\scripts\package.ps1`. The archive excludes credentials, uploads, caches and logs.

## What is implemented in this ZIP

**Backend (Java 21 / Spring Boot 3.4.5 / MySQL):**
- Role-based server-side authentication: OWNER/ADMIN, EMPLOYEE, CUSTOMER; form-login using secure sessions, CSRF tokens and an emailed 6-digit OTP challenge for MFA. BCrypt password hashes. Email verification, password reset and simulated phone verification are included (phone verification is via *email* in demo; not genuine SMS ownership proof).
- Dynamic customer registration, 3 seeded customers with 2 accounts each, seeded owner/employee and administrator-only generator for up to 1000 more fictional customers per call.
- Account applications, admin approval/rejection, account balance/statement postings and customer assignment to bank employees.
- Internal account and customer-to-customer transfers, including lookup by verified email and simulated verified phone identifier; transaction idempotency, locked account rows and balanced ledger journals. No real Zelle connection.
- Mobile check photo uploads (front + back JPEG/PNG), duplicate-image/check-number detection, admin/assigned employee review, instant after-approval credit with a $5 fee, and free next-business-day credit after approval. Federal holiday/weekend calendar approximation. **OCR is NOT implemented; review is manual.**
- Simulated payroll credits, $100 advance based on 4 weeks of payroll records, payroll-linked repayments, referral codes and one-time $500 bonus on first approved referred account if sponsor has an active account.
- One-time bill payments for fictional billers, due-date background processing, failed/paid statuses. No live external settlement or AutoPay.
- Loan obligations, OpenFEMA *optional* data ingestion, admin county-FIPS verification step and 30-day deferral under demo rules; in-app alerts, low-balance and large-transaction preferences, MailHog delivery, audit records.
- Swagger/OpenAPI at `http://localhost:3000/swagger-ui/index.html` after ADMIN login with MFA.

**Frontend:** responsive React + TypeScript + Vite, separate theme/navigation for customers, bank owner and employees, forms attached to REST API, email OTP login, account application, transfers, mobile check deposit, bill payments, notifications, alert preferences, referral code, admin check reviews, employees, payroll, loan/FEMA operations and audit history. Installable PWA shell with limited static-only cache.

## What is NOT finished / limitations

This is a **substantial Phase 1–4 source-code foundation, not the entire 29-section production application**. Further implementation and integration tests are needed before relying on all flows. In particular:
- React has ~20 working views, **not yet 45–55 fully built pages**. Separate credit-card management, business product settings, AutoPay, full loan lifecycle, electronic statements/PDF exports, customer support ticketing, multi-branch management, full fraud models and incident workflows are not built.
- Tesseract/OCR, explicit check image endorsements, photo quality checks, signature/MICR validation, source bank check clearing and sophisticated fraud detection are not built. Test check validation is based on image file type, duplicates and human review only.
- Only session + email OTP MFA is provided; authenticator TOTP, genuine phone SMS, verified device binding, passkeys, biometric login and identity vendor integration are not implemented.
- FEMA mapping does **not automatically geocode street addresses**. An administrator must supply an independently verified county FIPS and confirm the address; see `docs/FEMA.md`. API updates are optional, require internet, and remote schema changes could require maintenance.
- MailHog mail delivery is local-only. Failed MailHog messages are logged as warnings and in-app notifications persist. Full retry queues and production notifications are not built.
- Balance records are updated in the same SQL transaction as zero-sum journal postings for supported workflows. **This does not represent a regulator-audited or general ledger-complete banking accounting system.**
- The admin can generate historical *fictional* payroll for testing advances. It intentionally cannot support real payroll ingestion.
- Backend and frontend builds are now executed; see `docs/VERIFICATION.md` for current results and test boundaries. MySQL, mail delivery, real-device and Cloudflare testing are separate checks.
- For production, replace all example credentials, complete a threat model, add independent security review, stronger transaction controls, compliance policies, disaster recovery, real KYC, financial partner integration and robust monitoring.

## Requirements on Windows 11

- JDK 21, Java on PATH (`java -version`)
- Maven 3.9+ (`mvn -version`) and Node.js 20+ (`node -v`)
- MySQL Community Server 8.0/8.4 and MySQL Workbench installed directly on Windows
- MailHog Windows executable (no containers)
- VS Code, optional MySQL Workbench and Postman

## Quick start — Windows 11 (NO Docker)

**Install:** JDK 21, Maven 3.9+, Node.js 20+, MySQL Community Server + MySQL Workbench, and the Windows MailHog executable. Read **`docs/LOCAL_WINDOWS_NO_DOCKER.md`** for exact installation/setup instructions.

**Once, inside MySQL Workbench:** Connect to local MySQL as an administrator, run `scripts/setup-mysql-local.sql` to create the `atlas_bank` database and a dedicated development user. The backend then uses Flyway to create the actual tables.

**Terminal A — MailHog** (put the downloaded binary in `tools/` or specify `-ExePath`):

```powershell
cd atlas-banking-simulator
.\scripts\start-mailhog.ps1
```

**Terminal B — Spring Boot backend** (with MySQL Community Server running as a Windows service):

```powershell
cd atlas-banking-simulator
.\scripts\start-backend.ps1
```

**Terminal C — React frontend:**

```powershell
cd atlas-banking-simulator\frontend
npm install
npm run dev
```

Open **http://localhost:5173**. Vite proxies `/api` to Spring Boot on port `3000`. The MailHog test inbox is **http://localhost:8025**; database service is local port **3306**.

**Swagger:** `http://localhost:3000/swagger-ui/index.html` — for reliable administrator login/session and CSRF, use the single-origin build as shown below.

### Single-origin build (recommended for Swagger and Cloudflare)

```powershell
cd atlas-banking-simulator
.\scripts\build-single-origin.ps1
.\scripts\start-backend.ps1
```

Open **http://localhost:3000**. Stop any already-running backend instance first. The React production build is copied into Spring Boot static resources. Never expose ports 3306, 1025 or 8025 through Cloudflare.

## Demo accounts

| Role | Email | Password (local demo ONLY) |
|---|---|---|
| Bank owner | `owner@atlas.test` | `ChangeMe-Admin-123!` |
| Bank employee | `employee@atlas.test` | `ChangeMe-User-123!` |
| Customer | `customer1@atlas.test` | `ChangeMe-User-123!` |
| Customer | `customer2@atlas.test` | `ChangeMe-User-123!` |
| Customer | `customer3@atlas.test` | `ChangeMe-User-123!` |

After submitting the password, open MailHog to read the **six-digit login OTP**. All demo accounts have zero opening balance. Have an owner credit a checking account via the admin "Payroll simulation" view or generate 4 weeks using a documented demo endpoint. NEVER use these credentials or seed data in a deployed environment; set `DEMO_SEED=false` and override env passwords.

## Typical 10-minute test scenario

1. Open frontend as `owner@atlas.test`; fetch login code from MailHog and complete MFA.
2. Go to **Customers**; note customer IDs. An account ID is available after login as the customer in **My Accounts**.
3. Log out, sign in as `customer1@atlas.test`, navigate to **My accounts** (Checking ID usually 1, Savings 2), note account ID. All accounts start at $0.
4. Log in as OWNER; **Payroll simulation**, send a fictional `$800` deposit into customer1 Checking account ID.
5. Log in as customer1, transfer `$50` to `customer2@atlas.test` or move to own Savings account ID 2.
6. Upload `sample-data/test-check-front.png` and `sample-data/test-check-back.png` from your phone in **Deposit check**, select Instant `$5` or Standard Free, and submit a unique `TEST-XXXX` number.
7. Log in as OWNER, **Check approvals**, inspect and approve. The instant flow posts net `$495` for a `$500` check; standard waits until the next business day after approval.
8. Create a fictional bill payment, review notifications and change low-balance/large-transaction thresholds.
9. For advance eligibility, seed historic payroll using the administrator-only endpoint from Swagger or Postman (see `docs/API.md`) and request a $100 advance as customer.
10. Create test loan for customer and explore FEMA disaster sync with official OpenFEMA data (requires internet); do not assign verified county FIPS without evidence.

## Cloudflare HTTPS from a phone

**IMPORTANT:** Do not expose the raw quick tunnel on a public internet connection with seeded accounts, admin tools, or unmasked demo data. Use a named Cloudflare Tunnel with Cloudflare Access restricted to authorized tester email addresses and Spring Security in the app. Keep MailHog, MySQL and Swagger administrator-only/private. See `docs/CLOUDFLARE.md`.

For a local one-origin build, the tunnel's origin is `http://localhost:3000`. For safer phone testing, access your local IP on a private Wi-Fi through HTTPS or use a named access-restricted tunnel. No unsupported `--allowed-mail` cloudflared flag is provided.

## Database migration and audit

Flyway automatically executes `backend/src/main/resources/db/migration/V1__schema.sql`. JPA uses `ddl-auto=validate`; do not use `create-drop` or silently modify production schemas. Seeded users are inserted only when the database starts empty and `DEMO_SEED=true`.

## Development architecture

```text
Mobile PWA / browser
      |
React + TypeScript (Vite :5173 dev; Spring Boot :3000 production build)
      |
Spring Security session + MFA + CSRF + role and ownership checks
      |
Spring Boot controllers → domain workflows → ledger service
      |                          |
MySQL/Flyway/JPA             MailHog :1025 (local test mail)
      |
Admin-only integrations: OpenFEMA (optional)
```

See `docs/API.md`, `docs/FEMA.md`, `docs/CLOUDFLARE.md`, `docs/VERIFICATION.md` and `scripts/`.
