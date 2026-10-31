# MASTER DEVELOPMENT PROMPT
## Full-Stack Digital Banking Management System

### 1. Project Objective

Act as a senior full-stack software engineer, banking systems architect, security engineer, database architect, UI/UX designer, and QA automation engineer.

Build a complete, fully integrated, responsive digital banking simulation application using Java Spring Boot, React, TypeScript, HTML, CSS, JavaScript, and MySQL.

The application must support realistic banking business workflows using fictional customers, simulated funds, and test financial information.

This is not a static website or a collection of disconnected demo pages. Every implemented feature must connect to the backend, database, authorization rules, and relevant business logic.

Develop the system as a modular monolith initially, with a structure that can be separated into microservices later.

The application must run locally on Windows 11 using VS Code and be accessible from mobile devices through HTTPS using Cloudflare Tunnel.

Use a modern, professional banking design with separate role-based dashboards.

**Important:** This is a banking simulator, not a real financial institution. Never execute actual bank transfers, negotiate checks, or process real customer funds. Real financial integrations must remain disabled unless separately authorized and implemented.

---

### 2. Required Technology Stack

**Backend**
- Java 21
- Supported stable Spring Boot version
- Spring Web
- Spring Data JPA / Hibernate
- Spring Security
- Spring Validation
- Spring Mail
- Spring Scheduling
- Maven
- RESTful APIs
- OpenAPI / Swagger
- Database migrations with Flyway

**Frontend**
- React
- TypeScript
- Vite
- HTML5
- CSS3
- Responsive design
- React Router
- Accessible forms and navigation
- Progressive Web App (PWA)

**Database**
- MySQL Community Server
- MySQL Workbench
- Relational database design
- Transactional integrity and foreign keys
- Database migrations and seed data

**Integrations**
- MailHog for test email delivery
- Cloudflare Tunnel for HTTPS remote access
- OpenFEMA API
- Zippopotam.us ZIP code lookup
- U.S. Census Geocoder for address-to-county matching
- Google libphonenumber for telephone validation
- Tesseract OCR for simulated check recognition
- Java Datafaker for fictional test customers

**Security**
- Spring Security
- Role-based access control (RBAC)
- Secure server-side sessions
- HttpOnly, Secure, SameSite cookies
- CSRF protection
- MFA with authenticator-app TOTP and test email OTP
- Password hashing
- Session expiration
- Device recognition and risk-based authentication
- Audit logging

**Testing**
- JUnit 5
- Mockito
- REST Assured
- Postman collections
- Selenium WebDriver
- TestNG
- Maven
- Jenkins CI/CD
- JMeter for performance testing

Use environment variables for credentials and sensitive configuration. Do not hardcode passwords, secrets, or real personal information.

---

### 3. Application Users and Roles

Create separate interfaces with distinct themes, navigation, and permissions.

**A. Bank Owner / Super Administrator**

The bank owner must be able to:
- View the entire banking system.
- Create, update, disable, and manage employees.
- Assign employee roles and permissions.
- Assign customers to employees.
- View customer accounts.
- Review account applications.
- Manage checking, savings, and business account products.
- Manage configurable banking fees and limits.
- Review mobile check deposits.
- Monitor internal transfers.
- View bill payments.
- Manage referrals and cash advances.
- Configure FEMA deferral rules.
- Review fraud alerts.
- Access financial reports and audit logs.
- Monitor system operations.

**B. Bank Manager / Employee**

Create employee roles, including:
- Branch Manager
- Account Manager
- Bank Teller
- Fraud Analyst
- Customer Support

Employees must see only the functionality and customer records authorized by their roles and assignments.

Allow authorized employees to review account applications, verify customer documentation, review check deposits, resolve customer issues, and investigate suspicious transactions.

Require additional approval for sensitive administrative operations where appropriate.

**C. Customer / Account Holder**

Customers must be able to:
- Register and log in.
- Complete MFA.
- View and manage their profile.
- Apply for banking accounts.
- Access checking, savings, and business accounts.
- View balances and transactions.
- Transfer funds.
- Make simulated Zelle-style transfers.
- Deposit test checks with their phone camera.
- Pay bills.
- Manage simulated cards and loans.
- Request eligible cash advances.
- Refer friends.
- View FEMA disaster relief decisions.
- Set custom alerts.
- Review notifications and statements.
- Manage their recognized devices and sessions.

Enforce authorization in Spring Boot, not only by hiding frontend navigation.

---

### 4. Customer Registration and Information

Create a complete customer onboarding workflow.

**Customer information fields:**
- Customer ID
- First name
- Middle name
- Last name
- Date of birth
- Email address
- Mobile phone number
- Alternate phone number
- Residential address
- Mailing address
- City
- State
- ZIP code
- Country
- Employment status
- Employer name
- Occupation
- Annual income
- Identity verification status
- Account creation date
- Customer status
- Assigned employee or branch

For simulated identity verification, use fictional identity data only. Do not require actual SSNs or genuine government identification documents.

