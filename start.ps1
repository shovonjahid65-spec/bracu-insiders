$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw "Install and start Docker Desktop first." }
docker compose version | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Docker Compose is required." }
function New-Secret([int]$Length) {
    $bytes = New-Object byte[] $Length
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return ([BitConverter]::ToString($bytes)).Replace("-", "").ToLowerInvariant()
}
if (-not (Test-Path -LiteralPath ".env")) {
    $lines = @(
        ("DB_ADMIN_PASSWORD=" + (New-Secret 32)), ("DB_PASSWORD=" + (New-Secret 32)), "ADMIN_USERNAME=editor",
        ("ADMIN_PASSWORD=" + (New-Secret 24)), "HTTP_PORT=8080", "COOKIE_SECURE=false", "SEED_EDITION=true"
    )
    [IO.File]::WriteAllLines((Join-Path $PSScriptRoot ".env"), [string[]]$lines)
}
docker compose up --build -d --wait
if ($LASTEXITCODE -ne 0) { throw "Startup failed. Run 'docker compose logs app db' for details." }
$entry = Get-Content .env | Where-Object { $_ -match '^HTTP_PORT=' } | Select-Object -First 1
$sitePort = if ($entry) { $entry.Substring(10) } else { "8080" }
Write-Host "Website: http://localhost:$sitePort/"
Write-Host "Dashboard: http://localhost:$sitePort/admin"
Write-Host "Your first editor username and password are in .env."
