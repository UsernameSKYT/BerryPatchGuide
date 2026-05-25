# 방화벽 규칙 확인
if (-not (Get-NetFirewallRule -DisplayName "BerryPatchGuide Backend 8000" -ErrorAction SilentlyContinue)) {
    New-NetFirewallRule -DisplayName "BerryPatchGuide Backend 8000" -Direction Inbound -Protocol TCP -LocalPort 8000 -Action Allow -Profile Any
    Write-Output "방화벽 규칙 추가 완료"
} else {
    Write-Output "방화벽 규칙 이미 존재합니다"
}

# 기존 프로세스 종료
$existing = Get-NetTCPConnection -LocalPort 8000 -ErrorAction SilentlyContinue
if ($existing) {
    Stop-Process -Id $existing.OwningProcess -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 1
}

# 서버 백그라운드 실행
$backendPath = "C:\Users\김재경\Projects\BerryPatchGuide\backend"
$pythonPath = Join-Path $backendPath "venv\Scripts\python.exe"
$mainPath = Join-Path $backendPath "main.py"

Start-Process -FilePath $pythonPath -ArgumentList $mainPath -WorkingDirectory $backendPath -WindowStyle Hidden
Start-Sleep -Seconds 3

# 기동 확인
$proc = Get-NetTCPConnection -LocalPort 8000 -ErrorAction SilentlyContinue
if ($proc) {
    Write-Output "✅ 서버 실행 확인: http://0.0.0.0:8000 (PID: $($proc.OwningProcess))"
} else {
    Write-Output "❌ 서버 실행 실패 - 포트를 확인하세요."
}