**Validation requirements:**
- Validate email format.
- Verify email ownership through MailHog.
- Validate U.S. phone number format.
- Provide simulated phone OTP verification locally.
- Support optional real SMS integration through a separately configured provider.
- Validate ZIP code, city, and state.
- Use geocoding where available to help verify address information.
- Prevent duplicate customer registration.
- Apply configurable password rules.
- Record verification status.

Remember that address or phone format validation does not establish identity or ownership.

---

### 5. Customer Account Management

Support these account types:
1. Checking
2. Savings
3. Business Checking
4. Business Savings
5. Money Market
6. Certificate of Deposit
7. Credit Card
8. Personal Loan
9. Auto Loan
10. Mortgage
11. Joint Account

Allow administrators to configure which account products can be opened.

**Account lifecycle:**

Customer submits application → Administrator or authorized employee reviews → Application approved or rejected → Account created → Customer receives notification → Account appears on dashboard.

Support:
- Multiple accounts per customer
- Unique account identifiers
- Account balances
- Account status
- Available versus current balance
- Pending transactions
- Account restrictions
- Statements
- Account closure workflow
- Account ownership and joint-account access

Use a double-entry accounting ledger to record financial transactions. Do not implement transfers by independently modifying two customer balance fields.

---

### 6. Customer Dashboard

Create a professional banking dashboard with:

- Welcome message
- Customer profile access
- Checking account balances
- Savings account balances
- Business account balances
- Recent transactions
- Pending deposits
- Money transfer shortcut
- Deposit check shortcut
- Bill payment shortcut
- Credit card summary
- Loan summary
- Referral rewards
- Cash advance eligibility
- FEMA disaster relief messages
- Security alerts
- Notification center
- Account preferences

The dashboard must be responsive and display correctly on desktops, tablets, iPhones, and Android devices.

---

### 7. Mobile Check Deposit System

Implement a complete simulated mobile check-deposit workflow.

**Customer deposit page:**

The customer must:
1. Select checking or eligible savings account.
2. Enter the check amount.
3. Capture or upload the front image of the check.
4. Capture or upload the back image.
5. Choose deposit processing speed.
6. Review fees and expected availability.
7. Confirm the deposit.
8. Receive a tracking number.
9. Monitor deposit status.

**Deposit processing choices:**

Option A — Instant Deposit
- Processing fee: $5.00
- Funds available immediately after successful verification and required approval.
- Display the fee before confirmation.
- For a $500 check, show $495 net funds if the fee is deducted from proceeds.
- Charge the fee only when the instant-deposit transaction successfully completes.

Option B — Standard Deposit
- Fee: $0.00
- Funds become available in approximately 1–2 business days after approval, according to the simulated cutoff and holiday rules.
- Display the estimated availability date before confirmation.

**Verification:**
- Validate uploaded image formats and file size.
- Capture front and back images.
- Use OCR to extract simulated check information.
- Compare the extracted amount with customer-entered amount.
- Check duplicate check numbers and image fingerprints.
- Validate ownership and deposit eligibility.
- Detect incomplete or unreadable images.
- Flag suspicious deposits.
- Support manual administrator review.

Provide a fictional test-check generator using clearly marked VOID / TEST ONLY images and invalid test banking identifiers. Never require actual negotiable checks.

**Administrator functionality:**
- View pending deposits.
- Inspect front and back images.
- Review extracted check information.
- Approve or reject deposits.
- Add review notes.
- View fraud indicators.
- Review deposit history.

**Deposit statuses:**
- Submitted
- Verification Pending
- Under Review
- Approved
- Rejected
- Processing
- Funds Available
- Reversed

Do not release simulated funds before the required verification and approval conditions are satisfied.

Implement business-day calculations, cutoff times, and U.S. banking holidays.

Notify the customer through MailHog and the dashboard.

---

### 8. Money Transfer System

Implement these transfer types:
- Checking to Savings
- Savings to Checking
- Customer-to-customer internal transfer
- Business account transfers
- Transfers using registered email addresses
- Transfers using registered phone numbers
- Scheduled transfers
- Recurring transfers
- Simulated external bank transfers

**Zelle-style transfer simulation:**

Customer enters a verified recipient email or phone number → System identifies the eligible recipient → Customer enters an amount → System checks balance, account status, limits, and risk indicators → Customer confirms → Transfer posts atomically → Both customers receive notifications.

Do not claim official Zelle integration or use Zelle branding as an authorized banking service.

Required business rules:
- Sufficient available balance
- Transaction limits
- Recipient validation
- Duplicate request prevention
- Idempotency keys
- Atomic database transactions
- Appropriate concurrency locking
- Transfer status history
- Failure and reversal handling
- Fraud flags
- Audit trail

---

### 9. Bill Payment System

Customers must be able to:
- Add a biller.
- Store biller account information.
- Choose a funding account.
- Make one-time payments.
- Schedule future payments.
- Configure recurring payments.
- Enable AutoPay.
- View scheduled payments.
- Cancel eligible scheduled payments.
- View payment history.
- Download receipts.
- Receive payment notifications.

Include sample fictional billers for:
- Electricity
- Water
- Gas
- Internet
- Mobile phone
- Insurance
- Credit card payments

Use simulated billers and payments. Do not claim payments are delivered to real utility companies.

