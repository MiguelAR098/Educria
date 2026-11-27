param(
    [Parameter(Mandatory)]
    [ValidateSet('Construir', 'Exportar', 'Importar')]
    [string]$Modo,
    [string]$Arquivo,
    [string]$PastaDiscoDocker
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'comum.ps1')

function Confirmar-Espaco([string]$Caminho, [long]$Minimo) {
    $unidade = [IO.DriveInfo]::new([IO.Path]::GetPathRoot($Caminho))
    if (!$unidade.IsReady -or $unidade.AvailableFreeSpace -lt $Minimo) {
        $minimoGiB = [Math]::Ceiling($Minimo / 1GB)
        throw "Espaço insuficiente em $($unidade.Name). Libere pelo menos $minimoGiB GiB."
    }
    Write-Host ('Espaço livre em {0} {1:N1} GiB.' -f $unidade.Name, ($unidade.AvailableFreeSpace / 1GB))
}

function Confirmar-EspacoDocker([long]$Minimo) {
    $pastaDesktop = if ($PastaDiscoDocker) {
        (Resolve-Path -LiteralPath $PastaDiscoDocker).ProviderPath
    } else {
        Join-Path $env:LOCALAPPDATA 'Docker\wsl'
    }
    Confirmar-Espaco $pastaDesktop $Minimo
    if (!$PastaDiscoDocker) { Write-Host 'Se o armazenamento do Docker Desktop foi movido, use -PastaDiscoDocker com o local configurado.' }
}

function Nova-ConfiguracaoApresentacao {
    if (!(Test-Path -LiteralPath $PastaSegredosApresentacao)) {
        $acl = [Security.AccessControl.DirectorySecurity]::new()
        $acl.SetAccessRuleProtection($true, $false)
        foreach ($sid in Obter-IdentidadesSegredos) {
            $regra = [Security.AccessControl.FileSystemAccessRule]::new(
                $sid, 'FullControl', 'ContainerInherit, ObjectInherit', 'None', 'Allow')
            $acl.AddAccessRule($regra)
        }
        # Windows PowerShell usa este overload para aplicar a ACL na criação da pasta.
        [void][IO.Directory]::CreateDirectory($PastaSegredosApresentacao, $acl)
    }
    Confirmar-AclSegredos $PastaSegredosApresentacao -Pasta

    $faltantes = @($ArquivosSegredosApresentacao.Keys | Where-Object {
        !(Test-Path -LiteralPath (Join-Path $PastaSegredosApresentacao $_))
    })
    if ($faltantes.Count -gt 0) {
        $volumes = @(Invoke-DockerApresentacao @('volume', 'ls', '-q', '--filter', 'name=^educria-apresentacao_dados$'))
        if ($volumes.Count -gt 0) {
            throw 'Já existem dados da apresentação. Recupere os arquivos de credenciais originais deste PC.'
        }
    }

    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        foreach ($nome in $faltantes) {
            $bytes = New-Object byte[] $(if ($nome -eq 'jwt-segredo') { 48 } else { 24 })
            $rng.GetBytes($bytes)
            $segredo = if ($nome -eq 'jwt-segredo') {
                [Convert]::ToBase64String($bytes)
            } else {
                'Aa1' + [BitConverter]::ToString($bytes).Replace('-', '').ToLowerInvariant()
            }
            $saida = [IO.File]::Open((Join-Path $PastaSegredosApresentacao $nome), 'CreateNew', 'Write', 'None')
            try {
                $conteudo = [Text.Encoding]::ASCII.GetBytes($segredo)
                $saida.Write($conteudo, 0, $conteudo.Length)
            } finally { $saida.Dispose() }
        }
    } finally { $rng.Dispose() }
    Confirmar-SegredosApresentacao
    Write-Host 'Configuração local protegida disponível. Guarde os três arquivos somente no PC preparado.'
}

function Mostrar-Tamanhos($Imagens) {
    foreach ($imagem in $Imagens) {
        Write-Host ('{0} {1:N2} GiB ({2})' -f $imagem.tag, ($imagem.bytes / 1GB), $imagem.plataforma)
    }
}

function Confirmar-Manifesto($Manifesto) {
    $esperadas = @($ImagensApresentacao.Values)
    $entradas = @($Manifesto.imagens)
    if ($Manifesto.formato -ne 1 -or $entradas.Count -ne $esperadas.Count -or
        (Compare-Object $esperadas @($entradas | ForEach-Object tag) -CaseSensitive)) {
        throw 'O manifesto precisa listar exatamente as três imagens de apresentação.'
    }
    foreach ($imagem in $entradas) {
        if ($imagem.plataforma -cne 'linux/amd64' -or $imagem.id -cnotmatch '^sha256:[0-9a-f]{64}$' -or
            "$($imagem.bytes)" -cnotmatch '^[1-9][0-9]*$') {
            throw 'O manifesto contém imagem ou plataforma incompatível.'
        }
    }
}

