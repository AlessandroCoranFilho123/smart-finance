package app.util;

import app.model.Categoria;
import app.model.TipoTransacao;
import app.model.Transacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TransacaoSearchMatcher")
class TransacaoSearchMatcherTest {

    private final Transacao transacao = new Transacao(
            UUID.randomUUID(),
            "Café da manhã",
            "Refeição no centro",
            42_50L,
            TipoTransacao.Saida,
            LocalDate.of(2026, 4, 13),
            null,
            Categoria.Alimentacao
    );

    @Test
    @DisplayName("retorna verdadeiro quando a busca está vazia")
    void retornaVerdadeiroQuandoBuscaEstaVazia() {
        assertAll(
                () -> assertTrue(TransacaoSearchMatcher.corresponde(transacao, "")),
                () -> assertTrue(TransacaoSearchMatcher.corresponde(transacao, "   ")),
                () -> assertTrue(TransacaoSearchMatcher.corresponde(transacao, null))
        );
    }

    @Test
    @DisplayName("busca por descrição, comentário, categoria, tipo e data")
    void buscaPorCamposPrincipais() {
        assertAll(
                () -> assertTrue(TransacaoSearchMatcher.corresponde(transacao, "café")),
                () -> assertTrue(TransacaoSearchMatcher.corresponde(transacao, "centro")),
                () -> assertTrue(TransacaoSearchMatcher.corresponde(transacao, "alimentacao")),
                () -> assertTrue(TransacaoSearchMatcher.corresponde(transacao, "saida")),
                () -> assertTrue(TransacaoSearchMatcher.corresponde(transacao, "13/04/2026"))
        );
    }

    @Test
    @DisplayName("ignora acentos e maiúsculas na busca")
    void ignoraAcentosEMaiusculasNaBusca() {
        assertTrue(TransacaoSearchMatcher.corresponde(transacao, "REFEICAO"));
    }

    @Test
    @DisplayName("retorna falso quando nenhum campo combina")
    void retornaFalsoQuandoNenhumCampoCombina() {
        assertFalse(TransacaoSearchMatcher.corresponde(transacao, "internet"));
    }
}