Implement payment status, retries, duplicate prevention, and scheduled processing.

---

### 10. $100 Early Cash Advance

Create a bank-funded simulated cash-advance feature.

**Example eligibility rules:**
- Active checking account.
- At least four consecutive qualifying weekly direct deposits.
- Good account history.
- No unresolved negative balance.
- No unpaid previous cash advance.
- No disqualifying account restrictions.
- Maximum advance of $100.

These conditions must be configurable by authorized administrators.

**Workflow:**

Customer requests $100 → Eligibility engine evaluates direct-deposit history → System displays approval or rejection and the applicable terms → Customer accepts repayment terms → Simulated bank-funded advance is credited to checking → Outstanding advance is recorded → Repayment is collected according to the customer's authorization and agreed schedule.

The advance must not be funded by subtracting $100 from the customer's existing balance.

Track:
- Advance request
- Eligibility decision
- Approval
- Outstanding balance
- Repayment schedule
- Repayments
- Overdue status
- Notifications
- Audit history

Handle missing payroll deposits and insufficient repayment funds safely.

---

### 11. $500 Customer Referral System

Create a referral program.

**Workflow:**

Customer A receives a unique referral code → Shares it with Customer B → Customer B registers → Customer B opens an eligible account → Customer B completes qualifying conditions → System evaluates eligibility → Approved simulated $500 bonus is credited to Customer A.

Support:
- Unique referral codes
- Referral links
- Referral tracking
- Referral status
- Qualification requirements
- Referral history
- Bonus transaction history
- Self-referral prevention
- Duplicate reward prevention
- Admin review
- Fraud checks
- MailHog notifications

Provide a dedicated My Referrals page for customers and a referral-management page for administrators.

---

### 12. FEMA Disaster Relief and 30-Day Deferral

Integrate the official OpenFEMA Disaster Declarations API.

The application must automatically retrieve FEMA disaster declarations on a configurable schedule.

**Required workflow:**

1. Retrieve new FEMA disaster declarations.
2. Store disaster numbers, affected geographic areas, dates, and assistance information.
3. Match customers using verified location information and county FIPS codes.
4. Evaluate the bank's approved disaster-relief policy.
5. Identify eligible loan or payment obligations.
6. Determine whether a 30-day deferral can be granted.
7. Apply the deferral once per qualifying obligation and disaster.
8. Update affected payment schedules.
9. Create an audit trail.
10. Notify the customer through MailHog and the in-app notification center.
11. Display an approved deferral message on the customer dashboard.

**Important eligibility constraints:**

A FEMA disaster declaration does not automatically authorize a banking payment deferral.

Do not rely exclusively on ZIP code because ZIP codes can span multiple counties.

Use address-to-county matching and the relevant designated disaster area. Refer uncertain matches for review.

The simulated bank policy should initially support 30-day deferrals for eligible loan and mortgage payments. Allow the bank owner to configure additional eligible products.

Prevent repeated extensions and duplicate decisions.

Include:
- FEMA API failure handling
- Cached disaster data
- Manual review
- Approval status
- Deferral history
- Audit logging
- Customer notification history

---

### 13. Customer Preferences and Account Alerts

Create a Customer Preferences & Alerts page.

Allow individual notification settings for each account.

**Low balance alerts:**

Customers can choose thresholds such as:
- $1
- $50
- $100
- $500
- Any valid custom amount

**Transaction alerts:**
- Large withdrawals
- Large deposits
- High-value transfers
- Failed transactions
- Returned deposits
- Account restrictions

**Security alerts:**
- Unusual account activity
- Suspicious transfers
- New device login
- Repeated failed login attempts
- Password changes
- MFA changes

**Other alerts:**
- Check deposit approvals
- Deposit rejections
- Bill payment confirmations
- Referral bonuses
- Cash advance updates
- FEMA deferrals

Notification channels:
- Email
- In-app notifications
- Optional future push notifications

Provide email notifications through MailHog during local development.

Critical security notifications must not depend solely on optional customer alert preferences.

---

### 14. Email and Notification System

Integrate MailHog with Spring Boot.

**Development settings:**
- SMTP host: localhost
- SMTP port: 1025
- MailHog web interface: http://localhost:8025

Create reusable email templates for:
- Customer registration
- Email verification
- Login security notifications
- Password reset
- Account approval/rejection
- Check deposit confirmation
- Check approval/rejection
- Money transfer confirmation
- Bill payment notification
- Referral bonus
- Cash advance
- FEMA disaster relief
- Suspicious activity
- Low balance notifications

Store notification history in MySQL.

Implement reliable delivery attempts and error logging.

For local phone verification, create a simulated SMS OTP delivery mechanism separate from MailHog. Real SMS must require a separately configured provider.

---

### 15. Administrator and Employee Management

The bank owner must have a dedicated dashboard.

Required modules:
- Employee management
- Customer management
- Customer assignments
- Account opening approvals
- Check deposit approvals
- Transaction monitoring
- Fraud investigations
- Bill payment monitoring
- Loan management
- Cash advance monitoring
- Referral management
- FEMA relief management
- Bank fees and limits
- Reports
- Audit logs
- System settings

