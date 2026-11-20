"""Com Next em execução: python -m unittest discover -s frontend/tests -v

Dependências: python -m pip install playwright && python -m playwright install chromium
EDUCRIA_BASE_URL escolhe o servidor (padrão: http://127.0.0.1:3000).
EDUCRIA_API_REAL=1 executa também o fluxo com uma conta nova no backend de
desenvolvimento (SPRING_PROFILES_ACTIVE=dev). CHROMIUM_BIN é opcional.
"""
import os
import re
import unittest
from datetime import datetime, timedelta, timezone
from secrets import token_hex
from urllib.parse import urlparse
from uuid import uuid4

from playwright.sync_api import expect, sync_playwright


BASE = os.getenv("EDUCRIA_BASE_URL", "http://127.0.0.1:3000").rstrip("/")
ASSUNTO = "somar e subtrair"


class ApiDeTeste:
    """Contrato do aluno com estado por teste. Rejeita rotas e mutações inesperadas."""

    def __init__(self):
        self.aberta = False
        self.respondida = False
        self.situacao = "ATIVA"
        self.sessao_anterior = None
        self.falha_turma = False
        self.pendentes = []
        self.amanha = (datetime.now(timezone.utc).date() + timedelta(days=1)).isoformat()
        self.questao = dict(id=12, dificuldade=1, enunciado="Quanto é 7 + 8?",
                            alternativaA="13", alternativaB="15", alternativaC="16", alternativaD="17")

    def atender(self, route):
        req = route.request
        path = urlparse(req.url).path
        metodo = req.method
        if (metodo, path) == ("POST", "/api/auth/login"):
            return route.fulfill(json=dict(token="token-teste", id=2, nome="Ana", perfil="ALUNO",
                                           email=req.post_data_json["email"]))
        if req.headers.get("authorization") != "Bearer token-teste":
            return route.fulfill(status=401, json={})
        base = "/api/aluno/sessoes"
        if (metodo, path) == ("POST", base) and req.post_data_json == {"assuntoId": 4}:
            self.aberta = True
        elif (metodo, path) == ("PUT", base + "/7/questoes/12/resposta"):
            if not self.aberta or req.post_data_json != {"alternativa": "B"}:
                return route.fulfill(status=400, json={})
            self.respondida = True
        elif (metodo, path) == ("PUT", base + "/7/interromper"):
            self.situacao = "INTERROMPIDA"
        elif metodo != "GET":
            return route.fulfill(status=405, json={})

        resumo = dict(id=7, assuntoId=4, situacao=self.situacao, questoesApresentadas=1)
        feedback = dict(alternativa="B", correta=True, explicacao="7 mais 8 é 15.", nivelAnterior=1, nivelAtual=2)
        estado = dict(sessao=resumo, questaoAtual=self.questao, feedback=feedback if self.respondida else None)
        indicador = dict(id=7, assunto=ASSUNTO, situacao=self.situacao, iniciadaEm="2026-01-02T15:00:00Z",
                         apresentadas=1, acertos=int(self.respondida), erros=0,
                         nivelAntes=1 if self.respondida else None, nivelDepois=2 if self.respondida else None)
        revisao = dict(questaoId=12, enunciado=self.questao["enunciado"], assunto=ASSUNTO,
                       proximaRevisao=self.amanha, intervaloDias=1)
        if path == "/api/aluno/turma/pontos" and self.falha_turma:
            return route.fulfill(status=503, json={})
        dados = {
            "/api/aluno/desempenho": dict(nivelAtual=5, totalAcertos=17, totalErros=3, nivelMaximo=False,
                                          errosConsecutivos=0),
            "/api/aluno/turma/pontos": dict(turmaId=8, nomeTurma="Turma azul", pontosTotais=317),
            "/api/aluno/materias": [dict(id=1, nome="Matemática", totalAssuntos=1)],
            "/api/aluno/materias/1/assuntos": [dict(id=4, nome=ASSUNTO, nivelAtual=1 + int(self.respondida))],
            "/api/aluno/revisoes": dict(hoje=datetime.now(timezone.utc).date().isoformat(),
                                        proximaVirada=self.amanha + "T03:00:00Z", pendentes=self.pendentes,
                                        futuras=[revisao] if self.respondida else []),
            base: estado if metodo == "POST" else [resumo] if self.aberta else [],
            base + "/7": estado,
            base + "/7/interromper": estado,
            base + "/7/questoes/12/resposta": feedback,
            base + "/historico": dict(sessoes=[indicador] if self.aberta else [], proximoCursor=None),
            base + "/7/resultado": dict(sessao=indicador, anterior=self.sessao_anterior, revisoes=[revisao],
                                        questoes=[dict(numero=1, enunciado=self.questao["enunciado"],
                                                       alternativa="B", correta=True)]),
        }
        if path not in dados:
            return route.fulfill(status=404, json={})
        return route.fulfill(json=dados[path])


