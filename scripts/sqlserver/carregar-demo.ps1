[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidatePattern('\Aeducria_demo_[a-zA-Z0-9_]{1,60}\z')]
    [string]$Banco,

    [ValidatePattern('\A(?:(?:lpc:)?(?:\.|\(local\)|localhost|127\.0\.0\.1)(?:\\[a-zA-Z0-9_]{1,32})?|tcp:(?:localhost|127\.0\.0\.1),[0-9]{1,5})\z')]
    [string]$Servidor = 'lpc:.\SQLEXPRESS',

    [switch]$ConfiarCertificadoLocal
)

$ErrorActionPreference = 'Stop'
$sqlcmd = Get-Command sqlcmd.exe -CommandType Application
$raiz = Join-Path $PSScriptRoot '../..'
$arquivos = @(
    'criar_professor_demo.sql'
    'criar_professor_portugues_demo.sql'
    'criar_alunos_demo.sql'
    'criar_desempenho_assuntos_demo.sql'
    'criar_historico_demo.sql'
)

$lotes = @((Get-Content -Raw -Encoding UTF8 -LiteralPath (Join-Path $PSScriptRoot 'carga-demo-inicio.sql')))
foreach ($arquivo in $arquivos) {
    $lotes += (Get-Content -Raw -Encoding UTF8 -LiteralPath (Join-Path $raiz "backend/src/test/sql/$arquivo")) + "`nGO`n"
}
$lotes += "COMMIT TRANSACTION;`nGO`n"

# sqlcmd -i recebe UTF-8 sem BOM em vez de depender da codificacao do pipe do PowerShell.
$temporario = [System.IO.Path]::GetTempFileName()
try {
    [System.IO.File]::WriteAllText($temporario, ($lotes -join "`n"), [System.Text.UTF8Encoding]::new($false))
    $opcoes = @('-S', $Servidor, '-E', '-d', $Banco, '-N', '-I', '-b', '-r', '0', '-x', '-f', '65001', '-l', '10', '-i', $temporario)
    if ($ConfiarCertificadoLocal) { $opcoes += '-C' }
    & $sqlcmd.Path @opcoes
    if ($LASTEXITCODE -ne 0) { throw "A carga falhou (sqlcmd: $LASTEXITCODE)." }
    Write-Output "Carga de demonstracao concluida no banco $Banco."
} finally {
    [System.IO.File]::Delete($temporario)
}
