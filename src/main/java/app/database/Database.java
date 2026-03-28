package app.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    // Em produção (instalado): %APPDATA%\SmartFinance\financas.db
    // Em desenvolvimento (IDE): ./financas.db (fallback)
    private static final String URL;

    static {
        String appData = System.getenv("APPDATA");
        if (appData != null) {
            java.io.File dir = new java.io.File(appData, "SmartFinance");
            dir.mkdirs();
            URL = "jdbc:sqlite:" + new java.io.File(dir, "financas.db").getAbsolutePath();
        } else {
            URL = "jdbc:sqlite:financas.db";
        }
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
        Connection conn = DriverManager.getConnection(URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }
}
