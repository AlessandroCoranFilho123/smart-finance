package app.integration;

import app.database.Database;
import app.model.Categoria;
import app.model.Meta;
import app.model.TipoTransacao;
import app.model.Transacao;
import app.repository.MetaDAO;
import app.repository.TransacaoDAO;
import app.service.TransacaoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Integração - bootstrap de banco")
class DatabaseBootstrapIntegrationTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void clearDbOverride() {
        Database.clearOverrideUrlForTests();
    }

    @Test
    @DisplayName("máquina limpa cria banco e schema completo")
    void maquinaLimpaCriaBancoESchemaCompleto() throws Exception {
        Path dbFile = tempDir.resolve("clean-machine.db");
        assertFalse(Files.exists(dbFile));

        Database.overrideUrlForTests(jdbcUrl(dbFile));
        Database.inicializar();

        assertTrue(Files.exists(dbFile));

        try (Connection conn = DriverManager.getConnection(jdbcUrl(dbFile))) {
            Set<String> columns = columnsOf(conn, "transacao");
            assertAll(
                    () -> assertTrue(columns.contains("comentario")),
                    () -> assertTrue(columns.contains("categoria")),
                    () -> assertTrue(columns.contains("meta_id"))
            );
        }

        MetaDAO metaDAO = new MetaDAO();
        TransacaoDAO transacaoDAO = new TransacaoDAO();
        TransacaoService transacaoService = new TransacaoService(transacaoDAO, metaDAO);

        Meta meta = new Meta("Reserva", 500_00L);
        metaDAO.inserir(meta);
        transacaoService.registrar(
                TipoTransacao.Entrada,
                Categoria.Salario,
                1200_00L,
                null,
                "primeiro registro",
                LocalDate.of(2026, 3, 29)
        );

        assertAll(
                () -> assertEquals(1, metaDAO.listarTodas().size()),
                () -> assertEquals(1, transacaoDAO.listarTodas().size()),
                () -> assertEquals("primeiro registro", transacaoDAO.listarTodas().getFirst().comentario()),
                () -> assertEquals(1200_00L, transacaoDAO.calcularSaldo())
        );
    }

    @Test
    @DisplayName("banco legado é migrado sem perder dados")
    void bancoLegadoEhMigradoSemPerderDados() throws Exception {
        Path dbFile = tempDir.resolve("legacy.db");
        UUID transacaoId = UUID.randomUUID();

        try (Connection conn = DriverManager.getConnection(jdbcUrl(dbFile));
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("""
                    CREATE TABLE meta (
                        id TEXT PRIMARY KEY,
                        nome TEXT NOT NULL,
                        alvo_centavos INTEGER,
                        atual_centavos INTEGER NOT NULL DEFAULT 0
                    )
                    """);
            stmt.execute("""
                    CREATE TABLE transacao (
                        id TEXT PRIMARY KEY,
                        descricao TEXT NOT NULL,
                        valor_centavos INTEGER NOT NULL,
                        tipo TEXT NOT NULL,
                        data TEXT NOT NULL,
                        meta_id TEXT
                    )
                    """);
            stmt.execute("""
                    INSERT INTO transacao (id, descricao, valor_centavos, tipo, data, meta_id)
                    VALUES ('%s', 'Registro legado', 1999, 'Entrada', '2026-03-01', NULL)
                    """.formatted(transacaoId));
        }

        Database.overrideUrlForTests(jdbcUrl(dbFile));
        Database.inicializar();

        try (Connection conn = DriverManager.getConnection(jdbcUrl(dbFile))) {
            Set<String> columns = columnsOf(conn, "transacao");
            assertAll(
                    () -> assertTrue(columns.contains("comentario")),
                    () -> assertTrue(columns.contains("categoria"))
            );
        }

        TransacaoDAO transacaoDAO = new TransacaoDAO();
        Transacao transacao = transacaoDAO.buscarPorId(transacaoId);

        assertAll(
                () -> assertNotNull(transacao),
                () -> assertEquals("Registro legado", transacao.descricao()),
                () -> assertEquals("", transacao.comentario()),
                () -> assertNull(transacao.categoria()),
                () -> assertEquals(1999L, transacao.valorCentavos())
        );
    }

    private static Set<String> columnsOf(Connection conn, String tableName) throws Exception {
        Set<String> columns = new HashSet<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(%s)".formatted(tableName))) {
            while (rs.next()) {
                columns.add(rs.getString("name"));
            }
        }
        return columns;
    }

    private static String jdbcUrl(Path path) {
        return "jdbc:sqlite:" + path.toAbsolutePath();
    }
}
