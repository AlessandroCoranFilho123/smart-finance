package app.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GerenciadorChaveDados")
class GerenciadorChaveDadosTest {

    @Test
    @DisplayName("cria e guarda chave quando cofre esta vazio")
    void criaEGuardaChave() {
        CofreFake cofre = new CofreFake();
        GerenciadorChaveDados gerenciador = new GerenciadorChaveDados(cofre);

        SecretKey chave = gerenciador.obterOuCriarChave();

        assertAll(
                () -> assertNotNull(chave),
                () -> assertEquals(32, chave.getEncoded().length),
                () -> assertArrayEquals(
                        chave.getEncoded(),
                        cofre.memoria.get(GerenciadorChaveDados.NOME_CHAVE_DADOS)
                )
        );
    }

    @Test
    @DisplayName("reaproveita chave ja armazenada")
    void reaproveitaChaveArmazenada() {
        CofreFake cofre = new CofreFake();
        SecretKey chaveExistente = CriptografiaDados.gerarChave();
        cofre.salvar(
                GerenciadorChaveDados.NOME_CHAVE_DADOS,
                CriptografiaDados.chaveParaBytes(chaveExistente)
        );

        SecretKey chave = new GerenciadorChaveDados(cofre).obterOuCriarChave();

        assertArrayEquals(chaveExistente.getEncoded(), chave.getEncoded());
    }

    @Test
    @DisplayName("falha se a chave armazenada estiver invalida")
    void chaveArmazenadaInvalida() {
        CofreFake cofre = new CofreFake();
        cofre.salvar(GerenciadorChaveDados.NOME_CHAVE_DADOS, new byte[16]);

        assertThrows(IllegalStateException.class, () ->
                new GerenciadorChaveDados(cofre).obterOuCriarChave());
    }

    private static class CofreFake implements CofreChaves {
        private final Map<String, byte[]> memoria = new HashMap<>();

        @Override
        public Optional<byte[]> ler(String nome) {
            return Optional.ofNullable(memoria.get(nome));
        }

        @Override
        public void salvar(String nome, byte[] segredo) {
            memoria.put(nome, segredo);
        }

        @Override
        public void apagar(String nome) {
            memoria.remove(nome);
        }
    }
}
