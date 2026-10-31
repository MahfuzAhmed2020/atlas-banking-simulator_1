# Swagger UI / OpenAPI 3

Spring Boot 3.4.5 uses springdoc-openapi-starter-webmvc-ui 2.8.8. The Springdoc compatibility matrix supports Boot 3.4 with springdoc 2.7�2.8: https://springdoc.org/v2/ . CSRF support: https://springdoc.org/v2/faq.html . Exact frontend resolutions are recorded in frontend/package-lock.json; install with npm ci on Node 20+.

## Local Windows launch

Start MySQL and MailHog as described in LOCAL_WINDOWS_NO_DOCKER.md. Build the same-origin React app:

```powershell
.\scripts\build-single-origin.ps1
$env:SPRING_PROFILES_ACTIVE = 'local'
.\scripts\start-backend.ps1
```

Open http://localhost:3000, sign in as the seeded owner, and finish email MFA using the local MailHog inbox. In the same browser, open http://localhost:3000/swagger-ui/index.html . JSON is http://localhost:3000/v3/api-docs . The group selector contains the twelve requested business groups. The owner dashboard also displays an API documentation link when the documentation endpoint is enabled and accessible. Authentication endpoints implemented by Spring Security filters are explicitly included; controller paths are discovered automatically.

Use Try it out with fictional data. Reads need the browser-managed session. Writes refresh /api/auth/csrf and send its returned token and header; Swagger's cookie CSRF support is also enabled. JSESSIONID remains HttpOnly. No bearer tokens, disabled CSRF, or localStorage authentication is used. A new requestKey is required for a new transfer; reusing it retries the same transfer.

Documentation access requires ADMIN plus completed MFA. The owner has ADMIN authority. EMPLOYEE and CUSTOMER sessions cannot browse documentation. Viewing the customer schema as an administrator does not grant permission to execute customer operations: they continue to return 403. Customer and assigned-employee workflows can be exercised through their role-specific React interface or Postman sessions. A separate developer impersonation facility is not implemented. Signing into another role through Swagger replaces the browser session and removes documentation access; refresh after logging back in as an administrator.

## Coverage and limits

All implemented /api controller mappings are checked against generated OpenAPI by an automated test. GET, POST, PUT, PATCH, and DELETE are represented. Operations contain permission metadata, error responses, generated request/response schemas and path/query parameters. Transfer examples and validation annotations show amount precision and idempotency rules. Multipart check uploads expose both image inputs. Remaining untyped Map payloads and some response fields still need richer handwritten examples and dedicated DTOs; these are not claimed to have exhaustive field-level documentation.

GET /api/customer/accounts/{id}/transactions supports page (zero based), size (1�100), and direction (ALL, CREDIT, DEBIT), returning newest-first page content and totals. Existing postings, notification and audit endpoints retain their fixed limits of 80, 60 and 100 respectively. Other list endpoints are currently unpaged; do not assume page/size filters exist on them. Existing account application/check queues filter by their pending status. Validation and business-rule failures are 400, missing authentication 401, denied role/ownership/MFA/CSRF 403, and database conflicts 409. Successful status codes come from Spring MVC mappings; assignment deletion returns 204.

Changes to mapped controllers, DTO records and annotated model fields are incorporated on application restart. Groups are selected by path in OpenApiDocumentation.category; update this policy when introducing a new business module. Annotation summaries and business descriptions require maintenance when rules change.

## Production and tunnels

Documentation is disabled in the default profile and in prod. The local profile enables it for development by default; set DOCS_ENABLED=false to disable it even with local active. Do not activate local in deployed environments. Explicitly setting DOCS_ENABLED=true outside local opts into documentation, but ADMIN/MFA access checks still apply. Prod forces Secure session cookies and disables demo seeding. Keep sensitive credentials in environment variables.

Before a tunnel, use a named Cloudflare Tunnel and a Cloudflare Access application restricted to approved testers. Prefer leaving docs disabled on tunnel instances. Do not tunnel MySQL or MailHog. Instructions are in CLOUDFLARE.md. No tunnel or public deployment was created during implementation.

## Automated verification

```powershell
mvn -f backend/pom.xml test
cd frontend
npm ci
npm run build
```

Tests cover anonymous/customer denial, administrator MFA, generated groups and controller coverage, CSRF enforcement, role isolation, documentation disabled, JSON serialization, ledger reconciliation, transfer retries and insufficient funds. Tests use H2 in MySQL mode and Flyway, not a substitute for MySQL integration or a live browser Swagger check.

If Windows blocks local PowerShell scripts, launch the individual script with a process-only execution-policy override, for example:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\start-release.ps1
```

This does not change the machine's persistent execution policy. Set environment variables in the parent terminal first. The startup script defaults to the local profile; production operators must explicitly select prod.
