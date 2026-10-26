# Windows PowerShell 5.1 and PowerShell 7. Environment files are data, never executed.
param(
    [switch]$CheckOnly,
    [string]$EnvFile = '.env',
    [string]$JavaHome = '',
    [string]$PostgresDriver = ''
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$previousVariables = @{}

function Set-LocalEnvironment([string]$Name, [string]$Value) {
    if (-not $previousVariables.ContainsKey($Name)) {
        $previousVariables[$Name] = [Environment]::GetEnvironmentVariable($Name, 'Process')
    }
    [Environment]::SetEnvironmentVariable($Name, $Value, 'Process')
}

Push-Location -LiteralPath $projectRoot
try {
    if (-not [IO.Path]::IsPathRooted($EnvFile)) { $EnvFile = Join-Path $projectRoot $EnvFile }
    if (-not (Test-Path -LiteralPath $EnvFile)) { throw 'Missing .env. Copy .env.example and fill Core configuration first.' }
    $lineNumber = 0
    foreach ($line in Get-Content -LiteralPath $EnvFile -Encoding UTF8) {
        $lineNumber++
        if ([string]::IsNullOrWhiteSpace($line) -or $line.TrimStart().StartsWith('#')) { continue }
        if ($line -notmatch '^\s*([A-Z][A-Z0-9_]*)\s*=(.*)$') { throw "Invalid env format at line $lineNumber." }
        $name = $Matches[1]
        $value = $Matches[2].Trim()
        if ($name -notmatch '^(CORE_[A-Z0-9_]+|JWT_SECRET|FACE_SERVICE_TOKEN|TELEGRAM_BOT_TOKEN|TELEGRAM_CHAT_ID|JAVA_HOME)$') {
            throw "Unsupported env key at line $lineNumber. Use Core variable names from .env.example."
        }
        if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        # Explicit values already set in this terminal have priority.
        if ([string]::IsNullOrEmpty([Environment]::GetEnvironmentVariable($name, 'Process'))) {
            Set-LocalEnvironment $name $value
        }
    }
    if (-not $JavaHome) {
        if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
            $JavaHome = $env:JAVA_HOME
        } else {
            $JavaHome = 'C:\Program Files\Java\jdk-21.0.12'
        }
    }
    $java = Join-Path $JavaHome 'bin\java.exe'
    if (-not (Test-Path -LiteralPath $java)) { throw 'JDK not found. Pass -JavaHome with the JDK 21 directory.' }
    Set-LocalEnvironment 'JAVA_HOME' $JavaHome
    if (-not $PostgresDriver) {
        $driverRoot = Join-Path $env:USERPROFILE '.m2\repository\org\postgresql\postgresql'
        $driver = Get-ChildItem -LiteralPath $driverRoot -Recurse -Filter '*.jar' -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -notmatch '(sources|javadoc)' } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($driver) { $PostgresDriver = $driver.FullName }
    }
    if (-not $PostgresDriver -or -not (Test-Path -LiteralPath $PostgresDriver)) {
        throw 'PostgreSQL JDBC driver not found. Run .\mvnw.cmd -DskipTests compile once, or pass -PostgresDriver.'
    }
    & $java -cp $PostgresDriver (Join-Path $PSScriptRoot 'CoreDatabaseCheck.java')
    if ($LASTEXITCODE -ne 0) { throw 'Core preflight failed. No application was started.' }
    if (-not $CheckOnly) {
        & (Join-Path $projectRoot 'mvnw.cmd') spring-boot:run
        if ($LASTEXITCODE -ne 0) { throw 'Core stopped with an error.' }
    }
}
finally {
    foreach ($name in $previousVariables.Keys) {
        [Environment]::SetEnvironmentVariable($name, $previousVariables[$name], 'Process')
    }
    Pop-Location
}
