package app.repository;

import app.model.Categoria;
import app.model.Meta;
import app.model.TipoTransacao;
import app.model.Transacao;
import app.repository.TransacaoFiltro;
import app.service.TransacaoService;
import org.junit.jupiter.api.*;

import java.lang.reflect.Proxy;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração com SQLite in-memory.
 * A conexão compartilhada é protegida por um proxy que ignora close(),
 * evitando que o try-with-resources dos DAOs encerre a conexão entre testes.
 */
@DisplayName("Integração - DAO + SQLite in-memory")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseIntegrationTest {

    static Connection realConn;
    static Connection sharedConn; // proxy não-fechável

    // ── Proxy que ignora close() ──────────────────────────────────────────────
    static Connection nonClosing(Connection real) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class[]{Connection.class},
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) return null; // ignorar
                    if ("isClosed".equals(method.getName())) return false;
                    return method.invoke(real, args);
                }
        );
    }

    // ── DAOs que usam a conexão compartilhada ────────────────────────────────
    static class TestMetaDAO extends MetaDAO {
        @Override
        protected Connection getConn() {
            return sharedConn;
        }
    }

    static class TestTransacaoDAO extends TransacaoDAO {
        @Override
        protected Connection getConn() {
            return sharedConn;
        }
    }

    static class LegacySchemaTransacaoDAO extends TransacaoDAO {
        private final Connection realConn;
        private final Connection sharedConn;

        LegacySchemaTransacaoDAO() throws SQLException {
            this.realConn = DriverManager.getConnection("jdbc:sqlite::memory:");
            this.sharedConn = nonClosing(realConn);
            try (Statement s = realConn.createStatement()) {
                s.execute("PRAGMA foreign_keys = ON");
                s.execute("""
                        CREATE TABLE transacao (
                            id TEXT PRIMARY KEY,
                            descricao TEXT NOT NULL,
                            valor_centavos INTEGER NOT NULL,
                            tipo TEXT NOT NULL,
                            data TEXT NOT NULL,
                            meta_id TEXT,
                            categoria TEXT
                        )""");
            }
        }

        @Override
        protected Connection getConn() {
            return sharedConn;
        }

        void close() throws SQLException {
            realConn.close();
        }
    }

    // ── Setup / Teardown ─────────────────────────────────────────────────────
    @BeforeAll
    static void setup() throws Exception {
        realConn = DriverManager.getConnection("jdbc:sqlite::memory:");
        sharedConn = nonClosing(realConn);

        try (Statement s = realConn.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
            s.execute("""
                    CREATE TABLE IF NOT EXISTS meta (
                        id TEXT PRIMARY KEY, nome TEXT NOT NULL,
                        alvo_centavos INTEGER,
                        atual_centavos INTEGER NOT NULL DEFAULT 0)""");
            s.execute("""
                    CREATE TABLE IF NOT EXISTS transacao (
                        id TEXT PRIMARY KEY, descricao TEXT NOT NULL,
                        comentario TEXT NOT NULL DEFAULT '',
                        valor_centavos INTEGER NOT NULL, tipo TEXT NOT NULL,
                        data TEXT NOT NULL, meta_id TEXT, categoria TEXT,
                        FOREIGN KEY (meta_id) REFERENCES meta(id) ON DELETE SET NULL)""");
        }
    }

    @AfterAll
    static void teardown() throws Exception {
        if (realConn != null && !realConn.isClosed()) realConn.close();
    }

    @BeforeEach
    void limpar() throws Exception {
        try (Statement s = realConn.createStatement()) {
            s.execute("DELETE FROM transacao");
            s.execute("DELETE FROM meta");
        }
    }

    // ── MetaDAO ──────────────────────────────────────────────────────────────
    @Test
    @Order(1)
    @DisplayName("Inserir e listar metas")
    void inserirListarMetas() {
        MetaDAO dao = new TestMetaDAO();
        dao.inserir(new Meta("Viagem", 2000_00L));
        dao.inserir(new Meta("Carro", 50000_00L));
        assertEquals(2, dao.listarTodas().size());
    }

    @Test
    @Order(2)
    @DisplayName("buscarPorId retorna meta correta")
    void buscarMetaPorId() {
        MetaDAO dao = new TestMetaDAO();
        Meta m = new Meta("Poupança", 10000_00L);
        dao.inserir(m);
        Meta found = dao.buscarPorId(m.getId());
        assertNotNull(found);
        assertEquals("Poupança", found.getNome());
        assertEquals(10000_00L, found.getAlvoCentavos());
        assertEquals(0, found.getAtualCentavos());
    }

    @Test
    @Order(3)
    @DisplayName("atualizar persiste mudanças")
    void atualizarMeta() {
        MetaDAO dao = new TestMetaDAO();
        Meta m = new Meta(UUID.randomUUID(), "Viagem", 1000_00L, 0);
        dao.inserir(m);
        m.adicionar(500_00L);
        dao.atualizar(m);
        assertEquals(500_00L, dao.buscarPorId(m.getId()).getAtualCentavos());
    }

    @Test
    @Order(4)
    @DisplayName("deletar remove do banco")
    void deletarMeta() {
        MetaDAO dao = new TestMetaDAO();
        Meta m = new Meta("Temp", 100_00L);
        dao.inserir(m);
        dao.deletar(m.getId());
        assertNull(dao.buscarPorId(m.getId()));
        assertEquals(0, dao.listarTodas().size());
    }

    @Test
    @Order(5)
    @DisplayName("buscarPorId retorna null para ID inexistente")
    void buscarMetaInexistente() {
        assertNull(new TestMetaDAO().buscarPorId(UUID.randomUUID()));
    }

    @Test
    @Order(6)
    @DisplayName("Meta com alvo null persiste como null")
    void metaSemAlvo() {
        MetaDAO dao = new TestMetaDAO();
        Meta m = new Meta(UUID.randomUUID(), "SemAlvo", null, 0);
        dao.inserir(m);
        assertTrue(dao.buscarPorId(m.getId()).semAlvo());
    }

    // ── TransacaoDAO ──────────────────────────────────────────────────────────
    @Test
    @Order(7)
    @DisplayName("Inserir e listar transações")
    void inserirListarTransacoes() {
        TransacaoDAO dao = new TestTransacaoDAO();
        dao.inserir(new Transacao(UUID.randomUUID(), "Salário", "ok",
                5000_00L, TipoTransacao.Entrada, LocalDate.of(2024, 1, 5), null, Categoria.Salario));
        dao.inserir(new Transacao(UUID.randomUUID(), "Aluguel", "",
                1200_00L, TipoTransacao.Saida, LocalDate.of(2024, 1, 10), null, Categoria.Aluguel));
        assertEquals(2, dao.listarTodas().size());
    }

    @Test
    @Order(8)
    @DisplayName("calcularSaldo: entradas - saídas")
    void calcularSaldo() {
        TransacaoDAO dao = new TestTransacaoDAO();
        dao.inserir(new Transacao(UUID.randomUUID(), "Sal", "",
                5000_00L, TipoTransacao.Entrada, LocalDate.now(), null, Categoria.Salario));
        dao.inserir(new Transacao(UUID.randomUUID(), "Alug", "",
                1500_00L, TipoTransacao.Saida, LocalDate.now(), null, Categoria.Aluguel));
        assertEquals(3500_00L, dao.calcularSaldo());
    }

    @Test
    @Order(9)
    @DisplayName("calcularSaldo retorna 0 sem transações")
    void saldoVazioZero() {
        assertEquals(0L, new TestTransacaoDAO().calcularSaldo());
    }

    @Test
    @Order(10)
    @DisplayName("calcularTotalPorTipoEMes filtra por mês")
    void calcularTotalPorMes() {
        TransacaoDAO dao = new TestTransacaoDAO();
        dao.inserir(new Transacao(UUID.randomUUID(), "S1", "",
                3000_00L, TipoTransacao.Entrada, LocalDate.of(2024, 3, 1), null, Categoria.Salario));
        dao.inserir(new Transacao(UUID.randomUUID(), "S2", "",
                2000_00L, TipoTransacao.Entrada, LocalDate.of(2024, 3, 15), null, Categoria.Salario));
        dao.inserir(new Transacao(UUID.randomUUID(), "S3", "",
                1000_00L, TipoTransacao.Entrada, LocalDate.of(2024, 4, 1), null, Categoria.Salario));
        assertEquals(5000_00L, dao.calcularTotalPorTipoEMes(TipoTransacao.Entrada, "2024-03"));
        assertEquals(1000_00L, dao.calcularTotalPorTipoEMes(TipoTransacao.Entrada, "2024-04"));
        assertEquals(0L, dao.calcularTotalPorTipoEMes(TipoTransacao.Saida, "2024-03"));
    }

    @Test
    @Order(11)
    @DisplayName("excluir remove transação do banco")
    void excluirTransacao() {
        TransacaoDAO dao = new TestTransacaoDAO();
        UUID id = UUID.randomUUID();
        dao.inserir(new Transacao(id, "T", "", 100_00L, TipoTransacao.Saida, LocalDate.now(), null, Categoria.Outros));
        dao.excluir(id);
        assertNull(dao.buscarPorId(id));
    }

    @Test
    @Order(12)
    @DisplayName("buscarPorId retorna null para ID inexistente")
    void buscarTransacaoInexistente() {
        assertNull(new TestTransacaoDAO().buscarPorId(UUID.randomUUID()));
    }

    @Test
    @Order(13)
    @DisplayName("comentario persiste e é recuperado corretamente")
    void comentarioPersiste() {
        TransacaoDAO dao = new TestTransacaoDAO();
        UUID id = UUID.randomUUID();
        dao.inserir(new Transacao(id, "Mercado", "compras do mês", 350_00L,
                TipoTransacao.Saida, LocalDate.now(), null, Categoria.Alimentacao));
        assertEquals("compras do mês", dao.buscarPorId(id).comentario());
    }

    @Test
    @Order(14)
    @DisplayName("comentario null salvo como string vazia")
    void comentarioNuloViraVazio() {
        TransacaoDAO dao = new TestTransacaoDAO();
        UUID id = UUID.randomUUID();
        dao.inserir(new Transacao(id, "Sal", null, 5000_00L,
                TipoTransacao.Entrada, LocalDate.now(), null, Categoria.Salario));
        assertEquals("", dao.buscarPorId(id).comentario());
    }

    @Test
    @Order(15)
    @DisplayName("listarRecentes limita e ordena por data desc")
    void listarRecentes() {
        TransacaoDAO dao = new TestTransacaoDAO();
        dao.inserir(new Transacao(UUID.randomUUID(), "T1", "",
                100_00L, TipoTransacao.Entrada, LocalDate.of(2024, 1, 1), null, Categoria.Salario));
        dao.inserir(new Transacao(UUID.randomUUID(), "T2", "",
                200_00L, TipoTransacao.Entrada, LocalDate.of(2024, 3, 1), null, Categoria.Salario));
        dao.inserir(new Transacao(UUID.randomUUID(), "T3", "",
                300_00L, TipoTransacao.Entrada, LocalDate.of(2024, 2, 1), null, Categoria.Salario));
        List<Transacao> recentes = dao.listarRecentes(2);
        assertEquals(2, recentes.size());
        assertEquals("T2", recentes.get(0).descricao());
        assertEquals("T3", recentes.get(1).descricao());
    }

    @Test
    @Order(16)
    @DisplayName("listarRecentes usa rowid desc para desempatar mesma data")
    void listarRecentesDesempataPorRowId() {
        TransacaoDAO dao = new TestTransacaoDAO();
        LocalDate mesmaData = LocalDate.of(2024, 3, 1);
        dao.inserir(new Transacao(UUID.randomUUID(), "Primeira", "",
                100_00L, TipoTransacao.Entrada, mesmaData, null, Categoria.Salario));
        dao.inserir(new Transacao(UUID.randomUUID(), "Segunda", "",
                200_00L, TipoTransacao.Entrada, mesmaData, null, Categoria.Salario));

        List<Transacao> recentes = dao.listarRecentes(2);
        assertEquals("Segunda", recentes.get(0).descricao());
        assertEquals("Primeira", recentes.get(1).descricao());
    }

    @Test
    @Order(17)
    @DisplayName("categoria null persiste e é recuperada como null")
    void categoriaNullPersiste() {
        TransacaoDAO dao = new TestTransacaoDAO();
        UUID id = UUID.randomUUID();
        dao.inserir(new Transacao(id, "Misc", "", 50_00L, TipoTransacao.Saida, LocalDate.now(), null, null));
        assertNull(dao.buscarPorId(id).categoria());
    }

    @Test
    @Order(18)
    @DisplayName("deletar meta mantém transação e limpa meta_id por foreign key")
    void deletarMetaLimpaMetaIdDaTransacao() {
        MetaDAO metaDAO = new TestMetaDAO();
        TransacaoDAO transacaoDAO = new TestTransacaoDAO();
        Meta meta = new Meta("Reserva", 1000_00L);
        metaDAO.inserir(meta);

        UUID transacaoId = UUID.randomUUID();
        transacaoDAO.inserir(new Transacao(transacaoId, "Aplicação em meta: Reserva", "",
                300_00L, TipoTransacao.Saida, LocalDate.now(), meta.getId(), Categoria.AdicionarMeta));

        metaDAO.deletar(meta.getId());

        Transacao transacao = transacaoDAO.buscarPorId(transacaoId);
        assertNotNull(transacao);
        assertNull(transacao.metaId());
    }

    @Test
    @Order(19)
    @DisplayName("listarTodas ordena por data desc e rowid desc em empate")
    void listarTodasOrdenaPorDataERowId() {
        TransacaoDAO dao = new TestTransacaoDAO();
        LocalDate mesmaData = LocalDate.of(2024, 5, 10);
        dao.inserir(new Transacao(UUID.randomUUID(), "Primeira", "",
                100_00L, TipoTransacao.Entrada, mesmaData, null, Categoria.Salario));
        dao.inserir(new Transacao(UUID.randomUUID(), "Segunda", "",
                200_00L, TipoTransacao.Entrada, mesmaData, null, Categoria.Salario));

        List<Transacao> todas = dao.listarTodas();
        assertEquals("Segunda", todas.get(0).descricao());
        assertEquals("Primeira", todas.get(1).descricao());
    }

    @Test
    @Order(20)
    @DisplayName("migration adiciona comentario em schema legado")
    void migrationComentarioEmSchemaLegado() throws Exception {
        LegacySchemaTransacaoDAO dao = new LegacySchemaTransacaoDAO();
        try {
            UUID id = UUID.randomUUID();
            dao.inserir(new Transacao(id, "Salário", null,
                    5000_00L, TipoTransacao.Entrada, LocalDate.now(), null, Categoria.Salario));

            try (Statement s = dao.realConn.createStatement();
                 ResultSet rs = s.executeQuery("PRAGMA table_info(transacao)")) {
                boolean possuiComentario = false;
                while (rs.next()) {
                    if ("comentario".equals(rs.getString("name"))) {
                        possuiComentario = true;
                        break;
                    }
                }
                assertTrue(possuiComentario);
            }

            assertEquals("", dao.buscarPorId(id).comentario());
        } finally {
            dao.close();
        }
    }

    // ── Integração Service + DAO ──────────────────────────────────────────────
    @Test
    @Order(21)
    @DisplayName("AdicionarMeta: atualiza saldo da meta no banco")
    void fluxoAdicionarMeta() {
        MetaDAO mDao = new TestMetaDAO();
        TransacaoDAO tDao = new TestTransacaoDAO();
        tDao.inserir(new Transacao(UUID.randomUUID(), "Salário", "",
                10000_00L, TipoTransacao.Entrada, LocalDate.now(), null, Categoria.Salario));
        Meta meta = new Meta("Viagem", 3000_00L);
        mDao.inserir(meta);
        new TransacaoService(tDao, mDao)
                .registrar(TipoTransacao.Saida, Categoria.AdicionarMeta, 1000_00L, meta, "", LocalDate.now());
        assertEquals(1000_00L, mDao.buscarPorId(meta.getId()).getAtualCentavos());
        assertEquals(2, tDao.listarTodas().size());
    }

    @Test
    @Order(22)
    @DisplayName("Excluir AdicionarMeta reverte saldo da meta")
    void fluxoExcluirAdicionarMeta() {
        MetaDAO mDao = new TestMetaDAO();
        TransacaoDAO tDao = new TestTransacaoDAO();
        TransacaoService service = new TransacaoService(tDao, mDao);
        tDao.inserir(new Transacao(UUID.randomUUID(), "Salário", "",
                10000_00L, TipoTransacao.Entrada, LocalDate.now(), null, Categoria.Salario));
        Meta meta = new Meta("Viagem", 3000_00L);
        mDao.inserir(meta);
        service.registrar(TipoTransacao.Saida, Categoria.AdicionarMeta, 1000_00L, meta, "", LocalDate.now());
        UUID idAplic = tDao.listarTodas().stream()
                .filter(t -> t.categoria() == Categoria.AdicionarMeta)
                .findFirst().orElseThrow().id();
        service.excluirTransacao(idAplic, Categoria.AdicionarMeta);
        assertEquals(0L, mDao.buscarPorId(meta.getId()).getAtualCentavos());
    }

    @Test
    @Order(23)
    @DisplayName("Atualizar transação comum preserva UUID no banco")
    void fluxoAtualizarTransacaoComum() {
        MetaDAO mDao = new TestMetaDAO();
        TransacaoDAO tDao = new TestTransacaoDAO();
        TransacaoService service = new TransacaoService(tDao, mDao);
        UUID id = UUID.randomUUID();
        tDao.inserir(new Transacao(id, "Aluguel", "", 800_00L,
                TipoTransacao.Saida, LocalDate.now(), null, Categoria.Aluguel));
        service.atualizar(id, TipoTransacao.Saida, Categoria.Aluguel, 900_00L, null, "reajuste", LocalDate.now());
        Transacao atualizada = tDao.buscarPorId(id);
        assertNotNull(atualizada);
        assertEquals(id, atualizada.id());
        assertEquals(900_00L, atualizada.valorCentavos());
        assertEquals("reajuste", atualizada.comentario());
    }

    @Test
    @Order(24)
    @DisplayName("Atualização inválida preserva transação original")
    void fluxoAtualizacaoInvalidaRestauraOriginal() {
        MetaDAO mDao = new TestMetaDAO();
        TransacaoDAO tDao = new TestTransacaoDAO();
        TransacaoService service = new TransacaoService(tDao, mDao);
        UUID id = UUID.randomUUID();
        Transacao original = new Transacao(id, "Aluguel", "orig", 800_00L,
                TipoTransacao.Saida, LocalDate.now(), null, Categoria.Aluguel);
        tDao.inserir(original);

        assertThrows(IllegalArgumentException.class, () ->
                service.atualizar(id, TipoTransacao.Saida, Categoria.Aluguel, 0, null, "inválida", LocalDate.now()));

        assertEquals(original, tDao.buscarPorId(id));
    }

    @Test
    @Order(25)
    @DisplayName("RetirarMeta: reduz saldo e cria transação Entrada")
    void fluxoRetirarMeta() {
        MetaDAO mDao = new TestMetaDAO();
        TransacaoDAO tDao = new TestTransacaoDAO();
        Meta meta = new Meta(UUID.randomUUID(), "Poupança", 2000_00L, 1500_00L);
        mDao.inserir(meta);
        new TransacaoService(tDao, mDao)
                .registrar(TipoTransacao.Entrada, Categoria.RetirarMeta, 500_00L, meta, "resgate parcial", LocalDate.now());
        assertEquals(1000_00L, mDao.buscarPorId(meta.getId()).getAtualCentavos());
        Transacao t = tDao.listarTodas().get(0);
        assertEquals(TipoTransacao.Entrada, t.tipo());
        assertEquals("resgate parcial", t.comentario());
    }

    @Test
    @Order(26)
    @DisplayName("listarPorFiltro combina busca textual, tipo, categoria e datas")
    void listarPorFiltroCombinaCriterios() {
        TransacaoDAO dao = new TestTransacaoDAO();
        dao.inserir(new Transacao(UUID.randomUUID(), "Café da manhã", "padaria central",
                45_00L, TipoTransacao.Saida, LocalDate.of(2026, 4, 13), null, Categoria.Alimentacao));
        dao.inserir(new Transacao(UUID.randomUUID(), "Café da manhã", "fora do intervalo",
                30_00L, TipoTransacao.Saida, LocalDate.of(2026, 5, 1), null, Categoria.Alimentacao));
        dao.inserir(new Transacao(UUID.randomUUID(), "Salário", "pagamento",
                5000_00L, TipoTransacao.Entrada, LocalDate.of(2026, 4, 10), null, Categoria.Salario));

        List<Transacao> filtradas = dao.listarPorFiltro(new TransacaoFiltro(
                "cafe padaria",
                TipoTransacao.Saida,
                Categoria.Alimentacao,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30)
        ));

        assertAll(
                () -> assertEquals(1, filtradas.size()),
                () -> assertEquals("Café da manhã", filtradas.getFirst().descricao()),
                () -> assertEquals("padaria central", filtradas.getFirst().comentario())
        );
    }

    @Test
    @Order(27)
    @DisplayName("busca textual acompanha exclusões por trigger FTS")
    void buscaTextualAcompanhaExclusoes() {
        TransacaoDAO dao = new TestTransacaoDAO();
        UUID id = UUID.randomUUID();
        dao.inserir(new Transacao(id, "Internet", "fibra residencial",
                120_00L, TipoTransacao.Saida, LocalDate.of(2026, 4, 1), null, Categoria.Internet));

        assertEquals(1, dao.listarPorFiltro(new TransacaoFiltro("fibra", null, null, null, null)).size());

        dao.excluir(id);

        assertTrue(dao.listarPorFiltro(new TransacaoFiltro("fibra", null, null, null, null)).isEmpty());
    }
}
