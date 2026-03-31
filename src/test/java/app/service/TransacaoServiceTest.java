package app.service;

import app.model.Categoria;
import app.model.Meta;
import app.model.TipoTransacao;
import app.model.Transacao;
import app.repository.MetaDAO;
import app.repository.TransacaoDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TransacaoService - regras de negócio")
class TransacaoServiceTest {

    // ── Fakes ────────────────────────────────────────────────────────────────
    static class FakeTransacaoDAO extends TransacaoDAO {
        final Map<UUID, Transacao> db = new LinkedHashMap<>();
        long saldoFixo = 0;

        @Override
        public void inserir(Transacao t) {
            db.put(t.id(), t);
        }

        @Override
        public void excluir(UUID id) {
            db.remove(id);
        }

        @Override
        public Transacao buscarPorId(UUID id) {
            return db.get(id);
        }

        @Override
        public long calcularSaldo() {
            return saldoFixo;
        }

        @Override
        public List<Transacao> listarTodas() {
            return new ArrayList<>(db.values());
        }

        @Override
        public long calcularTotalPorTipoEMes(TipoTransacao tipo, String mes) {
            return db.values().stream()
                    .filter(t -> t.tipo() == tipo && t.data().toString().startsWith(mes))
                    .mapToLong(Transacao::valorCentavos).sum();
        }
    }

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
    }

    private FakeTransacaoDAO tDAO;
    private FakeMetaDAO mDAO;
    private TransacaoService service;

    @BeforeEach
    void setUp() {
        tDAO = new FakeTransacaoDAO();
        mDAO = new FakeMetaDAO();
        service = new TransacaoService(tDAO, mDAO);
    }

    @Test
    void construtorComDependenciasNulasLancaExcecao() {
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> new TransacaoService(null, mDAO)),
                () -> assertThrows(NullPointerException.class, () -> new TransacaoService(tDAO, null))
        );
    }

    // ── registrar: transação comum ───────────────────────────────────────────
    @Nested
    @DisplayName("registrar() - transação comum")
    class RegistrarComum {
        @Test
        void registraEntrada() {
            service.registrar(TipoTransacao.Entrada, Categoria.Salario, 5000_00L, null, "ok", LocalDate.now());
            assertEquals(1, tDAO.db.size());
            Transacao t = tDAO.db.values().iterator().next();
            assertEquals(TipoTransacao.Entrada, t.tipo());
            assertEquals(Categoria.Salario, t.categoria());
            assertEquals(5000_00L, t.valorCentavos());
        }

        @Test
        void registraSaida() {
            service.registrar(TipoTransacao.Saida, Categoria.Aluguel, 1200_00L, null, "", LocalDate.now());
            assertEquals(TipoTransacao.Saida, tDAO.db.values().iterator().next().tipo());
        }

        @Test
        void comentarioPassadoParaTransacao() {
            service.registrar(TipoTransacao.Saida, Categoria.Alimentacao, 100_00L, null, "mercado", LocalDate.now());
            assertEquals("mercado", tDAO.db.values().iterator().next().comentario());
        }

        @Test
        void comentarioNuloViraVazio() {
            service.registrar(TipoTransacao.Entrada, Categoria.Salario, 100_00L, null, null, LocalDate.now());
            assertEquals("", tDAO.db.values().iterator().next().comentario());
        }

        @Test
        void valorZeroLancaExcecao() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.registrar(TipoTransacao.Entrada, Categoria.Salario, 0, null, "", LocalDate.now()));
            assertTrue(tDAO.db.isEmpty());
        }

        @Test
        void valorNegativoLancaExcecao() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.registrar(TipoTransacao.Saida, Categoria.Alimentacao, -1L, null, "", LocalDate.now()));
        }

        @Test
        void tipoNuloLancaExcecao() {
            assertThrows(NullPointerException.class, () ->
                    service.registrar(null, Categoria.Salario, 100_00L, null, "", LocalDate.now()));
        }

        @Test
        void categoriaNulaLancaExcecao() {
            assertThrows(NullPointerException.class, () ->
                    service.registrar(TipoTransacao.Entrada, null, 100_00L, null, "", LocalDate.now()));
        }

        @Test
        void dataNulaLancaExcecao() {
            assertThrows(NullPointerException.class, () ->
                    service.registrar(TipoTransacao.Entrada, Categoria.Salario, 100_00L, null, "", null));
            assertTrue(tDAO.db.isEmpty());
        }

        @Test
        void adicionarMetaSemMetaLancaExcecao() {
            tDAO.saldoFixo = 1000_00L;
            assertThrows(IllegalArgumentException.class, () ->
                    service.registrar(TipoTransacao.Saida, Categoria.AdicionarMeta, 100_00L, null, "", LocalDate.now()));
        }

        @Test
        void retirarMetaSemMetaLancaExcecao() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.registrar(TipoTransacao.Entrada, Categoria.RetirarMeta, 100_00L, null, "", LocalDate.now()));
        }
    }

    // ── registrar: AdicionarMeta ─────────────────────────────────────────────
    @Nested
    @DisplayName("registrar() - AdicionarMeta")
    class RegistrarAdicionarMeta {
        @Test
        void adicionaAMetaEInsereSaida() {
            Meta meta = new Meta(UUID.randomUUID(), "Viagem", 1000_00L, 200_00L);
            mDAO.db.put(meta.getId(), meta);
            tDAO.saldoFixo = 500_00L;

            service.registrar(TipoTransacao.Saida, Categoria.AdicionarMeta, 300_00L, meta, "", LocalDate.now());

            assertEquals(500_00L, meta.getAtualCentavos()); // 200 + 300
            assertEquals(1, tDAO.db.size());
            Transacao t = tDAO.db.values().iterator().next();
            assertEquals(TipoTransacao.Saida, t.tipo());
            assertEquals(Categoria.AdicionarMeta, t.categoria());
            assertEquals(300_00L, t.valorCentavos());
            assertEquals(meta.getId(), t.metaId());
        }

        @Test
        void saldoInsuficienteLancaExcecao() {
            Meta meta = new Meta(UUID.randomUUID(), "V", 1000_00L, 0);
            tDAO.saldoFixo = 50_00L;
            assertThrows(IllegalArgumentException.class, () ->
                    service.registrar(TipoTransacao.Saida, Categoria.AdicionarMeta, 200_00L, meta, "", LocalDate.now()));
            assertNull(mDAO.ultimoAtualizado);
        }

        @Test
        void metaJaConcluidaLancaExcecao() {
            Meta meta = new Meta(UUID.randomUUID(), "V", 500_00L, 500_00L);
            tDAO.saldoFixo = 1000_00L;
            assertThrows(IllegalArgumentException.class, () ->
                    service.registrar(TipoTransacao.Saida, Categoria.AdicionarMeta, 100_00L, meta, "", LocalDate.now()));
        }

        @Test
        void limiteRestanteParaAlvo() {
            // Meta com 800/1000 — faltam 200; solicita 500 → deve adicionar só 200
            Meta meta = new Meta(UUID.randomUUID(), "V", 1000_00L, 800_00L);
            tDAO.saldoFixo = 1000_00L;
            service.registrar(TipoTransacao.Saida, Categoria.AdicionarMeta, 500_00L, meta, "", LocalDate.now());
            assertEquals(1000_00L, meta.getAtualCentavos());
            assertEquals(200_00L, tDAO.db.values().iterator().next().valorCentavos());
        }
    }

    // ── registrar: RetirarMeta ───────────────────────────────────────────────
    @Nested
    @DisplayName("registrar() - RetirarMeta")
    class RegistrarRetirarMeta {
        @Test
        void retiraDaMetaEInsereEntrada() {
            Meta meta = new Meta(UUID.randomUUID(), "P", 1000_00L, 600_00L);
            mDAO.db.put(meta.getId(), meta);
            service.registrar(TipoTransacao.Entrada, Categoria.RetirarMeta, 200_00L, meta, "", LocalDate.now());
            assertEquals(400_00L, meta.getAtualCentavos());
            assertEquals(TipoTransacao.Entrada, tDAO.db.values().iterator().next().tipo());
        }

        @Test
        void naoRetiraMaisQueDisponivel() {
            Meta meta = new Meta(UUID.randomUUID(), "P", 1000_00L, 100_00L);
            mDAO.db.put(meta.getId(), meta);
            service.registrar(TipoTransacao.Entrada, Categoria.RetirarMeta, 500_00L, meta, "", LocalDate.now());
            assertEquals(0, meta.getAtualCentavos());
            assertEquals(100_00L, tDAO.db.values().iterator().next().valorCentavos());
        }

        @Test
        void retiradaZeroLancaExcecao() {
            // meta vazia — retirar retorna 0 → service lança
            Meta meta = new Meta(UUID.randomUUID(), "P", 1000_00L, 0);
            assertThrows(IllegalArgumentException.class, () ->
                    service.registrar(TipoTransacao.Entrada, Categoria.RetirarMeta, 100_00L, meta, "", LocalDate.now()));
        }
    }

    // ── atualizar ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("atualizar()")
    class Atualizar {
        @Test
        void atualizaTransacaoComumPreservandoUUID() {
            UUID id = UUID.randomUUID();
            Transacao original = new Transacao(id, "Aluguel", "", 800_00L,
                    TipoTransacao.Saida, LocalDate.now(), null, Categoria.Aluguel);
            tDAO.db.put(id, original);

            service.atualizar(id, TipoTransacao.Saida, Categoria.Aluguel, 900_00L, null, "reajuste", LocalDate.now());

            Transacao atualizada = tDAO.db.get(id);
            assertNotNull(atualizada);
            assertEquals(900_00L, atualizada.valorCentavos());
            assertEquals("reajuste", atualizada.comentario());
            assertEquals(id, atualizada.id()); // UUID preservado
        }

        @Test
        void atualizarAdicionarMetaRevalidaSaldo() {
            // Configura: meta com 200/1000, saldo 500
            Meta meta = new Meta(UUID.randomUUID(), "V", 1000_00L, 200_00L);
            mDAO.db.put(meta.getId(), meta);
            tDAO.saldoFixo = 500_00L;

            // Cria transação original de AdicionarMeta
            UUID id = UUID.randomUUID();
            Transacao orig = new Transacao(id, "Aplicação em meta: V", "", 200_00L,
                    TipoTransacao.Saida, LocalDate.now(), meta.getId(), Categoria.AdicionarMeta);
            tDAO.db.put(id, orig);

            // A exclusão da original deve reverter os 200 da meta
            // Depois registrar com novo valor 300
            service.atualizar(id, TipoTransacao.Saida, Categoria.AdicionarMeta, 300_00L, meta, "", LocalDate.now());

            // Meta deve ter 300 (não 200+300=500)
            assertEquals(300_00L, meta.getAtualCentavos());
        }

        @Test
        void idNuloLancaExcecao() {
            assertThrows(NullPointerException.class, () ->
                    service.atualizar(null, TipoTransacao.Saida, Categoria.Aluguel, 100_00L, null, "", LocalDate.now()));
        }

        @Test
        void transacaoOriginalInexistenteLancaExcecao() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.atualizar(UUID.randomUUID(), TipoTransacao.Saida, Categoria.Aluguel, 100_00L, null, "", LocalDate.now()));
        }

        @Test
        void falhaNaAtualizacaoComumPreservaOriginal() {
            UUID id = UUID.randomUUID();
            Transacao original = new Transacao(id, "Aluguel", "orig", 800_00L,
                    TipoTransacao.Saida, LocalDate.now(), null, Categoria.Aluguel);
            tDAO.db.put(id, original);

            assertThrows(IllegalArgumentException.class, () ->
                    service.atualizar(id, TipoTransacao.Saida, Categoria.Aluguel, 0, null, "inválida", LocalDate.now()));

            assertEquals(original, tDAO.db.get(id));
            assertEquals(1, tDAO.db.size());
        }

        @Test
        void falhaNaAtualizacaoDeMetaPreservaOriginalEEstadoDaMeta() {
            Meta meta = new Meta(UUID.randomUUID(), "Viagem", 1000_00L, 200_00L);
            mDAO.db.put(meta.getId(), meta);
            tDAO.saldoFixo = 50_00L;

            UUID id = UUID.randomUUID();
            Transacao original = new Transacao(id, "Aplicação em meta: Viagem", "", 200_00L,
                    TipoTransacao.Saida, LocalDate.now(), meta.getId(), Categoria.AdicionarMeta);
            tDAO.db.put(id, original);

            assertThrows(IllegalArgumentException.class, () ->
                    service.atualizar(id, TipoTransacao.Saida, Categoria.AdicionarMeta, 500_00L, meta, "", LocalDate.now()));

            assertEquals(200_00L, meta.getAtualCentavos());
            assertEquals(original, tDAO.db.get(id));
            assertEquals(1, tDAO.db.size());
        }
    }

    // ── excluirTransacao ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("excluirTransacao()")
    class ExcluirTransacao {
        @Test
        void excluiTransacaoComum() {
            UUID id = UUID.randomUUID();
            tDAO.db.put(id, new Transacao(id, "Aluguel", "", 800_00L,
                    TipoTransacao.Saida, LocalDate.now(), null, Categoria.Aluguel));
            service.excluirTransacao(id, Categoria.Aluguel);
            assertFalse(tDAO.db.containsKey(id));
        }

        @Test
        void excluirAdicionarMetaReverteMeta() {
            UUID id = UUID.randomUUID();
            UUID metaId = UUID.randomUUID();
            Meta meta = new Meta(metaId, "V", 1000_00L, 500_00L);
            mDAO.db.put(metaId, meta);
            tDAO.db.put(id, new Transacao(id, "Aplic", "", 300_00L,
                    TipoTransacao.Saida, LocalDate.now(), metaId, Categoria.AdicionarMeta));

            service.excluirTransacao(id, Categoria.AdicionarMeta);

            assertEquals(200_00L, meta.getAtualCentavos()); // 500 - 300 = 200
            assertFalse(tDAO.db.containsKey(id));
        }

        @Test
        void excluirRetirarMetaRestauraMeta() {
            UUID id = UUID.randomUUID();
            UUID metaId = UUID.randomUUID();
            Meta meta = new Meta(metaId, "P", 1000_00L, 200_00L);
            mDAO.db.put(metaId, meta);
            tDAO.db.put(id, new Transacao(id, "Resgate", "", 150_00L,
                    TipoTransacao.Entrada, LocalDate.now(), metaId, Categoria.RetirarMeta));

            service.excluirTransacao(id, Categoria.RetirarMeta);

            assertEquals(350_00L, meta.getAtualCentavos()); // 200 + 150 = 350
        }

        @Test
        void transacaoInexistenteFazNada() {
            service.excluirTransacao(UUID.randomUUID(), Categoria.Aluguel); // não lança
            assertTrue(tDAO.db.isEmpty());
        }

        @Test
        void categoriaInformadaErradaNaoImpedeReversaoCorreta() {
            UUID id = UUID.randomUUID();
            UUID metaId = UUID.randomUUID();
            Meta meta = new Meta(metaId, "V", 1000_00L, 500_00L);
            mDAO.db.put(metaId, meta);
            tDAO.db.put(id, new Transacao(id, "Aplic", "", 300_00L,
                    TipoTransacao.Saida, LocalDate.now(), metaId, Categoria.AdicionarMeta));

            service.excluirTransacao(id, Categoria.Aluguel);

            assertEquals(200_00L, meta.getAtualCentavos());
            assertFalse(tDAO.db.containsKey(id));
        }

        @Test
        void idNuloLancaExcecao() {
            assertThrows(NullPointerException.class, () -> service.excluirTransacao(null, Categoria.Aluguel));
        }
    }

    // ── cálculos mensais ──────────────────────────────────────────────────────
    @Nested
    @DisplayName("Cálculos mensais")
    class CalculosMensais {
        @Test
        void calcularReceitasMes() {
            tDAO.db.put(UUID.randomUUID(), new Transacao(UUID.randomUUID(), "S", "",
                    5000_00L, TipoTransacao.Entrada, LocalDate.of(2024, 3, 10), null, Categoria.Salario));
            tDAO.db.put(UUID.randomUUID(), new Transacao(UUID.randomUUID(), "S2", "",
                    2000_00L, TipoTransacao.Entrada, LocalDate.of(2024, 3, 20), null, Categoria.Salario));
            assertEquals(7000_00L, service.calcularReceitasMes(YearMonth.of(2024, 3)));
        }

        @Test
        void calcularDespesasMes() {
            tDAO.db.put(UUID.randomUUID(), new Transacao(UUID.randomUUID(), "A", "",
                    1200_00L, TipoTransacao.Saida, LocalDate.of(2024, 4, 5), null, Categoria.Aluguel));
            assertEquals(1200_00L, service.calcularDespesasMes(YearMonth.of(2024, 4)));
        }

        @Test
        void receitasMesErradoRetornaZero() {
            tDAO.db.put(UUID.randomUUID(), new Transacao(UUID.randomUUID(), "S", "",
                    5000_00L, TipoTransacao.Entrada, LocalDate.of(2024, 3, 1), null, Categoria.Salario));
            assertEquals(0L, service.calcularReceitasMes(YearMonth.of(2024, 4)));
        }

        @Test
        void saldoDisponivelDelegaAoDAO() {
            tDAO.saldoFixo = 7500_00L;
            assertEquals(7500_00L, service.calcularSaldoDisponivelCentavos());
        }
    }
}
