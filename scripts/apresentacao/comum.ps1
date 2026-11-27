$RaizApresentacao = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$PastaSegredosApresentacao = Join-Path $RaizApresentacao '.educria-apresentacao'
$UrlApresentacao = 'http://127.0.0.1:3000'
$NomeApresentacao = 'educria-apresentacao'
$ComposeApresentacao = Join-Path $RaizApresentacao 'compose.apresentacao.yaml'
$ImagensApresentacao = [ordered]@{
    db = 'educria-apresentacao-sql:1'
    api = 'educria-apresentacao-api:1'
    web = 'educria-apresentacao-web:1'
}
$ArquivosSegredosApresentacao = [ordered]@{
    'sql-senha' = '^Aa1[0-9a-f]{48}$'
    'jwt-segredo' = '^[A-Za-z0-9+/]{64}$'
    'demo-senha' = '^Aa1[0-9a-f]{48}$'
}

function Confirmar-DockerApresentacao {
    $docker = Get-Command docker.exe -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if (!$docker) { throw 'Docker Desktop ausente. Use um PC autorizado com Docker Desktop atualizado.' }
    $script:ExecutavelDockerApresentacao = $docker.Source

    # Uma instalação local jamais deve herdar um contexto Docker remoto.
    Get-ChildItem Env: | Where-Object { $_.Name -like 'COMPOSE_*' -or $_.Name -match '^DOCKER_(HOST|CONTEXT|TLS|TLS_VERIFY|CERT_PATH)$' } |
        ForEach-Object { Remove-Item -LiteralPath ('Env:' + $_.Name) }
    $env:COMPOSE_DISABLE_ENV_FILE = '1'
    $env:DOCKER_DEFAULT_PLATFORM = 'linux/amd64'

    # No PowerShell 5.1, stderr de um executável redirecionado pode virar erro terminante.
    $ErrorActionPreference = 'Continue'
    $script:HostDockerApresentacao = 'npipe:////./pipe/dockerDesktopLinuxEngine'
    $formato = '{{.OSType}}|{{.Architecture}}|{{.OperatingSystem}}|{{.ServerVersion}}'
    $info = & $script:ExecutavelDockerApresentacao --host $script:HostDockerApresentacao info --format $formato 2>$null
    if ($LASTEXITCODE -ne 0) {
        $script:HostDockerApresentacao = 'npipe:////./pipe/docker_engine'
        $info = & $script:ExecutavelDockerApresentacao --host $script:HostDockerApresentacao info --format $formato 2>$null
    }
    if ($LASTEXITCODE -ne 0) { throw 'Inicie o Docker Desktop local e aguarde o engine Linux ficar disponível.' }
    $dados = "$info" -split '\|', 4
    if ($dados.Count -ne 4 -or $dados[0] -cne 'linux' -or $dados[1] -notin @('amd64', 'x86_64') -or
        $dados[2] -notmatch '^Docker Desktop(?:\s|$)') {
        throw 'O pacote precisa do engine Linux x64 local do Docker Desktop.'
    }
    if ($dados[3] -notmatch '^v?(\d+)\.' -or [int]$Matches[1] -lt 28) {
        throw 'Use Docker Engine 28 ou posterior no Docker Desktop para manter a porta publicada restrita ao loopback.'
    }

    $versao = Invoke-DockerApresentacao @('compose', 'version', '--short')
    if ($versao -notmatch '^v?(?:2|5)\.[0-9]+(?:\.[0-9]+)?(?:[-+].*)?$') {
        throw 'Este pacote precisa do Docker Compose v2 ou v5.'
    }
    $up = Invoke-DockerApresentacao @('compose', 'up', '--help')
    if (($up -join "`n") -notmatch '--wait-timeout') {
        throw 'Use uma versão do Docker Compose com suporte a up --wait-timeout.'
    }
}

function Invoke-DockerApresentacao([string[]]$Argumentos) {
    & $script:ExecutavelDockerApresentacao --host $script:HostDockerApresentacao @Argumentos
    if ($LASTEXITCODE -ne 0) { throw "Docker falhou (código $LASTEXITCODE)." }
}

function Invoke-ComposeApresentacao([string[]]$Argumentos) {
    $opcoes = @('compose', '--project-name', $NomeApresentacao,
        '--project-directory', $RaizApresentacao, '--file', $ComposeApresentacao)
    Invoke-DockerApresentacao ($opcoes + $Argumentos)
}

