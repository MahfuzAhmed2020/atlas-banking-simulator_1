$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Push-Location (Join-Path $root 'frontend')
try {
 npm ci
 if ($LASTEXITCODE -ne 0) { throw 'npm ci failed' }
 npm run build
 if ($LASTEXITCODE -ne 0) { throw 'frontend build failed' }
} finally { Pop-Location }
$static = Join-Path $root 'backend/src/main/resources/static'
New-Item -ItemType Directory -Force -Path $static | Out-Null
Copy-Item -Path (Join-Path $root 'frontend/dist/*') -Destination $static -Force -Recurse
Write-Host 'React build installed in Spring Boot static resources. Next: .\scripts\start-backend.ps1'
