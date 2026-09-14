# Libera el puerto 8080 matando el proceso que lo ocupe.
# Uso:  powershell -ExecutionPolicy Bypass -File scripts/kill-8080.ps1
$conn = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($conn) {
    foreach ($c in $conn) {
        Stop-Process -Id $c.OwningProcess -Force -ErrorAction SilentlyContinue
        Write-Host "Proceso $($c.OwningProcess) detenido. Puerto 8080 liberado."
    }
} else {
    Write-Host "Puerto 8080 ya esta libre."
}