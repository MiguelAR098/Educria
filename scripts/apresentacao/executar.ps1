param(
    [switch]$NaoAbrir,
    [switch]$Parar,
    [switch]$Diagnostico
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'comum.ps1')

function Confirmar-PortaApresentacao {
    $enderecos = [Net.NetworkInformation.IPGlobalProperties]::GetIPGlobalProperties().GetActiveTcpListeners()
    $ouvintes = @($enderecos | Where-Object Port -EQ 3000)
    if (!$ouvintes) { return }

    $idWeb = Invoke-ComposeApresentacao @('ps', '-q', 'web')
    if ($idWeb) {
        $formato = '{{.State.Running}}|{{json .NetworkSettings.Ports}}'
        $estado, $jsonPortas = (Invoke-DockerApresentacao @('container', 'inspect', '--format', $formato, $idWeb)) -split '\|', 2
        $portas = @(($jsonPortas | ConvertFrom-Json).'3000/tcp')
        if ($estado -ceq 'true' -and $portas.Count -eq 1 -and $portas[0].HostIp -eq '127.0.0.1' -and $portas[0].HostPort -eq '3000' -and
            !($ouvintes | Where-Object { $_.Address.ToString() -ne '127.0.0.1' })) { return }
    }
    throw 'A porta local 3000 já está em uso. Identifique e feche o aplicativo responsável ou use outro PC autorizado.'
}

function Obter-StatusApresentacao([string]$Caminho) {
    $pedido = [Net.HttpWebRequest]::Create("$UrlApresentacao$Caminho")
    $pedido.AllowAutoRedirect = $false
    $pedido.Proxy = $null
    $pedido.Timeout = 5000
    try {
        $resposta = $pedido.GetResponse()
    } catch [Net.WebException] {
        $resposta = $_.Exception.Response
        if (!$resposta) { return 0 }
    }
    try {
        return [int]$resposta.StatusCode
    } finally {
        $resposta.Dispose()
    }
}

function Mostrar-DiagnosticoApresentacao {
    Invoke-ComposeApresentacao @('ps', '-a')
    $segredos = @(foreach ($nome in $ArquivosSegredosApresentacao.Keys) {
        $arquivo = Join-Path $PastaSegredosApresentacao $nome
        if (Test-Path -LiteralPath $arquivo -PathType Leaf) {
            Confirmar-AclSegredos $arquivo
            [IO.File]::ReadAllText($arquivo)
        }
    })
    $saude = @(foreach ($servico in $ImagensApresentacao.Keys) {
        $id = Invoke-ComposeApresentacao @('ps', '-aq', $servico)
        if ($id) {
            $estado = Invoke-DockerApresentacao @('container', 'inspect', '--format', '{{json .State.Health}}', $id) | ConvertFrom-Json
            if ($estado) {
                $ultimo = $estado.Log | Select-Object -Last 1
                "Saúde $servico $($estado.Status) $($ultimo.Output)"
            }
        }
    })
    $argumentos = @('logs', '--tail', '100', '--no-color', 'db', 'api', 'web')
    $preferencia = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $linhas = @(Invoke-ComposeApresentacao $argumentos 2>&1)
    } finally {
        $ErrorActionPreference = $preferencia
    }
    foreach ($linha in ($saude + $linhas)) {
        $texto = $linha.ToString()
        foreach ($segredo in $segredos) {
            if ($segredo) { $texto = $texto.Replace($segredo, '[oculto]') }
        }
        $texto -replace 'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+', '[token oculto]'
    }
}

try {
    Confirmar-DockerApresentacao
    Confirmar-ProjetoApresentacao
    Confirmar-ConfiguracaoApresentacao
    if ($Parar) {
        Invoke-ComposeApresentacao @('stop', '--timeout', '30', 'web', 'api', 'db')
        Write-Host 'A apresentação parou. Os dados e as credenciais locais foram preservados.'
        exit 0
    }
    if ($Diagnostico) {
        Mostrar-DiagnosticoApresentacao
        exit 0
    }

    Confirmar-SegredosApresentacao
    foreach ($tag in $ImagensApresentacao.Values) { $null = Obter-ImagemApresentacao $tag }
    Confirmar-PortaApresentacao
    Write-Host 'Iniciando a apresentação com imagens locais. A primeira execução pode demorar alguns minutos.'
    Invoke-ComposeApresentacao @('up', '-d', '--no-build', '--pull', 'never', '--wait', '--wait-timeout', '300', 'db', 'api', 'web')

    $login = Obter-StatusApresentacao '/login'
    $protegido = Obter-StatusApresentacao '/api/aluno/desempenho'
    if ($login -ne 200 -or $protegido -notin @(401, 403)) {
        throw "A validação HTTP falhou. Login retornou $login e a rota protegida retornou $protegido (0 indica sem resposta)."
    }
    Write-Host "EDUCRIA pronto em http://localhost:3000 (login $login, API protegida $protegido)."
    if (!$NaoAbrir) { Start-Process 'http://localhost:3000' }
    exit 0
} catch {
    [Console]::Error.WriteLine($_.Exception.Message)
    [Console]::Error.WriteLine('Os dados foram mantidos. Para coletar o estado e os logs, execute apresentar.cmd -Diagnostico.')
    exit 1
}
