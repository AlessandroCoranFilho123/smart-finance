package app.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {

    private static final String URL = "jdbc:sqlite:financas.db";

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