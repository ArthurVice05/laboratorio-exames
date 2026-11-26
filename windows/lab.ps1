param([string]$acao = "status", [string]$nome = "todos")
. "$PSScriptRoot\comum.ps1"
Compilar-Se-Preciso
if ($nome -eq "todos") { $lista = @($apps.Keys) } else { $lista = @($nome) }
switch ($acao) {
  "ligar"    { foreach ($n in $lista) { Ligar $n }; Status }
  "desligar" { foreach ($n in $lista) { Desligar $n }; Status }
  default    { Status }
}