class DashboardAluno(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.playwright = sync_playwright().start()
        cls.browser = cls.playwright.chromium.launch(executable_path=os.getenv("CHROMIUM_BIN") or None)

    @classmethod
    def tearDownClass(cls):
        cls.browser.close()
        cls.playwright.stop()

    def setUp(self):
        self.contexto = self.browser.new_context(base_url=BASE)
        self.addCleanup(self.contexto.close)
        self.page = self.contexto.new_page()
        self.email = f"painel-{uuid4()}@example.test"
        self.senha = token_hex(18)

    def login(self):
        self.page.goto("/login")
        self.page.get_by_label("E-mail").fill(self.email)
        self.page.get_by_label("Senha", exact=True).fill(self.senha)
        self.page.get_by_role("button", name="Entrar", exact=True).click()
        expect(self.page.get_by_role("heading", name="Minhas matérias")).to_be_visible()

    def fluxo(self):
        page = self.page
        self.login()
        expect(page.get_by_text("Você ainda não iniciou uma sessão.")).to_be_visible()
        page.get_by_role("link", name="Fazer atividade").click()
        combo = page.get_by_label("Assunto da sessão")
        opcao = combo.locator("option").filter(has_text=ASSUNTO)
        expect(opcao).to_have_count(1)
        combo.select_option(opcao.get_attribute("value"))
        with page.expect_response(lambda r: r.request.method == "POST" and
                                  urlparse(r.url).path == "/api/aluno/sessoes") as inicio:
            page.get_by_role("button", name="Iniciar sessão").click()
        estado = inicio.value.json()
        self.assertIsNone(estado.get("feedback"))
        campos_publicos = {"id", "assuntoId", "dificuldade", "enunciado",
                           "alternativaA", "alternativaB", "alternativaC", "alternativaD"}
        self.assertFalse(set(estado["questaoAtual"]) - campos_publicos)
        enviar = page.get_by_role("button", name="Enviar resposta")
        expect(enviar).to_be_disabled()
        pergunta = page.get_by_role("heading", name=re.compile(r"Quanto é \d+ \+ \d+"))
        enunciado = pergunta.inner_text()
        a, b = re.search(r"(\d+) \+ (\d+)", enunciado).groups()
        # As duas questões de entrada desse assunto na carga de desenvolvimento são adições.
        label = page.locator("fieldset label").filter(has=page.get_by_text(str(int(a) + int(b)), exact=True))
        letra = label.locator("input").get_attribute("value")
        label.click()
        enviar.click()
        expect(page.get_by_text("Você acertou!")).to_be_visible()
        expect(page.get_by_text("Nível neste assunto: 1 → 2")).to_be_visible()
        page.get_by_role("button", name="Pausar e sair").click()
        toggle = page.get_by_role("button", name=f"Evolução de {ASSUNTO}", exact=True)
        item = page.get_by_role("listitem").filter(has=toggle)
        expect(item.get_by_text("Nível 2/10")).to_be_visible()
        toggle.click()
        expect(item.get_by_text("Nível inicial: 1 · Nível final: 2")).to_be_visible()
        expect(page.get_by_text("Acertos: 1 · Erros: 0", exact=False)).to_be_visible()
        page.get_by_role("link", name="Minhas revisões").click()
        expect(page.get_by_role("heading", name="Futuras (1)")).to_be_visible()
        expect(page.get_by_text(enunciado, exact=True)).to_be_visible()
        page.get_by_role("link", name="Voltar ao painel").click()
        page.get_by_role("link", name="Histórico de sessões").click()
        page.get_by_role("link").filter(has_text=ASSUNTO).click()
        expect(page.get_by_role("heading", name="Resultado da sessão")).to_be_visible()
        page.reload()
        expect(page.get_by_text("Resultado parcial")).to_be_visible()
        expect(page.get_by_text("Acertou", exact=True)).to_be_visible()
        expect(page.get_by_text(f"Sua alternativa: {letra}", exact=False)).to_be_visible()

    def abrir_questao_de_teste(self):
        api = ApiDeTeste()
        api.aberta = True
        self.page.route("**/api/**", api.atender)
        self.login()
        self.page.goto("/aluno/atividade/7")
        expect(self.page.get_by_role("heading", name=api.questao["enunciado"])).to_be_focused()
        return api

    def test_selecao_e_envio_por_teclado(self):
        page = self.page
        self.abrir_questao_de_teste()
        envios = []
        page.on("request", lambda r: envios.append(r.post_data_json) if r.url.endswith("/resposta") else None)
        enviar = page.get_by_role("button", name="Enviar resposta")
        expect(enviar).to_be_disabled()
        enviar.evaluate("botao => botao.click()")
        margem = page.viewport_size["width"] - 12
        for tecla, letra in zip(("Shift+a", "b", "Shift+c", "d", "1", "2", "3", "4"), "ABCDABCD"):
            page.mouse.click(margem, 350)
            self.assertFalse(page.evaluate("document.activeElement.closest('section') !== null"))
            page.keyboard.press(tecla)
            radio = page.locator(f'input[value="{letra}"]')
            expect(radio).to_be_checked()
            expect(radio).to_be_focused()
        expect(page.get_by_role("checkbox")).to_have_count(0)
        page.keyboard.press("Control+a")
        expect(page.locator('input[value="D"]')).to_be_checked()
        page.get_by_role("region", name="Quanto é 7 + 8?").evaluate("""secao => {
            const campo = document.createElement('textarea');
            secao.append(campo);
            campo.focus();
        }""")
        page.keyboard.type("abc123")
        expect(page.get_by_role("textbox")).to_have_value("abc123")
        expect(page.locator('input[value="D"]')).to_be_checked()
        page.get_by_role("textbox").evaluate("campo => campo.remove()")
        page.locator('input[value="D"]').focus()
        page.keyboard.press("2")
        page.keyboard.press("Tab")
        expect(enviar).to_be_focused()
        page.keyboard.press("Shift+Tab")
        expect(page.locator('input[value="B"]')).to_be_focused()
        expect(page.get_by_text("7 mais 8 é 15.")).to_have_count(0)
        self.assertEqual(envios, [])
        page.keyboard.press("Tab")
        page.keyboard.press("Enter")
        expect(page.get_by_role("group", name="Correção da resposta")).to_be_focused()
        expect(page.get_by_text("Você acertou!")).to_be_visible()
        expect(page.get_by_text("7 mais 8 é 15.")).to_be_visible()
        page.keyboard.press("Tab")
        expect(page.get_by_role("button", name="Próxima questão")).to_be_focused()
        self.assertEqual(envios, [{"alternativa": "B"}])

    def test_layout_movel_falha_e_reenvio_sem_duplicar(self):
        page = self.page
        page.set_viewport_size({"width": 320, "height": 640})
        api = self.abrir_questao_de_teste()
        enviar = page.get_by_role("button", name="Enviar resposta")
        for largura in (320, 390):
            page.set_viewport_size({"width": largura, "height": 700})
            self.assertTrue(page.evaluate("document.documentElement.scrollWidth <= innerWidth"))
            alvos = [*page.locator("fieldset label").all(), enviar, page.get_by_role("button", name="Pausar e sair")]
            for alvo in alvos:
                caixa = alvo.bounding_box()
                self.assertGreaterEqual(min(caixa["width"], caixa["height"]), 44)
        page.locator("fieldset label").nth(1).click()
        tentativas = []
        # Segura a primeira requisição para observar a interface durante o envio.
        page.route("**/questoes/*/resposta", lambda rota: tentativas.append(rota))
        enviar.evaluate("botao => { botao.click(); botao.click(); }")
        expect(page.get_by_role("button", name="Enviando…")).to_be_disabled()
        expect(page.get_by_text("Enviando sua resposta…")).to_have_count(1)
        self.assertEqual(len(tentativas), 1)
        tentativas[0].fulfill(status=503, json={})
        expect(page.get_by_role("alert").filter(has_text="tente enviar novamente")).to_be_visible()
        expect(page.locator('input[value="B"]')).to_be_checked()
        expect(page.get_by_role("button", name="Próxima questão")).to_have_count(0)
        page.unroute("**/questoes/*/resposta")
        def confirmar(rota):
            tentativas.append(rota)
            api.atender(rota)
        page.route("**/questoes/*/resposta", confirmar)
        enviar.click()
        expect(page.get_by_text("Você acertou!")).to_be_visible()
        self.assertEqual(len(tentativas), 2)

    def test_comparacao_do_assunto_no_resultado(self):
        api = ApiDeTeste()
        self.page.route("**/api/**", api.atender)
        self.login()
        self.page.goto("/aluno/historico/7")
        titulo = self.page.get_by_role("heading", name="Comparação com a sessão anterior deste assunto")
        tabela = self.page.get_by_role("table")
        expect(titulo).to_be_visible()
        expect(self.page.get_by_text("A comparação ficará disponível", exact=False)).to_be_visible()
        expect(tabela).to_have_count(0)

        api.situacao = "ENCERRADA"
        self.page.reload()
        expect(self.page.get_by_text("Esta é sua primeira sessão concluída neste assunto.", exact=False)).to_be_visible()
        expect(tabela).to_have_count(0)

        api.sessao_anterior = dict(id=6, assunto=ASSUNTO, iniciadaEm="2026-01-01T15:00:00Z",
                                   situacao="ENCERRADA", apresentadas=2, respondidas=2, acertos=2, erros=1)
        self.page.reload()
        expect(tabela.get_by_role("row", name="Erros 0 1")).to_be_visible()
        expect(tabela.get_by_role("row", name="Apresentadas 1 2")).to_be_visible()

    def test_fluxo_no_navegador(self):
        api = ApiDeTeste()
        self.page.route("**/api/**", api.atender)
        self.fluxo()
        self.page.goto("/aluno")
        expect(self.page.get_by_text("317 pts")).to_be_visible()
        expect(self.page.get_by_text("Nível 5 de 10")).to_be_visible()
        expect(self.page.get_by_text(re.compile("%"))).to_have_count(0)

    def test_revisao_pendente_e_erro_da_turma(self):
        api = ApiDeTeste()
        api.falha_turma = True
        api.pendentes = [dict(questaoId=11, enunciado="Quanto é 3 + 4?", assunto=ASSUNTO,
                              proximaRevisao="2026-01-01", intervaloDias=1)]
        self.page.route("**/api/**", api.atender)
        self.login()
        expect(self.page.get_by_text("Para revisar hoje: 1")).to_be_visible()
        expect(self.page.get_by_text("Quanto é 3 + 4?", exact=False)).to_be_visible()
        alerta = self.page.get_by_role("alert").filter(has_text="os pontos da turma")
        expect(alerta).to_be_visible()
        expect(self.page.get_by_text("0 pts", exact=True)).to_have_count(0)
        api.falha_turma = False
        alerta.get_by_role("button", name="Tentar novamente").click()
        expect(self.page.get_by_text("317 pts")).to_be_visible()

    def test_evolucao_paginada_preserva_assunto_e_niveis_legados(self):
        api = ApiDeTeste()
        page = self.page
        page.route("**/api/**", api.atender)
        # As duas sessões têm assuntos homônimos. Só a segunda página contém o ID escolhido.
        comum = dict(situacao="ENCERRADA", iniciadaEm="2026-01-02T15:00:00Z", apresentadas=1,
                     acertos=1, erros=0, assunto=ASSUNTO)
        outra = dict(comum, id=8, nivelAntes=6, nivelDepois=7)
        legada = dict(comum, id=7, nivelAntes=None, nivelDepois=None)
        page.route("**/api/aluno/sessoes", lambda r: r.fulfill(json=[
            dict(id=8, assuntoId=9, situacao="ENCERRADA", questoesApresentadas=1),
            dict(id=7, assuntoId=4, situacao="ENCERRADA", questoesApresentadas=1),
        ]))
        def pagina(route):
            anterior = urlparse(route.request.url).query == "antesDe=8"
            return route.fulfill(json=dict(sessoes=[legada if anterior else outra],
                                           proximoCursor=None if anterior else 8))
        page.route("**/api/aluno/sessoes/historico*", pagina)
        self.login()
        page.get_by_role("button", name=f"Evolução de {ASSUNTO}").click()
        page.get_by_role("button", name="Buscar sessões anteriores").click()
        expect(page.get_by_text("Nível inicial: sem registro · Nível final: sem registro")).to_be_visible()
        expect(page.get_by_text("Nível inicial: 6 · Nível final: 7")).to_have_count(0)

    def cadastrar_na_api_real(self):
        cadastro = self.contexto.request.post("/api/auth/register", data={
            "nome": "Aluno de teste", "email": self.email, "senha": self.senha,
        }, headers={"X-Educria-Client": "web"})
        self.assertEqual(cadastro.status, 201)

    @unittest.skipUnless(os.getenv("EDUCRIA_API_REAL") == "1", "requer backend dev e PostgreSQL")
    def test_reaprender_apos_sair_e_entrar(self):
        self.cadastrar_na_api_real()
        self.login()
        page = self.page
        page.get_by_role("link", name="Fazer atividade").click()
        combo = page.get_by_label("Assunto da sessão")
        combo.select_option(combo.locator("option").filter(has_text=ASSUNTO).get_attribute("value"))
        page.get_by_role("button", name="Iniciar sessão").click()
        enunciado = page.locator("#enunciado-atual")
        primeira = enunciado.inner_text()
        sessao = page.url
        enviar = page.get_by_role("button", name="Enviar resposta")
        def resolver(acertar):
            expect(page.get_by_role("radio").first).to_be_enabled()
            a, op, b = re.search(r"(\d+) ([+-]) (\d+)", enunciado.inner_text()).groups()
            valor = int(a) + int(b) if op == "+" else int(a) - int(b)
            for indice, item in enumerate(page.locator("fieldset label").all(), 1):
                if (item.get_by_text(str(valor), exact=True).count() > 0) == acertar:
                    page.mouse.click(page.viewport_size["width"] - 12, 350)
                    page.keyboard.press(str(indice))
                    break
            expect(enviar).to_be_enabled()
            enviar.click()
            expect(page.get_by_text("Você acertou!" if acertar else "Resposta incorreta.", exact=True)).to_be_visible()

        resolver(False)
        expect(page.get_by_text("Esta questão voltará", exact=False)).to_be_visible()
        expect(page.get_by_text("Para reaprender: 1", exact=False)).to_be_visible()
        page.get_by_role("button", name="Pausar e sair").click()
        page.wait_for_url("**/aluno")
        page.get_by_role("link", name="Minhas revisões").click()
        expect(page.get_by_role("heading", name="Futuras (0)")).to_be_visible()
        # Um contexto sem cookies/storage força nova autenticação.
        novo = self.browser.new_context(base_url=BASE)
        self.addCleanup(novo.close)
        self.page = page = novo.new_page()
        self.login()
        page.goto(sessao)
        page.get_by_role("button", name="Retomar sessão").click()
        expect(page.get_by_text("Resposta incorreta.", exact=True)).to_be_visible()
        enunciado = page.locator("#enunciado-atual")
        enviar = page.get_by_role("button", name="Enviar resposta")
        for _ in range(2):
            anterior = enunciado.inner_text()
            page.get_by_role("button", name="Próxima questão").click()
            expect(enunciado).not_to_have_text(anterior)
            expect(enunciado).not_to_have_text(primeira)
            resolver(True)
        page.get_by_role("button", name="Próxima questão").click()
        expect(enunciado).to_have_text(primeira)
        expect(enviar).to_be_disabled()
        expect(page.get_by_text("Nova tentativa de uma questão anterior")).to_be_visible()
        resolver(True)
        expect(page.get_by_text("Para reaprender: 0", exact=False)).to_be_visible()
        page.reload()
        expect(page.get_by_text("Próxima revisão agendada.")).to_be_visible()
        page.get_by_role("button", name="Pausar e sair").click()
        page.get_by_role("link", name="Histórico de sessões").click()
        page.get_by_role("link").filter(has_text=ASSUNTO).click()
        expect(page.locator("p").filter(has_text="Tentativa 1 ·")).to_contain_text("Errou")
        expect(page.locator("p").filter(has_text="Tentativa 2 ·")).to_contain_text("Acertou")

    @unittest.skipUnless(os.getenv("EDUCRIA_API_REAL") == "1", "requer backend dev e PostgreSQL")
    def test_fluxo_com_api_real(self):
        self.cadastrar_na_api_real()
        self.fluxo()


if __name__ == "__main__":
    unittest.main()
