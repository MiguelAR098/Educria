package com.educria.exception;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.educria.controller.AuthController;
import com.educria.controller.HistoricoSessaoController;
import com.educria.controller.RespostaSessaoController;
import com.educria.controller.SessaoEstudoController;
import com.educria.service.AuthService;
import com.educria.service.HistoricoSessaoService;
import com.educria.service.SessaoEstudoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {
    private static final String SESSAO = "/api/aluno/sessoes/12";
    private static final String RESPOSTA = SESSAO + "/questoes/4/resposta";
    private static final Principal TITULAR = () -> "titular@example.test";

    private final SessaoEstudoService sessoes = mock(SessaoEstudoService.class);
    private final AuthService auth = mock(AuthService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new SessaoEstudoController(sessoes),
            new RespostaSessaoController(sessoes), new AuthController(auth),
            new HistoricoSessaoController(mock(HistoricoSessaoService.class)))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    void falhaInternaRetornaMensagemPublicaERegistraPilhaSemSegredos() throws Exception {
        var falha = new IllegalStateException("token-exclusivo-fixture",
                new IllegalArgumentException("senha-exclusiva-fixture e consulta interna"));
        when(sessoes.consultar(TITULAR.getName(), 12L)).thenThrow(falha);
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        var captura = new ListAppender<ILoggingEvent>();
        captura.start();
        logger.addAppender(captura);
        try {
            mvc.perform(get(SESSAO).principal(TITULAR)).andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.message").value("Ocorreu um erro interno. Tente novamente mais tarde."))
                    .andExpect(content().string(not(containsString("fixture"))))
                    .andExpect(jsonPath("$.trace").doesNotExist());

            var evento = captura.list.getFirst();
            assertTrue(evento.getFormattedMessage().contains("IllegalArgumentException"));
            assertTrue(evento.getFormattedMessage().contains("GlobalExceptionHandlerTest"));
            assertFalse(evento.getFormattedMessage().contains("fixture"));
            assertNull(evento.getThrowableProxy());
        } finally {
            logger.detachAppender(captura);
            captura.stop();
        }
    }

    @Test
    void jsonMalformadoOuAusenteRecebe400SemDetalhesDoParser() throws Exception {
        mvc.perform(post("/api/aluno/sessoes").principal(TITULAR)
                        .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Envie os dados no formato JSON esperado."));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(put(RESPOSTA).principal(TITULAR).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message")
                        .value("Confira os IDs da sessão e da questão e envie a alternativa em JSON"));
    }

    @Test
    void idOuPosicaoMalformadosRecebem400PreservandoHandlersLocais() throws Exception {
        mvc.perform(get("/api/aluno/sessoes/invalida").principal(TITULAR))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(put(SESSAO + "/questoes/apresentar").principal(TITULAR))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(get("/api/aluno/sessoes/historico").param("antesDe", "invalido").principal(TITULAR))
                .andExpect(status().isBadRequest()).andExpect(content().string(""));
    }

    @Test
    void errosDeValidacaoMantemOMapaDeCampos() throws Exception {
        mvc.perform(put(RESPOSTA).principal(TITULAR)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"passo\":0}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.alternativa").value("Informe uma alternativa"))
                .andExpect(jsonPath("$.errors.passo").exists());
    }

    @ParameterizedTest
    @EnumSource(value = HttpStatus.class, names = {"BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "NOT_FOUND", "CONFLICT"})
    void apiExceptionPreservaStatusEMensagem(HttpStatus statusEsperado) throws Exception {
        when(sessoes.consultar(TITULAR.getName(), 12L)).thenThrow(new ApiException("Mensagem pública", statusEsperado));

        mvc.perform(get(SESSAO).principal(TITULAR)).andExpect(status().is(statusEsperado.value()))
                .andExpect(jsonPath("$.status").value(statusEsperado.value()))
                .andExpect(jsonPath("$.message").value("Mensagem pública"));
    }

    @Test
    void falhasDeLoginEAutorizacaoMantem401E403() throws Exception {
        when(auth.login(any())).thenThrow(new BadCredentialsException("segredo"))
                .thenThrow(new DisabledException("conta desabilitada"));
        for (int tentativa = 0; tentativa < 2; tentativa++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"aluno@example.test\",\"senha\":\"senha-teste\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos"));
        }
        when(sessoes.consultar(TITULAR.getName(), 12L)).thenThrow(new AccessDeniedException("detalhe da regra"));
        mvc.perform(get(SESSAO).principal(TITULAR)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acesso não autorizado."));
    }
}
