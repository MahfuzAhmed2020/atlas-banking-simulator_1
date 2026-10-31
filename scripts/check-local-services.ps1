# Verify local prerequisites; no containers required.
$checks = @(
  @{ Name='MySQL Community Server'; Port=3306 },
  @{ Name='MailHog SMTP'; Port=1025 },
  @{ Name='MailHog Web UI'; Port=8025 }
)
foreach ($check in $checks) {
  $ok = Test-NetConnection -ComputerName '127.0.0.1' -Port $check.Port -InformationLevel Quiet -WarningAction SilentlyContinue
  if ($ok) { Write-Host "OK: $($check.Name) on port $($check.Port)" -ForegroundColor Green }
  else { Write-Warning "$($check.Name) is NOT reachable on port $($check.Port)" }
}
foreach ($cmd in @('java','mvn','node','npm')) {
  if (Get-Command $cmd -ErrorAction SilentlyContinue) { Write-Host "OK: $cmd found" -ForegroundColor Green }
  else { Write-Warning "$cmd is not on PATH" }
}
