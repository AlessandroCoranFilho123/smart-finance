package app.service;

import app.model.Meta;
import app.repository.MetaDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MetaService - edge cases")
class MetaServiceEdgeCaseTest {

    static class FakeMetaDAO extends MetaDAO {
        final Map<UUID, Meta> db = new LinkedHashMap<>();
        Meta ultimoAtualizado;

        @Override
        public void inserir(Meta m) {
            db.put(m.getId(), m);
        }

        @Override
        public void atualizar(Meta m) {
            ultimoAtualizado = m;
            db.put(m.getId(), m);
        }

        @Override
        public Meta buscarPorId(UUID id) {
            return db.get(id);
        }

        @Override
        public List<Meta> listarTodas() {
            return new ArrayList<>(db.values());
        }

        @Override
        public void deletar(UUID id) {
            db.remove(id);
        }
    }

    private FakeMetaDAO dao;
    private MetaService service;

    @BeforeEach
    void setUp() {
        dao = new FakeMetaDAO();
        service = new MetaService(dao);
    }

    @Nested
    @DisplayName("criarMeta validacoes")
    class CriarMetaValidacoes {
        @Test
        @DisplayName("nome nulo lanca excecao")
        void nomeNuloLancaExcecao() {
            assertThrows(NullPointerException.class, () ->
                    service.criarMeta(null, 100_00L));
        }

        @Test
        @DisplayName("nome vazio lanca excecao")
        void nomeVazioLancaExcecao() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.criarMeta("   ", 100_00L));
        }

        @Test
        @DisplayName("nome apenas com espacos lanca excecao")
        void nomeEspacosLancaExcecao() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.criarMeta("  \t  ", 100_00L));
        }

        @Test
        @DisplayName("alvo zero lanca excecao")
        void alvoZeroLancaExcecao() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.criarMeta("Teste", 0));
        }

        @Test
        @DisplayName("alvo negativo lanca excecao")
        void alvoNegativoLancaExcecao() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.criarMeta("Teste", -1));
        }
    }

    @Nested
    @DisplayName("atualizarMeta validacoes")
    class AtualizarMetaValidacoes {
        @Test
        @DisplayName("meta nula lanca excecao")
        void metaNulaLancaExcecao() {
            assertThrows(NullPointerException.class, () ->
                    service.atualizarMeta(null));
        }

        @Test
        @DisplayName("meta sem alvo lanca excecao")
        void metaSemAlvoLancaExcecao() {
            Meta metaSemAlvo = new Meta(UUID.randomUUID(), "Sem", null, 100_00L);
            assertThrows(IllegalArgumentException.class, () ->
                    service.atualizarMeta(metaSemAlvo));
        }

        @Test
        @DisplayName("meta com alvo zero lanca excecao")
        void metaAlvoZeroLancaExcecao() {
            Meta metaAlvoZero = new Meta(UUID.randomUUID(), "Zero", 0L, 0L);
            assertThrows(IllegalArgumentException.class, () ->
                    service.atualizarMeta(metaAlvoZero));
        }
    }

    @Nested
    @DisplayName("buscarPorId validacoes")
    class BuscarPorIdValidacoes {
        @Test
        @DisplayName("id nulo lanca excecao")
        void idNuloLancaExcecao() {
            assertThrows(NullPointerException.class, () ->
                    service.buscarPorId(null));
        }
    }

    @Nested
    @DisplayName("deletarMeta validacoes")
    class DeletarMetaValidacoes {
        @Test
        @DisplayName("id nulo lanca excecao")
        void idNuloLancaExcecao() {
            assertThrows(NullPointerException.class, () ->
                    service.deletarMeta(null));
        }

        @Test
        @DisplayName("deletar meta inexistente nao lanca excecao")
        void deletarMetaInexistenteNaoLanca() {
            assertDoesNotThrow(() ->
                    service.deletarMeta(UUID.randomUUID()));
        }
    }

    @Nested
    @DisplayName("calcularProgresso")
    class CalcularProgresso {
        @Test
        @DisplayName("meta nula lanca excecao")
        void metaNulaLancaExcecao() {
            assertThrows(NullPointerException.class, () ->
                    service.calcularProgresso(null));
        }

        @Test
        @DisplayName("retorna porcentagem correta")
        void retornaPorcentagemCorreta() {
            Meta m = new Meta(UUID.randomUUID(), "T", 200_00L, 100_00L);
            assertEquals(50.0, service.calcularProgresso(m), 0.001);
        }
    }

    @Nested
    @DisplayName("metaConcluida")
    class MetaConcluida {
        @Test
        @DisplayName("meta nula lanca excecao")
        void metaNulaLancaExcecao() {
            assertThrows(NullPointerException.class, () ->
                    service.metaConcluida(null));
        }

        @Test
        @DisplayName("meta sem alvo nao considerada concluida")
        void metaSemAlvoNaoConcluida() {
            Meta m = new Meta(UUID.randomUUID(), "T", null, 100_00L);
            assertFalse(service.metaConcluida(m));
        }

        @Test
        @DisplayName("meta no alvo considerada concluida")
        void metaNoAlvoConcluida() {
            Meta m = new Meta(UUID.randomUUID(), "T", 100_00L, 100_00L);
            assertTrue(service.metaConcluida(m));
        }

        @Test
        @DisplayName("meta acima do alvo considerada concluida")
        void metaAcimaAlvoConcluida() {
            Meta m = new Meta(UUID.randomUUID(), "T", 50_00L, 100_00L);
            assertTrue(service.metaConcluida(m));
        }

        @Test
        @DisplayName("meta abaixo do alvo nao considerada concluida")
        void metaAbaixoAlvoNaoConcluida() {
            Meta m = new Meta(UUID.randomUUID(), "T", 200_00L, 100_00L);
            assertFalse(service.metaConcluida(m));
        }
    }

    @Nested
    @DisplayName("calcularRestante")
    class CalcularRestante {
        @Test
        @DisplayName("meta nula lanca excecao")
        void metaNulaLancaExcecao() {
            assertThrows(NullPointerException.class, () ->
                    service.calcularRestante(null));
        }

        @Test
        @DisplayName("retorna restante correto")
        void retornaRestanteCorreto() {
            Meta m = new Meta(UUID.randomUUID(), "T", 500_00L, 200_00L);
            assertEquals(300_00L, service.calcularRestante(m));
        }
    }

    @Nested
    @DisplayName("contarMetasConcluidas")
    class ContarMetasConcluidas {
        @Test
        @DisplayName("nenhuma meta retorna zero")
        void nenhumaMetaRetornaZero() {
            assertEquals(0, service.contarMetasConcluidas());
        }

        @Test
        @DisplayName("conta apenas metas concluidas")
        void contaApenasConcluidas() {
            Meta concluida = new Meta(UUID.randomUUID(), "C", 100_00L, 100_00L);
            Meta incompleta = new Meta(UUID.randomUUID(), "I", 200_00L, 100_00L);
            dao.inserir(concluida);
            dao.inserir(incompleta);
            assertEquals(1, service.contarMetasConcluidas());
        }
    }
}
