# Chạy từ bất kỳ thư mục nào, luôn dùng môi trường AI chính của dự án.
param(
    [switch]$CheckOnly,
    [switch]$Demo
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$runtimePython = Join-Path $projectRoot '.venv-runtime\Scripts\python.exe'
$envFile = Join-Path $projectRoot '.env'

if (-not (Test-Path -LiteralPath $runtimePython)) {
    throw 'Chưa có .venv-runtime. Chạy scripts/setup_runtime.ps1 trước.'
}
if (-not (Test-Path -LiteralPath $envFile)) {
    throw 'Chưa có .env. Sao chép .env.example thành .env rồi điền cấu hình thật.'
}

Push-Location -LiteralPath $projectRoot
try {
    # Biến môi trường có sẵn được ưu tiên hơn .env. Không chạy nội dung .env như mã.
    & $runtimePython -X utf8 -m src.face_auth.check_config --env-file $envFile
    if ($LASTEXITCODE -ne 0) { throw 'Kiểm tra cấu hình chưa đạt; chưa khởi chạy AI.' }
    if (-not $CheckOnly) {
        $previousDemo = $env:FACE_ENABLE_DEMO
        try {
            if ($Demo) { $env:FACE_ENABLE_DEMO = 'true' }
            & $runtimePython -m uvicorn src.face_auth.main:app --env-file $envFile --host 127.0.0.1 --port 8003 --no-access-log
            if ($LASTEXITCODE -ne 0) { throw 'Face Service dừng do lỗi khởi chạy.' }
        }
        finally {
            $env:FACE_ENABLE_DEMO = $previousDemo
        }
    }
}
finally {
    Pop-Location
}
