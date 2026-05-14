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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
            Map<String, ColumnInfo> columns = columnsOf(conn, "transacao");
            Set<String> transacaoIndexes = indexNamesOf(conn, "transacao");
            List<ForeignKeyInfo> foreignKeys = foreignKeysOf(conn, "transacao");
            assertAll(
                    () -> assertTrue(columns.containsKey("comentario")),
                    () -> assertEquals("TEXT", columns.get("comentario").type()),
                    () -> assertTrue(columns.get("comentario").notNull()),
                    () -> assertTrue(columns.containsKey("categoria")),
                    () -> assertEquals("TEXT", columns.get("categoria").type()),
                    () -> assertTrue(columns.containsKey("meta_id")),
                    () -> assertEquals("TEXT", columns.get("meta_id").type()),
                    () -> assertTrue(foreignKeys.stream().anyMatch(fk ->
                            fk.fromColumn().equals("meta_id")
                                    && fk.toTable().equals("meta")
                                    && fk.toColumn().equals("id")
                                    && fk.onDelete().equalsIgnoreCase("SET NULL"))),
                    () -> assertTrue(transacaoIndexes.contains("idx_transacao_data_desc")),
                    () -> assertTrue(transacaoIndexes.contains("idx_transacao_tipo_data")),
                    () -> assertTrue(transacaoIndexes.contains("idx_transacao_categoria_data")),
                    () -> assertTrue(transacaoIndexes.contains("idx_transacao_meta_id")),
                    () -> assertTrue(objectExists(conn, "transacao_fts"))
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
    @DisplayName("bootstrap é idempotente e preserva dados existentes")
    void bootstrapEhIdempotenteEPreservaDadosExistentes() throws Exception {
        Path dbFile = tempDir.resolve("idempotent.db");
        Database.overrideUrlForTests(jdbcUrl(dbFile));

        Database.inicializar();

        MetaDAO metaDAO = new MetaDAO();
        TransacaoDAO transacaoDAO = new TransacaoDAO();
        Meta meta = new Meta("Reserva de emergência", 1500_00L);
        metaDAO.inserir(meta);
        UUID transacaoId = UUID.randomUUID();
        transacaoDAO.inserir(new Transacao(
                transacaoId,
                "Aporte inicial",
                "guardado para imprevistos",
                750_00L,
                TipoTransacao.Entrada,
                LocalDate.of(2026, 4, 1),
                meta.getId(),
                Categoria.Salario
        ));

        Database.inicializar();

        try (Connection conn = DriverManager.getConnection(jdbcUrl(dbFile))) {
            Map<String, ColumnInfo> columns = columnsOf(conn, "transacao");
            assertAll(
                    () -> assertTrue(columns.containsKey("comentario")),
                    () -> assertTrue(columns.containsKey("categoria"))
            );
        }

        Transacao transacao = transacaoDAO.buscarPorId(transacaoId);
        assertAll(
                () -> assertEquals(1, metaDAO.listarTodas().size()),
                () -> assertEquals(1, transacaoDAO.listarTodas().size()),
                () -> assertNotNull(transacao),
                () -> assertEquals("guardado para imprevistos", transacao.comentario()),
                () -> assertEquals(meta.getId(), transacao.metaId())
        );
    }

    @Test
    @DisplayName("banco legado é migrado sem perder dados")
    void bancoLegadoEhMigradoSemPerderDados() throws Exception {
        Path dbFile = tempDir.resolve("legacy.db");
        UUID transacaoId = UUID.randomUUID();

        criarSchemaLegado(dbFile, false);
        inserirTransacaoLegada(dbFile, transacaoId, null, 1999L, "Entrada", "2026-03-01");

        Database.overrideUrlForTests(jdbcUrl(dbFile));
        Database.inicializar();

        try (Connection conn = DriverManager.getConnection(jdbcUrl(dbFile))) {
            Map<String, ColumnInfo> columns = columnsOf(conn, "transacao");
            assertAll(
                    () -> assertTrue(columns.containsKey("comentario")),
                    () -> assertEquals("TEXT", columns.get("comentario").type()),
                    () -> assertTrue(columns.containsKey("categoria"))
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

    @Test
    @DisplayName("banco com comentário legado adiciona categoria sem perder comentário")
    void bancoComComentarioLegadoAdicionaCategoriaSemPerderComentario() throws Exception {
        Path dbFile = tempDir.resolve("legacy-comment-only.db");
        UUID transacaoId = UUID.randomUUID();

        criarSchemaLegado(dbFile, true);
        inserirTransacaoLegada(dbFile, transacaoId, "comentário preservado", 2550L, "Saida", "2026-03-02");

        Database.overrideUrlForTests(jdbcUrl(dbFile));
        Database.inicializar();

        try (Connection conn = DriverManager.getConnection(jdbcUrl(dbFile))) {
            Map<String, ColumnInfo> columns = columnsOf(conn, "transacao");
            assertAll(
                    () -> assertTrue(columns.containsKey("comentario")),
                    () -> assertTrue(columns.containsKey("categoria"))
            );
        }

        TransacaoDAO transacaoDAO = new TransacaoDAO();
        Transacao transacao = transacaoDAO.buscarPorId(transacaoId);

        assertAll(
                () -> assertNotNull(transacao),
                () -> assertEquals("Registro legado", transacao.descricao()),
                () -> assertEquals("comentário preservado", transacao.comentario()),
                () -> assertNull(transacao.categoria()),
                () -> assertEquals(2550L, transacao.valorCentavos())
        );
    }

    @Test
    @DisplayName("schema completo aplica foreign key real em meta_id")
    void schemaCompletoAplicaForeignKeyRealEmMetaId() throws Exception {
        Path dbFile = tempDir.resolve("foreign-key.db");
        Database.overrideUrlForTests(jdbcUrl(dbFile));
        Database.inicializar();

        assertThrows(SQLException.class, () -> {
            try (Connection conn = Database.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("""
                        INSERT INTO transacao (
                            id, descricao, comentario, valor_centavos, tipo, data, meta_id, categoria
                        ) VALUES (
                            '11111111-1111-1111-1111-111111111111',
                            'Meta inexistente',
                            '',
                            1000,
                            'Saida',
                            '2026-04-01',
                            '22222222-2222-2222-2222-222222222222',
                            'AdicionarMeta'
                        )
                        """);
            }
        });
    }

    @Test
    @DisplayName("conexão aplica PRAGMAs de integridade e privacidade")
    void conexaoAplicaPragmasDeIntegridadeEPrivacidade() throws Exception {
        Path dbFile = tempDir.resolve("pragma-security.db");
        Database.overrideUrlForTests(jdbcUrl(dbFile));
        Database.inicializar();

        try (Connection conn = Database.getConnection()) {
            assertAll(
                    () -> assertEquals(1, pragmaInt(conn, "foreign_keys")),
                    () -> assertEquals(5000, pragmaInt(conn, "busy_timeout")),
                    () -> assertEquals(1, pragmaInt(conn, "secure_delete"))
            );
        }
    }

    private static void criarSchemaLegado(Path dbFile, boolean incluirComentario) throws Exception {
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

            if (incluirComentario) {
                stmt.execute("""
                        CREATE TABLE transacao (
                            id TEXT PRIMARY KEY,
                            descricao TEXT NOT NULL,
                            comentario TEXT NOT NULL DEFAULT '',
                            valor_centavos INTEGER NOT NULL,
                            tipo TEXT NOT NULL,
                            data TEXT NOT NULL,
                            meta_id TEXT
                        )
                        """);
            } else {
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
            }
        }
    }

    private static void inserirTransacaoLegada(
            Path dbFile,
            UUID transacaoId,
            String comentario,
            long valorCentavos,
            String tipo,
            String data
    ) throws Exception {
        try (Connection conn = DriverManager.getConnection(jdbcUrl(dbFile));
             Statement stmt = conn.createStatement()) {
            if (comentario == null) {
                stmt.execute("""
                        INSERT INTO transacao (id, descricao, valor_centavos, tipo, data, meta_id)
                        VALUES ('%s', 'Registro legado', %d, '%s', '%s', NULL)
                        """.formatted(transacaoId, valorCentavos, tipo, data));
            } else {
                stmt.execute("""
                        INSERT INTO transacao (id, descricao, comentario, valor_centavos, tipo, data, meta_id)
                        VALUES ('%s', 'Registro legado', '%s', %d, '%s', '%s', NULL)
                        """.formatted(transacaoId, comentario, valorCentavos, tipo, data));
            }
        }
    }

    private static Map<String, ColumnInfo> columnsOf(Connection conn, String tableName) throws Exception {
        Map<String, ColumnInfo> columns = new HashMap<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(%s)".formatted(tableName))) {
            while (rs.next()) {
                ColumnInfo columnInfo = new ColumnInfo(
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getInt("notnull") == 1,
                        rs.getString("dflt_value")
                );
                columns.put(columnInfo.name(), columnInfo);
            }
        }
        return columns;
    }

    private static List<ForeignKeyInfo> foreignKeysOf(Connection conn, String tableName) throws Exception {
        List<ForeignKeyInfo> foreignKeys = new java.util.ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA foreign_key_list(%s)".formatted(tableName))) {
            while (rs.next()) {
                foreignKeys.add(new ForeignKeyInfo(
                        rs.getString("table"),
                        rs.getString("from"),
                        rs.getString("to"),
                        rs.getString("on_delete")
                ));
            }
        }
        return foreignKeys;
    }

    private static Set<String> indexNamesOf(Connection conn, String tableName) throws Exception {
        Set<String> indexes = new HashSet<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA index_list(%s)".formatted(tableName))) {
            while (rs.next()) {
                indexes.add(rs.getString("name"));
            }
        }
        return indexes;
    }

    private static boolean objectExists(Connection conn, String name) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT 1
                FROM sqlite_master
                WHERE name = ?
                LIMIT 1
                """)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static int pragmaInt(Connection conn, String pragmaName) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA " + pragmaName)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static String jdbcUrl(Path path) {
        return "jdbc:sqlite:" + path.toAbsolutePath();
    }

    private record ColumnInfo(String name, String type, boolean notNull, String defaultValue) {
    }

    private record ForeignKeyInfo(String toTable, String fromColumn, String toColumn, String onDelete) {
    }
}