Employee records must include:
- Employee ID
- Name
- Email
- Role
- Department
- Branch
- Employment status
- Assigned customers
- Account creation date
- Last login
- Permission history

Support employee activation, deactivation, and role updates.

Ensure an employee cannot independently grant themselves additional privileges.

---

### 16. Application Security

Implement security suitable for a realistic banking simulation.

Required:
- Secure password hashing
- Secure session cookies
- Session inactivity timeout
- MFA
- Role-based authorization
- Resource-level authorization
- CSRF protection
- Input validation
- Rate limiting
- Login attempt limits
- Account lockout controls
- Audit logs
- Secure image upload processing
- Sensitive data encryption where appropriate
- Masked account identifiers
- Secure error responses
- Device recognition
- Concurrent-session management
- Session invalidation on logout

Use privacy-conscious device recognition rather than invasive fingerprinting.

Do not store authentication tokens in browser localStorage.

Protect all customer and employee data from unauthorized access.

Never expose passwords, OTP secrets, MySQL credentials, or MailHog through public application routes.

---

### 17. Database Design

Create a properly normalized MySQL schema.

Include tables or equivalent entities for:

**Identity and Security**
- users
- roles
- permissions
- user_roles
- customer_profiles
- addresses
- identity_verifications
- employee_profiles
- employee_customer_assignments
- sessions
- recognized_devices
- mfa_credentials

**Banking**
- account_products
- bank_accounts
- account_owners
- account_applications
- ledger_accounts
- journal_entries
- ledger_postings
- financial_transactions
- account_holds
- transaction_limits

**Deposits and Transfers**
- check_deposits
- check_images
- deposit_reviews
- transfers
- transfer_recipients
- direct_deposits

**Payments and Credit**
- billers
- bill_payments
- scheduled_payments
- loans
- loan_payments
- credit_cards
- card_transactions
- cash_advances
- advance_repayments

**Additional Features**
- referrals
- referral_rewards
- fema_disasters
- disaster_area_mappings
- deferral_decisions
- payment_deferrals
- alert_preferences
- notifications
- audit_logs

Define primary keys, foreign keys, indexes, constraints, transaction boundaries, and data retention considerations.

Use DECIMAL for financial values, never floating-point arithmetic for money.

Use transactional ledger posting, idempotency, and concurrency control to prevent inconsistent balances.

---

### 18. Seed Data and Scalability

Initially generate:
- 1 Bank Owner
- 1 Branch Manager
- 2 Bank Employees
- 3 Customers
- Checking and Savings accounts
- Sample transactions
- Simulated direct deposits
- Test checks
- Sample billers
- Loan and referral test scenarios

Use Datafaker to generate fictional customer data.

Create database seed scripts that can generate:
- 3 customers
- 100 customers
- 1,000 customers
- Additional configurable amounts

Never manually create separate code for each customer.

Use pagination and indexing for large record collections.

Provide performance tests for 100 and 1,000 customer scenarios.

---

### 19. Responsive UI and Progressive Web App

Create one React application with role-specific layouts.

**Desktop:**
- Sidebar navigation
- Top navigation
- Search
- Banking dashboards
- Transaction tables
- Administrative reports

**iPhone and Samsung:**
- Responsive mobile banking layout
- Touch-friendly buttons
- Bottom navigation for customers
- Mobile account cards
- Camera access for check deposits
- Responsive forms
- Mobile notifications

**PWA:**
- Web app manifest
- App icon
- Installable home-screen application
- Standalone app display mode
- Appropriate service worker
- HTTPS support
- Responsive loading and error states

The app must work as a responsive website in a regular browser and offer an app-like experience when installed.

Do not cache sensitive banking information, account balances, or authentication data for offline use.

---

### 20. Cloudflare Integration

Support temporary secure remote access to the localhost application.

Required:
- Configure the React production build and Spring Boot API to be served under one origin where practical.
- Run the application locally.
- Configure Cloudflare Tunnel for HTTPS.
- Document quick-tunnel development testing.
- Document a named Cloudflare Tunnel with Cloudflare Access for restricted tester access.
- Restrict access to approved testers where possible.
- Do not expose MySQL, MailHog, development consoles, or administrative debugging endpoints.

Cloudflare Access must be configured using supported Cloudflare functionality; do not invent unsupported command-line flags.

Spring Security remains responsible for application-level authorization.

Provide Windows 11 PowerShell setup instructions.

---

### 21. Required Pages

Target approximately 45–55 functional pages or routed views across:

**Public and Authentication**
- Home
- About
- Contact
- Register
- Login
- MFA Verification
- Forgot Password
- Reset Password

**Customer**
- Dashboard
- My Accounts
- Checking
- Savings
- Business Accounts
- Account Details
- Transaction History
- Transfer Money
- Transfer Recipients
- Deposit Check
- Deposit History
- Bill Payments
- Scheduled Payments
- Credit Cards
- Loans
- Cash Advance
- Referrals
- FEMA Relief
- Notifications
- Preferences and Alerts
- Profile
- Security Settings
- Statements