function Confirmar-ListaNoTar([string]$Caminho, $Manifesto) {
    $tar = Join-Path $env:SystemRoot 'System32\tar.exe'
    if (!(Test-Path -LiteralPath $tar -PathType Leaf)) { throw 'Importação requer o tar.exe incluído no Windows.' }
    $json = & $tar -xOf $Caminho manifest.json
    if ($LASTEXITCODE -ne 0) { throw 'O arquivo não contém o manifesto de uma exportação docker image save.' }
    $imagens = @(($json | Out-String | ConvertFrom-Json))
    $tags = @($imagens | ForEach-Object { $_.RepoTags })
    if ($imagens.Count -ne 3 -or $tags.Count -ne 3 -or (Compare-Object @($ImagensApresentacao.Values) $tags -CaseSensitive)) {
        throw 'O arquivo Docker precisa conter somente as imagens reservadas da apresentação.'
    }
    foreach ($imagem in $imagens) {
        $tag = @($imagem.RepoTags)[0]
        $esperado = $Manifesto.imagens | Where-Object tag -CEQ $tag
        $digest = $esperado.id.Substring(7)
        if ($imagem.Config -cnotin @("$digest.json", "blobs/sha256/$digest")) {
            throw "O conteúdo da imagem $tag não confere com o manifesto."
        }
    }
}

try {
    Confirmar-DockerApresentacao
    Confirmar-ProjetoApresentacao
    Confirmar-ConfiguracaoApresentacao

    if ($Modo -ne 'Construir') {
        if ([string]::IsNullOrWhiteSpace($Arquivo)) { throw 'Informe -Arquivo com o caminho do arquivo .tar.' }
        $caminho = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($Arquivo)
        if ([IO.Path]::GetExtension($caminho) -ine '.tar' -or !(Test-Path -LiteralPath (Split-Path -Parent $caminho) -PathType Container)) {
            throw 'Informe um arquivo .tar em uma pasta local já existente.'
        }
        $manifesto = "$caminho.json"
    }

    switch ($Modo) {
        'Construir' {
            Confirmar-EspacoDocker 12GB
            Nova-ConfiguracaoApresentacao
            Invoke-ComposeApresentacao @('build', '--pull', 'db', 'api', 'web')
            Mostrar-Tamanhos @($ImagensApresentacao.Values | ForEach-Object { Obter-ImagemApresentacao $_ })
        }
        'Exportar' {
            if ((Test-Path -LiteralPath $caminho) -or (Test-Path -LiteralPath $manifesto)) {
                throw 'O arquivo de transporte ou seu manifesto já existe. Escolha outro nome.'
            }
            $imagens = @($ImagensApresentacao.Values | ForEach-Object { Obter-ImagemApresentacao $_ })
            [long]$bytesImagens = ($imagens | Measure-Object bytes -Sum).Sum
            Confirmar-Espaco $caminho ($bytesImagens + 1GB)
            Invoke-DockerApresentacao (@('image', 'save', '--output', $caminho) + @($ImagensApresentacao.Values))
            $metadados = [ordered]@{
                formato = 1
                sha256 = (Get-FileHash -LiteralPath $caminho -Algorithm SHA256).Hash
                imagens = $imagens
            }
            [IO.File]::WriteAllText($manifesto, ($metadados | ConvertTo-Json -Depth 4), [Text.Encoding]::ASCII)
            Write-Host ('Arquivo de transporte {0:N2} GiB. Transfira o arquivo .tar e o .tar.json juntos.' -f ((Get-Item -LiteralPath $caminho).Length / 1GB))
            Mostrar-Tamanhos $imagens
        }
        'Importar' {
            if (!(Test-Path -LiteralPath $caminho -PathType Leaf) -or !(Test-Path -LiteralPath $manifesto -PathType Leaf)) {
                throw 'O arquivo .tar e seu manifesto .tar.json precisam estar na mesma pasta.'
            }
            $metadados = Get-Content -LiteralPath $manifesto -Raw | ConvertFrom-Json
            Confirmar-Manifesto $metadados
            if ((Get-FileHash -LiteralPath $caminho -Algorithm SHA256).Hash -ine $metadados.sha256) {
                throw 'A soma SHA-256 não confere. Copie o arquivo e o manifesto novamente.'
            }
            Confirmar-ListaNoTar $caminho $metadados
            [long]$bytesImagens = ($metadados.imagens | Measure-Object bytes -Sum).Sum
            Confirmar-EspacoDocker ($bytesImagens + 6GB)
            foreach ($imagem in $metadados.imagens) {
                $existente = Obter-ImagemApresentacao $imagem.tag -Opcional
                if ($existente -and $existente.id -cne $imagem.id) {
                    $dependentes = @(Invoke-DockerApresentacao @('container', 'ls', '-aq', '--filter', "ancestor=$($existente.id)"))
                    if ($dependentes.Count -gt 0) {
                        throw "Há um contêiner que usa a tag $($imagem.tag) com outro ID. Mantenha os dados e confira a versão com a equipe."
                    }
                }
            }
            Invoke-DockerApresentacao @('image', 'load', '--input', $caminho)
            $carregadas = @($metadados.imagens | ForEach-Object {
                $imagem = Obter-ImagemApresentacao $_.tag
                if ($imagem.id -cne $_.id) { throw "Identidade divergente após carregar $($_.tag)." }
                $imagem
            })
            Nova-ConfiguracaoApresentacao
            Mostrar-Tamanhos $carregadas
        }
    }
    if ($Modo -ne 'Exportar') { Write-Host 'Para ensaiar e verificar a prontidão, execute apresentar.cmd -NaoAbrir.' }
    exit 0
} catch {
    [Console]::Error.WriteLine($_.Exception.Message)
    exit 1
}
