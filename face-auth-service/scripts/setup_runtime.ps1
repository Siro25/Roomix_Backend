# Tạo môi trường chạy AI CPU trên Windows, tách biệt môi trường unit test.
param(
    [string]$PythonExecutable = "$env:LOCALAPPDATA\Programs\Python\Python311\python.exe"
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot

if (-not (Test-Path -LiteralPath $PythonExecutable)) {
    throw 'Không tìm thấy Python 3.11. Truyền đường dẫn bằng -PythonExecutable.'
}
$pythonVersion = & $PythonExecutable -c 'import sys; print("%s.%s" % sys.version_info[:2])'
if ($LASTEXITCODE -ne 0 -or $pythonVersion -ne '3.11') {
    throw 'Script này cần Python 3.11 để dùng bộ AI đã chọn.'
}

$runtimePython = Join-Path $projectRoot '.venv-runtime\Scripts\python.exe'
if (-not (Test-Path -LiteralPath $runtimePython)) {
    & $PythonExecutable -m venv .venv-runtime
    if ($LASTEXITCODE -ne 0) { throw 'Không tạo được môi trường Python.' }
}
$runtimeVersion = & $runtimePython -c 'import sys; print("%s.%s" % sys.version_info[:2])'
if ($LASTEXITCODE -ne 0 -or $runtimeVersion -ne '3.11') {
    throw '.venv-runtime đang dùng sai phiên bản Python. Không tự ghi đè môi trường có sẵn.'
}

& $runtimePython -m pip install --upgrade pip
if ($LASTEXITCODE -ne 0) { throw 'Không cập nhật được pip.' }

# Cài wheel CPU từ nguồn PyTorch. Dependencies còn lại được giải từ PyPI
# bằng requirements.txt ngay sau đó, không để môi trường ở trạng thái dở dang.
& $runtimePython -m pip install torch==2.2.2 torchvision==0.17.2 --index-url https://download.pytorch.org/whl/cpu --no-deps
if ($LASTEXITCODE -ne 0) { throw 'Không cài được PyTorch CPU.' }

& $runtimePython -m pip install -r requirements.txt
if ($LASTEXITCODE -ne 0) { throw 'Không cài đủ dependencies Face Auth.' }

& $runtimePython -m pip check
if ($LASTEXITCODE -ne 0) { throw 'Dependencies chưa tương thích.' }

Write-Host 'Đã cài môi trường. Kiểm tra AI bằng:'
Write-Host '.\.venv-runtime\Scripts\python.exe -m src.face_auth.check_runtime --models'