**Administrator**
- Admin Dashboard
- Customers
- Customer Details
- Account Applications
- Account Approvals
- Employees
- Employee Assignments
- Deposit Approvals
- Transfer Monitoring
- Fraud Alerts
- Bill Payment Monitoring
- Loans and Advances
- Referral Management
- FEMA Deferrals
- Reports
- Audit Logs
- Bank Settings

**Employee**
- Employee Dashboard
- Assigned Customers
- Customer Verification
- Deposit Review
- Account Requests
- Support Cases

Share reusable components rather than duplicating common frontend layouts.

Every completed page must have functional navigation, appropriate backend connections, and clear loading, success, empty, and error states.

Do not create empty placeholder pages merely to satisfy a page count.

---

### 22. Backend API Requirements

Implement REST APIs for:
- Authentication
- MFA
- Customer registration
- Customer profiles
- Account applications
- Account management
- Checking and Savings
- Transaction history
- Transfers
- Transfer recipients
- Check deposits
- Deposit approvals
- Bill payments
- Loan payments
- Cash advances
- Referrals
- FEMA deferrals
- Customer alerts
- Notifications
- Employee management
- Administrative approvals
- Reports
- Audit history

Use appropriate GET, POST, PUT, PATCH, and DELETE methods.

Include:
- Request and response DTOs
- Bean validation
- Consistent error responses
- HTTP status codes
- Authorization checks
- Pagination
- Filtering
- OpenAPI documentation
- Service-level business validation
- Integration tests

Do not expose database entities directly where sensitive fields could leak.

---

### 23. Automated Testing

Build a professional QA automation framework.

**Backend**
- JUnit 5 unit tests
- Mockito service tests
- Spring Boot integration tests
- Database transaction tests

**API**
- REST Assured
- Authentication and authorization tests
- Response validation
- JSON schema validation
- Positive and negative test cases
- Invalid inputs
- Duplicate transaction tests
- Concurrent transfer tests
- Deposit approval tests
- FEMA eligibility tests

**UI**
- Selenium WebDriver
- Java
- TestNG
- Page Object Model
- Login testing
- Customer account navigation
- Check deposits
- Transfers
- Bill payments
- Administrator approvals

**Performance**
- JMeter
- 100-customer data testing
- 1,000-customer data testing
- Transaction concurrency tests

Create a Jenkinsfile for build, test, and reporting.

Provide Postman collections for manual API testing.

---

### 24. Example End-to-End Business Scenario

The application must support this complete fictional scenario:

1. Customer John Smith registers.
2. John verifies his email with MailHog.
3. John completes MFA.
4. John enters his personal and address information.
5. John applies for a checking account.
6. An administrator reviews his application.
7. The administrator approves it.
8. John logs in and sees his checking account.
9. A simulated weekly payroll deposit of $800 arrives.
10. John transfers $200 to his savings account.
11. John sends $50 to another customer using their registered email.
12. John captures a fictional $500 check on his iPhone.
13. John chooses instant deposit and agrees to the $5 fee.
14. The simulated deposit is verified and approved.
15. John receives $495 net available funds.
16. John pays a simulated $150 electricity bill.
17. John qualifies for a $100 cash advance after meeting the required payroll history conditions.
18. John refers another qualifying customer and receives a simulated $500 reward.
19. A qualifying FEMA disaster declaration affects John's verified location.
20. The eligibility service grants a simulated 30-day deferral to an eligible loan payment.
21. John receives an email and in-app notification.
22. John sets a low-balance alert of $100.
23. The system detects a suspicious transaction and alerts John.
24. The administrator can view the relevant transaction and decision audit trails.

All transactions must reconcile correctly with the double-entry ledger.

All security and administrative operations must follow role-based permissions.

All notifications must have traceable delivery records.

---

### 25. Required Project Folder Structure

Create an organized project similar to:

banking-platform/
- backend/
  - pom.xml
  - src/main/java/
  - src/main/resources/
  - src/test/
- frontend/
  - package.json
  - src/
  - public/
- database/
  - seed/
  - sample-data/
- infra/
  - docker/
  - cloudflare/
  - mailhog/
- tests/
  - postman/
  - selenium/
  - performance/
- docs/
  - architecture/
  - api/
  - setup/
  - business-rules/
- scripts/
  - setup/
  - start/
  - stop/
- .env.example
- README.md
- Jenkinsfile

Follow consistent Java package naming and React component conventions.

Use clean code and separate controllers, services, repositories, entities, DTOs, validation, security, and business rules.

---

### 26. Development Execution Instructions

Implement the project in phases, but keep it runnable throughout development.

**Phase 1: Foundation**
- Create folder structure.
- Configure Spring Boot.
- Configure React and TypeScript.
- Configure MySQL.
- Add Flyway migrations.
- Configure MailHog.
- Implement initial application health checks.

**Phase 2: Identity and Access**
- Registration
- Login
- MFA
- Sessions
- Roles
- Customer/Employee/Admin dashboards

**Phase 3: Core Banking**
- Customer accounts
- Checking and Savings
- Ledger
- Transactions
- Transfers
- Account approvals

**Phase 4: Advanced Transactions**
- Mobile check deposits
- Instant and standard deposit fees
- Bill payments
- Scheduled payments
- Direct deposits

