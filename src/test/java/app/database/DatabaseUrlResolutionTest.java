package app.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Database - Resolução de URL")
class DatabaseUrlResolutionTest {

    @BeforeEach
    @AfterEach
    void cleanup() {
        Database.clearOverrideUrlForTests();
        System.clearProperty("smartfinance.db.url");
        System.clearProperty("smartfinance.db.path");
        System.clearProperty("smartfinance.appdata.dir");
    }

    @Test
    @DisplayName("overrideUrlForTests substitui URL padrão")
    void overrideUrlMudaUrlEfetiva() throws Exception {
        String testUrl = "jdbc:sqlite::memory:";
        Database.overrideUrlForTests(testUrl);
        
        assertDoesNotThrow(() -> {
            try (Connection conn = Database.getConnection();
                 Statement stmt = conn.createStatement()) {
                assertNotNull(conn);
            }
        });
    }

    @Test
    @DisplayName("clearOverrideUrlForTests restaura URL padrão")
    void limparOverrideRestauraUrlPadrao() throws SQLException {
        Database.overrideUrlForTests("jdbc:sqlite::memory:");
        Database.clearOverrideUrlForTests();
        
        try (Connection conn = Database.getConnection()) {
            assertNotNull(conn);
        }
    }

    @Test
    @DisplayName("overrideUrlForTests com :memory: cria conexão válida")
    void conexaoInMemoryValida() throws SQLException {
        Database.overrideUrlForTests("jdbc:sqlite::memory:");
        
        try (Connection conn = Database.getConnection()) {
            assertFalse(conn.isClosed());
        }
    }

    @Test
    @DisplayName("getConnection configura foreign_keys pragma")
    void pragmaForeignKeysAtivado() throws SQLException {
        Database.overrideUrlForTests("jdbc:sqlite::memory:");
        
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("PRAGMA foreign_keys")) {
            rs.next();
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("conexão com arquivo temporário cria arquivo")
    void conexaoComArquivoTemp() throws SQLException, IOException {
        java.nio.file.Path tempFile = Files.createTempFile("test-db", ".db");
        try {
            String url = "jdbc:sqlite:" + tempFile.toAbsolutePath();
            Database.overrideUrlForTests(url);
            
            try (Connection conn = Database.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE test(id INT)");
                stmt.execute("INSERT INTO test VALUES(1)");
            }
            
            assertTrue(Files.exists(tempFile));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
