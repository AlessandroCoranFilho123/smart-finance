package app.service;

import app.model.Meta;
import app.repository.MetaDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MetaService - regras de negócio")
class MetaServiceTest {

    // ── Fake manual de MetaDAO ──────────────────────────────────────────────
    static class FakeMetaDAO extends MetaDAO {
        final List<Meta> db = new ArrayList<>();
        Meta inserido;
        Meta atualizado;
        UUID deletado;

        @Override
        public void inserir(Meta m) {
            inserido = m;
            db.add(m);
        }

        @Override
        public void atualizar(Meta m) {
            atualizado = m;
            db.replaceAll(x -> x.getId().equals(m.getId()) ? m : x);
        }

        @Override
        public void deletar(UUID id) {
            deletado = id;
            db.removeIf(m -> m.getId().equals(id));
        }

        @Override
        public List<Meta> listarTodas() {
            return new ArrayList<>(db);
        }

        @Override
        public Meta buscarPorId(UUID id) {
            return db.stream().filter(m -> m.getId().equals(id)).findFirst().orElse(null);
        }
    }

    private FakeMetaDAO dao;
    private MetaService service;

    @BeforeEach
    void setUp() {
        dao = new FakeMetaDAO();
        service = new MetaService(dao);
    }

    @Test
    void construtorComDaoNuloLancaExcecao() {
        assertThrows(NullPointerException.class, () -> new MetaService(null));
    }

    @Nested
    @DisplayName("criarMeta()")
    class CriarMeta {
        @Test
        void delegaAoDAO() {
            service.criarMeta("Viagem", 1000_00L);
            assertNotNull(dao.inserido);
            assertEquals("Viagem", dao.inserido.getNome());
            assertEquals(1000_00L, dao.inserido.getAlvoCentavos());
        }

        @Test
        void nomeETrimado() {
            service.criarMeta("  Carro  ", 500_00L);
            assertEquals("Carro", dao.inserido.getNome());
        }

        @Test
        void nomeNuloLancaExcecao() {
            assertThrows(NullPointerException.class, () -> service.criarMeta(null, 100_00L));
            assertNull(dao.inserido);
        }

        @Test
        void nomeVazioLancaExcecao() {
            assertThrows(IllegalArgumentException.class, () -> service.criarMeta("   ", 100_00L));
            assertNull(dao.inserido);
        }

        @Test
        void valorZeroLancaExcecao() {
            assertThrows(IllegalArgumentException.class, () -> service.criarMeta("X", 0));
            assertNull(dao.inserido);
        }

        @Test
        void valorNegativoLancaExcecao() {
            assertThrows(IllegalArgumentException.class, () -> service.criarMeta("X", -1));
            assertNull(dao.inserido);
        }
    }

    @Nested
    @DisplayName("atualizarMeta()")
    class AtualizarMeta {
        @Test
        void delegaAoDAO() {
            Meta m = new Meta("Carro", 5000_00L);
            dao.db.add(m);
            service.atualizarMeta(m);
            assertEquals(m, dao.atualizado);
        }

        @Test
        void metaNulaLancaExcecao() {
            assertThrows(NullPointerException.class, () -> service.atualizarMeta(null));
        }

        @Test
        void semAlvoLancaExcecao() {
            assertThrows(IllegalArgumentException.class,
                    () -> service.atualizarMeta(new Meta(UUID.randomUUID(), "T", null, 0)));
        }
    }

    @Nested
    @DisplayName("calcularProgresso()")
    class CalcularProgresso {
        @Test
        void zeroSemAporte() {
            assertEquals(0.0, service.calcularProgresso(new Meta(UUID.randomUUID(), "T", 1000_00L, 0)));
        }

        @Test
        void cinquentaPorcento() {
            assertEquals(50.0, service.calcularProgresso(new Meta(UUID.randomUUID(), "T", 1000_00L, 500_00L)), 0.01);
        }

        @Test
        void cemPorcento() {
            assertEquals(100.0, service.calcularProgresso(new Meta(UUID.randomUUID(), "T", 500_00L, 500_00L)), 0.01);
        }