**Phase 5: Banking Programs**
- $100 cash advances
- $500 referrals
- FEMA deferral integration
- Customer preferences
- Fraud and security alerts

**Phase 6: Administration**
- Employee management
- Customer assignments
- Approvals
- Audit history
- Reports

**Phase 7: Mobile and Integration**
- Responsive layouts
- PWA installation
- Phone camera
- Cloudflare Tunnel
- Email notifications

**Phase 8: Testing and Delivery**
- Unit tests
- Integration tests
- API tests
- UI tests
- Performance tests
- Security checks
- Documentation
- Final ZIP package

### Execution Rules

1. Generate actual source code and configuration files, not just pseudocode or explanations.
2. Work directly in the project workspace when filesystem access is available.
3. Create each required Java class, React component, SQL migration, configuration file, and test.
4. Use supported stable dependencies and record exact versions.
5. Keep the backend and frontend integrated.
6. Ensure every business operation enforces authorization.
7. Implement financial transactions atomically.
8. Use realistic, fictional test data.
9. Avoid hardcoded user-specific values and secrets.
10. Avoid unnecessary paid third-party services.
11. Provide mock adapters for unavailable external providers.
12. Run builds and tests where the environment allows.
13. Fix build, compilation, and test errors before marking a phase complete.
14. Do not claim a feature is tested if it has not been executed.
15. Document incomplete integrations or features clearly.
16. Do not claim production banking certification or regulatory compliance.
17. Provide setup commands specifically for Windows 11 PowerShell.
18. Provide documented test credentials that work only in the local development environment.
19. Keep completed modules working while adding new modules.
20. Package the completed source code into a downloadable ZIP when file-generation tools are available.

### 27. Final Deliverables

The finished application must include:

- Spring Boot backend source code
- React + TypeScript frontend source code
- Responsive desktop and mobile UI
- PWA configuration
- MySQL schema and migrations
- MySQL seed scripts
- Three role-based dashboard experiences
- Customer registration and MFA
- Account management
- Secure financial ledger
- Money transfers
- Check deposits
- Bill payments
- Cash advances
- Referral system
- FEMA integration
- Customer alert preferences
- Employee management
- MailHog integration
- Cloudflare setup instructions
- Automated test suites
- Postman collection
- Jenkins pipeline
- Environment configuration examples
- Detailed README
- PowerShell startup commands
- Troubleshooting instructions
- Source-code ZIP package

### 28. Definition of Done

A feature is complete only when:
- The frontend works.
- The backend endpoint works.
- Data is stored and retrieved correctly.
- Authorization is enforced.
- Relevant business rules are validated.
- Positive and negative cases are tested.
- Errors are handled appropriately.
- Audit events are recorded where necessary.
- Documentation is updated.

At the end of each phase, report what is implemented, what has been tested, what remains incomplete, and exact commands to run the application.

**Begin with Phase 1 immediately. Create a working project structure, Spring Boot backend, React/TypeScript frontend, MySQL integration, Flyway migrations, MailHog configuration, and basic role-based authentication. Then continue implementing the remaining phases in dependency order. Do not stop at architecture or pseudocode.**

**Ultimate goal: Deliver a realistic, fully integrated, secure, maintainable digital banking simulation that supports multiple customer accounts, banking employees, administrators, responsive mobile access, and all specified business workflows using simulated funds and fictional financial data.**

29. Swagger UI and OpenAPI Integration
Integrate Swagger UI with OpenAPI 3 into the Spring Boot banking application using the compatible springdoc-openapi dependency.
Requirements:
1. Automatically generate API documentation for all Spring Boot REST controllers.
2. Enable Swagger UI at http://localhost:3000/swagger-ui/index.html.
3. Enable OpenAPI JSON at http://localhost:3000/v3/api-docs.
4. Organize APIs into groups:
   - Authentication and MFA
   - Customer Management
   - Checking and Savings Accounts
   - Money Transfers
   - Mobile Check Deposits
   - Bill Payments
   - Cash Advances
   - Referral System
   - FEMA Disaster Relief
   - Notifications and Alerts
   - Employee Management
   - Administrator Operations
5. Document GET, POST, PUT, PATCH, and DELETE endpoints.
6. Include request parameters, JSON request/response examples, HTTP status codes, and error responses.
7. Configure Swagger authentication using the application's secure session and CSRF mechanism.
8. Support testing authorized API endpoints directly through Swagger UI.
9. Clearly identify customer, employee, and administrator permissions.
10. Use OpenAPI annotations such as @Operation, @Tag, @ApiResponse, and @Schema.
11. Include pagination, filtering, and validation documentation.
12. Protect Swagger UI and API documentation behind administrator/developer authorization in local development.
13. Disable Swagger access in production unless explicitly authorized.
14. Never expose Swagger UI publicly through Cloudflare without appropriate access restrictions.
15. Ensure Swagger documentation updates automatically when controllers or DTOs change.
Final requirement: All implemented banking APIs must be documented, testable through Swagger UI, and integrated with Spring Security. Start building my complete Spring Boot banking application now using the full master prompt, including Swagger UI and all 29 sections.
Generate the actual source code for the backend, React + TypeScript frontend, MySQL database, MailHog, MFA, Cloudflare, PWA, and all banking modules.
Provide a complete, runnable project with all necessary files, configurations, database scripts, automated tests, and Windows 11 PowerShell instructions.
Package everything into a downloadable ZIP file.
Implement it in phases, verify the code where possible, fix errors, and clearly identify anything incomplete. Do not provide pseudocode or empty placeholder pages.


