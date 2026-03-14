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
            dir.mkdirs(); // cria a pasta se não existir
            URL = "jdbc:sqlite:" + new java.io.File(dir, "financas.db").getAbsolutePath();
        } else {
            // Fallback para desenvolvimento sem APPDATA (Linux/Mac)
            URL = "jdbc:sqlite:financas.db";
        }
    }

    private Database() {
    }

    public static void inicializar() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS meta (
                        id             TEXT PRIMARY KEY,
                        nome           TEXT    NOT NULL,
                        alvo_centavos  INTEGER,
                        atual_centavos INTEGER NOT NULL DEFAULT 0
                    )
                    """);

            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS transacao (
                        id             TEXT PRIMARY KEY,
                        descricao      TEXT    NOT NULL,
                        valor_centavos INTEGER NOT NULL,
                        tipo           TEXT    NOT NULL,
                        data           TEXT    NOT NULL,
                        meta_id        TEXT,
                        categoria      TEXT,
                        FOREIGN KEY (meta_id) REFERENCES meta (id) ON DELETE SET NULL
                    )
                    """);

            try {
                stmt.execute("ALTER TABLE transacao ADD COLUMN categoria TEXT");
            } catch (SQLException ignored) {
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inicializar banco de dados", e);
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