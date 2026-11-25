"""Com Next em execução: python -m unittest discover -s frontend/tests -p test_cobertura_banco.py -v."""

import json
import os
import re
import unittest
from urllib.parse import parse_qs, urlparse

from playwright.sync_api import expect, sync_playwright


BANCO = "/api/professor/questoes"
COBERTURA = BANCO + "/cobertura"
BASE = os.getenv("EDUCRIA_BASE_URL", "http://127.0.0.1:3000").rstrip("/")


class ApiDeCobertura:
    """As rotas simuladas mantêm o banco local do teste e verificam cada requisição."""

    def __init__(self):
        self.assuntos = [dict(id=4, nome="frações"), dict(id=8, nome="geometria")]
        self.questoes = {}
        self.atividade_inativa = set()
        self.falha_cobertura = False
        self.consultas_cobertura = []
        self.consultas_lista = []

    def adicionar_questao(self, assunto=4, dificuldade=1, enunciado="Questão de teste", **campos):
        identificador = max(self.questoes, default=0) + 1
        nome = next(a["nome"] for a in self.assuntos if a["id"] == assunto)
        questao = dict(id=identificador, assuntoId=assunto, assunto=nome, dificuldade=dificuldade,
                      enunciado=enunciado, alternativaA="Quatro", alternativaB="Cinco",
                      alternativaC="Seis", alternativaD="Sete", respostaCorreta="A",
                      explicacao=None, ativa=True, autor=None, totalRespostas=0, taxaAcerto=None)
        questao.update(campos)
        self.questoes[identificador] = questao
        return questao

    def cobertura(self):
        linhas = []
        for assunto in self.assuntos:
            contagens = [0] * 10
            for questao in self.questoes.values():
                if (questao["assuntoId"] == assunto["id"] and questao["ativa"]
                        and questao["id"] not in self.atividade_inativa):
                    contagens[questao["dificuldade"] - 1] += 1
            linhas.append(dict(assunto, contagens=contagens, total=sum(contagens)))
        return dict(assuntos=linhas, total=sum(linha["total"] for linha in linhas),
                    combinacoesVazias=sum(linha["contagens"].count(0) for linha in linhas))

    def atender(self, rota):
        req = rota.request
        url = urlparse(req.url)
        metodo = req.method
        if req.headers.get("authorization") != "Bearer token-teste":
            return rota.fulfill(status=401, json={})
        if (metodo, url.path) == ("GET", COBERTURA):
            self.consultas_cobertura.append(url.query)
            if self.falha_cobertura:
                return rota.fulfill(status=503, json={})
            return rota.fulfill(json=self.cobertura())
        if (metodo, url.path) == ("GET", BANCO):
            filtros = parse_qs(url.query)
            self.consultas_lista.append(filtros)
            questoes = list(self.questoes.values())
            if "assuntoId" in filtros:
                questoes = [q for q in questoes if q["assuntoId"] == int(filtros["assuntoId"][0])]
            materia = "Matemática" if self.assuntos else "Português"
            return rota.fulfill(json=dict(materia=materia, assuntos=self.assuntos, questoes=questoes))
        if (metodo, url.path) == ("POST", BANCO):
            campos = req.post_data_json.copy()
            questao = self.adicionar_questao(assunto=campos.pop("assuntoId"), **campos)
            return rota.fulfill(status=201, json=questao)
        if (metodo, url.path) == ("POST", "/api/professor/assuntos"):
            assunto = dict(id=12, nome=req.post_data_json["nome"])
            self.assuntos.append(assunto)
            return rota.fulfill(status=201, json=assunto)
        if (metodo, url.path) == ("POST", BANCO + "/importar"):
            items = req.post_data_json
            for item in items:
                assunto = next(a["id"] for a in self.assuntos if a["nome"] == item["conceito"])
                self.adicionar_questao(assunto, item["dificuldade"], item["enunciado"])
            return rota.fulfill(json=dict(importadas=len(items), repetidas=0, recusadas=0, erros=[]))
        if url.path.startswith(BANCO + "/") and metodo in ("PUT", "PATCH"):
            questao = self.questoes[int(url.path.split("/")[4])]
            questao.update(req.post_data_json)
            questao["assunto"] = next(a["nome"] for a in self.assuntos if a["id"] == questao["assuntoId"])
            return rota.fulfill(json=questao)
        return rota.fulfill(status=404, json={})