# ADDITIONAL MASTER PROMPT — SECTIONS 30–35
## Advanced Mobile Banking UI and Customer Services

Use my five uploaded banking app screenshots as visual and functional references.

Expand my existing Spring Boot + React + TypeScript banking application into a professional, mobile-first digital banking platform.

Recreate the general navigation patterns, card layouts, interaction quality, and banking functionality shown in the references, but use original branding, icons, styling, and content.

Every implemented feature must connect to the Spring Boot backend, Spring Security, and MySQL database. Do not create empty placeholder pages.

### 30. Advanced Card Manager

Create a complete Card Manager page for each customer's debit and credit cards.

Required functions:

1. View all debit and credit cards.
2. Activate a new card.
3. Lock and unlock a card instantly.
4. Report a lost or stolen card.
5. Request a replacement card.
6. Request temporary spending limit increases.
7. Change or reset the card PIN using a secure simulated workflow.
8. Manage daily ATM withdrawal limits.
9. Set debit and credit card spending limits.
10. Enable or disable online transactions.
11. Enable or disable international transactions.
12. View merchant transaction history.
13. Manage recurring merchant payments.
14. View linked payment methods.
15. Include Apple Pay and PayPal integration screens, with simulated connections until authorized real integrations are configured.
16. Manage card-specific fraud and transaction alerts.
17. Require MFA for sensitive card operations.
18. Notify customers of card status changes through MailHog.

Create administrator controls for card issuance, activation review, replacement requests, and fraud investigations.

Never display full card numbers, CVVs, or plaintext PINs.

### 31. Advanced Pay & Transfer Page

Design a Pay & Transfer screen similar in structure to the supplied reference image.

Include:

- Transfer between my accounts.
- Internal customer-to-customer transfers.
- Transfer activity and history.
- Scheduled transfers.
- Recurring transfers.
- External bank account management.
- Simulated ACH transfers.
- Zelle-style transfers using verified phone numbers or email addresses.
- Pay a bill.
- Bill payment activity.
- Schedule bill payments.
- Manage billers and AutoPay.
- Download transfer and bill payment receipts.

Every navigation item must open a functional page.

Implement transfer confirmation, MFA where required, duplicate prevention, balance validation, and transaction status tracking.

Use fictional accounts and simulated payment networks.

### 32. Mobile More Menu and Customer Services

Create a modern More page using a two-column grid of large, rounded, touch-friendly cards, inspired by the uploaded screenshots.

**Quick-access services:**
- Mobile Check Deposit
- Open a New Account
- Card Manager
- Personal Settings
- Account Services
- Manage Alerts
- FEMA Disaster Relief
- $100 Cash Advance
- $500 Referral Program
- Loans and Credit Cards
- Statements and Documents
- Security and Device Management

**Customer Support:**
- Contact Us
- Secure Messaging
- Customer Support Chat
- Support Ticket Management
- Branch and ATM Locator
- Frequently Asked Questions

**Application Settings:**
- Language Preference
- Accessibility Settings
- Customer Feedback
- Privacy Settings
- Security Information
- Terms of Use
- Logout

Support a working secure-messaging system stored in MySQL. Customer support messages must be accessible only to the customer and authorized support employees.

Implement configurable sample branch locations for simulation.

### 33. Advanced Customer Alerts and Notification Center

Create a dedicated Manage Alerts screen following the general layout of my reference image.

Group alerts into categories.

**Bank Accounts**
- Low-balance thresholds: $1, $50, $100, $500, or custom.
- Large withdrawals.
- Large deposits.
- Unusual account activity.
- Overdraft warnings.
- Incoming direct deposits.

**Debit and ATM Cards**
- Card purchases.
- ATM withdrawals.
- Suspicious merchant activity.
- Card locked or unlocked.
- Card replacement updates.

**Credit Cards**
- Large transactions.
- Credit utilization.
- Payment confirmations.
- Available credit.
- Suspicious transactions.

**General Security**
- New device login.
- Multiple failed logins.
- Password changes.
- MFA changes.
- Secure messages.
- Account restrictions.

**Bill Pay**
- Upcoming bills.
- Scheduled payments.
- Failed payments.
- Completed payments.

**Additional Banking Alerts**
- Check deposit approvals and rejections.
- Instant deposit fee confirmations.
- $100 cash advance decisions.
- $500 referral bonus updates.
- FEMA 30-day payment deferral notifications.

Allow customers to enable or disable optional alerts separately for each account.

Provide email notifications through MailHog and in-app notifications through the React dashboard.

Mandatory security notifications must remain active independently of optional alert settings.

Store all preferences and notification history in MySQL.

### 34. Professional Responsive Mobile UI and PWA

