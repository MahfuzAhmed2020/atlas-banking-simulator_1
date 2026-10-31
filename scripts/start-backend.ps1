# Run after MySQL Community Server and MailHog are started on Windows.
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
  throw 'Maven not found on PATH. Install Maven 3.9+ and restart PowerShell.'
}
if (-not $env:DB_URL) { $env:DB_URL='jdbc:mysql://localhost:3306/atlas_bank?useSSL=false&allowPublicKeyRetrieval=true' }
if (-not $env:DB_USER) { $env:DB_USER='atlas' }
if (-not $env:DB_PASSWORD) { $env:DB_PASSWORD='atlas_dev_password' }
if (-not $env:MAIL_HOST) { $env:MAIL_HOST='127.0.0.1' }
if (-not $env:MAIL_PORT) { $env:MAIL_PORT='1025' }
if (-not $env:SPRING_PROFILES_ACTIVE) { $env:SPRING_PROFILES_ACTIVE='local' }
Push-Location (Join-Path $root 'backend')
try {
  & mvn spring-boot:run
  if ($LASTEXITCODE -ne 0) { throw "Spring Boot exited with code $LASTEXITCODE" }
} finally { Pop-Location }
