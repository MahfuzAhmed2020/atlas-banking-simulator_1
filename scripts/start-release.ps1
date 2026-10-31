$ErrorActionPreference='Stop'
$projectRoot=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if (-not $env:SPRING_PROFILES_ACTIVE) { $env:SPRING_PROFILES_ACTIVE='local' }
& java -jar (Join-Path $projectRoot 'release/atlas-banking.jar')
if ($LASTEXITCODE -ne 0) { throw 'Application failed to start; check MySQL credentials and port 3000.' }