Build a polished mobile-first banking application.

**Mobile design requirements:**

- Original banking brand identity.
- Consistent green, white, and neutral design system inspired by the references, with configurable colors.
- Professional header with account/profile access.
- Light neutral page backgrounds.
- White cards with rounded corners.
- Clear icons and readable text.
- Responsive account cards.
- Smooth page transitions.
- Loading indicators and skeleton screens.
- Success and error messages.
- Confirmation dialogs for financial actions.
- Touch-friendly buttons and controls.
- Proper spacing for iPhone and Android screen sizes.
- Accessibility and keyboard navigation.

**Mobile bottom navigation:**

1. Accounts
2. Pay & Transfer
3. Products
4. More

The active navigation item must be visually highlighted.

Keep the bottom navigation accessible while scrolling, with appropriate safe-area spacing on devices with gesture navigation.

**Desktop behavior:**

When opened in a desktop browser, automatically adapt the layout to use:

- Sidebar navigation.
- Larger dashboards.
- Tables.
- Multi-column layouts.
- Administrative management screens.

**PWA behavior:**

Allow customers to install the application on iPhones and Android devices using supported browser installation mechanisms.

Use standalone display mode when launched as an installed PWA.

Keep account balances, personal banking information, and authentication data out of offline caches.

### 35. Additional Banking Product and Account Services

Expand the customer Products and Account Services sections.

**Products:**
- Checking Account
- Savings Account
- Business Checking
- Business Savings
- Credit Cards
- Personal Loans
- Auto Loans
- Mortgages
- Certificates of Deposit

**Account Services:**
- Request Account Closure
- Account Statements
- Direct Deposit Setup
- View Routing and Masked Account Numbers
- Manage Beneficiaries
- Request Account Documents
- Manage Joint Account Holders
- Account Restrictions
- Dispute Transactions
- Request Stop Payments for eligible simulated payments
- View Service Requests
- Track Account Applications

Every product must include an appropriate application, request, or management workflow with authorization and persistence.

Customer requests needing approval must appear in the authorized employee or administrator dashboard.

### Implementation Requirements

1. Implement the pages using React and TypeScript with reusable components.
2. Use React Router for navigation.
3. Connect all financial actions to Spring Boot REST APIs.
4. Document APIs using Swagger UI.
5. Store records and preferences in MySQL.
6. Apply Spring Security roles and object-level authorization.
7. Require MFA for sensitive operations.
8. Use MailHog for email testing.
9. Provide responsive layouts for iPhone, Samsung, tablets, and desktop.
10. Maintain the existing simulated ledger and audit-trail rules.
11. Implement confirmation dialogs, loading states, errors, success notifications, and transaction receipts.
12. Add JUnit, REST Assured, and Selenium tests for new features.
13. Keep all services runnable on Windows 11 without Docker.
14. Preserve the previously implemented banking modules and business rules.
15. Clearly identify any feature requiring an external provider or unavailable integration.
16. Run builds and tests where possible, and report verification results.

**Final instruction:** Extend my existing banking application using all 35 master-prompt sections. Use the uploaded screenshots as design references. Implement the new pages with working navigation, REST APIs, MySQL persistence, secure permissions, and automated tests. Do not replace completed banking functionality or claim that simulated payment integrations are live services.

### 36. Automated PDF Document Generation and Email Delivery

Implement a complete PDF document management system using Spring Boot, React, TypeScript, MySQL, and MailHog.

**Required PDF Documents:**

1. Monthly checking and savings account statements.
2. Business account statements.
3. Account balance confirmation letters.
4. Transaction history reports.
5. Direct deposit confirmation letters.
6. Check deposit receipts.
7. Money transfer and bill payment receipts.
8. Credit card statements.
9. Loan statements and repayment schedules.
10. Cash advance approval letters.
11. FEMA 30-day payment deferral letters.
12. $500 referral reward confirmation letters.
13. Banking promotional offers and product offers.
14. Loan and credit card offer letters.
15. Account opening and closure confirmation letters.

**PDF Features:**

- Generate professional, bank-branded PDFs using Apache PDFBox.
- Include bank logo, customer name, masked account number, statement period, opening balance, transactions, fees, and closing balance where applicable.
- Generate monthly statements automatically using Spring Boot Scheduler.
- Allow customers to select dates and download historical statements.
- Allow customers to request balance confirmation PDFs.
- Send test PDF attachments through MailHog.
- Offer secure in-app document downloads and notifications.
- Allow administrators to generate and review authorized documents.
- Support configurable promotional offers and customer notification preferences.
- Store document metadata and delivery history in MySQL.
- Protect documents with Spring Security authorization.
- Prevent customers from accessing other customers' documents.
- Maintain audit logs for document generation, viewing, and delivery.
- Use immutable statement snapshots and reconcile statement totals with the banking ledger.

For real banking use, deliver sensitive statements through authenticated secure download links rather than ordinary unencrypted email attachments by default.

All documents in the local simulator must use fictional customer data and simulated balances.

**Final Requirement:** Integrate all PDF document features with the Customer Dashboard, Employee Portal, Administrator Dashboard, Swagger UI, and MailHog without Docker.