class CoberturaDoBanco(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        playwright = sync_playwright().start()
        cls.addClassCleanup(playwright.stop)
        cls.browser = playwright.chromium.launch(executable_path=os.getenv("CHROMIUM_BIN") or None)
        cls.addClassCleanup(cls.browser.close)

    def setUp(self):
        contexto = self.browser.new_context(base_url=BASE)
        self.addCleanup(contexto.close)
        self.page = contexto.new_page()
        self.api = ApiDeCobertura()
        self.page.route("**/api/**", self.api.atender)
        self.page.add_init_script("""localStorage.setItem('educria-auth-storage', JSON.stringify({version: 0,
          state: {token: 'token-teste', user: {id: 2, nome: 'Ana', email: 'professor@example.test', role: 'PROFESSOR'}}}));
        """)
        self.erros = []
        self.page.on("pageerror", lambda erro: self.erros.append(str(erro)))
        self.addCleanup(self.assertEqual, self.erros, [])
        self.matriz = self.page.get_by_role("region", name="Matriz de cobertura por assunto e dificuldade")

    def celula(self, assunto, dificuldade, contagem, estado):
        unidade = "questão elegível" if contagem == 1 else "questões elegíveis"
        return self.matriz.get_by_role("button", name=f"{assunto}, dificuldade {dificuldade}, {contagem} {unidade}, {estado}",
                                       exact=True)

    def conferir_totais(self, questoes, vazias):
        for rotulo, total in (("Total de questões elegíveis", questoes), ("Combinações sem questões", vazias)):
            campo = self.page.locator("dt").filter(has_text=re.compile(f"^{rotulo}$"))
            expect(campo.locator("xpath=following-sibling::dd[1]")).to_have_text(str(total))

    def test_filtros_nao_alteram_a_cobertura_completa(self):
        for indice in range(6):
            self.api.adicionar_questao(enunciado=f"Questão introdutória {indice + 1}")
        self.api.adicionar_questao(dificuldade=5, enunciado="Questão elegível de nível cinco")
        inativa = self.api.adicionar_questao(dificuldade=5, enunciado="Questão de atividade inativa")
        self.api.atividade_inativa.add(inativa["id"])
        self.page.goto("/professor/questoes")
        expect(self.page.get_by_role("heading", name="Cobertura do banco", exact=True)).to_be_visible()
        expect(self.matriz.get_by_role("button")).to_have_count(20)
        linha_fracoes = self.matriz.get_by_role("row").filter(
            has=self.page.get_by_role("rowheader", name="frações", exact=True))
        expect(linha_fracoes.get_by_role("cell").last).to_have_text("7")
        expect(self.celula("frações", 1, 6, "meta atingida")).to_be_visible()
        expect(self.celula("frações", 10, 0, "sem questões")).to_be_visible()
        self.conferir_totais(7, 18)

        nivel_cinco = self.celula("frações", 5, 1, "abaixo da meta")
        nivel_cinco.press("Enter")
        expect(nivel_cinco).to_have_attribute("aria-pressed", "true")
        expect(nivel_cinco).to_be_focused()
        filtros = self.page.get_by_role("group", name="Filtros da listagem")
        expect(filtros.get_by_label("Assunto", exact=True)).to_have_value("4")
        expect(filtros.get_by_label("Dificuldade", exact=True)).to_have_value("5")
        expect(self.page.get_by_role("heading", name="Questão elegível de nível cinco")).to_be_visible()
        expect(self.page.get_by_role("heading", name="Questão de atividade inativa")).to_be_visible()
        expect(self.page.get_by_role("heading", name="Questão introdutória 1", exact=True)).to_have_count(0)
        self.assertIn({"assuntoId": ["4"]}, self.api.consultas_lista)

        self.celula("geometria", 7, 0, "sem questões").click()
        expect(self.page.get_by_text(re.compile(r"nenhuma questão.*filtro", re.I))).to_be_visible()
        expect(self.matriz.get_by_role("button")).to_have_count(20)
        self.conferir_totais(7, 18)

        self.page.get_by_role("button", name="Limpar filtros", exact=True).click()
        expect(self.page.get_by_role("heading", name="Questão introdutória 1", exact=True)).to_be_visible()
        expect(filtros.get_by_label("Assunto", exact=True)).to_have_value("")
        expect(filtros.get_by_label("Dificuldade", exact=True)).to_have_value("")
        expect(self.matriz.locator("button[aria-pressed=true]")).to_have_count(0)
        self.assertTrue(self.api.consultas_cobertura)
        self.assertTrue(all(not query for query in self.api.consultas_cobertura))

    def test_nova_questao_herda_assunto_e_dificuldade_da_matriz(self):
        self.page.goto("/professor/questoes")
        self.celula("geometria", 7, 0, "sem questões").click()
        self.page.get_by_role("button", name="Nova questão", exact=True).click()

        expect(self.page.locator("#q-assunto")).to_have_value("8")
        expect(self.page.locator("#q-dificuldade")).to_have_value("7")
        enunciado = "Questão cadastrada a partir da matriz"
        self.page.get_by_label("Enunciado", exact=True).fill(enunciado)
        for letra, texto in zip("ABCD", ("Quatro", "Cinco", "Seis", "Sete")):
            self.page.get_by_label(f"Texto da alternativa {letra}", exact=True).fill(texto)
        self.page.get_by_role("button", name="Cadastrar questão", exact=True).click()

        expect(self.page.get_by_role("heading", name=enunciado, exact=True)).to_be_visible()
        expect(self.celula("geometria", 7, 1, "abaixo da meta")).to_be_visible()

    def test_atualiza_a_cobertura_apos_as_mutacoes(self):
        self.page.goto("/professor/questoes")
        expect(self.page.get_by_text("Nenhuma questão elegível no banco.", exact=True)).to_be_visible()
        self.conferir_totais(0, 20)

        self.page.get_by_label("Novo assunto", exact=True).fill("equações")
        self.page.get_by_role("button", name="Adicionar assunto", exact=True).click()
        expect(self.celula("equações", 1, 0, "sem questões")).to_be_visible()
        self.conferir_totais(0, 30)

        self.page.get_by_role("button", name="Nova questão", exact=True).click()
        self.page.locator("#q-assunto").select_option("4")
        self.page.locator("#q-dificuldade").select_option("3")
        self.page.get_by_label("Enunciado", exact=True).fill("Questão criada no teste")
        for letra, texto in zip("ABCD", ("Quatro", "Cinco", "Seis", "Sete")):
            self.page.get_by_label(f"Texto da alternativa {letra}", exact=True).fill(texto)
        self.page.get_by_role("button", name="Cadastrar questão", exact=True).click()
        expect(self.celula("frações", 3, 1, "abaixo da meta")).to_be_visible()
        self.conferir_totais(1, 29)

        self.page.get_by_role("button", name="Editar", exact=True).click()
        self.page.locator("#q-assunto").select_option("8")
        self.page.locator("#q-dificuldade").select_option("10")
        self.page.get_by_role("button", name="Salvar alterações", exact=True).click()
        expect(self.celula("frações", 3, 0, "sem questões")).to_be_visible()
        expect(self.celula("geometria", 10, 1, "abaixo da meta")).to_be_visible()
        self.conferir_totais(1, 29)

        self.page.get_by_role("button", name="Desativar", exact=True).click()
        self.conferir_totais(0, 30)
        self.page.get_by_role("button", name="Reativar", exact=True).click()
        self.conferir_totais(1, 29)

        self.page.get_by_role("button", name="Importar / exportar JSON", exact=True).click()
        self.page.get_by_label("JSON das questões", exact=True).fill(json.dumps([
            dict(conceito="equações", dificuldade=2, enunciado="Questão importada no teste",
                 alts=["4", "5", "6", "7"], correta=0),
        ]))
        self.page.get_by_role("button", name="Importar", exact=True).click()
        expect(self.celula("equações", 2, 1, "abaixo da meta")).to_be_visible()
        self.conferir_totais(2, 28)

    def test_carregamento_e_falha_nao_aparecem_como_banco_vazio(self):
        pendentes = []
        self.page.route(f"**{COBERTURA}", lambda rota: pendentes.append(rota))
        self.api.falha_cobertura = True
        with self.page.expect_request(lambda req: urlparse(req.url).path == COBERTURA):
            self.page.goto("/professor/questoes")
        expect(self.page.get_by_role("status").filter(has_text=re.compile("carregando.*cobertura", re.I))).to_be_visible()
        self.assertEqual(len(pendentes), 1)
        self.page.unroute(f"**{COBERTURA}")
        pendentes[0].fulfill(status=503, json={})
        erro = self.page.get_by_role("alert").filter(has_text=re.compile("cobertura", re.I))
        expect(erro).to_be_visible()
        expect(self.page.get_by_text("Nenhuma questão elegível no banco.", exact=True)).to_have_count(0)

        self.api.falha_cobertura = False
        self.page.get_by_role("button", name="Tentar novamente", exact=True).click()
        expect(self.page.get_by_text("Nenhuma questão elegível no banco.", exact=True)).to_be_visible()
        expect(erro).to_have_count(0)

    def test_materia_sem_assuntos(self):
        self.api.assuntos = []
        self.page.goto("/professor/questoes")
        expect(self.page.get_by_text("Esta matéria ainda não tem assuntos cadastrados.", exact=True)).to_be_visible()
        expect(self.matriz).to_have_count(0)
        expect(self.page.get_by_role("button", name="Nova questão", exact=True)).to_be_disabled()

    def test_matriz_rola_em_tela_pequena_com_texto_ampliado(self):
        self.page.set_viewport_size({"width": 320, "height": 720})
        self.page.add_init_script("localStorage.setItem('educria.atividade.tamanhoTexto', 'Muito grande');")
        self.page.goto("/professor/questoes")
        expect(self.page.locator("html")).to_have_attribute("data-tamanho-texto", "Muito grande")
        expect(self.matriz).to_be_visible()
        expect(self.page.get_by_role("button", name="Adicionar assunto", exact=True)).to_be_visible()
        self.assertTrue(self.page.evaluate("document.documentElement.scrollWidth <= innerWidth"))
        self.assertTrue(self.matriz.evaluate("elemento => elemento.scrollWidth > elemento.clientWidth"))
        self.matriz.focus()
        expect(self.matriz).to_be_focused()
        celula = self.celula("geometria", 10, 0, "sem questões")
        celula.press("Space")
        expect(celula).to_have_attribute("aria-pressed", "true")
        expect(celula).to_be_in_viewport()


if __name__ == "__main__":
    unittest.main()