function Confirmar-ProjetoApresentacao {
    $ids = @(Invoke-DockerApresentacao @('container', 'ls', '-aq', '--filter', "label=com.docker.compose.project=$NomeApresentacao"))
    foreach ($id in $ids) {
        $rotulos = Invoke-DockerApresentacao @('container', 'inspect', '--format', '{{json .Config.Labels}}', $id) | ConvertFrom-Json
        $servico = $rotulos.'com.docker.compose.service'
        $pasta = $rotulos.'com.docker.compose.project.working_dir'
        if ($rotulos.'br.educria.apresentacao' -cne '1' -or !$servico -or !$ImagensApresentacao.Contains($servico) -or
            !$pasta -or $pasta.Replace('/', '\').TrimEnd('\') -ine $RaizApresentacao.TrimEnd('\')) {
            throw 'Já existe um projeto Docker com o nome reservado em outro local ou sem o rótulo da apresentação.'
        }
    }
    $volumes = @(Invoke-DockerApresentacao @('volume', 'ls', '-q', '--filter', 'name=^educria-apresentacao_dados$'))
    foreach ($volume in $volumes) {
        $rotulos = Invoke-DockerApresentacao @('volume', 'inspect', '--format', '{{json .Labels}}', $volume) | ConvertFrom-Json
        if ($rotulos.'com.docker.compose.project' -cne $NomeApresentacao -or $rotulos.'com.docker.compose.volume' -cne 'dados') {
            throw 'O volume de nome reservado não pertence ao projeto Compose da apresentação.'
        }
    }
}

function Confirmar-ConfiguracaoApresentacao {
    if (!(Test-Path -LiteralPath $ComposeApresentacao -PathType Leaf)) {
        throw 'Arquivo compose.apresentacao.yaml não encontrado. Extraia o pacote completo.'
    }
    $json = Invoke-ComposeApresentacao @('config', '--format', 'json') | Out-String
    $modelo = $json | ConvertFrom-Json
    $nomes = @($modelo.services.PSObject.Properties.Name)
    if ($nomes.Count -ne 3 -or (Compare-Object $nomes @($ImagensApresentacao.Keys))) {
        throw 'O pacote deve conter somente os serviços db, api e web.'
    }
    foreach ($servico in $ImagensApresentacao.Keys) {
        $config = $modelo.services.$servico
        if ($config.image -cne $ImagensApresentacao[$servico] -or $config.platform -cne 'linux/amd64' -or
            $config.labels.'br.educria.apresentacao' -cne '1' -or $config.network_mode -eq 'host') {
            throw "Configuração de imagem, plataforma ou isolamento inesperada no serviço $servico."
        }
        if ($servico -ne 'web' -and $config.ports) { throw "O serviço $servico tem porta publicada." }
    }
    $portas = @($modelo.services.web.ports)
    if ($portas.Count -ne 1 -or $portas[0].host_ip -ne '127.0.0.1' -or $portas[0].published -ne '3000' -or
        $portas[0].target -ne 3000 -or $portas[0].protocol -ne 'tcp') {
        throw 'A única publicação permitida é 127.0.0.1:3000:3000 no serviço web.'
    }
    if ($modelo.volumes.dados.name -cne "${NomeApresentacao}_dados" -or $modelo.volumes.dados.external -or
        $modelo.networks.PSObject.Properties.Count -lt 1) {
        throw 'O volume e a rede da apresentação devem ser isolados.'
    }
    foreach ($rede in $modelo.networks.PSObject.Properties) {
        if ($rede.Value.external -or !$rede.Value.name.StartsWith("${NomeApresentacao}_")) {
            throw 'A configuração da apresentação referencia uma rede externa ou de outro projeto.'
        }
    }
}

function Obter-ImagemApresentacao([string]$Tag, [switch]$Opcional) {
    $formato = '{{.Id}}|{{.Os}}/{{.Architecture}}|{{.Size}}'
    $ErrorActionPreference = 'Continue'
    $resultado = & $script:ExecutavelDockerApresentacao --host $script:HostDockerApresentacao image inspect --format $formato $Tag 2>$null
    if ($LASTEXITCODE -ne 0 -and $Opcional) { return }
    if ($LASTEXITCODE -ne 0) { throw "Imagem local ausente: $Tag. Use preparar-apresentacao.cmd antes de iniciar." }
    $partes = $resultado -split '\|'
    if ($partes.Count -ne 3 -or $partes[0] -cnotmatch '^sha256:[0-9a-f]{64}$' -or $partes[1] -cne 'linux/amd64' -or
        $partes[2] -notmatch '^[1-9][0-9]*$') {
        throw "A imagem $Tag não corresponde à plataforma linux/amd64."
    }
    [pscustomobject]@{ tag = $Tag; id = $partes[0]; plataforma = $partes[1]; bytes = [int64]$partes[2] }
}

function Obter-IdentidadesSegredos {
    @(
        [Security.Principal.WindowsIdentity]::GetCurrent().User
        [Security.Principal.SecurityIdentifier]::new('S-1-5-18')
    )
}

function Confirmar-RecursoSegredos([string]$Caminho, [switch]$Pasta) {
    $item = Get-Item -LiteralPath $Caminho -Force
    if (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -or $item.PSIsContainer -ne $Pasta.IsPresent) {
        throw 'Um caminho reservado da configuração local é um link ou recurso incompatível.'
    }
}

function Confirmar-AclSegredos([string]$Caminho, [switch]$Pasta) {
    Confirmar-RecursoSegredos $Caminho -Pasta:$Pasta
    $acl = if ($Pasta) {
    [IO.Directory]::GetAccessControl($Caminho)
    } else {
    [IO.File]::GetAccessControl($Caminho)
    }
    $permitidas = @(Obter-IdentidadesSegredos | ForEach-Object Value)
    $regras = @($acl.GetAccessRules($true, $true, [Security.Principal.SecurityIdentifier]))
    if (($Pasta -and !$acl.AreAccessRulesProtected) -or !$regras -or
        @($regras | Where-Object { $_.IdentityReference.Value -notin $permitidas }).Count -ne 0) {
        throw 'A configuração local tem permissões inesperadas. Verifique a pasta com a equipe que preparou este PC.'
    }
}

function Confirmar-SegredosApresentacao {
    if (!(Test-Path -LiteralPath $PastaSegredosApresentacao -PathType Container)) {
        throw 'Configuração local ausente. Execute preparar-apresentacao.cmd antes de iniciar.'
    }
    Confirmar-AclSegredos $PastaSegredosApresentacao -Pasta
    foreach ($nome in $ArquivosSegredosApresentacao.Keys) {
        $caminho = Join-Path $PastaSegredosApresentacao $nome
        if (!(Test-Path -LiteralPath $caminho -PathType Leaf)) { throw "Arquivo local ausente: $nome." }
        Confirmar-AclSegredos $caminho
        if ([IO.File]::ReadAllText($caminho) -cnotmatch ($ArquivosSegredosApresentacao[$nome] + '\z')) {
            throw "Arquivo local malformado: $nome."
        }
    }
}