        @Test
        void naoUltrapassaCem() {
            assertEquals(100.0, service.calcularProgresso(new Meta(UUID.randomUUID(), "T", 100_00L, 200_00L)), 0.01);
        }

        @Test
        void metaNulaLancaExcecao() {
            assertThrows(NullPointerException.class, () -> service.calcularProgresso(null));
        }
    }

    @Nested
    @DisplayName("metaConcluida()")
    class MetaConcluida {
        @Test
        void concluidaQuandoAtingida() {
            assertTrue(service.metaConcluida(new Meta(UUID.randomUUID(), "T", 300_00L, 300_00L)));
        }

        @Test
        void concluidaQuandoUltrapassada() {
            assertTrue(service.metaConcluida(new Meta(UUID.randomUUID(), "T", 300_00L, 400_00L)));
        }

        @Test
        void naoConcluidaQuandoAbaixo() {
            assertFalse(service.metaConcluida(new Meta(UUID.randomUUID(), "T", 300_00L, 100_00L)));
        }

        @Test
        void metaNulaLancaExcecao() {
            assertThrows(NullPointerException.class, () -> service.metaConcluida(null));
        }

        @Test
        void metaSemAlvoNaoConcluida() {
            assertFalse(service.metaConcluida(new Meta(UUID.randomUUID(), "Livre", null, 500_00L)));
        }
    }

    @Nested
    @DisplayName("listar e contar")
    class ListarContar {
        @Test
        void listarDelegaAoDAO() {
            dao.db.add(new Meta("A", 100_00L));
            dao.db.add(new Meta("B", 200_00L));
            assertEquals(2, service.listarTodasMetas().size());
        }

        @Test
        void contarApenasConcluidasCorreto() {
            dao.db.add(new Meta(UUID.randomUUID(), "C1", 100_00L, 100_00L)); // concluida
            dao.db.add(new Meta(UUID.randomUUID(), "C2", 200_00L, 50_00L));  // em andamento
            dao.db.add(new Meta(UUID.randomUUID(), "C3", 300_00L, 300_00L)); // concluida
            assertEquals(2, service.contarMetasConcluidas());
        }

        @Test
        void contarZeroQuandoNenhumaConcluida() {
            dao.db.add(new Meta(UUID.randomUUID(), "C1", 100_00L, 10_00L));
            assertEquals(0, service.contarMetasConcluidas());
        }

        @Test
        void contarIgnoraMetasSemAlvo() {
            dao.db.add(new Meta(UUID.randomUUID(), "Livre", null, 900_00L));
            dao.db.add(new Meta(UUID.randomUUID(), "Carro", 500_00L, 500_00L));
            assertEquals(1, service.contarMetasConcluidas());
        }
    }

    @Nested
    @DisplayName("buscar, deletar e restante")
    class BuscarDeletarRestante {
        @Test
        void buscarPorIdDelegaAoDAO() {
            Meta meta = new Meta("Reserva", 300_00L);
            dao.db.add(meta);
            assertEquals(meta, service.buscarPorId(meta.getId()));
        }

        @Test
        void buscarPorIdNuloLancaExcecao() {
            assertThrows(NullPointerException.class, () -> service.buscarPorId(null));
        }

        @Test
        void deletarMetaDelegaAoDAO() {
            Meta meta = new Meta("Reserva", 300_00L);
            dao.db.add(meta);
            service.deletarMeta(meta.getId());
            assertEquals(meta.getId(), dao.deletado);
            assertTrue(dao.db.isEmpty());
        }

        @Test
        void deletarMetaNuloLancaExcecao() {
            assertThrows(NullPointerException.class, () -> service.deletarMeta(null));
        }

        @Test
        void calcularRestanteDelegaAoModelo() {
            assertEquals(150_00L, service.calcularRestante(new Meta(UUID.randomUUID(), "Reserva", 500_00L, 350_00L)));
        }

        @Test
        void calcularRestanteMetaNulaLancaExcecao() {
            assertThrows(NullPointerException.class, () -> service.calcularRestante(null));
        }
    }
}
