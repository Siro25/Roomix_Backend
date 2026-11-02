param(
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $repositoryRoot 'compose.yml'
$coreResetFile = Join-Path $PSScriptRoot 'demo-reset\reset-core-demo.sql'
$rentalResetFile = Join-Path $PSScriptRoot 'demo-reset\reset-rental-demo.sql'
$coreSeedFile = Join-Path $repositoryRoot 'Data.sql'
$rentalSeedFile = Join-Path $repositoryRoot 'RentalData.sql'

if (-not $Force) {
    $confirmation = Read-Host 'Reset Roomix demo data for admin.seed, landlord.seed and tenant.seed? Type RESET to continue'
    if ($confirmation -cne 'RESET') {
        Write-Host 'Cancelled. No data was changed.' -ForegroundColor Yellow
        exit 0
    }
}

function Invoke-PostgresFile {
    param(
        [string]$Service,
        [string]$Database,
        [string]$File
    )

    $containerId = (docker compose -f $composeFile ps -q $Service).Trim()
    if (-not $containerId) {
        throw "Docker service is not running: $Service"
    }

    $remoteFile = "/tmp/roomix-reset-$([guid]::NewGuid())-$([System.IO.Path]::GetFileName($File))"
    Write-Host "Running $([System.IO.Path]::GetFileName($File)) on $Database..." -ForegroundColor Cyan
    docker cp $File "${containerId}:$remoteFile"
    if ($LASTEXITCODE -ne 0) {
        throw "Could not copy database script into ${Service}: $File"
    }

    try {
        docker compose -f $composeFile exec -T $Service psql -v ON_ERROR_STOP=1 -U postgres -d $Database -f $remoteFile
        if ($LASTEXITCODE -ne 0) {
            throw "Database script failed: $File"
        }
    }
    finally {
        docker compose -f $composeFile exec -T $Service rm -f $remoteFile | Out-Null
    }
}

function Invoke-PostgresTransaction {
    param(
        [string]$Service,
        [string]$Database,
        [string[]]$Files
    )

    $combinedFile = Join-Path ([System.IO.Path]::GetTempPath()) "roomix-$Database-$([guid]::NewGuid()).sql"
    $lines = [System.Collections.Generic.List[string]]::new()
    $lines.Add('BEGIN;')

    foreach ($file in $Files) {
        foreach ($line in [System.IO.File]::ReadAllLines($file)) {
            if ($line -notmatch '^\s*(BEGIN|COMMIT);\s*$') {
                $lines.Add($line)
            }
        }
    }

    $lines.Add('COMMIT;')
    [System.IO.File]::WriteAllLines(
        $combinedFile,
        $lines,
        [System.Text.UTF8Encoding]::new($false)
    )

    try {
        Invoke-PostgresFile -Service $Service -Database $Database -File $combinedFile
    }
    finally {
        Remove-Item -LiteralPath $combinedFile -Force -ErrorAction SilentlyContinue
    }
}

Push-Location $repositoryRoot
try {
    Write-Host 'Resetting core_db atomically...' -ForegroundColor Cyan
    Invoke-PostgresTransaction -Service 'core-db' -Database 'core_db' -Files @($coreResetFile, $coreSeedFile)

    Write-Host 'Resetting rental_db atomically...' -ForegroundColor Cyan
    Invoke-PostgresTransaction -Service 'rental-db' -Database 'rental_db' -Files @($rentalResetFile, $rentalSeedFile)

    Write-Host ''
    Write-Host 'Demo data was reset successfully.' -ForegroundColor Green
    Write-Host 'Main accounts: admin.seed, landlord.seed, tenant.seed'
    Write-Host 'Password: Roomix@123'
    Write-Host 'Loaded 19 demo users, 10 houses, 20 floors, 52 rooms, 52 posts, rental requests, contracts and invoices.'
}
finally {
    Pop-Location
}
