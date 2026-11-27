param([switch]$Reiniciar)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'comum.ps1')

function Testar-Api([string]$Rota, [int]$Esperado, [string]$Token, [hashtable]$Corpo) {
    $metodo = if ($Corpo) { [Net.Http.HttpMethod]::Post } else { [Net.Http.HttpMethod]::Get }
    $pedido = [Net.Http.HttpRequestMessage]::new($metodo, "$UrlApresentacao$Rota")
    try {
        $pedido.Headers.Add('X-Educria-Client', 'web')
        if ($Token) { $pedido.Headers.Authorization = [Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer', $Token) }
        if ($Corpo) {
            $pedido.Content = [Net.Http.StringContent]::new(
                ($Corpo | ConvertTo-Json -Compress), [Text.Encoding]::UTF8, 'application/json')
        }
        try {
            $resposta = $cliente.SendAsync($pedido).GetAwaiter().GetResult()
        } catch {
            throw "Falha ao consultar $Rota na apresentação local."
        }
        try {
            if ([int]$resposta.StatusCode -ne $Esperado) {
                throw "A rota $Rota retornou HTTP $([int]$resposta.StatusCode). Esperado HTTP $Esperado."
            }
            if ($Esperado -eq 200) {
                $json = $resposta.Content.ReadAsStringAsync().GetAwaiter().GetResult()
                try { return ($json | ConvertFrom-Json) }
                catch { throw "Resposta JSON inválida em $Rota." }
            }
        } finally {
            $resposta.Dispose()
        }
    } finally {
        $pedido.Dispose()
    }
}

function Obter-AssinaturaDoBanco {
    $saida = @(Invoke-ComposeApresentacao @('exec', '-T', 'db', 'bash', '/opt/educria/banco.sh', 'verificar'))
    $assinatura = @($saida | Where-Object { $_ -cmatch '^SNAPSHOT=[A-F0-9]{64}$' })
    if ($assinatura.Count -ne 1) { throw 'A verificação SQL não retornou uma assinatura válida.' }
    return $assinatura[0]
}

function Testar-Demonstracao {
    $tokens = @{}
    foreach ($perfil in @('ALUNO', 'PROFESSOR', 'ADMIN')) {
        $email = $perfil.ToLowerInvariant() + '@demo.educria.test'
        $sessao = Testar-Api -Rota '/api/auth/login' -Esperado 200 -Corpo @{ email = $email; senha = $senhaDemo }
        if ($sessao.perfil -cne $perfil -or !$sessao.token -or !$sessao.id) {
            throw "Login de $perfil não confirmou o perfil ou os dados da sessão."
        }
        $tokens[$perfil] = $sessao.token
    }

    $painel = @{ ALUNO = '/api/aluno/desempenho'; PROFESSOR = '/api/professor/perfil'; ADMIN = '/api/admin/turmas' }
    foreach ($perfil in $tokens.Keys) {
        foreach ($outro in $painel.Keys) {
            if ($perfil -ne $outro) {
                $null = Testar-Api -Rota $painel[$outro] -Esperado 403 -Token $tokens[$perfil]
            }
        }
    }

    $desempenho = Testar-Api -Rota $painel.ALUNO -Esperado 200 -Token $tokens.ALUNO
    $professor = Testar-Api -Rota $painel.PROFESSOR -Esperado 200 -Token $tokens.PROFESSOR
    $turma = @(Testar-Api -Rota $painel.ADMIN -Esperado 200 -Token $tokens.ADMIN |
        Where-Object codigo -CEQ 'DEMO-EDUCRIA')
    if ($turma.Count -ne 1 -or $turma[0].totalAlunos -lt 2 -or $professor.materia -cne 'Matemática' -or
        $professor.turmas.id -notcontains $turma[0].id -or $desempenho.totalAcertos -lt 1) {
        throw 'Painéis sem a turma, matéria ou desempenho esperados.'
    }
    $detalhe = Testar-Api -Rota "/api/professor/turmas/$($turma[0].id)" -Esperado 200 -Token $tokens.PROFESSOR
    $alunos = @(Testar-Api -Rota "/api/admin/turmas/$($turma[0].id)/alunos" -Esperado 200 -Token $tokens.ADMIN)
    if (@($detalhe.alunos).Count -lt 2 -or $alunos.Count -lt 2) { throw 'Turma sem os alunos fictícios vinculados.' }

    $historico = Testar-Api -Rota '/api/aluno/sessoes/historico' -Esperado 200 -Token $tokens.ALUNO
    $sessoes = @(Testar-Api -Rota '/api/aluno/sessoes' -Esperado 200 -Token $tokens.ALUNO)
    $estudo = $sessoes | Where-Object situacao -EQ 'INTERROMPIDA' | Select-Object -First 1
    if (!$estudo) { $estudo = $sessoes | Select-Object -First 1 }
    if (@($historico.sessoes).Count -lt 2 -or !$estudo.id) { throw 'Histórico ou sessão de Matemática incompletos.' }
    $estado = Testar-Api -Rota "/api/aluno/sessoes/$($estudo.id)" -Esperado 200 -Token $tokens.ALUNO
    if ($estado.sessao.id -ne $estudo.id -or
        ($estado.sessao.situacao -ne 'ENCERRADA' -and !$estado.questaoAtual.id)) {
        throw 'Não foi possível confirmar o estado da sessão de Matemática.'
    }
    Write-Host 'Três logins, seis bloqueios de perfil, painéis, histórico e sessão verificados pela API.'
}

try {
    Confirmar-DockerApresentacao
    Confirmar-ProjetoApresentacao
    Confirmar-ConfiguracaoApresentacao
    Confirmar-SegredosApresentacao
    $senhaDemo = [IO.File]::ReadAllText((Join-Path $PastaSegredosApresentacao 'demo-senha'))
    Add-Type -AssemblyName System.Net.Http
    $handler = [Net.Http.HttpClientHandler]::new()
    $handler.AllowAutoRedirect = $false
    $handler.UseProxy = $false
    $cliente = [Net.Http.HttpClient]::new($handler)
    $cliente.Timeout = [TimeSpan]::FromSeconds(12)

    $antes = Obter-AssinaturaDoBanco
    Write-Host 'SQL verificado: V1 a V5, 160 ordens distintas, grade 8 x 10 x 2 e contas vinculadas.'
    Testar-Demonstracao
    if ($Reiniciar) {
        Invoke-ComposeApresentacao @('stop', '--timeout', '30', 'web', 'api', 'db')
        Invoke-ComposeApresentacao @('up', '-d', '--no-build', '--pull', 'never',
            '--wait', '--wait-timeout', '300', 'db', 'api', 'web')
        if ((Obter-AssinaturaDoBanco) -cne $antes) {
            throw 'Os registros da apresentação mudaram durante o reinício. Repita sem uso concorrente para investigar.'
        }
        Testar-Demonstracao
        Write-Host 'Registros verificados antes e depois do reinício, com a mesma assinatura.'
    }
    exit 0
} catch {
    [Console]::Error.WriteLine($_.Exception.Message)
    exit 1
} finally {
    if ($cliente) { $cliente.Dispose() }
}
