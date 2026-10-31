# Windows 11 — Start Atlas Banking with NO Docker

This guide runs four ordinary Windows programs: MySQL Community Server, MailHog, Spring Boot, and React/Vite. **MySQL Workbench is a GUI, not the database server.** Workbench and MySQL Community Server are both required for this workflow.

## 1. Install Windows prerequisites (once)

- Install **MySQL Community Server 8.0/8.4** and **MySQL Workbench** from https://dev.mysql.com/downloads/installer/ . During setup choose a root password and configure MySQL to start as a Windows service on local port **3306**.
- Install **JDK 21**, **Apache Maven 3.9+**, and **Node.js 20+**. Check `java -version`, `mvn -version`, `node -v`, and `npm -v` in PowerShell.
- Download the **Windows MailHog executable** from https://github.com/mailhog/MailHog/releases (official publisher). Place it at `atlas-banking-simulator/tools/MailHog_windows_amd64.exe`, or use its actual path with `-ExePath`. Do not download executables from unofficial mirrors.

## 2. Set up MySQL through Workbench (once)

1. Ensure the MySQL service is running (Windows Services application: `services.msc`; choose the MySQL service installed on your computer).
2. Open MySQL Workbench, connect to `localhost:3306` as the local MySQL administrator.
3. Open `scripts/setup-mysql-local.sql` and execute it. It creates database `atlas_bank` and a development-only account `atlas` with password `atlas_dev_password`.
4. Later connect from Workbench as `atlas` to view application tables. **Do not manually create the tables**; Flyway creates them when the backend starts successfully.

If you prefer different database credentials, edit the SQL script before running it and set `DB_USER` / `DB_PASSWORD` in the PowerShell session before running the backend.

## 3. Start MailHog (PowerShell terminal A)

```powershell
cd path\to\atlas-banking-simulator
.\scripts\start-mailhog.ps1
```

If the EXE is stored elsewhere:

```powershell
.\scripts\start-mailhog.ps1 -ExePath 'C:\Tools\MailHog_windows_amd64.exe'
```

Keep this terminal open; verify `http://localhost:8025`. MailHog collects test mail on port **1025** and shows it on **8025**. It does **not** send real email or SMS to the internet.

## 4. Start Spring Boot (PowerShell terminal B)

```powershell
cd path\to\atlas-banking-simulator
.\scripts\start-backend.ps1
```

The backend starts at `http://localhost:3000` using MySQL on `localhost:3306` and MailHog on `localhost:1025`. Flyway automatically runs the schema migration. You can override connection details in the terminal:

```powershell
$env:DB_USER='atlas'
$env:DB_PASSWORD='atlas_dev_password'
$env:DB_URL='jdbc:mysql://localhost:3306/atlas_bank?useSSL=false&allowPublicKeyRetrieval=true'
.\scripts\start-backend.ps1
```

## 5. Start React frontend (PowerShell terminal C)

```powershell
cd path\to\atlas-banking-simulator\frontend
npm install
npm run dev
```

Open `http://localhost:5173`. For MFA, read your test OTP in `http://localhost:8025`.

## 6. Swagger UI and phone testing

For Swagger and Cloudflare, the easiest approach is the **single-origin build**, after your normal development build has been verified:

```powershell
cd path\to\atlas-banking-simulator
.\scripts\build-single-origin.ps1
.\scripts\start-backend.ps1
```

Then open `http://localhost:3000` and, after admin authentication, `http://localhost:3000/swagger-ui/index.html`. Do not run a second backend at the same time.

For iPhone/Android testing, use a **named Cloudflare Tunnel protected by Cloudflare Access** pointing at `http://localhost:3000`, as documented in `docs/CLOUDFLARE.md`. Do **not** expose MailHog, Workbench, MySQL, or the admin testing endpoints publicly. Demo passwords and financial data must be fictional.

## Troubleshooting

- `Communications link failure`: Check that the Windows MySQL service is running on port 3306 and Workbench can connect.
- `Access denied for user atlas`: Re-run the SQL provisioning with an administrator connection or update `DB_USER` and `DB_PASSWORD` to match your credentials.
- `Unable to send mail`: Run MailHog first and verify ports 1025/8025.
- `Port 3000 in use`: Stop any existing Spring Boot process.
- `mvn is not recognized`: Install Maven and restart PowerShell.
- Check all prerequisites with `.\scripts\check-local-services.ps1`.

This project is an **unverified development simulator**. The backend/frontend builds and end-to-end workflows still need execution and debugging on your Windows computer; the ZIP alone does not prove the application works end to end.
