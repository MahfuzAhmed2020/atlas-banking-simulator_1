# Download the Windows MailHog binary from https://github.com/mailhog/MailHog/releases
# Place the EXE under tools\MailHog_windows_amd64.exe, or supply -ExePath.
param(
  [string]$ExePath = (Join-Path $PSScriptRoot '..\tools\MailHog_windows_amd64.exe')
)
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $ExePath -PathType Leaf)) {
  throw "MailHog executable not found: $ExePath`nDownload the Windows executable and pass -ExePath <path>."
}
Write-Host 'Starting MailHog locally: SMTP 127.0.0.1:1025; inbox http://127.0.0.1:8025'
& $ExePath -smtp-bind-addr '127.0.0.1:1025' -api-bind-addr '127.0.0.1:8025' -ui-bind-addr '127.0.0.1:8025'
