# REST API examples

All non-GET requests require the CSRF header. In the SPA, this is handled by `frontend/src/api.ts`. Swagger requires an ADMIN session plus MFA, and POSTs may require the CSRF token in the Try it Out tool. Same-origin cookies are required.

OpenAPI: `GET /v3/api-docs` and `http://localhost:3000/swagger-ui/index.html`.

## Authentication

- `GET /api/auth/csrf` → `{token,header}`
- `POST /api/auth/register` JSON `{fullName,email,password,phone,street,city,stateCode,zipCode,referralCode}`
- `POST /api/auth/verify-email` JSON `{email,code}`
- `POST /api/auth/login` form-encoded `email=...&password=...`, include `X-XSRF-TOKEN` and session cookie
- `POST /api/auth/mfa/verify` JSON `{code}`; OTP is in MailHog at `http://localhost:8025`
- `GET /api/auth/me`, `POST /api/auth/logout`
- `POST /api/auth/password/request`, `POST /api/auth/password/reset`
- `POST /api/auth/phone/request`, `POST /api/auth/phone/verify` **only SIMULATED phone proof via email**

## Customer routes

- `GET /api/customer/dashboard` and `/api/customer/accounts`
- `POST /api/customer/applications` `{"accountType":"CHECKING"}`
- `GET /api/customer/accounts/{id}/postings`
- `POST /api/customer/transfers` `{"fromAccountId":1,"toAccountId":3,"amount":100,"requestKey":"transfer-unique-0001"}`
- `POST /api/customer/transfers` `{"fromAccountId":1,"recipientEmailOrPhone":"customer2@atlas.test","amount":50,"requestKey":"transfer-unique-0002"}`
- `POST /api/customer/checks` `multipart/form-data`: `accountId`, `checkNumber`, `amount`, `instant`, `front` and `back` image files
- `GET /api/customer/checks` and `GET /api/customer/checks/{id}/{front|back}`
- `POST /api/customer/bills` `{"accountId":1,"biller":"Demo Water Company","reference":"BILL-TEST-1","amount":30,"scheduledOn":"2026-11-05","requestKey":"bill-unique-0001"}`
- `POST /api/customer/advances` `{"accountId":1}`
- `GET /api/customer/referrals`, `/api/customer/notifications`, `/api/customer/preferences`, `/api/customer/loans`, `/api/customer/deferrals`
- `PUT /api/customer/preferences/{accountId}` JSON `{"lowBalance":100,"largeTransaction":500,"emailEnabled":true}`

## Administrator

- `GET /api/admin/overview`, `/customers`, `/employees`, `/applications`, `/checks`, `/audits`
- `POST /api/admin/applications/{id}/approve` or `/reject`
- `POST /api/admin/checks/{id}/review` JSON `{"approve":true}`
- `POST /api/admin/payroll` JSON `{"accountId":1,"amount":800,"employer":"Fictional Employer"}`
- `POST /api/admin/employees` JSON `{"fullName":"Demo Employee","email":"new.employee@atlas.test","password":"ChangeMe-Employee-123!"}`
- `POST /api/admin/assign` JSON `{"employeeId":2,"customerId":3}`
- `POST /api/admin/loans` JSON `{"customerId":3,"name":"Personal loan","amountDue":300,"dueOn":"2026-12-01"}`
- `POST /api/admin/fema/sync`, `POST /api/admin/fema/evaluate`
- `POST /api/admin/fema/verify-county/{userId}` JSON `{"countyFips":"36061"}`
- `POST /api/admin/demo/generate?count=100`: add fictional customers only when `DEMO_SEED=true`.
- `POST /api/admin/demo/payroll-history?accountId=1&amount=800`: create 4 fictional weekly payroll records and matching ledger credits to qualify for the simulated advance; **development only**, not a real payroll feature.

## Employee

- `GET /api/employee/customers` — assigned customers only
- `GET /api/employee/checks` — assigned customer checks only
- `POST /api/employee/checks/{id}/review` JSON `{"approve":true}`

All `/api/customer` endpoints require the CUSTOMER role; admin/employee endpoints have role restrictions. Account IDs must also be owned by the authenticated customer, and employees can only review assigned customers.
