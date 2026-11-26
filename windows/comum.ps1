# Funcoes usadas pelo lab.ps1
$ProgressPreference = "SilentlyContinue"
$raiz = Split-Path -Parent $PSScriptRoot
Set-Location $raiz

$jdk = Get-ChildItem "C:\Program Files\Microsoft", "C:\Program Files\Eclipse Adoptium", "C:\Program Files\Java" -Directory -Filter "jdk-21*" -ErrorAction SilentlyContinue | Select-Object -First 1
if ($jdk) { $java = Join-Path $jdk.FullName "bin\java.exe"; $env:JAVA_HOME = $jdk.FullName } else { $java = "java" }

$env:RABBITMQ_USER = "guest"
$env:RABBITMQ_PASS = "guest"

$apps = [ordered]@{
  "produtor"      = @("produtor", 8080)
  "portal"        = @("portal", 8081)
  "comunicacao"   = @("comunicacao", 8082)
  "triagem"       = @("triagem", 8083)
  "auditoria"     = @("auditoria", 8084)
  "indicadores-1" = @("indicadores", 8085)
  "indicadores-2" = @("indicadores", 8086)
}

function Pid-Da-Porta($porta) {
  $c = Get-NetTCPConnection -LocalPort $porta -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
  if ($c) { return $c.OwningProcess } else { return $null }
}

function Responde($porta) {
  try { Invoke-WebRequest "http://localhost:$porta/" -UseBasicParsing -TimeoutSec 2 | Out-Null; return $true }
  catch { return ($_.Exception.Response -ne $null) }
}

function Compilar-Se-Preciso {
  if (-not (Test-Path "target\laboratorio.jar")) {
    Write-Host "Projeto ainda nao compilado. Compilando (pode levar alguns minutos)..."
    & .\mvnw.cmd -q -DskipTests package
  }
}

function Ligar($n) {
  $perfil = $apps[$n][0]; $porta = $apps[$n][1]
  if (Pid-Da-Porta $porta) { Write-Host ("  {0,-14} ja estava rodando" -f $n); return }
  New-Item -ItemType Directory -Force -Path "logs" | Out-Null
  Start-Process $java -WindowStyle Hidden `
    -ArgumentList "-jar target\laboratorio.jar --spring.profiles.active=$perfil --server.port=$porta --app.instancia=$n" `
    -RedirectStandardOutput "logs\$n.log" -RedirectStandardError "logs\$n-erro.log"
  Write-Host -NoNewline ("  {0,-14} ligando" -f $n)
  for ($i = 0; $i -lt 90; $i++) {
    if (Responde $porta) { Write-Host " OK" -ForegroundColor Green; return }
    Write-Host -NoNewline "."
    Start-Sleep -Seconds 1
  }
  Write-Host " ERRO (veja logs\$n.log)" -ForegroundColor Red
}

function Desligar($n) {
  $p = Pid-Da-Porta $apps[$n][1]
  if ($p) { Stop-Process -Id $p -Force; Write-Host ("  {0,-14} desligado" -f $n) -ForegroundColor Yellow }
  else { Write-Host ("  {0,-14} ja estava desligado" -f $n) }
}

function Status {
  Write-Host ""
  foreach ($n in $apps.Keys) {
    $porta = $apps[$n][1]
    if (Pid-Da-Porta $porta) { Write-Host ("  {0,-14} porta {1}  rodando" -f $n, $porta) -ForegroundColor Green }
    else { Write-Host ("  {0,-14} porta {1}  parado" -f $n, $porta) -ForegroundColor DarkGray }
  }
  Write-Host ""
}
