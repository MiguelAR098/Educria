param(
    [Parameter(Mandatory)]
    [ValidatePattern('\Aeducria_(?:sqlserver_teste|demo)_[a-zA-Z0-9_]{1,60}\z')]
    [string]$Banco,
    [Parameter(Mandatory)]
    [ValidateSet('Criar','Demonstracao','Verificar','Dicionario')]
    [string]$Etapa,
    [ValidatePattern('\A(?:(?:lpc:)?(?:\.|\(local\)|localhost|127\.0\.0\.1)(?:\\[a-zA-Z0-9_]{1,32})?|tcp:(?:localhost|127\.0\.0\.1),[0-9]{1,5})\z')]
    [string]$Servidor = 'lpc:.\SQLEXPRESS'
)

$ErrorActionPreference = 'Stop'
if ($Etapa -ne 'Criar' -and $Banco -notmatch '\Aeducria_sqlserver_teste_') {
    throw 'As provas academicas exigem uma base educria_sqlserver_teste_.'
}
$conexao = [System.Data.SqlClient.SqlConnectionStringBuilder]::new()
$conexao['Data Source'] = $Servidor
$conexao['Initial Catalog'] = if ($Etapa -eq 'Criar') { 'master' } else { $Banco }
$conexao['Integrated Security'] = $true
$conexao['Connect Timeout'] = 5
$conexao['Application Name'] = 'Educria Acadêmico'
$c = [System.Data.SqlClient.SqlConnection]::new($conexao.ConnectionString)

function ExecutarScript([string]$Arquivo, [bool]$MostrarResultado) {
    $sql = Get-Content -Raw -Encoding UTF8 -LiteralPath (Join-Path $PSScriptRoot "academico/$Arquivo")
    foreach ($lote in ($sql -split '(?im)^\s*GO\s*$')) {
        if ([string]::IsNullOrWhiteSpace($lote)) { continue }
        $q = $c.CreateCommand()
        try {
            $q.CommandText = $lote
            if ($Arquivo -eq '01-dados-demo.sql') {
                $q.Parameters.Add('@HashDemo', [System.Data.SqlDbType]::NVarChar, 60).Value = $env:EDUCRIA_DEMO_BCRYPT
            }
            if (!$MostrarResultado) {
                [void]$q.ExecuteNonQuery()
                continue
            }
            $leitor = $q.ExecuteReader()
            try {
                do {
                    if ($leitor.FieldCount -gt 0) {
                        $colunas = for ($i=0; $i -lt $leitor.FieldCount; $i++) { $leitor.GetName($i) }
                        Write-Output ($colunas -join ' | ')
                        while ($leitor.Read()) {
                            $valores = for ($i=0; $i -lt $leitor.FieldCount; $i++) { $leitor.GetValue($i) }
                            Write-Output ($valores -join ' | ')
                        }
                    }
                } while ($leitor.NextResult())
            } finally { $leitor.Dispose() }
        } finally { $q.Dispose() }
    }
}

try {
    $c.Open()
    $q = $c.CreateCommand()
    $q.Parameters.Add('@banco', [System.Data.SqlDbType]::NVarChar, 128).Value = $Banco
    if ($Etapa -eq 'Criar') {
        $q.CommandText = 'SELECT DB_ID(@banco)'
        if ($q.ExecuteScalar() -ne [DBNull]::Value) { throw 'Já existe um banco com esse nome.' }
        $q.Parameters.Clear()
        # O nome é validado pelo padrão do parâmetro e o comando cria somente uma base nova.
        $q.CommandText = "CREATE DATABASE [$Banco] COLLATE Latin1_General_100_CS_AS_SC"
        [void]$q.ExecuteNonQuery()
        $c.ChangeDatabase($Banco)
        $q.CommandText = "EXEC sys.sp_addextendedproperty @name=N'EducriaAmbiente', @value=N'descartavel'"
        [void]$q.ExecuteNonQuery()
        Write-Output "Banco descartável criado: $Banco"
        return
    }
    $q.CommandText = @'
SELECT COUNT(*) FROM sys.extended_properties
WHERE class=0 AND name=N'EducriaAmbiente' AND CONVERT(nvarchar(30),value)=N'descartavel'
  AND DB_NAME()=@banco
'@
    if ($q.ExecuteScalar() -ne 1) { throw 'Banco sem identificação de ambiente descartável.' }
    if ($Etapa -eq 'Demonstracao') {
        if ($env:EDUCRIA_DEMO_BCRYPT -cnotmatch '^\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}$') {
            throw 'Defina EDUCRIA_DEMO_BCRYPT com um hash BCrypt de 60 caracteres.'
        }
        ExecutarScript '01-dados-demo.sql' $false
        ExecutarScript '02-automacao.sql' $false
        Write-Output "Demonstração disponível em $Banco"
    } elseif ($Etapa -eq 'Dicionario') {
        ExecutarScript '05-dicionario.sql' $true
    } else {
        ExecutarScript '03-verificacao.sql' $true
        ExecutarScript '04-integridade.sql' $true
    }
} finally { $c.Dispose() }
