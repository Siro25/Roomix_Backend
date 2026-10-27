param(
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $repositoryRoot 'compose.yml'
$coreResetFile = Join-Path $PSScriptRoot 'demo-reset\reset-core-demo.sql'
$rentalResetFile = Join-Path $PSScriptRoot 'demo-reset\reset-rental-demo.sql'
$coreSeedFile = Join-Path $repositoryRoot 'Data.sql'

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

Push-Location $repositoryRoot
try {
    Invoke-PostgresFile -Service 'rental-db' -Database 'rental_db' -File $rentalResetFile
    Invoke-PostgresFile -Service 'core-db' -Database 'core_db' -File $coreResetFile
    Invoke-PostgresFile -Service 'core-db' -Database 'core_db' -File $coreSeedFile

    Write-Host ''
    Write-Host 'Demo data was reset successfully.' -ForegroundColor Green
    Write-Host 'Accounts: admin.seed, landlord.seed, tenant.seed'
    Write-Host 'Password: Roomix@123'
    Write-Host 'Room 101 is APPROVED and available; room 201 has a PENDING post for the admin moderation demo.'
}
finally {
    Pop-Location
}
