$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$archivePath = Join-Path $projectRoot 'release/atlas-banking-simulator.zip'
New-Item -ItemType Directory -Force (Join-Path $projectRoot 'release') | Out-Null
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
# Explicit source roots: excludes credentials, database state, uploads, caches and logs.
$sourceRoots = @('backend/src','frontend/src','frontend/public','scripts','docs','tests','sample-data','infra','.github')
$files = @()
foreach ($sourceRoot in $sourceRoots) {
 $absoluteRoot = Join-Path $projectRoot $sourceRoot
 if (Test-Path -LiteralPath $absoluteRoot) { $files += Get-ChildItem -LiteralPath $absoluteRoot -Recurse -File }
}
foreach ($relativeFile in @('README.md','Jenkinsfile','.env.example','.gitignore','backend/pom.xml','frontend/package.json','frontend/package-lock.json','frontend/tsconfig.json','frontend/vite.config.ts','frontend/index.html','release/atlas-banking.jar')) {
 $absoluteFile = Join-Path $projectRoot $relativeFile
 if (Test-Path -LiteralPath $absoluteFile) { $files += Get-Item -LiteralPath $absoluteFile }
}
$stream = [System.IO.File]::Open($archivePath,[System.IO.FileMode]::Create)
$archive = [System.IO.Compression.ZipArchive]::new($stream,[System.IO.Compression.ZipArchiveMode]::Create)
try {
 foreach ($file in $files) {
  if ($file.Name -eq '.env' -or $file.Extension -in @('.log','.pfx','.key','.pem')) { continue }
  $relative = $file.FullName.Substring($projectRoot.Length + 1).Replace('\','/')
  [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive,$file.FullName,$relative,[System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
 }
} finally { $archive.Dispose(); $stream.Dispose() }
Get-FileHash -LiteralPath $archivePath -Algorithm SHA256
Write-Host "Package: $archivePath"
