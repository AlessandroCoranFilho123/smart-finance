package app.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    // Em produção (instalado): %APPDATA%\SmartFinance\financas.db
    // Em desenvolvimento (IDE): ./financas.db (fallback)
    private static final String DEFAULT_URL;
    private static volatile String testOverrideUrl;

    static {
        DEFAULT_URL = resolveDefaultUrl();
    }

    private Database() {
    }

    public static void inicializar() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Tabela meta
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS meta (
                        id             TEXT PRIMARY KEY,
                        nome           TEXT    NOT NULL,
                        alvo_centavos  INTEGER,
                        atual_centavos INTEGER NOT NULL DEFAULT 0
                    )
                    """);

            // Tabela transacao — schema completo com comentario e categoria
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS transacao (
                        id             TEXT PRIMARY KEY,
                        descricao      TEXT    NOT NULL,
                        comentario     TEXT    NOT NULL DEFAULT '',
                        valor_centavos INTEGER NOT NULL,
                        tipo           TEXT    NOT NULL,
                        data           TEXT    NOT NULL,
                        meta_id        TEXT,
                        categoria      TEXT,
                        FOREIGN KEY (meta_id) REFERENCES meta (id) ON DELETE SET NULL
                    )
                    """);

            // Migrations para bancos existentes que não possuem as colunas novas
            executarMigrationSegura(stmt, "ALTER TABLE transacao ADD COLUMN comentario TEXT NOT NULL DEFAULT ''");
            executarMigrationSegura(stmt, "ALTER TABLE transacao ADD COLUMN categoria TEXT");

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inicializar banco de dados", e);
        }
    }

    private static String resolveDefaultUrl() {
        String explicitUrl = System.getProperty("smartfinance.db.url");
        if (explicitUrl != null && !explicitUrl.isBlank()) {
            return explicitUrl;
        }

        String explicitPath = System.getProperty("smartfinance.db.path");
        if (explicitPath != null && !explicitPath.isBlank()) {
            java.io.File file = new java.io.File(explicitPath).getAbsoluteFile();
            java.io.File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            return "jdbc:sqlite:" + file.getAbsolutePath();
        }

        String appDataOverride = System.getProperty("smartfinance.appdata.dir");
        if (appDataOverride != null && !appDataOverride.isBlank()) {
            java.io.File dir = new java.io.File(appDataOverride, "SmartFinance");
            dir.mkdirs();
            return "jdbc:sqlite:" + new java.io.File(dir, "financas.db").getAbsolutePath();
        }

        String appData = System.getenv("APPDATA");
        if (appData != null) {
            java.io.File dir = new java.io.File(appData, "SmartFinance");
            dir.mkdirs();
            return "jdbc:sqlite:" + new java.io.File(dir, "financas.db").getAbsolutePath();
        }

        return "jdbc:sqlite:financas.db";
    }

    private static String getEffectiveUrl() {
        return testOverrideUrl != null ? testOverrideUrl : DEFAULT_URL;
    }

    /**
     * Hook de testes para isolar o banco sem alterar o comportamento de produção.
     */
    public static void overrideUrlForTests(String jdbcUrl) {
        testOverrideUrl = jdbcUrl;
    }

    public static void clearOverrideUrlForTests() {
        testOverrideUrl = null;
    }

    /**
     * Executa um ALTER TABLE ignorando o erro caso a coluna já exista.
     * Usado exclusivamente para migrations de schema incremental.
     */
    private static void executarMigrationSegura(Statement stmt, String sql) {
        try {
            stmt.execute(sql);
        } catch (SQLException ignored) {
            // Coluna já existe — comportamento esperado em bancos existentes
        }
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(getEffectiveUrl());
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }
}
